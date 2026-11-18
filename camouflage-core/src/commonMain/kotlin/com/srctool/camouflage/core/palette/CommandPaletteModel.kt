package com.srctool.camouflage.core.palette

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.srctool.camouflage.core.speedsearch.CamoMatcher
import com.srctool.camouflage.core.speedsearch.MatchKind
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One result of a [CamoCommandSource]. [id] identifies it across sessions (the palette remembers the recent
 * ones by it). [shortcut] is the key hint shown at the row's end ("Ctrl+Shift+P"), [subtitle] a second line of
 * context (a file's folder) that is read by screen readers and shown when there is no shortcut. [onSelect] runs
 * after the palette has closed.
 */
@Immutable
public data class CamoCommandItem(
    val id: Any,
    val title: String,
    val onSelect: () -> Unit,
    val subtitle: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val shortcut: String? = null,
)

/**
 * A group of results: "Actions", "Files", "Symbols". [search] answers a query (empty for the recents view)
 * with at most [limit] items, best first; it runs off the main thread's concern, is cancelled by the next
 * keystroke, and may take its time: the other groups show at once and this one shows a loading row. The palette
 * highlights each title with [matchKind] (`null` for none, when the source ranks by something else).
 *
 * For an in-memory list use [CamoCommandSource.of].
 */
@Immutable
public class CamoCommandSource(
    public val title: String,
    public val matchKind: MatchKind? = MatchKind.Fuzzy,
    public val limit: Int = 8,
    public val search: suspend (query: String) -> List<CamoCommandItem>,
) {
    public companion object {
        /**
         * A source over [items]: scores every title against the query, on a background dispatcher for a long list (ten
         * thousand items are fine) and keeps the best [limit]. An empty query lists the first [limit] items.
         */
        public fun of(
            title: String,
            items: List<CamoCommandItem>,
            matchKind: MatchKind = MatchKind.Fuzzy,
            limit: Int = 8,
        ): CamoCommandSource = CamoCommandSource(title, matchKind, limit) { query ->
            // A long list is scored off the main thread; a short one inline (no dispatcher hop to wait for).
            if (items.size > OFF_THREAD_ITEMS) {
                withContext(Dispatchers.Default) { rank(items, query, matchKind, limit) }
            } else {
                rank(items, query, matchKind, limit)
            }
        }
    }
}

private const val OFF_THREAD_ITEMS = 1000

// The best [limit] items for [query], best score first and in list order among equals.
internal fun rank(items: List<CamoCommandItem>, query: String, kind: MatchKind, limit: Int): List<CamoCommandItem> {
    if (query.isEmpty()) return items.take(limit)
    val scored = ArrayList<Pair<Int, CamoCommandItem>>()
    for (item in items) {
        val match = CamoMatcher.score(query, item.title, kind) ?: continue
        scored += match.score to item
    }
    return scored.sortedByDescending { it.first }.take(limit).map { it.second }
}

/**
 * The command palette's state: whether it is [open], the typed [query] and the [recent] item ids, newest first.
 * The app owns it: open it from its own shortcut (Ctrl+K, double Shift), and persist [recent] (see [snapshot])
 * if recents should outlive the process.
 */
@Stable
public class CamoCommandPaletteState(recent: List<Any> = emptyList(), private val maxRecent: Int = 20) {
    /** Whether the palette is showing. */
    public var open: Boolean by mutableStateOf(false)

    /** What the user typed. Reset when the palette opens. */
    public var query: String by mutableStateOf("")

    private val recents = mutableStateListOf<Any>().apply { addAll(recent.take(maxRecent)) }

    /** The ids of the items run most recently, newest first. */
    public val recent: List<Any> get() = recents.toList()

    /** Shows the palette with an empty query. */
    public fun show() {
        query = ""
        open = true
    }

    /** Hides the palette. */
    public fun hide() {
        open = false
    }

    /** Shows the palette when hidden, hides it when showing. */
    public fun toggle() {
        if (open) hide() else show()
    }

    /** Records [id] as the most recent item. The palette does this when an item runs. */
    public fun remember(id: Any) {
        recents.remove(id)
        recents.add(0, id)
        while (recents.size > maxRecent) recents.removeAt(recents.size - 1)
    }

    /** The [recent] ids, for the app to save and give back to the constructor. */
    public fun snapshot(): List<Any> = recents.toList()
}

/** Overrides for the command palette. `null` keeps the default. */
@Immutable
public data class CommandPaletteTheme(
    val placeholder: String? = null,
    val noResults: String? = null,
    val loading: String? = null,
    val maxWidth: Dp? = null,
    val maxHeight: Dp? = null,
)
