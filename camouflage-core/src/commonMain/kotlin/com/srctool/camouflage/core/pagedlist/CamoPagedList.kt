package com.srctool.camouflage.core.pagedlist

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.DebugChecks
import com.srctool.camouflage.core.list.ListDividers
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skeleton.CamoSkeletonListItem
import com.srctool.camouflage.core.skeleton.SkeletonPhaseScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private const val PULL_RESISTANCE = 0.5f

/**
 * A list screen that loads from an API: skeleton rows while a page loads, the next page when the user nears the
 * end, pull to refresh on touch devices, an empty state, and a retry when a page fails. It's controlled: it
 * renders [state] and never fetches. [onLoadMore] fires once per state when the last visible row is within
 * `prefetchDistance` of the end, and never while loading, after `endReached` or with a next-page error.
 * [onRefresh] enables the pull gesture (there's no gesture with a mouse: offer a refresh action too).
 * [itemKey] must be unique and stable.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // the rows-by-state table, the trigger and the pull, in one place
@Composable
public fun <T> CamoPagedList(
    state: CamoPagedListState<T>,
    itemKey: (T) -> Any,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    item: @Composable LazyItemScope.(item: T, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onRefresh: (() -> Unit)? = null,
    skeleton: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    firstPageError: (@Composable (message: String) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    separator: (@Composable (index: Int) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val skin = requireSkin("CamoPagedList")
    val theme = requireTheme("CamoPagedList")
    val style = remember(skin, theme) { skin.pagedList.defaults(theme).merge(theme.components.pagedList) }
    val list = remember(skin, theme) { skin.listItem.defaults(theme).merge(theme.components.listItem) }
    val line = remember(skin, theme) { skin.divider.defaults(theme).merge(theme.components.divider) }
    if (DebugChecks.enabled) remember(state.items) { checkUniqueKeys(state.items, itemKey) }

    // Pull to refresh: started by the gesture, finished by the first idle state after it.
    var refreshing by remember { mutableStateOf(false) }
    var triggeredBy by remember { mutableStateOf<CamoPagedListState<T>?>(null) }
    LaunchedEffect(state) {
        if (refreshing && state !== triggeredBy && state.loading == PagedLoad.None) {
            refreshing = false
            listState.animateScrollToItem(0)
        }
    }
    val threshold = with(LocalDensity.current) { style.refreshThreshold.toPx() }
    val latestRefresh by rememberUpdatedState(onRefresh)
    val pull = remember { PullToRefresh() }
    pull.threshold = threshold
    pull.enabled = onRefresh != null && !refreshing
    pull.onRelease = {
        refreshing = true
        triggeredBy = state
        latestRefresh?.invoke()
    }

    // Load more: once per state that's near the end and can load.
    val latest by rememberUpdatedState(state)
    val prefetch = style.prefetchDistance
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val s = latest
            val canLoad = s.loading == PagedLoad.None && !s.endReached && s.error?.scope != PagedErrorScope.NextPage
            if (canLoad && info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - prefetch) s else null
        }.distinctUntilChanged().filterNotNull().collect { onLoadMore() }
    }

    val loading = state.loading
    val firstPageFailed = state.error?.scope == PagedErrorScope.FirstPage && loading == PagedLoad.None
    val fullSkeleton = loading == PagedLoad.Initial || (loading == PagedLoad.Refresh && !style.refreshKeepsItems)
    val showItems = !fullSkeleton && !firstPageFailed
    val isEmpty = state.items.isEmpty() && loading == PagedLoad.None && state.error == null
    val firstPageMessage = state.error?.message.orEmpty()
    val retryFocus = remember { FocusRequester() }
    val skeletonRow: @Composable () -> Unit = skeleton ?: { CamoSkeletonListItem() }
    val announce = when (loading) {
        PagedLoad.Initial, PagedLoad.Refresh -> style.strings.loading
        PagedLoad.More -> style.strings.loadingMore
        PagedLoad.None -> null
    }
    val divider: @Composable (Int) -> Unit = separator ?: { _ ->
        if (list.dividers != ListDividers.None) {
            val inset = if (list.dividers == ListDividers.Inset) list.dividerInsetStart else 0.dp
            Box(Modifier.fillMaxWidth().padding(start = inset).height(line.thickness).background(line.color))
        }
    }

    Box(modifier.nestedScroll(pull).pointerInput(pull) { pull.trackPointerType(this) }) {
        SkeletonPhaseScope(active = fullSkeleton || loading == PagedLoad.More) {
            LazyColumn(state = listState, contentPadding = contentPadding) {
                if (header != null) item(key = "camo-header") { header() }
                when {
                    fullSkeleton -> skeletonRows(style.initialSkeletonCount, skeletonRow)
                    firstPageFailed -> item(key = "camo-error") {
                        val fill = Modifier.fillParentMaxSize()
                        if (firstPageError != null) {
                            Box(fill) { firstPageError(firstPageMessage) }
                        } else {
                            skin.pagedList.Error(firstPageMessage, onRetry, retryFocus, theme, style, fill)
                            LaunchedEffect(Unit) { retryFocus.requestFocus() }
                        }
                    }
                    isEmpty -> item(key = "camo-empty") {
                        val fill = Modifier.fillParentMaxSize()
                        if (empty != null) Box(fill) { empty() } else skin.pagedList.Empty(theme, style, fill)
                    }
                }
                if (showItems) {
                    itemsIndexed(state.items, key = { _, v -> itemKey(v) }) { index, value ->
                        val fade = if (style.itemFadeIn) {
                            Modifier.animateItem(
                                fadeInSpec = tween(theme.motion.durationMedium),
                                placementSpec = null,
                                fadeOutSpec = null,
                            )
                        } else {
                            Modifier
                        }
                        Column(fade) {
                            if (index > 0) divider(index - 1)
                            item(value, index)
                        }
                    }
                    if (loading == PagedLoad.More) skeletonRows(style.moreSkeletonCount, skeletonRow)
                    val next = state.error
                    if (next?.scope == PagedErrorScope.NextPage && loading == PagedLoad.None) {
                        item(key = "camo-retry") {
                            skin.pagedList.RetryRow(next.message, onRetry, theme, style, Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        skin.pagedList.RefreshIndicator(
            PagedListState(loading, state.endReached, state.error?.scope, isEmpty, refreshing) {
                if (threshold > 0f) pull.offset / threshold else 0f
            },
            theme,
            style,
            Modifier.align(Alignment.TopCenter),
        )
        // One polite announcement per load; the skeleton rows themselves are hidden.
        Spacer(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (announce != null) contentDescription = announce
            },
        )
    }
}

private fun LazyListScope.skeletonRows(count: Int, row: @Composable () -> Unit) {
    items(count, key = { "camo-skeleton-$it" }) { Box(Modifier.clearAndSetSemantics {}) { row() } }
}

private fun <T> checkUniqueKeys(items: List<T>, itemKey: (T) -> Any) {
    val seen = HashSet<Any>(items.size)
    for (it in items) {
        val key = itemKey(it)
        check(seen.add(key)) { "CamoPagedList: two items share the key $key. itemKey must be unique." }
    }
}

/**
 * The pull gesture: overscroll at the top by touch moves [offset] (with resistance); releasing past [threshold]
 * calls [onRelease]. Mouse and trackpad scrolling never pull.
 */
private class PullToRefresh : NestedScrollConnection {
    var offset by mutableFloatStateOf(0f)
    var threshold = 0f
    var enabled = false
    var onRelease: () -> Unit = {}
    private var touch = false

    suspend fun trackPointerType(scope: androidx.compose.ui.input.pointer.PointerInputScope) {
        scope.awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val type = event.changes.firstOrNull()?.type ?: continue
                touch = type == PointerType.Touch || type == PointerType.Stylus
            }
        }
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (offset <= 0f || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        val used = maxOf(available.y, -offset)
        offset += used
        return Offset(0f, used)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val pulling = enabled && touch && source == NestedScrollSource.UserInput
        if (!pulling || available.y <= 0f) return Offset.Zero
        offset += available.y * PULL_RESISTANCE
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (offset <= 0f) return Velocity.Zero
        if (enabled && offset >= threshold) onRelease()
        animate(offset, 0f) { v, _ -> offset = v }
        return available
    }
}
