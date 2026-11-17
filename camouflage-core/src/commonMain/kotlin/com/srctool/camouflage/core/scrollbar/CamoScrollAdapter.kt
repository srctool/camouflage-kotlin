package com.srctool.camouflage.core.scrollbar

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * How far and how much a scrolling container scrolls, in pixels, for a `CamoScrollbar`: the visible [viewport], the
 * whole [content] (an estimate for lazy content) and the current [offset]. [scrollBy] moves the content without
 * animation (a thumb drag or a track click). Read the three lengths in composition: they are state-backed.
 */
@Stable
public interface CamoScrollAdapter {
    /** The visible length. */
    public val viewport: Float

    /** The whole content's length. */
    public val content: Float

    /** How far the content has scrolled. */
    public val offset: Float

    /** Scrolls the content by [delta] pixels. */
    public fun scrollBy(delta: Float)
}

private class StateAdapter(private val state: ScrollState) : CamoScrollAdapter {
    override val viewport: Float get() = state.viewportSize.toFloat()
    override val content: Float get() = state.maxValue + state.viewportSize.toFloat()
    override val offset: Float get() = state.value.toFloat()
    override fun scrollBy(delta: Float) {
        state.dispatchRawDelta(delta)
    }
}

// A lazy list knows only the items it has laid out: the content is the average size seen times the item count.
private class ListAdapter(private val state: LazyListState) : CamoScrollAdapter {
    private val info get() = state.layoutInfo
    private val vertical get() = info.orientation == Orientation.Vertical
    private val average: Float
        get() {
            val visible = info.visibleItemsInfo
            return if (visible.isEmpty()) 0f else visible.sumOf { it.size }.toFloat() / visible.size
        }
    override val viewport: Float
        get() = (if (vertical) info.viewportSize.height else info.viewportSize.width).toFloat() -
            info.beforeContentPadding - info.afterContentPadding
    override val content: Float get() = average * info.totalItemsCount
    override val offset: Float get() = state.firstVisibleItemIndex * average + state.firstVisibleItemScrollOffset
    override fun scrollBy(delta: Float) {
        (state as ScrollableState).dispatchRawDelta(delta)
    }
}

/** The adapter for a `Modifier.verticalScroll` or `horizontalScroll` container's [state]. */
@Composable
public fun rememberCamoScrollAdapter(state: ScrollState): CamoScrollAdapter = remember(state) { StateAdapter(state) }

/** The adapter for a `LazyColumn` or `LazyRow`'s [state]; the content length is an estimate from the items so far. */
@Composable
public fun rememberCamoScrollAdapter(state: LazyListState): CamoScrollAdapter = remember(state) { ListAdapter(state) }
