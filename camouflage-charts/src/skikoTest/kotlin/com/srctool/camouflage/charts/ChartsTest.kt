package com.srctool.camouflage.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.skin.carbon.CarbonSkin
import com.srctool.camouflage.skin.carbon.carbonLight
import com.srctool.camouflage.skin.cupertino.CupertinoSkin
import com.srctool.camouflage.skin.cupertino.cupertinoLight
import com.srctool.camouflage.skin.fluent.FluentSkin
import com.srctool.camouflage.skin.material.MaterialSkin
import com.srctool.camouflage.skin.material.materialBaselineLight
import com.srctool.camouflage.skin.fluent.fluentTheme
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.minimal.minimalLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ChartsTest {
    @Test
    fun aSparklineIsOneImageWithItsSummary() = runComposeUiTest {
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) {
                CamoSparkline(listOf(3.0, 5.0, 9.0), Modifier.size(80.dp, 20.dp), description = "Signups")
            }
        }
        onNode(hasContentDescription("Signups: from 3 to 9, up 200%")).assertExists()
    }

    @Test
    fun aSparklineWithNoDataStillDraws() = runComposeUiTest {
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) { CamoSparkline(emptyList()) }
        }
        onNode(hasContentDescription("Trend no data")).assertExists()
    }

    private fun resolved(
        skin: CamoSkin,
        theme: CamoTheme,
        overrides: ChartStyle? = null,
        into: (ResolvedChartStyle) -> Unit,
    ): @Composable () -> Unit = {
        CamouflageProvider(skin, theme) { into(rememberChartStyle(overrides)) }
    }

    @Test
    fun theSkinsPresetShapesTheStyle() = runComposeUiTest {
        var minimal: ResolvedChartStyle? = null
        var fluent: ResolvedChartStyle? = null
        var material: ResolvedChartStyle? = null
        var cupertino: ResolvedChartStyle? = null
        var carbon: ResolvedChartStyle? = null
        setContent {
            resolved(MinimalSkin, minimalLight) { minimal = it }()
            resolved(FluentSkin, fluentTheme()) { fluent = it }()
            resolved(MaterialSkin, materialBaselineLight) { material = it }()
            resolved(CupertinoSkin, cupertinoLight) { cupertino = it }()
            resolved(CarbonSkin, carbonLight) { carbon = it }()
        }
        assertEquals(1.5.dp, minimal!!.lineWidth)
        assertEquals(2.dp, fluent!!.lineWidth)
        assertTrue(fluent!!.gridDashed)
        assertTrue(!minimal!!.gridDashed)
        assertEquals(3.dp, material!!.lineWidth)
        assertEquals(8.dp, material!!.barRadius)
        assertEquals(0.3f, material!!.donutThickness)
        assertEquals(2.dp, cupertino!!.lineWidth)
        assertEquals(4.dp, cupertino!!.barRadius)
        assertEquals(0.4f, cupertino!!.donutThickness)
        assertEquals(1.5.dp, carbon!!.lineWidth)
        assertEquals(0.dp, carbon!!.barRadius)
        assertEquals(0.35f, carbon!!.donutThickness)
        assertTrue(carbon!!.gridDashed)
    }

    @Test
    fun tokensThenParametersOverridePresets() = runComposeUiTest {
        var tokenStyle: ResolvedChartStyle? = null
        var paramStyle: ResolvedChartStyle? = null
        val branded = minimalLight.copy(tokenSets = listOf(ChartTokens(ChartStyle(lineWidth = 4.dp, barGap = 9.dp))))
        setContent {
            resolved(MinimalSkin, branded) { tokenStyle = it }()
            resolved(MinimalSkin, branded, ChartStyle(lineWidth = 7.dp)) { paramStyle = it }()
        }
        assertEquals(4.dp, tokenStyle!!.lineWidth)
        assertEquals(9.dp, tokenStyle!!.barGap)
        assertEquals(7.dp, paramStyle!!.lineWidth)
        assertEquals(9.dp, paramStyle!!.barGap)
    }

    @Test
    fun aRegisteredPresetStylesASkinByName() = runComposeUiTest {
        CamoCharts.registerPreset("minimal-test-brand", ChartStyle(barRadius = 11.dp))
        var style: ResolvedChartStyle? = null
        setContent {
            resolved(MinimalSkin.copy(name = "minimal-test-brand"), minimalLight) { style = it }()
        }
        assertEquals(11.dp, style!!.barRadius)
    }

    @Test
    fun thePaletteLeavesOutColorsLostOnTheSurface() = runComposeUiTest {
        var style: ResolvedChartStyle? = null
        setContent { resolved(MinimalSkin, minimalLight) { style = it }() }
        assertTrue(style!!.palette.size >= 3)
        assertNotEquals(style!!.seriesColor(0), style!!.seriesColor(1))
        assertEquals(style!!.seriesColor(0), style!!.seriesColor(style!!.palette.size))
    }

    @Test
    fun theLegendTogglesSeries() = runComposeUiTest {
        var items by mutableStateOf(listOf(LegendItem("Sales", Color.Red), LegendItem("Costs", Color.Blue)))
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) {
                CamoChartFrame("Revenue", "2 series", legend = items,
                    onLegendToggle = { i ->
                        items = items.mapIndexed { j, item -> if (i == j) item.copy(visible = !item.visible) else item }
                    }) {
                    CamoText("plot")
                }
            }
        }
        onNodeWithText("Costs").performClick()
        assertTrue(!items[1].visible)
        assertTrue(items[0].visible)
    }

    @Test
    fun thePlotIsAnImageNamedByTitleAndSummary() = runComposeUiTest {
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) {
                CamoChartFrame("Revenue", "Line chart, 2 series") { CamoText("plot") }
            }
        }
        onNode(hasContentDescription("Revenue. Line chart, 2 series")).assertExists()
    }

    @Test
    fun theTableSwitchShowsTheDataAsRows() = runComposeUiTest {
        var shown by mutableStateOf(false)
        val series = listOf(ChartSeries("Sales", listOf(1.0, null)))
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) {
                CamoChartFrame("Revenue", "summary",
                    table = { CamoChartDataTable(listOf("Jan", "Feb"), series, "Revenue data") },
                    showTable = shown, onShowTableChange = { shown = it }) { CamoText("plot") }
            }
        }
        onNodeWithText("Jan").assertDoesNotExist()
        onNodeWithText("View as table").performClick()
        onNodeWithText("Jan").assertExists()
        onNodeWithText("Feb").assertExists()
        onNodeWithText("-").assertExists()
    }

    @Test
    fun loadingAndEmptyReplaceThePlot() = runComposeUiTest {
        var loading by mutableStateOf(true)
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) {
                CamoChartFrame("Revenue", "s", loading = loading, isEmpty = true) { CamoText("plot") }
            }
        }
        onNodeWithText("plot").assertDoesNotExist()
        onNodeWithText("No data").assertDoesNotExist()
        loading = false
        onNodeWithText("No data").assertExists()
        onNodeWithText("plot").assertDoesNotExist()
    }

    @Test
    fun aCustomRendererTakesOverTheDrawing() = runComposeUiTest {
        var drawn = 0
        val renderer = object : ChartRenderer {
            override fun drawSparkline(scope: androidx.compose.ui.graphics.drawscope.DrawScope, spec: SparklineSpec,
                style: ResolvedChartStyle) { drawn++ }
        }
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight) {
                CamoChartsProvider(renderer) { CamoSparkline(listOf(1.0, 2.0), Modifier.size(50.dp, 20.dp)) }
            }
        }
        waitForIdle()
        assertTrue(drawn > 0)
    }

    @Test
    fun noMotionDrawsAtOnce() = runComposeUiTest {
        var progress = -1f
        val renderer = object : ChartRenderer {
            override fun drawSparkline(scope: androidx.compose.ui.graphics.drawscope.DrawScope, spec: SparklineSpec,
                style: ResolvedChartStyle) { progress = spec.progress }
        }
        setContent {
            CamouflageProvider(MinimalSkin, minimalLight.copy(motion = minimalLight.motion.reduced())) {
                CamoChartsProvider(renderer) { CamoSparkline(listOf(1.0, 2.0), Modifier.size(50.dp, 20.dp)) }
            }
        }
        waitForIdle()
        assertEquals(1f, progress)
    }
}
