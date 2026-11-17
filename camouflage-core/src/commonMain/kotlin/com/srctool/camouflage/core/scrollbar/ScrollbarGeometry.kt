package com.srctool.camouflage.core.scrollbar

import kotlin.math.max
import kotlin.math.min

// The scrollbar's geometry as pure functions (no window): thumb size and place from the scroll fraction, and the
// scroll offset a drag or a track click asks for. All lengths are in pixels along the scrolling axis.

/** The thumb's length on a [track]: the viewport's share of the content, at least [min]; `0` when nothing scrolls. */
internal fun thumbLength(viewport: Float, content: Float, track: Float, min: Float): Float {
    if (content <= viewport || viewport <= 0f || track <= 0f) return 0f
    return max(min(min, track), viewport / content * track).coerceAtMost(track)
}

/** Where the thumb starts on the track for the scroll [offset]. */
internal fun thumbStart(offset: Float, viewport: Float, content: Float, track: Float, thumb: Float): Float {
    val scrollable = content - viewport
    if (scrollable <= 0f) return 0f
    return (offset / scrollable).coerceIn(0f, 1f) * (track - thumb)
}

/** How far the content scrolls for a thumb dragged by [thumbDelta]. */
internal fun scrollForThumbDrag(thumbDelta: Float, viewport: Float, content: Float, track: Float, thumb: Float): Float {
    val room = track - thumb
    if (room <= 0f) return 0f
    return thumbDelta * (content - viewport) / room
}

/** A track click at [position]: a viewport toward it (`-1` before the thumb, `1` after it, `0` on the thumb). */
internal fun pageDirection(position: Float, thumbStart: Float, thumbLength: Float): Int = when {
    position < thumbStart -> -1
    position > thumbStart + thumbLength -> 1
    else -> 0
}
