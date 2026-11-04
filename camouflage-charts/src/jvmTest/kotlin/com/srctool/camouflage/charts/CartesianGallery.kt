package com.srctool.camouflage.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
private val Sales = ChartSeries("Sales", listOf(12.0, 18.0, 15.0, 24.0, 21.0, 30.0))
private val Costs = ChartSeries("Costs", listOf(8.0, 10.0, null, 14.0, 13.0, 16.0))
private val Height = 130.dp

/** One scene of every line and bar form, for the golden screenshots. */
@Composable
internal fun CartesianGallery() {
    Column(Modifier.padding(12.dp), Arrangement.spacedBy(10.dp)) {
        CamoLineChart(
            Months, listOf(Sales, Costs), "Line", markers = MarkerVisibility.Always, xAxis = ChartAxis(title = "Month"),
            yAxis = ChartAxis(title = "Units"), plotHeight = Height,
        )
        CamoLineChart(
            Months, listOf(Sales, Costs), "Stacked area, smooth", kind = LineKind.StackedArea, curve = LineCurve.Smooth,
            plotHeight = Height,
        )
        CamoBarChart(Months, listOf(Sales, Costs), "Grouped", valueLabels = true, plotHeight = Height)
        CamoBarChart(
            Months.take(3), listOf(Sales, Costs), "Stacked, horizontal", orientation = BarOrientation.Horizontal,
            mode = BarMode.Stacked, plotHeight = Height,
        )
    }
}
