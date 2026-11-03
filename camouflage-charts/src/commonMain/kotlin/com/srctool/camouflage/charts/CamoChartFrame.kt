package com.srctool.camouflage.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.chip.CamoChip
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skeleton.CamoSkeleton
import com.srctool.camouflage.core.text.CamoText

private val LegendDot = 8.dp

/**
 * What every chart is wrapped in: a [title], the legend (each entry a toggle chip, see [onLegendToggle]), the plot
 * ([content]) and, when [table] is given, a switch to the same data as a table ([showTable], [onShowTableChange]).
 * The plot is one image node whose text is "[title]. [summary]". While [loading] a skeleton of [plotHeight]
 * stands in; with no data ([isEmpty]) the [empty] slot (or "No data") does.
 */
@Composable
public fun CamoChartFrame(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    legend: List<LegendItem> = emptyList(),
    onLegendToggle: ((Int) -> Unit)? = null,
    loading: Boolean = false,
    isEmpty: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    plotHeight: Dp = 200.dp,
    table: (@Composable () -> Unit)? = null,
    showTable: Boolean = false,
    onShowTableChange: ((Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val spacing = Camouflage.spacing
    Column(modifier, Arrangement.spacedBy(spacing.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            CamoText(title, style = Camouflage.typography.titleSmall)
        }
        if (legend.isNotEmpty()) Legend(legend, onLegendToggle)
        when {
            loading -> CamoSkeleton(plotHeight)
            isEmpty -> if (empty != null) empty() else CamoText("No data")
            showTable && table != null -> Box(Modifier.height(plotHeight)) { table() }
            else -> Box(Modifier.semantics { role = Role.Image; contentDescription = "$title. $summary" }) {
                content()
            }
        }
        val plotShown = !loading && !isEmpty
        if (plotShown && table != null && onShowTableChange != null) {
            CamoChip(
                "View as table",
                selected = showTable,
                onClick = { onShowTableChange(!showTable) },
            )
        }
    }
}

@Composable
private fun Legend(items: List<LegendItem>, onToggle: ((Int) -> Unit)?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            CamoChip(
                item.label,
                selected = if (onToggle != null) item.visible else null,
                onClick = onToggle?.let { toggle -> { toggle(index) } },
                leading = { Box(Modifier.size(LegendDot).background(item.color, CircleShape)) },
            )
        }
    }
}

/** The chart's data as a table: a row per category, a column per series. */
@Composable
public fun CamoChartDataTable(
    categories: List<String>,
    series: List<ChartSeries>,
    caption: String,
    modifier: Modifier = Modifier,
    format: (Double) -> String = ::formatChartNumber,
) {
    val columns = listOf(CamoColumn<Int>("category", "Category", cell = { CamoText(categories[it]) })) +
        series.mapIndexed { column, s ->
            CamoColumn<Int>(column, s.name, cell = { row ->
                CamoText(s.values.getOrNull(row)?.let(format) ?: "-")
            })
        }
    CamoDataTable(columns, categories.indices.toList(), { it }, caption, modifier)
}
