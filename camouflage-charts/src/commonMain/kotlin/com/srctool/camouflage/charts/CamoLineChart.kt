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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

private val DefaultPlotHeight = 220.dp
private const val LABEL_SPACING = 1.6f

/**
 * A chart of values over an ordered axis, as lines, areas or stacked areas, with a legend that toggles series, a
 * tooltip and crosshair at the nearest category, and a data table. [categories] label the x axis (one per value);
 * [times] (epoch milliseconds, ascending, [timeZoneOffsetMinutes] from UTC) places them on a time axis with
 * calendar ticks instead. The plot is one image node whose text is [title] and each series' trend; focusing it
 * walks the points (Left and Right between categories, Up and Down between series, Home and End) and announces
 * "Jan, Sales, 42". A `null` value breaks the line; stacked areas treat negative values as zero. The time axis
 * keeps its direction in a right-to-left layout, as time does.
 */
@Composable
public fun CamoLineChart(
    categories: List<String>,
    series: List<ChartSeries>,
    title: String,
    modifier: Modifier = Modifier,
    kind: LineKind = LineKind.Line,
    curve: LineCurve = LineCurve.Linear,
    markers: MarkerVisibility = MarkerVisibility.OnHover,
    xAxis: ChartAxis = ChartAxis(),
    yAxis: ChartAxis = ChartAxis(),
    times: List<Long>? = null,
    timeZoneOffsetMinutes: Int = 0,
    loading: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    showTable: Boolean = false,
    plotHeight: Dp = DefaultPlotHeight,
    style: ChartStyle? = null,
) {
    val resolved = rememberChartStyle(style)
    var hidden by remember(series.size) { mutableStateOf(setOf<Int>()) }
    var table by remember(showTable) { mutableStateOf(showTable) }
    val colors = seriesColors(series, resolved)
    val legend = series.mapIndexed { index, item -> LegendItem(item.name, colors[index], index !in hidden) }
    CamoChartFrame(
        title = title,
        summary = chartSummary("Line chart", categories, series),
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
        val axes = LineAxes(xAxis, yAxis, times, timeZoneOffsetMinutes)
        val input = LineInput(
            categories, shown.map { series[it] }, shown.map { colors[it] }, kind, curve, markers, axes,
        )
        LinePlot(input, resolved, plotHeight)
    }
}

internal class LineAxes(val x: ChartAxis, val y: ChartAxis, val times: List<Long>?, val offsetMinutes: Int)

internal class LineInput(
    val categories: List<String>,
    val series: List<ChartSeries>,
    val colors: List<Color>,
    val kind: LineKind,
    val curve: LineCurve,
    val markers: MarkerVisibility,
    val axes: LineAxes,
)

// Everything the line chart draws for one size: the plot rectangle, the points and the labels.
internal class LineScene(val layout: PlotLayout, val model: LineModel, val labels: List<AxisLabel>)

private fun lineScene(input: LineInput, size: Size, env: PlotEnv): LineScene {
    val values = input.series.map { it.values }
    val bands = if (input.kind == LineKind.StackedArea) stack(values) else null
    val domain = lineDomain(input.kind, values, bands, input.axes.y)
    val yTexts = domain.ticks.map(input.axes.y.format)
    val insets = env.insets(yTexts, topTitle = input.axes.y.title != null, bottomTitle = input.axes.x.title != null)
    val layout = plotLayout(size, insets, env.rtl)
    val xs = categoryPositions(layout.plot, input.categories.size, input.axes.times)
    val model = lineModel(input.kind, layout.plot, values, bands, input.colors, domain, xs)
    val labels = valueLabels(model, yTexts, layout.plot, env) + xLabels(input, model, layout.plot, env) +
        env.titles(layout, input.axes.y.title, input.axes.x.title)
    return LineScene(layout, model, labels)
}

// The value labels on the start side of the plot.
private fun valueLabels(model: LineModel, texts: List<String>, plot: Rect, env: PlotEnv): List<AxisLabel> {
    val x = if (env.rtl) plot.right + env.gap else plot.left - env.gap
    val side = if (env.rtl) LabelSide.After else LabelSide.Before
    return model.domain.ticks.mapIndexed { index, tick ->
        AxisLabel(texts[index], Offset(x, model.yScale.map(tick)), side)
    }
}

