package com.srctool.camouflage.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * One Tab stop for a group of items (radios, segments, tabs): Tab enters on the selected item, or the last
 * focused or first enabled one, and leaves with the next Tab. The arrow keys move between the enabled items,
 * wrapping around, and select the item they land on. Left and Right follow the reading direction.
 */
@Stable
public class RovingFocus internal constructor() {
    private data class Item(val requester: FocusRequester, val enabled: Boolean, val selected: Boolean)

    private val items = mutableStateMapOf<Int, Item>()
    private val selectors = mutableMapOf<Int, () -> Unit>()
    // The item that has focus, or is about to get it from an arrow key; null while focus is outside the group.
    private var current by mutableStateOf<Int?>(null)
    private var lastFocused: Int? = null

    // The one focusable item. Read by each item's focus properties whenever focus moves.
    internal val tabStop: Int?
        get() {
            val enabled = items.filterValues { it.enabled }.keys.sorted()
            return current?.takeIf { it in enabled }
                ?: enabled.firstOrNull { items.getValue(it).selected }
                ?: lastFocused?.takeIf { it in enabled }
                ?: enabled.firstOrNull()
        }

    internal fun register(index: Int, requester: FocusRequester, enabled: Boolean, selected: Boolean) {
        val item = Item(requester, enabled, selected)
        if (items[index] != item) items[index] = item
    }

    internal fun selector(index: Int, onSelect: () -> Unit) {
        selectors[index] = onSelect
    }

    internal fun unregister(index: Int, requester: FocusRequester) {
        if (items[index]?.requester == requester) {
            items.remove(index)
            selectors.remove(index)
        }
    }

    internal fun focusChanged(index: Int, focused: Boolean) {
        if (focused) {
            current = index
            lastFocused = index
        } else if (current == index) {
            current = null
        }
    }

    // Moves focus by `step` among the enabled items and selects the target. False when there's nowhere to go.
    internal fun move(from: Int, step: Int): Boolean {
        val enabled = items.filterValues { it.enabled }.keys.sorted()
        val at = enabled.indexOf(from)
        if (enabled.size < 2 || at < 0) return false
        val target = enabled[(at + step).mod(enabled.size)]
        current = target
        selectors[target]?.invoke()
        // The target is the Tab stop from now on, so it can take focus before the selection recomposes.
        items.getValue(target).requester.requestFocus()
        return true
    }
}

/** A [RovingFocus] for one group. Give each item [rovingFocusItem]. */
@Composable
public fun rememberRovingFocus(): RovingFocus = remember { RovingFocus() }

/**
 * Makes this node item [index] of [focus]'s group: focusable only as the group's Tab stop, and moving (and
 * selecting, through [onSelect]) with the arrow keys. Put it before the item's `selectable`.
 */
@Composable
public fun Modifier.rovingFocusItem(
    focus: RovingFocus,
    index: Int,
    enabled: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
): Modifier {
    val requester = remember { FocusRequester() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    SideEffect {
        focus.register(index, requester, enabled, selected)
        focus.selector(index, onSelect)
    }
    DisposableEffect(focus, index, requester) { onDispose { focus.unregister(index, requester) } }
    return this
        .focusRequester(requester)
        .focusProperties { canFocus = enabled && focus.tabStop == index }
        .onFocusChanged { focus.focusChanged(index, it.isFocused) }
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val step = when (event.key) {
                Key.DirectionDown -> 1
                Key.DirectionUp -> -1
                Key.DirectionRight -> if (rtl) -1 else 1
                Key.DirectionLeft -> if (rtl) 1 else -1
                else -> return@onPreviewKeyEvent false
            }
            focus.move(index, step)
        }
}
