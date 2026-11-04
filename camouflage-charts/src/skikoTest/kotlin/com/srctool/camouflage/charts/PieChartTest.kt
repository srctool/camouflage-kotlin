package com.srctool.camouflage.charts

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.minimal.minimalLight
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PieChartTest {
    private val slices = listOf(PieSlice("A", 3.0), PieSlice("B", 1.0))

    private class Recorder : ChartRenderer {
        var pie: PieChartSpec? = null
        override fun drawSparkline(scope: DrawScope, spec: SparklineSpec, style: ResolvedChartStyle) = Unit
        override fun drawPieChart(scope: DrawScope, spec: PieChartSpec, style: ResolvedChartStyle) { pie = spec }
    }

    private fun ComposeUiTest.chart(recorder: Recorder? = null, content: @Composable () -> Unit) = setContent {
        CamouflageProvider(MinimalSkin, minimalLight.copy(motion = minimalLight.motion.reduced())) {
            if (recorder == null) content() else CamoChartsProvider(recorder, content)
        }
    }

    private val image = hasContentDescription("Traffic. Pie chart, 2 slices, largest: A 75%")

    private fun stateIs(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun aPieIsOneImageAndTheLegendListsValueAndShare() = runComposeUiTest {
        chart { CamoPieChart(slices, "Traffic", Modifier.width(300.dp)) }
        onNode(image).assertExists()
        onNodeWithText("A: 3, 75%").assertExists()
        onNodeWithText("B: 1, 25%").assertExists()
    }

    @Test
    fun aDonutShowsItsCenterContent() = runComposeUiTest {
        chart {
            CamoPieChart(slices, "Traffic", Modifier.width(300.dp), donut = true, center = { CamoText("Total 4") })
        }
        onNodeWithText("Total 4").assertExists()
        onNode(hasContentDescription("Traffic. Donut chart, 2 slices, largest: A 75%")).assertExists()
    }

    @Test
    fun hoveringShowsTheSlice() = runComposeUiTest {
        chart { CamoPieChart(slices, "Traffic", Modifier.width(300.dp)) }
        onNode(image).performMouseInput { moveTo(center) }
        onNodeWithText("3 (75%)").assertExists()
    }

    @Test
    fun clickingSelectsAndClickingAgainClears() = runComposeUiTest {
        var selected: Int? = null
        val picks = mutableListOf<Int?>()
        chart {
            CamoPieChart(slices, "Traffic", Modifier.width(300.dp), selected = selected,
                onSelect = { picks += it; selected = it })
        }
        onNode(image).performClick()
        waitForIdle()
        assertEquals(listOf<Int?>(0), picks)
    }

    @Test
    fun theSelectedSliceIsHandedToTheRenderer() = runComposeUiTest {
        val recorder = Recorder()
        chart(recorder) { CamoPieChart(slices, "Traffic", Modifier.width(300.dp), selected = 1) }
        waitForIdle()
        assertEquals(1, recorder.pie!!.selected)
        assertEquals(2, recorder.pie!!.slices.size)
    }

    @Test
    fun theLegendHidesASlice() = runComposeUiTest {
        val recorder = Recorder()
        chart(recorder) { CamoPieChart(slices, "Traffic", Modifier.width(300.dp)) }
        onNodeWithText("B: 1, 25%").performClick()
        waitForIdle()
        assertEquals(1, recorder.pie!!.slices.size)
        assertEquals(360f, recorder.pie!!.slices.single().sweep)
    }

    @Test
    fun theKeyboardWalksSlicesAndEnterSelects() = runComposeUiTest {
        val picks = mutableListOf<Int?>()
        chart { CamoPieChart(slices, "Traffic", Modifier.width(300.dp), onSelect = { picks += it }) }
        val plot = onNode(isFocusable() and hasAnyAncestor(image))
        plot.requestFocus()
        plot.assert(stateIs("A, 75%, 3"))
        plot.performKeyInput { pressKey(Key.DirectionDown) }
        plot.assert(stateIs("B, 25%, 1"))
        plot.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf<Int?>(1), picks)
    }

    @Test
    fun allZeroIsEmpty() = runComposeUiTest {
        chart { CamoPieChart(listOf(PieSlice("A", 0.0)), "Traffic") }
        onNodeWithText("No data").assertExists()
    }
}
