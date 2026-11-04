package com.srctool.camouflage.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

private val DefaultBarPlotHeight = 220.dp
private const val VALUE_LABEL_GAP = 3f
private const val BAR_LABEL_SPACING = 1.4f

internal class BarInput(
    val categories: List<String>,
    val series: List<ChartSeries>,
    val colors: List<Color>,
    val orientation: BarOrientation,
    val mode: BarMode,
    val valueLabels: Boolean,
    val xAxis: ChartAxis,
    val yAxis: ChartAxis,
) {
    val vertical: Boolean get() = orientation == BarOrientation.Vertical
}

/** The value axis of a bar chart: bars start at zero, and a stacked category reaches its positive and negative sums. */
internal fun barDomain(values: List<List<Double?>>, mode: BarMode, axis: ChartAxis): ValueDomain {
    val categories = values.maxOfOrNull { it.size } ?: 0
    val extents = if (mode == BarMode.Stacked) {
        (0 until categories).flatMap { c ->
            val column = values.mapNotNull { it.getOrNull(c) }
            listOf(column.filter { it > 0 }.sum(), column.filter { it < 0 }.sum())
        }
    } else {
        values.flatMap { it.filterNotNull() }
    }
    return valueDomain(extents.minOrNull(), extents.maxOrNull(), includeZero = true, axis = axis)
}

/**
 * Bars comparing values across categories: vertical or horizontal, grouped side by side or stacked, with a legend
 * that toggles series, a tooltip on the bar under the pointer and a data table. [yAxis] is always the value axis and
 * [xAxis] the category axis, whichever way the bars run. [valueLabels] writes each bar's value at its free end (a
 * stack's total at its end). The plot is one image node; focusing it walks the bars (Left and Right between
 * categories, Up and Down between series) and announces "Q1, North, 42". Bars grow from zero; negative bars go the
 * other way. In a right-to-left layout vertical bars run from the right and horizontal bars start at the right edge.
 */
@Composable
public fun CamoBarChart(
    categories: List<String>,
    series: List<ChartSeries>,
    title: String,
    modifier: Modifier = Modifier,
    orientation: BarOrientation = BarOrientation.Vertical,
    mode: BarMode = BarMode.Grouped,
    valueLabels: Boolean = false,
    xAxis: ChartAxis = ChartAxis(),
    yAxis: ChartAxis = ChartAxis(),
    loading: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    showTable: Boolean = false,
    plotHeight: Dp = DefaultBarPlotHeight,
    style: ChartStyle? = null,
) {
    val resolved = rememberChartStyle(style)
    var hidden by remember(series.size) { mutableStateOf(setOf<Int>()) }
    var table by remember(showTable) { mutableStateOf(showTable) }
    val colors = seriesColors(series, resolved)
    val legend = series.mapIndexed { index, item -> LegendItem(item.name, colors[index], index !in hidden) }
    CamoChartFrame(
        title = title,
        summary = chartSummary("Bar chart", categories, series),
        modifier = modifier,
        legend = legend,
        onLegendToggle = { hidden = hidden.toggled(it) },
        loading = loading,
        isEmpty = noChartData(categories, series),
        empty = empty,
        plotHeight = plotHeight,
        table = { CamoChartDataTable(categories, series, "$title data", format = yAxis.format) },
        showTable = table,
        onShowTableChange = { table = it },
    ) {
        val shown = series.indices.filter { it !in hidden }
        val input = BarInput(
            categories, shown.map { series[it] }, shown.map { colors[it] }, orientation, mode, valueLabels,
            xAxis, yAxis,
        )
        BarPlot(input, resolved, plotHeight)
    }
}

// Everything the bar chart draws for one size: the plot rectangle, the bars and the labels.
internal class BarScene(
    val layout: PlotLayout,
    val domain: ValueDomain,
    val scale: LinearScale,
    val bars: List<BarRect>,
    val labels: List<AxisLabel>,
)

