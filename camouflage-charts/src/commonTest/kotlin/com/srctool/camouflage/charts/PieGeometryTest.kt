package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PieGeometryTest {
    private val angles = sliceAngles(listOf(1.0, 1.0, 2.0), -90f)
    private val center = Offset(100f, 100f)

    @Test
    fun anglesShareAFullTurnByValue() {
        assertEquals(listOf(-90f to 90f, 0f to 90f, 90f to 180f), angles)
        assertEquals(360f, angles.sumOf { it.second.toDouble() }.toFloat())
    }

    @Test
    fun oneSliceGetsTheFullTurnAndNegativesCountAsZero() {
        assertEquals(listOf(-90f to 360f), sliceAngles(listOf(5.0), -90f))
        assertEquals(listOf(-90f to 360f, 270f to 0f), sliceAngles(listOf(3.0, -1.0), -90f))
        assertEquals(listOf(0f to 0f, 0f to 0f), sliceAngles(listOf(0.0, 0.0), 0f))
        assertTrue(sliceAngles(emptyList(), 0f).isEmpty())
    }

    @Test
    fun aPointFindsItsSliceByAngleAndRadius() {
        assertEquals(0, sliceAt(Offset(100f, 10f), center, 100f, 0f, angles))
        assertEquals(1, sliceAt(Offset(190f, 100f), center, 100f, 0f, angles))
        assertEquals(2, sliceAt(Offset(100f, 190f), center, 100f, 0f, angles))
        assertEquals(2, sliceAt(Offset(10f, 100f), center, 100f, 0f, angles))
        assertNull(sliceAt(Offset(300f, 100f), center, 100f, 0f, angles))
    }

    @Test
    fun aDonutHoleHasNoSlice() {
        assertNull(sliceAt(center, center, 100f, 50f, angles))
        assertEquals(1, sliceAt(Offset(170f, 100f), center, 100f, 50f, angles))
    }

    @Test
    fun aZeroSliceIsNeverHit() {
        val withZero = sliceAngles(listOf(1.0, 0.0, 1.0), -90f)
        assertEquals(2, sliceAt(Offset(10f, 100f), center, 100f, 0f, withZero))
    }

    @Test
    fun polarPointsClockwiseFromThreeOClock() {
        val down = polar(Offset(0f, 0f), 10f, 90f)
        assertTrue(abs(down.x) < 0.001f && abs(down.y - 10f) < 0.001f)
        assertEquals(Offset(10f, 0f), polar(Offset(0f, 0f), 10f, 0f))
    }

    @Test
    fun sharesReadAsPercentsAndSlivers() {
        assertEquals("45%", shareText(0.45))
        assertEquals("<1%", shareText(0.004))
        assertEquals("0%", shareText(0.0))
        assertEquals("100%", shareText(1.0))
    }

    @Test
    fun theSummaryNamesTheLargestSlice() {
        val slices = listOf(PieSlice("Search", 4500.0), PieSlice("Social", 3000.0), PieSlice("Mail", 1500.0))
        assertEquals("Pie chart, 3 slices, largest: Search 50%", pieSummary("Pie chart", slices))
        assertEquals("Pie chart, 1 slice, largest: A 100%", pieSummary("Pie chart", listOf(PieSlice("A", 2.0))))
        assertEquals("Pie chart, 0 slices", pieSummary("Pie chart", emptyList()))
    }
}
