package com.srctool.camouflage.core.speedsearch

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import com.srctool.camouflage.core.util.RovingFocus

/**
 * What a lazy collection does for a speed search: report the match [count] for the overlay, and after the query
 * changes move focus to a match (a filtered list may have dropped the focused row). [hitRows] are the matching
 * rows' indexes in the list as shown.
 */
@Composable
internal fun SpeedSearchFocus(
    state: CamoSpeedSearchState,
    count: Int,
    hitRows: List<Int>,
    listState: LazyListState,
    focus: RovingFocus,
) {
    SideEffect { state.matchCount = count }
    LaunchedEffect(state.query, hitRows) {
        if (!state.active || hitRows.isEmpty()) return@LaunchedEffect
        val from = if (state.mode == SpeedSearchMode.Jump) focus.lastFocusedIndex ?: 0 else 0
        val target = hitRows.firstOrNull { it >= from } ?: hitRows.first()
        listState.scrollToItem(target)
        withFrameNanos { }
        focus.focusItem(target)
    }
}
