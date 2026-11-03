package com.srctool.camouflage.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Tone

private val Trend = listOf(3.0, 5.0, 4.0, 8.0, 6.0, 9.0, 7.0, 12.0)
private val Gappy = listOf(3.0, 5.0, null, 8.0, 6.0, null, null, 12.0)
private val Box = Modifier.size(120.dp, 36.dp)

/** One scene of every sparkline form and a frame with a legend and its table, for the golden screenshots. */
@Composable
internal fun ChartsGallery() {
    Column(Modifier.padding(16.dp), Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CamoSparkline(Trend, Box)
            CamoSparkline(Trend, Box, kind = SparklineKind.Area)
            CamoSparkline(Trend, Box, kind = SparklineKind.Bars)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CamoSparkline(Gappy, Box, highlight = SparklineHighlight.Extremes)
            CamoSparkline(listOf(5.0), Box)
            CamoSparkline(listOf(4.0, 4.0, 4.0), Box, kind = SparklineKind.Area, tone = Tone.Success)
        }
        CamoChartFrame(
            title = "Revenue",
            summary = "2 series, 3 categories",
            legend = listOf(
                LegendItem("Sales", androidx.compose.ui.graphics.Color(0xFF3366CC)),
                LegendItem("Costs", androidx.compose.ui.graphics.Color(0xFFCC6633), visible = false),
            ),
            onLegendToggle = {},
            plotHeight = 80.dp,
            table = {
                CamoChartDataTable(
                    listOf("Jan", "Feb", "Mar"),
                    listOf(ChartSeries("Sales", listOf(1.0, 2.5, 4.0)), ChartSeries("Costs", listOf(0.5, null, 3.0))),
                    "Revenue data",
                )
            },
            showTable = true,
            onShowTableChange = {},
        ) { CamoText("plot") }
        CamoChartFrame("Loading", "", loading = true, plotHeight = 40.dp) {}
        CamoChartFrame("Empty", "", isEmpty = true) {}
    }
}
