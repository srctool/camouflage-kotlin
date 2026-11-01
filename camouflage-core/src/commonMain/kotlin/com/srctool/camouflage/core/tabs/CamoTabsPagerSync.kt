package com.srctool.camouflage.core.tabs

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * A pager and a tab bar kept in step: the pager is the truth. Pass [selected] and [onSelect] to `CamoTabs`
 * (a tap animates the pager to that page) and [position] as its `indicatorPosition` (the indicator follows a
 * swipe).
 */
@Stable
public class CamoTabsPagerSync<T> internal constructor(
    public val selected: T,
    public val onSelect: (T) -> Unit,
    public val position: () -> Float,
)

/**
 * Syncs [pagerState] with the tab bar for [tabs], one tab per page. The selected tab follows the pager's current
 * page, so it changes once a swipe passes the halfway point.
 */
@Composable
public fun <T> rememberCamoTabsPagerSync(pagerState: PagerState, tabs: List<CamoTab<T>>): CamoTabsPagerSync<T> {
    require(tabs.isNotEmpty()) { "rememberCamoTabsPagerSync needs at least one tab." }
    val scope = rememberCoroutineScope()
    val current = tabs[pagerState.currentPage.coerceIn(0, tabs.lastIndex)].value
    return CamoTabsPagerSync(
        selected = current,
        onSelect = { value ->
            val index = tabs.indexOfFirst { it.value == value }
            if (index >= 0) scope.launch { pagerState.animateScrollToPage(index) }
        },
        position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
    )
}
