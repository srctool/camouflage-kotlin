package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarGeometryTest {
    private val plot = Rect(0f, 0f, 100f, 100f)
    private val zeroToTen = 0.0..10.0

    private fun bars(
        series: List<List<Double?>>,
        categories: Int = series.first().size,
        mode: BarMode = BarMode.Grouped,
        orientation: BarOrientation = BarOrientation.Vertical,
        domain: ClosedFloatingPointRange<Double> = zeroToTen,
        barGap: Float = 0f,
        rtl: Boolean = false,
    ) = barRects(plot, categories, series, mode, orientation, domain, barGap, 0f, rtl)

    @Test
    fun verticalBarsRiseFromTheBottomOneBandPerCategory() {
        val result = bars(listOf(listOf(10.0, 5.0)))
        assertEquals(Rect(0f, 0f, 50f, 100f), result[0].rect)
        assertEquals(Rect(50f, 50f, 100f, 100f), result[1].rect)
        assertEquals(BarEdge.Top, result[0].end)
    }

    @Test
    fun groupedBarsSitSideBySideWithTheGap() {
        val result = bars(listOf(listOf(10.0), listOf(5.0)), barGap = 4f)
        assertEquals(0f, result[0].rect.left)
        assertEquals(48f, result[0].rect.width)
        assertEquals(52f, result[1].rect.left)
        assertEquals(50f, result[1].rect.top)
    }

    @Test
    fun groupGapLeavesSpaceBetweenGroups() {
        val result = barRects(plot, 2, listOf(listOf(10.0, 10.0)), BarMode.Grouped, BarOrientation.Vertical,
            zeroToTen, 0f, 1f, false)
        assertEquals(12.5f, result[0].rect.left)
        assertEquals(25f, result[0].rect.width)
        assertEquals(62.5f, result[1].rect.left)
    }

    @Test
    fun stackedBarsPileUpAndNegativesGoDownSeparately() {
        val result = bars(listOf(listOf(4.0), listOf(-2.0), listOf(3.0)), mode = BarMode.Stacked, domain = -2.0..8.0)
        assertEquals(Rect(0f, 40f, 100f, 80f), result[0].rect)
        assertEquals(Rect(0f, 80f, 100f, 100f), result[1].rect)
        assertEquals(Rect(0f, 10f, 100f, 40f), result[2].rect)
        assertEquals(listOf(null, BarEdge.Bottom, BarEdge.Top), result.map { it.end })
    }

    @Test
    fun aNegativeBarHangsFromZero() {
        val result = bars(listOf(listOf(-5.0)), domain = -10.0..10.0)
        assertEquals(Rect(0f, 50f, 100f, 75f), result[0].rect)
        assertEquals(BarEdge.Bottom, result[0].end)
    }

    @Test
    fun aZeroValueIsAZeroHeightBarAtTheBaseline() {
        val result = bars(listOf(listOf(0.0, 10.0)))
        assertEquals(0f, result[0].rect.height)
        assertEquals(100f, result[0].rect.top)
    }

    @Test
    fun gapsHaveNoBar() {
        val result = bars(listOf(listOf(null, 10.0)))
        assertEquals(listOf(1), result.map { it.category })
    }

    @Test
    fun horizontalBarsRunRightOneRowPerCategory() {
        val result = bars(listOf(listOf(10.0, 5.0)), orientation = BarOrientation.Horizontal)
        assertEquals(Rect(0f, 0f, 100f, 50f), result[0].rect)
        assertEquals(Rect(0f, 50f, 50f, 100f), result[1].rect)
        assertEquals(BarEdge.Right, result[0].end)
    }

    @Test
    fun rightToLeftMirrorsVerticalAndHorizontalBars() {
        val vertical = bars(listOf(listOf(10.0, 5.0)), rtl = true)
        assertEquals(Rect(50f, 0f, 100f, 100f), vertical[0].rect)
        assertEquals(Rect(0f, 50f, 50f, 100f), vertical[1].rect)
        val horizontal = bars(listOf(listOf(10.0, 5.0)), orientation = BarOrientation.Horizontal, rtl = true)
        assertEquals(Rect(0f, 0f, 100f, 50f), horizontal[0].rect)
        assertEquals(Rect(50f, 50f, 100f, 100f), horizontal[1].rect)
        assertEquals(BarEdge.Left, horizontal[1].end)
    }

    @Test
    fun noCategoriesOrSeriesMeansNoBars() {
        assertTrue(barRects(plot, 0, listOf(listOf(1.0)), BarMode.Grouped, BarOrientation.Vertical, zeroToTen, 0f, 0f,
            false).isEmpty())
        assertTrue(barRects(plot, 2, emptyList(), BarMode.Grouped, BarOrientation.Vertical, zeroToTen, 0f, 0f,
            false).isEmpty())
    }

    @Test
    fun theBarNearestThePointIsTheOneUnderIt() {
        val result = bars(listOf(listOf(10.0, 5.0)))
        assertEquals(0, nearestBar(result, Offset(10f, 90f), true)!!.category)
        assertEquals(1, nearestBar(result, Offset(80f, 95f), true)!!.category)
        assertEquals(1, nearestBar(result, Offset(80f, 5f), true)!!.category)
        assertNull(nearestBar(emptyList(), Offset(1f, 1f), true))
        val rows = bars(listOf(listOf(10.0, 5.0)), orientation = BarOrientation.Horizontal)
        assertEquals(1, nearestBar(rows, Offset(95f, 80f), false)!!.category)
    }

    @Test
    fun aStackedDomainReachesTheSums() {
        val domain = barDomain(listOf(listOf(4.0), listOf(-2.0), listOf(3.0)), BarMode.Stacked, ChartAxis())
        assertEquals(-2.0, domain.low)
        assertEquals(8.0, domain.high)
        val grouped = barDomain(listOf(listOf(4.0), listOf(-2.0), listOf(3.0)), BarMode.Grouped, ChartAxis())
        assertEquals(-2.0, grouped.low)
        assertEquals(4.0, grouped.high)
        assertEquals(0.0, barDomain(listOf(listOf(4.0)), BarMode.Grouped, ChartAxis()).low)
    }
}
