package com.srctool.camouflage.core

import com.srctool.camouflage.core.scrollbar.pageDirection
import com.srctool.camouflage.core.scrollbar.scrollForThumbDrag
import com.srctool.camouflage.core.scrollbar.thumbLength
import com.srctool.camouflage.core.scrollbar.thumbStart
import kotlin.test.Test
import kotlin.test.assertEquals

// The scrollbar's geometry, with no window.
class ScrollbarGeometryTest {
    @Test
    fun theThumbIsTheViewportsShareOfTheTrack() {
        assertEquals(50f, thumbLength(viewport = 100f, content = 200f, track = 100f, min = 8f))
        assertEquals(25f, thumbLength(viewport = 100f, content = 400f, track = 100f, min = 8f))
    }

    @Test
    fun theThumbHasAMinimumLength() {
        assertEquals(32f, thumbLength(viewport = 100f, content = 100_000f, track = 100f, min = 32f))
    }

    @Test
    fun noThumbWhenTheContentFits() {
        assertEquals(0f, thumbLength(viewport = 100f, content = 100f, track = 100f, min = 32f))
        assertEquals(0f, thumbLength(viewport = 100f, content = 50f, track = 100f, min = 32f))
        assertEquals(0f, thumbLength(viewport = 0f, content = 200f, track = 100f, min = 32f))
    }

    @Test
    fun theThumbNeverLongerThanATrackShorterThanItsMinimum() {
        assertEquals(20f, thumbLength(viewport = 100f, content = 1000f, track = 20f, min = 32f))
    }

    @Test
    fun theThumbMovesAlongTheTrackWithTheOffset() {
        assertEquals(0f, thumbStart(0f, 100f, 200f, 100f, 50f))
        assertEquals(25f, thumbStart(50f, 100f, 200f, 100f, 50f))
        assertEquals(50f, thumbStart(100f, 100f, 200f, 100f, 50f))
        assertEquals(50f, thumbStart(500f, 100f, 200f, 100f, 50f)) // clamped
    }

    @Test
    fun aThumbDragScrollsTheContentInProportion() {
        // The thumb has 50 of room for 100 of scrolling: one pixel of thumb is two of content.
        assertEquals(20f, scrollForThumbDrag(10f, 100f, 200f, 100f, 50f))
        assertEquals(0f, scrollForThumbDrag(10f, 100f, 200f, 100f, 100f)) // no room
    }

    @Test
    fun aTrackClickPagesTowardTheClick() {
        assertEquals(-1, pageDirection(position = 5f, thumbStart = 20f, thumbLength = 30f))
        assertEquals(1, pageDirection(position = 80f, thumbStart = 20f, thumbLength = 30f))
        assertEquals(0, pageDirection(position = 30f, thumbStart = 20f, thumbLength = 30f))
    }
}