// The labels below the plot: calendar ticks on a time axis, else the categories, thinned out to fit.
private fun xLabels(input: LineInput, model: LineModel, plot: Rect, env: PlotEnv): List<AxisLabel> {
    val below = plot.bottom + env.gap
    val times = validTimes(input.axes.times, input.categories.size)
    val style = env.text.style
    if (times != null) {
        val widest = env.text.measurer.widest(listOf("00:00", "Mar 30", "2030"), style)
        val max = (plot.width / (widest * LABEL_SPACING)).toInt().coerceAtLeast(2)
        val offset = input.axes.offsetMinutes
        val ticks = timeTicks(times.first(), times.last(), max, offset)
        val scale = LinearScale(times.first().toDouble(), times.last().toDouble(), model.xs.first(), model.xs.last())
        return ticks.ticks.map { tick ->
            val at = Offset(scale.map(tick.toDouble()), below)
            AxisLabel(formatChartTime(tick, ticks.step, offset), at, LabelSide.Below)
        }
    }
    val widest = env.text.measurer.widest(input.categories, style) * LABEL_SPACING
    val slot = if (model.xs.size < 2) plot.width else plot.width / (model.xs.size - 1)
    val step = ceil(widest / slot).toInt().coerceAtLeast(1)
    return input.categories.indices.filter { it % step == 0 }
        .map { AxisLabel(input.categories[it], Offset(model.xs[it], below), LabelSide.Below) }
}

@Composable
private fun LinePlot(input: LineInput, style: ResolvedChartStyle, plotHeight: Dp) {
    val env = rememberPlotEnv()
    val state = remember { PlotInput() }
    val progress = rememberDrawIn(input.series to input.kind, style)
    BoxWithConstraints(Modifier.fillMaxWidth().height(plotHeight)) {
        val scene = lineScene(input, Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()), env)
        val xs = scene.model.xs
        val hovered = state.pointer?.let { nearestIndex(xs, it.x) }
        val active = if (input.markers == MarkerVisibility.Never) null else hovered ?: state.walk?.first
        val announce = state.walk?.let { (c, s) -> pointText(input, c, s) }
        Box(
            Modifier
                .fillMaxSize()
                .plotInput(state, input.categories.size, input.series.size, announce)
                .drawBehind { drawLineScene(scene, input, env, style, active, state.walk?.second, progress) },
        ) {
            if (active != null) ChartTooltip(input.categories[active], tooltipRows(input, active), xs[active])
        }
    }
}

private fun DrawScope.drawLineScene(
    scene: LineScene,
    input: LineInput,
    env: PlotEnv,
    style: ResolvedChartStyle,
    active: Int?,
    focusedSeries: Int?,
    progress: Float,
) {
    val plot = scene.layout.plot
    val ticks = scene.model.domain.ticks.map(scene.model.yScale::map)
    drawGrid(plot, ticks, vertical = false, style, env.colors.outlineVariant, env.colors.outline)
    drawAxisLabels(env.text.measurer, scene.labels, env.text.style)
    if (active != null) {
        val x = scene.model.xs[active]
        drawLine(env.colors.outline, Offset(x, plot.top), Offset(x, plot.bottom))
    }
    val always = input.markers == MarkerVisibility.Always
    val spec = LineChartSpec(plot, scene.model.specs, input.curve, always, active, focusedSeries, progress)
    env.renderer.drawLineChart(this, spec, style)
}

// What the pointer or focus points at: the category's values, and for stacked areas their total.
private fun tooltipRows(input: LineInput, category: Int): List<TooltipRow> {
    val format = input.axes.y.format
    val rows = input.series.mapIndexedNotNull { index, item ->
        item.values.getOrNull(category)?.let { TooltipRow(input.colors[index], "${item.name}: ${format(it)}") }
    }
    if (input.kind != LineKind.StackedArea) return rows
    val total = input.series.sumOf { maxOf(0.0, it.values.getOrNull(category) ?: 0.0) }
    return rows + TooltipRow(null, "Total: ${format(total)}")
}

private fun pointText(input: LineInput, category: Int, series: Int): String {
    val label = input.categories.getOrElse(category) { "" }
    val item = input.series.getOrNull(series) ?: return label
    return "$label, ${item.name}, ${item.values.getOrNull(category)?.let(input.axes.y.format) ?: "no data"}"
}
