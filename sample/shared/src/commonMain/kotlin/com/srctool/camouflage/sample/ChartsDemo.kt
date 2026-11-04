package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.charts.BarMode
import com.srctool.camouflage.charts.BarOrientation
import com.srctool.camouflage.charts.CamoBarChart
import com.srctool.camouflage.charts.CamoLineChart
import com.srctool.camouflage.charts.CamoPieChart
import com.srctool.camouflage.charts.CamoSparkline
import com.srctool.camouflage.charts.ChartAxis
import com.srctool.camouflage.charts.ChartSeries
import com.srctool.camouflage.charts.LineCurve
import com.srctool.camouflage.charts.LineKind
import com.srctool.camouflage.charts.MarkerVisibility
import com.srctool.camouflage.charts.PieSlice
import com.srctool.camouflage.charts.SparklineKind
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone

// The M11 milestone screen: a dashboard of charts that follow the skin, the brand and dark mode. KPI cards with
// sparklines, revenue as a line, area or stacked area, regional bars (grouped or stacked, vertical or horizontal) and
// a traffic donut with its total in the hole, selectable by touch and keys. Every chart reads as a table too.

private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug")
private val Revenue = listOf(
    ChartSeries("Subscriptions", listOf(12.0, 15.0, 14.0, 19.0, 22.0, 21.0, 27.0, 31.0)),
    ChartSeries("Services", listOf(6.0, 7.0, null, 9.0, 8.0, 12.0, 11.0, 14.0)),
    ChartSeries("Hardware", listOf(4.0, 3.0, 5.0, 4.0, 6.0, 5.0, 7.0, 6.0)),
)
private val Quarters = listOf("Q1", "Q2", "Q3", "Q4")
private val Regions = listOf(
    ChartSeries("North", listOf(42.0, 38.0, 51.0, 47.0)),
    ChartSeries("South", listOf(28.0, 35.0, 30.0, 44.0)),
    ChartSeries("West", listOf(19.0, 22.0, 31.0, 26.0)),
)
private val Traffic = listOf(
    PieSlice("Search", 4500.0),
    PieSlice("Social", 3000.0),
    PieSlice("Email", 1500.0),
    PieSlice("Direct", 900.0),
)
private val Kpis = listOf(
    Triple("Revenue", "31k", listOf(12.0, 15.0, 14.0, 19.0, 22.0, 21.0, 27.0, 31.0)),
    Triple("Orders", "1,204", listOf(40.0, 38.0, 45.0, 41.0, 52.0, 49.0, 60.0, 58.0)),
    Triple("Refunds", "18", listOf(9.0, 7.0, 8.0, 6.0, 7.0, 5.0, 6.0, 4.0)),
)
private const val DONUT_TOTAL = "9,900"

@Composable
internal fun ChartsDemo() {
    var kind by remember { mutableStateOf(LineKind.Line) }
    var curve by remember { mutableStateOf(LineCurve.Smooth) }
    var mode by remember { mutableStateOf(BarMode.Grouped) }
    var orientation by remember { mutableStateOf(BarOrientation.Vertical) }
    var selected by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.l)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(Camouflage.spacing.m)) {
            Kpis.forEachIndexed { index, (label, value, trend) ->
                Column(Modifier.weight(1f), Arrangement.spacedBy(Camouflage.spacing.xs)) {
                    CamoText(label, style = Camouflage.typography.labelMedium)
                    CamoText(value, style = Camouflage.typography.titleLarge)
                    CamoSparkline(
                        trend,
                        Modifier.fillMaxWidth(),
                        kind = if (index == 1) SparklineKind.Bars else SparklineKind.Area,
                        tone = if (index == 2) Tone.Error else Tone.Default,
                        description = "$label, last 8 months",
                    )
                }
            }
        }
        Options("Revenue as", LineKind.entries, kind, { it.name }) { kind = it }
        Options("Curve", listOf(LineCurve.Linear, LineCurve.Smooth, LineCurve.CatmullRom, LineCurve.Step), curve,
            { it.name }) { curve = it }
        CamoLineChart(
            Months, Revenue, "Revenue by line, in thousands", Modifier.fillMaxWidth(), kind = kind, curve = curve,
            markers = MarkerVisibility.OnHover, xAxis = ChartAxis(title = "Month"), yAxis = ChartAxis(title = "USD k"),
        )
        Options("Regions", BarMode.entries, mode, { it.name }) { mode = it }
        Options("Direction", BarOrientation.entries, orientation, { it.name }) { orientation = it }
        CamoBarChart(
            Quarters, Regions, "Orders by region", Modifier.fillMaxWidth(), orientation = orientation, mode = mode,
            valueLabels = true,
        )
        CamoPieChart(
            Traffic, "Traffic by source", Modifier.fillMaxWidth(), donut = true, selected = selected,
            onSelect = { selected = it },
            center = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    CamoText(DONUT_TOTAL, style = Camouflage.typography.titleMedium)
                    CamoText("visits", style = Camouflage.typography.labelSmall)
                }
            },
        )
        CamoText(
            selected?.let { "${Traffic[it].label} selected" } ?: "Select a slice by tap, click or Enter",
            style = Camouflage.typography.bodySmall,
        )
    }
}

// A labelled row of options; the chosen one is solid.
@Composable
private fun <T> Options(label: String, options: List<T>, chosen: T, name: (T) -> String, onChoose: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
        verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.xs),
    ) {
        CamoText(label, style = Camouflage.typography.labelMedium)
        options.forEach {
            CamoButton(
                name(it), { onChoose(it) },
                appearance = if (it == chosen) Appearance.Solid else Appearance.Outline,
            )
        }
    }
}
