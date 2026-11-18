package com.srctool.camouflage.core.speedsearch

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

/** What speed search does with the items that don't match. */
public enum class SpeedSearchMode {
    /** Hide them (a tree keeps the ancestors of the matches). */
    Filter,

    /** Keep them all and only move focus to the next match. */
    Jump,
}

/**
 * The state of a speed search: the typed [query], with no field. Hand it to a component (`CamoTree(speedSearch =
 * state)`) or use it with your own lists: [match] says whether and where an item's text matches, for
 * [CamoHighlightedText]. Typing in a focused collection appends, Backspace removes, Esc clears.
 *
 * [mode] picks filtering or jumping; [kind] how the query matches (see [CamoMatcher]).
 */
@Stable
public class CamoSpeedSearchState(
    public val mode: SpeedSearchMode = SpeedSearchMode.Filter,
    public val kind: MatchKind = MatchKind.Auto,
) {
    /** What the user typed. Empty when no search is running. */
    public var query: String by mutableStateOf("")
        private set

    /** How many items match, as the collection last counted them (for the overlay and the announcement). */
    public var matchCount: Int by mutableIntStateOf(0)
        internal set

    /** Whether a search is running. */
    public val active: Boolean get() = query.isNotEmpty()

    /** Appends [text] to the query. */
    public fun type(text: String) {
        query += text
    }

    /** Removes the last typed character. */
    public fun backspace() {
        query = query.dropLast(1)
    }

    /** Ends the search. */
    public fun clear() {
        query = ""
    }

    /** The ranges of [text] that match the query, `null` when it doesn't, an empty list while no search runs. */
    public fun match(text: String): List<IntRange>? = CamoMatcher.match(query, text, kind)

    /** Whether [text] matches the query (always `true` while no search runs). */
    public fun matches(text: String): Boolean = match(text) != null

    // Handles a key for a collection: Esc and Backspace only while a search runs; printable keys always.
    internal fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        return when {
            event.key == Key.Escape && active -> true.also { clear() }
            event.key == Key.Backspace && active -> true.also { backspace() }
            else -> typedChar(event, allowSpace = active)?.let { true.also { _ -> type(it.toString()) } } ?: false
        }
    }
}

/** Typing in the focused collection feeds [state]; Esc clears it first and only then goes on. */
public fun Modifier.camoSpeedSearch(state: CamoSpeedSearchState?): Modifier =
    if (state == null) this else onPreviewKeyEvent { state.onKey(it) }

private const val DELETE = 0x7F

// The printable character [event] types, without a shortcut modifier. Space only counts when [allowSpace]: it is
// a select key before a search starts.
internal fun typedChar(event: KeyEvent, allowSpace: Boolean = false): Char? {
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return null
    val code = event.utf16CodePoint
    val floor = if (allowSpace) ' '.code - 1 else ' '.code
    return code.takeIf { it > floor && it != DELETE }?.toChar()
}
