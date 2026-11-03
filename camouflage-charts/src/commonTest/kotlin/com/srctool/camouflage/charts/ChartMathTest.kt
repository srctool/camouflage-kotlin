package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartMathTest {
    @Test
    fun ticksStepByNiceNumbers() {
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), niceTicks(3.0, 97.0, 5))
        assertEquals(listOf(0.0, 0.5, 1.0), niceTicks(0.1, 0.9, 3))
    }

    @Test
    fun ticksCoverTheRange() {
        val ticks = niceTicks(-13.0, 41.0)
        assertTrue(ticks.first() <= -13.0 && ticks.last() >= 41.0)
    }

    @Test
    fun aFlatRangeWidensAroundItsValue() {
        val ticks = niceTicks(7.0, 7.0)
        assertTrue(ticks.first() < 7.0 && ticks.last() > 7.0)
        val zero = niceTicks(0.0, 0.0)
        assertTrue(zero.first() < 0.0 && zero.last() > 0.0)
    }

    @Test
    fun ticksHaveNoFloatingPointDrift() {
        assertEquals(listOf(0.0, 0.1, 0.2, 0.3, 0.4, 0.5), niceTicks(0.0, 0.5, 6).map { (it * 10).toLong() / 10.0 })
    }

    @Test
    fun numbersReadWithoutTrailingZeros() {
        assertEquals("3", formatChartNumber(3.0))
        assertEquals("3.5", formatChartNumber(3.46))
        assertEquals("-2.5", formatChartNumber(-2.5))
        assertEquals("0", formatChartNumber(-0.01))
        assertEquals("1234567", formatChartNumber(1234567.0))
    }

    @Test
    fun aScaleMapsAndInverts() {
        val y = LinearScale(0.0, 100.0, 200f, 0f)
        assertEquals(200f, y.map(0.0))
        assertEquals(0f, y.map(100.0))
        assertEquals(50.0, y.invert(100f))
    }

    @Test
    fun theSummaryNamesTheTrend() {
        assertEquals("Trend from 3 to 9, up 200%", sparklineSummary(listOf(3.0, 5.0, 9.0)))
        assertEquals("Signups: from 10 to 5, down 50%", sparklineSummary(listOf(10.0, null, 5.0), "Signups"))
        assertEquals("Trend from 4 to 4, flat", sparklineSummary(listOf(4.0, 4.0)))
        assertEquals("Trend from 0 to 6, up by 6", sparklineSummary(listOf(0.0, 6.0)))
        assertEquals("Trend at 7", sparklineSummary(listOf(null, 7.0)))
        assertEquals("Load: no data", sparklineSummary(listOf(null, null), "Load"))
    }

    @Test
    fun downsamplingKeepsThePeaks() {
        val values = List(1000) { if (it == 500) 99.0 else 1.0 }
        val reduced = downsample(values, 50)
        assertTrue(reduced.size <= 100)
        assertTrue(reduced.contains(99.0))
        assertEquals(values.take(10), downsample(values.take(10), 50))
    }

    @Test
    fun pointsBreakAtGaps() {
        val points = sparklinePoints(listOf(0.0, null, 2.0, 4.0), 0.0..4.0, Size(30f, 10f), 0f)
        assertEquals(listOf(1, 2), segments(points).map { it.size })
        assertEquals(Offset(0f, 10f), points[0])
        assertEquals(Offset(30f, 0f), points[3])
    }

    @Test
    fun aFlatSeriesSitsInTheMiddle() {
        val points = sparklinePoints(listOf(5.0, 5.0), 5.0..5.0, Size(10f, 10f), 0f)
        assertEquals(5f, points[0]!!.y)
    }

    @Test
    fun aSinglePointIsCentered() {
        assertEquals(5f, sparklinePoints(listOf(1.0), 0.0..2.0, Size(10f, 10f), 0f)[0]!!.x)
    }

    @Test
    fun extremesAreTheTopAndBottomPoints() {
        val found = extremes(listOf(Offset(0f, 5f), Offset(1f, 1f), null, Offset(2f, 9f)))
        assertEquals(listOf(Offset(1f, 1f), Offset(2f, 9f)), found)
    }
}
