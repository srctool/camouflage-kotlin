package com.srctool.camouflage.charts

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultPieHeight = 220.dp
private const val LABEL_RADIUS = 0.62f
private const val LIGHT_TEXT = 0.5f
private const val SELECTED_ROOM = 2

/** The angle of 12 o'clock: slices start there by default. */
public const val TOP: Float = -90f

// Everything the pie draws for one size: the circle, the slices that are shown and their angles.
internal class PieScene(
    val center: Offset,
    val outer: Float,
    val inner: Float,
    val angles: List<Pair<Float, Float>>,
)

internal class PieInput(
    val slices: List<PieSlice>,
    val shown: List<Int>,
    val colors: List<Color>,
    val donut: Boolean,
    val startAngle: Float,
    val selected: Int?,
    val format: (Double) -> String,
) {
    val total: Double = shown.sumOf { maxOf(0.0, slices[it].value) }
    fun share(index: Int): Double = if (total <= 0.0) 0.0 else maxOf(0.0, slices[index].value) / total
}

/**
 * Parts of a whole, as a pie or (with [donut]) a ring with [center] content in the hole. Each slice shows its share
 * when it is wide enough ([ResolvedChartStyle.labelThreshold]); the legend lists every slice with its value and
 * share and hides or shows slices. The app owns the selection ([selected], [onSelect]): clicking a slice, or Enter
 * or Space on the focused one, calls [onSelect] with its index (`null` when it was already selected), and the selected
 * slice moves out. The plot is one image node; focusing it walks the slices clockwise (Left, Right, Up and Down) and
 * announces "Search, 45%, 4,500". Negative values count as zero; with every value zero the empty state shows. Angles
 * keep their clockwise direction in right-to-left layouts.
 */
@Composable
public fun CamoPieChart(
    slices: List<PieSlice>,
    title: String,
    modifier: Modifier = Modifier,
    donut: Boolean = false,
    center: (@Composable () -> Unit)? = null,
    selected: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    startAngle: Float = TOP,
    valueFormat: (Double) -> String = ::formatChartNumber,
    loading: Boolean = false,
    empty: (@Composable () -> Unit)? = null,
    showTable: Boolean = false,
    plotHeight: Dp = DefaultPieHeight,
    style: ChartStyle? = null,
) {
    val resolved = rememberChartStyle(style)
    var hidden by remember(slices.size) { mutableStateOf(setOf<Int>()) }
    var table by remember(showTable) { mutableStateOf(showTable) }
    val colors = slices.mapIndexed { index, slice -> slice.color.takeOrElse { resolved.seriesColor(index) } }
    val shown = slices.indices.filter { it !in hidden }
    val input = PieInput(slices, shown, colors, donut, startAngle, selected, valueFormat)
    val legend = slices.mapIndexed { index, slice ->
        val share = if (index in hidden) "" else ", ${shareText(input.share(index))}"
        LegendItem("${slice.label}: ${valueFormat(slice.value)}$share", colors[index], index !in hidden)
    }
    CamoChartFrame(
        title = title,
        summary = pieSummary(if (donut) "Donut chart" else "Pie chart", slices),
        modifier = modifier,
        legend = legend,
        onLegendToggle = { hidden = hidden.toggled(it) },
        loading = loading,
        isEmpty = slices.isEmpty() || slices.all { it.value <= 0.0 },
        empty = empty,
        plotHeight = plotHeight,
        table = {
            val values = ChartSeries("Value", slices.map { maxOf(0.0, it.value) })
            CamoChartDataTable(slices.map { it.label }, listOf(values), "$title data", format = valueFormat)
        },
        showTable = table,
        onShowTableChange = { table = it },
    ) {
        PiePlot(input, resolved, plotHeight, center, onSelect)
    }
}

private fun pieScene(input: PieInput, width: Float, height: Float, style: ResolvedChartStyle, env: PlotEnv): PieScene {
    val room = with(env.density) { style.selectedOffset.toPx() } * SELECTED_ROOM
    val outer = maxOf(0f, minOf(width, height) / 2 - env.pad - room)
    val inner = if (input.donut) outer * (1f - style.donutThickness) else 0f
    val angles = sliceAngles(input.shown.map { input.slices[it].value }, input.startAngle)
    return PieScene(Offset(width / 2, height / 2), outer, inner, angles)
}