private fun barScene(input: BarInput, size: Size, style: ResolvedChartStyle, env: PlotEnv): BarScene {
    val values = input.series.map { it.values }
    val domain = barDomain(values, input.mode, input.yAxis)
    val valueTexts = domain.ticks.map(input.yAxis.format)
    val valueTitle = input.yAxis.title
    val categoryTitle = input.xAxis.title
    val topTitle = if (input.vertical) valueTitle else categoryTitle
    val bottomTitle = if (input.vertical) categoryTitle else valueTitle
    val insets = env.insets(if (input.vertical) valueTexts else input.categories, topTitle != null, bottomTitle != null)
    val layout = plotLayout(size, insets, env.rtl)
    val plot = layout.plot
    val scale = when {
        input.vertical -> LinearScale(domain.low, domain.high, plot.bottom, plot.top)
        env.rtl -> LinearScale(domain.low, domain.high, plot.right, plot.left)
        else -> LinearScale(domain.low, domain.high, plot.left, plot.right)
    }
    val gap = with(env.density) { style.barGap.toPx() }
    val bars = barRects(
        plot, input.categories.size, values, input.mode, input.orientation, domain.range, gap, style.groupGap, env.rtl,
    )
    val labels = barAxisLabels(input, layout, scale, domain.ticks, valueTexts, bars, env) +
        env.titles(layout, topTitle, bottomTitle)
    return BarScene(layout, domain, scale, bars, labels)
}

// Value labels on the value axis, categories on the other.
private fun barAxisLabels(
    input: BarInput,
    layout: PlotLayout,
    scale: LinearScale,
    ticks: List<Double>,
    valueTexts: List<String>,
    bars: List<BarRect>,
    env: PlotEnv,
): List<AxisLabel> {
    val plot = layout.plot
    val startX = if (env.rtl) plot.right + env.gap else plot.left - env.gap
    val startSide = if (env.rtl) LabelSide.After else LabelSide.Before
    val below = plot.bottom + env.gap
    val values = ticks.mapIndexed { index, tick ->
        if (input.vertical) {
            AxisLabel(valueTexts[index], Offset(startX, scale.map(tick)), startSide)
        } else {
            AxisLabel(valueTexts[index], Offset(scale.map(tick), below), LabelSide.Below)
        }
    }
    val band = (if (input.vertical) plot.width else plot.height) / input.categories.size
    val across = if (input.vertical) {
        env.text.measurer.widest(input.categories, env.text.style)
    } else {
        env.labelHeight
    }
    val step = ceil(across * BAR_LABEL_SPACING / band).toInt().coerceAtLeast(1)
    val names = input.categories.indices.filter { it % step == 0 }.mapNotNull { category ->
        val center = categoryCenter(bars, category, input.vertical) ?: return@mapNotNull null
        if (input.vertical) {
            AxisLabel(input.categories[category], Offset(center, below), LabelSide.Below)
        } else {
            AxisLabel(input.categories[category], Offset(startX, center), startSide)
        }
    }
    return values + names
}

// The center of a category's bars across the category axis, or `null` when it has none.
private fun categoryCenter(bars: List<BarRect>, category: Int, vertical: Boolean): Float? {
    val own = bars.filter { it.category == category }
    if (own.isEmpty()) return null
    return if (vertical) {
        (own.minOf { it.rect.left } + own.maxOf { it.rect.right }) / 2
    } else {
        (own.minOf { it.rect.top } + own.maxOf { it.rect.bottom }) / 2
    }
}

