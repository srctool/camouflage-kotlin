package com.srctool.camouflage.charts

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.minimal.minimalLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LineBarChartsTest {
    private val months = listOf("Jan", "Feb", "Mar")
    private val sales = ChartSeries("Sales", listOf(1.0, 2.5, 4.0))
    private val costs = ChartSeries("Costs", listOf(0.5, null, 3.0))

    private class Recorder : ChartRenderer {
        var lines: LineChartSpec? = null
        var bars: BarChartSpec? = null
        override fun drawSparkline(scope: DrawScope, spec: SparklineSpec, style: ResolvedChartStyle) = Unit
        override fun drawLineChart(scope: DrawScope, spec: LineChartSpec, style: ResolvedChartStyle) { lines = spec }
        override fun drawBarChart(scope: DrawScope, spec: BarChartSpec, style: ResolvedChartStyle) { bars = spec }
    }

    private fun ComposeUiTest.chart(recorder: Recorder? = null, content: @Composable () -> Unit) = setContent {
        CamouflageProvider(MinimalSkin, minimalLight.copy(motion = minimalLight.motion.reduced())) {
            if (recorder == null) content() else CamoChartsProvider(recorder, content)
        }
    }

    private val lineImage = hasContentDescription(
        "Revenue. Line chart, 2 series, 3 categories. Sales: from 1 to 4, up 300%; Costs: from 0.5 to 3, up 500%",
    )

    private fun stateIs(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun aLineChartIsOneImageWithEachSeriesTrend() = runComposeUiTest {
        chart { CamoLineChart(months, listOf(sales, costs), "Revenue", Modifier.width(400.dp)) }
        onNode(lineImage).assertExists()
    }

    @Test
    fun theLegendHidesASeriesFromTheChart() = runComposeUiTest {
        val recorder = Recorder()
        chart(recorder) { CamoLineChart(months, listOf(sales, costs), "Revenue", Modifier.width(400.dp)) }
        waitForIdle()
        assertEquals(2, recorder.lines!!.series.size)
        onNodeWithText("Costs").performClick()
        waitForIdle()
        assertEquals(1, recorder.lines!!.series.size)
    }

    @Test
    fun hoveringShowsTheNearestCategorysValues() = runComposeUiTest {
        chart { CamoLineChart(months, listOf(sales, costs), "Revenue", Modifier.width(400.dp)) }
        onNodeWithText("Sales: 2.5").assertDoesNotExist()
        onNode(lineImage).performMouseInput { moveTo(center) }
        onNodeWithText("Sales: 2.5").assertExists()
        onNodeWithText("Feb").assertExists()
    }

    @Test
    fun aStackedAreaTooltipAddsTheTotal() = runComposeUiTest {
        chart {
            CamoLineChart(months, listOf(sales, costs), "Revenue", Modifier.width(400.dp), kind = LineKind.StackedArea)
        }
        onNode(hasContentDescription("Revenue. ", substring = true)).performMouseInput { moveTo(center) }
        onNodeWithText("Total: 2.5").assertExists()
    }

    @Test
    fun theKeyboardWalksPointsAndAnnouncesThem() = runComposeUiTest {
        chart { CamoLineChart(months, listOf(sales, costs), "Revenue", Modifier.width(400.dp)) }
        val plot = onNode(isFocusable() and hasAnyAncestor(lineImage))
        plot.requestFocus()
        plot.assert(stateIs("Jan, Sales, 1"))
        plot.performKeyInput { pressKey(Key.DirectionRight) }
        plot.assert(stateIs("Feb, Sales, 2.5"))
        plot.performKeyInput { pressKey(Key.DirectionDown) }
        plot.assert(stateIs("Feb, Costs, no data"))
        plot.performKeyInput { pressKey(Key.MoveEnd) }
        plot.assert(stateIs("Mar, Costs, 3"))
    }

    @Test
    fun theTableShowsTheSameData() = runComposeUiTest {
        chart { CamoLineChart(months, listOf(sales, costs), "Revenue", Modifier.width(400.dp)) }
        onNodeWithText("View as table").performClick()
        onNodeWithText("Mar").assertExists()
        onNodeWithText("-").assertExists()
    }

    @Test
    fun noDataSaysSo() = runComposeUiTest {
        chart { CamoLineChart(emptyList(), emptyList(), "Revenue") }
        onNodeWithText("No data").assertExists()
    }

    @Test
    fun aBarChartIsOneImageAndHoverShowsTheBar() = runComposeUiTest {
        val regions = listOf("North", "South")
        val quarters = listOf("Q1", "Q2")
        val north = ChartSeries("North", listOf(42.0, 10.0))
        val south = ChartSeries("South", listOf(7.0, 30.0))
        chart { CamoBarChart(quarters, listOf(north, south), "Regions", Modifier.width(400.dp)) }
        val image = hasContentDescription(
            "Regions. Bar chart, 2 series, 2 categories. North: from 42 to 10, down 76%; South: from 7 to 30, up 329%",
        )
        onNode(image).assertExists()
        val plot = onNode(isFocusable() and hasAnyAncestor(image))
        plot.requestFocus()
        plot.assert(stateIs("Q1, North, 42"))
        plot.performKeyInput { pressKey(Key.DirectionDown) }
        plot.assert(stateIs("Q1, South, 7"))
        assertEquals(2, regions.size)
    }

    @Test
    fun aStackedBarTooltipAddsTheTotal() = runComposeUiTest {
        val a = ChartSeries("A", listOf(4.0))
        val b = ChartSeries("B", listOf(3.0))
        chart { CamoBarChart(listOf("Q1"), listOf(a, b), "Stack", Modifier.width(400.dp), mode = BarMode.Stacked) }
        onNode(hasContentDescription("Stack. ", substring = true)).performMouseInput { moveTo(center) }
        onNodeWithText("Total: 7").assertExists()
    }

    @Test
    fun barsMirrorInARightToLeftLayout() = runComposeUiTest {
        val ltr = Recorder()
        val rtl = Recorder()
        val data = listOf(ChartSeries("A", listOf(1.0, 2.0)))
        chart(ltr) { CamoBarChart(listOf("x", "y"), data, "Bars", Modifier.width(400.dp)) }
        waitForIdle()
        val first = ltr.bars!!.bars.first().rect
        setContentRtl(rtl) { CamoBarChart(listOf("x", "y"), data, "Bars", Modifier.width(400.dp)) }
        assertTrue(first.left < ltr.bars!!.plot.center.x)
        assertTrue(rtl.bars!!.bars.first().rect.left > rtl.bars!!.plot.center.x)
    }

    private fun ComposeUiTest.setContentRtl(recorder: Recorder, content: @Composable () -> Unit) {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CamouflageProvider(MinimalSkin, minimalLight.copy(motion = minimalLight.motion.reduced())) {
                    CamoChartsProvider(recorder, content)
                }
            }
        }
        waitForIdle()
    }
}
