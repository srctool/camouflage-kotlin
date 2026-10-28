package com.srctool.camouflage.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState

/**
 * A clickable row's toggle: a display-only switch, checkbox or radio inside the row reports its role and state
 * here, and the row announces itself as that control ("Wi-Fi, switch, on") instead of a button.
 */
@Stable
internal class RowToggle {
    var role: Role? by mutableStateOf(null)
    var state: ToggleableState? by mutableStateOf(null)
    var selected: Boolean? by mutableStateOf(null)
}

internal val LocalRowToggle = compositionLocalOf<RowToggle?> { null }

/** Reports a display-only control to the clickable row around it, if there is one. */
@Composable
internal fun ReportRowToggle(role: Role, state: ToggleableState? = null, selected: Boolean? = null) {
    val row = LocalRowToggle.current ?: return
    DisposableEffect(row, role, state, selected) {
        row.role = role
        row.state = state
        row.selected = selected
        onDispose {
            row.role = null
            row.state = null
            row.selected = null
        }
    }
}