// A value at each bar's free end; a stack gets its side's total there.
private fun barValueLabels(input: BarInput, bars: List<BarRect>): List<AxisLabel> {
    val format = input.yAxis.format
    return bars.filter { it.end != null }.map { bar ->
        val shown = if (input.mode == BarMode.Stacked) {
            bars.filter { it.category == bar.category && (it.value >= 0) == (bar.value >= 0) }.sumOf { it.value }
        } else {
            bar.value
        }
        val rect = bar.rect
        val text = format(shown)
        when (bar.end) {
            BarEdge.Top -> AxisLabel(text, Offset(rect.center.x, rect.top - VALUE_LABEL_GAP), LabelSide.Above)
            BarEdge.Bottom -> AxisLabel(text, Offset(rect.center.x, rect.bottom + VALUE_LABEL_GAP), LabelSide.Below)
            BarEdge.Right -> AxisLabel(text, Offset(rect.right + VALUE_LABEL_GAP, rect.center.y), LabelSide.After)
            else -> AxisLabel(text, Offset(rect.left - VALUE_LABEL_GAP, rect.center.y), LabelSide.Before)
        }
    }
}

@Composable
private fun BarPlot(input: BarInput, style: ResolvedChartStyle, plotHeight: Dp) {
    val env = rememberPlotEnv()
    val state = remember { PlotInput() }
    val progress = rememberDrawIn(input.series to input.mode, style)
    BoxWithConstraints(Modifier.fillMaxWidth().height(plotHeight)) {
        val scene = barScene(input, Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()), style, env)
        val bars = scene.bars
        val hit = state.pointer?.takeIf { scene.layout.plot.contains(it) }?.let { nearestBar(bars, it, input.vertical) }
        val focus = state.walk?.let { (c, s) -> bars.firstOrNull { it.category == c && it.series == s } }
        val current = hit ?: focus
        val announce = state.walk?.let { (c, s) -> barText(input, c, s) }
        Box(
            Modifier
                .fillMaxSize()
                .plotInput(state, input.categories.size, input.series.size, announce)
                .drawBehind { drawBarScene(scene, input, env, style, current?.let { bars.indexOf(it) }, progress) },
        ) {
            if (current != null) {
                val rows = barRows(input, current.category, current.series)
                ChartTooltip(input.categories[current.category], rows, current.rect.center.x)
            }
        }
    }
}

private fun DrawScope.drawBarScene(
    scene: BarScene,
    input: BarInput,
    env: PlotEnv,
    style: ResolvedChartStyle,
    active: Int?,
    progress: Float,
) {
    val plot = scene.layout.plot
    val ticks = scene.domain.ticks.map(scene.scale::map)
    drawGrid(plot, ticks, !input.vertical, style, env.colors.outlineVariant, env.colors.outline)
    drawAxisLabels(env.text.measurer, scene.labels, env.text.style)
    val marks = scene.bars.map { BarMark(it.rect, input.colors[it.series], it.end) }
    val baseline = scene.scale.map(0.0.coerceIn(scene.domain.low, scene.domain.high))
    env.renderer.drawBarChart(this, BarChartSpec(plot, marks, baseline, input.vertical, active, progress), style)
    if (input.valueLabels && progress >= 1f) {
        drawAxisLabels(env.text.measurer, barValueLabels(input, scene.bars), env.text.style)
    }
}

// The hovered or focused bar's series and value; a stacked category adds its total.
private fun barRows(input: BarInput, category: Int, series: Int): List<TooltipRow> {
    val format = input.yAxis.format
    val item = input.series[series]
    val value = item.values.getOrNull(category)?.let(format) ?: "-"
    val own = TooltipRow(input.colors[series], "${item.name}: $value")
    if (input.mode != BarMode.Stacked) return listOf(own)
    val total = input.series.sumOf { it.values.getOrNull(category) ?: 0.0 }
    return listOf(own, TooltipRow(null, "Total: ${format(total)}"))
}

private fun barText(input: BarInput, category: Int, series: Int): String {
    val label = input.categories.getOrElse(category) { "" }
    val item = input.series.getOrNull(series) ?: return label
    return "$label, ${item.name}, ${item.values.getOrNull(category)?.let(input.yAxis.format) ?: "no data"}"
}
