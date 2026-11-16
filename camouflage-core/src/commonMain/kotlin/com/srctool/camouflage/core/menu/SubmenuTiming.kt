package com.srctool.camouflage.core.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import kotlinx.coroutines.delay

private const val SUBMENU_OPEN_MILLIS = 250L
private const val SUBMENU_CLOSE_MILLIS = 120L

internal enum class SubmenuKey { Open, Back }

// The arrow toward the child opens a submenu row; the arrow away and Esc close this submenu.
internal fun submenuKey(key: Key, toward: Key, away: Key, canOpen: Boolean, canBack: Boolean): SubmenuKey? = when {
    key == toward && canOpen -> SubmenuKey.Open
    (key == away || key == Key.Escape) && canBack -> SubmenuKey.Back
    else -> null
}

// The submenu's timing: hovering a submenu row opens it after a moment; hovering another row closes it after a
// short grace (so the pointer can cross a neighbour on its way to the child, and stays while it is over the
// child); closing it from the keyboard returns focus to its row.
@Composable
internal fun SubmenuTimers(
    rows: List<MenuRow>,
    requesters: List<FocusRequester>,
    hovered: Int,
    open: Int,
    childHovered: Boolean,
    openRow: (Int) -> Unit,
    closeChild: () -> Unit,
) {
    val keyboard = LocalInputModeManager.current
    var wasOpen by remember { mutableIntStateOf(-1) }
    LaunchedEffect(hovered) {
        val row = rows.getOrNull(hovered)
        if (row?.submenu != null && row.item.enabled && open != hovered) {
            delay(SUBMENU_OPEN_MILLIS)
            openRow(hovered)
        }
    }
    LaunchedEffect(open) {
        if (open == -1 && wasOpen != -1 && keyboard.inputMode == InputMode.Keyboard) requesters[wasOpen].requestFocus()
        wasOpen = open
    }
    LaunchedEffect(hovered, open, childHovered) {
        if (pointerLeftTheOpenRow(open, hovered, childHovered)) {
            delay(SUBMENU_CLOSE_MILLIS)
            closeChild()
        }
    }
}

// A submenu is open and the pointer is on another row of this level (not on its way into the child).
internal fun pointerLeftTheOpenRow(open: Int, hovered: Int, childHovered: Boolean): Boolean =
    open != -1 && hovered != -1 && hovered != open && !childHovered
