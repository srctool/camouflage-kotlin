package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LineGeometryTest {
    @Test
    fun stackingAddsSeriesUpAndLeavesGapsAndNegativesOut() {
        val bands = stack(listOf(listOf(1.0, 2.0, null), listOf(3.0, -4.0, 5.0)))
        assertEquals(listOf(StackBand(0.0, 1.0), StackBand(0.0, 2.0), null), bands[0])
        assertEquals(listOf(StackBand(1.0, 4.0), StackBand(2.0, 2.0), StackBand(0.0, 5.0)), bands[1])
    }

    @Test
    fun theNearestIndexIsThePositionClosestToX() {
        val positions = listOf(0f, 10f, 20f)
        assertEquals(1, nearestIndex(positions, 14f))
        assertEquals(2, nearestIndex(positions, 16f))
        assertNull(nearestIndex(emptyList(), 5f))
    }

    @Test
    fun gapsSplitALineIntoRuns() {
        val p = Offset(0f, 0f)
        assertEquals(listOf(0..0, 2..3), gapRuns(listOf(p, null, p, p, null)))
        assertEquals(emptyList(), gapRuns(listOf(null, null)))
    }

    @Test
    fun aStepGoesAcrossThenUp() {
        val steps = stepPoints(listOf(Offset(0f, 5f), Offset(10f, 8f)))
        assertEquals(listOf(Offset(0f, 5f), Offset(10f, 5f), Offset(10f, 8f)), steps)
    }

    @Test
    fun monotoneSmoothingNeverOvershoots() {
        val points = listOf(Offset(0f, 10f), Offset(10f, 0f), Offset(20f, 0f), Offset(30f, 10f), Offset(40f, 9f))
        val pieces = curvePieces(points, LineCurve.Smooth)
        assertEquals(4, pieces.size)
        pieces.forEachIndexed { index, piece ->
            val low = minOf(points[index].y, points[index + 1].y)
            val high = maxOf(points[index].y, points[index + 1].y)
            assertTrue(piece.control1.y in low..high && piece.control2.y in low..high, "piece $index overshoots")
        }
    }

    @Test
    fun catmullRomPassesThroughTheDataButCanOvershoot() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(20f, 0f), Offset(30f, 10f))
        val pieces = curvePieces(points, LineCurve.CatmullRom)
        assertEquals(points.drop(1), pieces.map { it.end })
        assertTrue(pieces[1].control2.y < 0f)
        assertEquals(0f, curvePieces(points, LineCurve.Smooth)[1].control2.y)
    }

    @Test
    fun linearAndStepHaveNoCubicPieces() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 5f))
        assertTrue(curvePieces(points, LineCurve.Linear).isEmpty())
        assertTrue(curvePieces(points, LineCurve.Step).isEmpty())
        assertTrue(curvePieces(points.take(1), LineCurve.Smooth).isEmpty())
    }

    @Test
    fun categoriesSpreadEvenlyOrFollowTime() {
        val plot = Rect(0f, 0f, 100f, 10f)
        assertEquals(listOf(0f, 50f, 100f), categoryPositions(plot, 3, null))
        assertEquals(listOf(50f), categoryPositions(plot, 1, null))
        assertEquals(listOf(0f, 25f, 100f), categoryPositions(plot, 3, listOf(0L, 10L, 40L)))
        assertEquals(listOf(0f, 50f, 100f), categoryPositions(plot, 3, listOf(0L, 10L, 5L)))
        assertTrue(categoryPositions(plot, 0, null).isEmpty())
    }

    @Test
    fun theValueAxisIncludesZeroForAreasAndHonorsFixedBounds() {
        val values = listOf(listOf<Double?>(12.0, 18.0))
        assertTrue(lineDomain(LineKind.Line, values, null, ChartAxis()).low > 0.0)
        assertEquals(0.0, lineDomain(LineKind.Area, values, null, ChartAxis()).low)
        val fixed = lineDomain(LineKind.Line, values, null, ChartAxis(min = 0.0, max = 50.0))
        assertEquals(0.0..50.0, fixed.range)
        val stacked = lineDomain(LineKind.StackedArea, values + values, stack(values + values), ChartAxis())
        assertTrue(stacked.high >= 36.0)
    }

    @Test
    fun theModelPutsAreasOnTheBaselineOrTheStackBelow() {
        val plot = Rect(0f, 0f, 100f, 100f)
        val values = listOf(listOf<Double?>(10.0, 10.0), listOf(10.0, null))
        val bands = stack(values)
        val domain = lineDomain(LineKind.StackedArea, values, bands, ChartAxis(min = 0.0, max = 20.0))
        val colors = listOf(Color.Red, Color.Blue)
        val stacked = lineModel(LineKind.StackedArea, plot, values, bands, colors, domain, listOf(0f, 100f))
        assertEquals(Offset(0f, 50f), stacked.specs[0].upper[0])
        assertEquals(Offset(0f, 100f), stacked.specs[0].lower!![0])
        assertEquals(Offset(0f, 0f), stacked.specs[1].upper[0])
        assertEquals(Offset(0f, 50f), stacked.specs[1].lower!![0])
        assertNull(stacked.specs[1].upper[1])
        val line = lineModel(LineKind.Line, plot, values, null, colors, domain, listOf(0f, 100f))
        assertNull(line.specs[0].lower)
    }

    @Test
    fun keysWalkCategoriesAndSeriesWithinBounds() {
        assertEquals(1 to 0, walkTarget(Key.DirectionRight, 0 to 0, 3, 2))
        assertEquals(2 to 0, walkTarget(Key.DirectionRight, 2 to 0, 3, 2))
        assertEquals(0 to 0, walkTarget(Key.DirectionLeft, 0 to 0, 3, 2))
        assertEquals(1 to 1, walkTarget(Key.DirectionDown, 1 to 0, 3, 2))
        assertEquals(1 to 0, walkTarget(Key.DirectionUp, 1 to 1, 3, 2))
        assertEquals(2 to 1, walkTarget(Key.MoveEnd, 0 to 1, 3, 2))
        assertEquals(0 to 1, walkTarget(Key.MoveHome, 2 to 1, 3, 2))
        assertNull(walkTarget(Key.A, 0 to 0, 3, 2))
        assertNull(walkTarget(Key.DirectionRight, 0 to 0, 0, 0))
    }

    @Test
    fun theSummaryNamesEachSeriesTrend() {
        val summary = chartSummary("Line chart", listOf("Jan", "Feb"), listOf(ChartSeries("Sales", listOf(3.0, 9.0))))
        assertEquals("Line chart, 1 series, 2 categories. Sales: from 3 to 9, up 200%", summary)
        assertEquals("Bar chart, 0 series, 1 category", chartSummary("Bar chart", listOf("Q1"), emptyList()))
    }

    @Test
    fun noDataMeansNoCategoriesOrOnlyGaps() {
        assertTrue(noChartData(emptyList(), listOf(ChartSeries("a", listOf(1.0)))))
        assertTrue(noChartData(listOf("x"), listOf(ChartSeries("a", listOf(null)))))
        assertTrue(!noChartData(listOf("x"), listOf(ChartSeries("a", listOf(1.0)))))
    }
}