@Composable
private fun PiePlot(
    input: PieInput,
    style: ResolvedChartStyle,
    plotHeight: Dp,
    center: (@Composable () -> Unit)?,
    onSelect: ((Int?) -> Unit)?,
) {
    val env = rememberPlotEnv()
    val state = remember { PlotInput() }
    val progress = rememberDrawIn(input.slices.map { it.value } to input.shown, style)
    BoxWithConstraints(Modifier.fillMaxWidth().height(plotHeight)) {
        val scene = pieScene(input, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), style, env)
        val hover = state.pointer?.let { sliceAt(it, scene.center, scene.outer, scene.inner, scene.angles) }
        val walk = state.walk?.first?.takeIf { it in input.shown.indices }
        val active = hover ?: walk
        val toggle: (Int) -> Unit = { shownIndex ->
            val index = input.shown[shownIndex]
            onSelect?.invoke(if (index == input.selected) null else index)
        }
        val announce = walk?.let { pieText(input, input.shown[it]) }
        val tap = rememberUpdatedState(scene)
        val toggleNow = rememberUpdatedState(toggle)
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // The scene is rebuilt on every hover: read it fresh instead of restarting the gesture.
                    detectTapGestures { point ->
                        val now = tap.value
                        sliceAt(point, now.center, now.outer, now.inner, now.angles)?.let(toggleNow.value)
                    }
                }
                .plotInput(state, input.shown.size, 1, announce, linear = true, onActivate = { walk?.let(toggle) })
                .drawBehind { drawPieScene(scene, input, env, style, active, progress) },
            Alignment.Center,
        ) {
            if (center != null && input.donut && scene.inner > 0f) {
                val hole = with(env.density) { (scene.inner * 2).toDp() }
                Box(Modifier.size(hole), Alignment.Center) { center() }
            }
            if (active != null) {
                val index = input.shown[active]
                val (start, sweep) = scene.angles[active]
                val anchor = polar(scene.center, (scene.outer + scene.inner) / 2, start + sweep / 2)
                val row = TooltipRow(input.colors[index], "${input.format(input.slices[index].value)} " +
                    "(${shareText(input.share(index))})")
                ChartTooltip(input.slices[index].label, listOf(row), anchor.x)
            }
        }
    }
}

private fun DrawScope.drawPieScene(
    scene: PieScene,
    input: PieInput,
    env: PlotEnv,
    style: ResolvedChartStyle,
    active: Int?,
    progress: Float,
) {
    val marks = scene.angles.mapIndexed { i, (start, sweep) ->
        PieSliceMark(start, sweep, input.colors[input.shown[i]])
    }
    val selected = input.selected?.let { input.shown.indexOf(it) }?.takeIf { it >= 0 }
    val spec = PieChartSpec(scene.center, scene.outer, scene.inner, marks, selected, active, progress)
    env.renderer.drawPieChart(this, spec, style)
    if (progress < 1f) return
    val radius = if (input.donut) (scene.outer + scene.inner) / 2 else scene.outer * LABEL_RADIUS
    scene.angles.forEachIndexed { i, (start, sweep) ->
        val share = input.share(input.shown[i])
        if (sweep <= 0f || share < style.labelThreshold) return@forEachIndexed
        val offset = if (i == selected) style.selectedOffset.toPx() else 0f
        val middle = start + sweep / 2
        val at = polar(polar(scene.center, offset, middle), radius, middle)
        val color = marks[i].color
        val text = if (color.luminance() > LIGHT_TEXT) Color.Black else Color.White
        val label = AxisLabel(shareText(share), at, LabelSide.Center)
        drawAxisLabels(env.text.measurer, listOf(label), env.text.style.withColor(text))
    }
}

private fun TextStyle.withColor(color: Color): TextStyle = copy(color = color)

private fun pieText(input: PieInput, index: Int): String {
    val slice = input.slices[index]
    val selected = if (index == input.selected) ", selected" else ""
    return "${slice.label}, ${shareText(input.share(index))}, ${input.format(slice.value)}$selected"
}
