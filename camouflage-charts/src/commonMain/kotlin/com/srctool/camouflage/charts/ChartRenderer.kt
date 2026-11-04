package com.srctool.camouflage.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect

/** What a sparkline draws: [values] as a [kind] over [range], with [highlight] and the draw-in [progress]. */
public class SparklineSpec(
    public val values: List<Double?>,
    public val kind: SparklineKind,
    public val range: ClosedFloatingPointRange<Double>,
    public val highlight: SparklineHighlight,
    public val color: Color,
    public val progress: Float,
)

/** One series of a line chart: its [upper] edge (`null` is a gap) and, for areas, the [lower] edge it fills down to. */
public class LineSeriesSpec(
    public val color: Color,
    public val upper: List<Offset?>,
    public val lower: List<Offset?>?,
)

/**
 * What a line chart draws inside [plot]: the [series] (areas first, then lines) joined by [curve], a dot on every point
 * when [markers], on [activeCategory] (hovered or focused) and a ring on [focusedSeries]' dot, with the draw-in [progress].
 */
public class LineChartSpec(
    public val plot: Rect,
    public val series: List<LineSeriesSpec>,
    public val curve: LineCurve,
    public val markers: Boolean,
    public val activeCategory: Int?,
    public val focusedSeries: Int?,
    public val progress: Float,
)

/** One bar: its [rect] at full size, its [color] and the [edge] that is its free end (`null` inside a stack). */
public class BarMark(public val rect: Rect, public val color: Color, public val edge: BarEdge?)

/**
 * What a bar chart draws inside [plot]: the [bars], growing from [baseline] (a pixel position along the value axis,
 * `vertical` bars grow up or down, the others left or right) by [progress]; [active] is the bar under the pointer
 * or focus.
 */
public class BarChartSpec(
    public val plot: Rect,
    public val bars: List<BarMark>,
    public val baseline: Float,
    public val vertical: Boolean,
    public val active: Int?,
    public val progress: Float,
)

/**
 * Draws chart marks. The default draws lines, areas and bars from the [ResolvedChartStyle]; supply your own through
 * [CamoChartsProvider] to take over the drawing completely (the frame, axes, legend and tables stay).
 */
@Stable
public interface ChartRenderer {
    /** Draws a sparkline into [scope], which is exactly the sparkline's box. */
    public fun drawSparkline(scope: DrawScope, spec: SparklineSpec, style: ResolvedChartStyle)

    /** Draws the lines, areas and markers of a line chart into [scope], the whole chart box (the grid is drawn). */
    public fun drawLineChart(scope: DrawScope, spec: LineChartSpec, style: ResolvedChartStyle): Unit =
        DefaultChartRenderer.drawLineChart(scope, spec, style)

    /** Draws the bars of a bar chart into [scope], the whole chart box. */
    public fun drawBarChart(scope: DrawScope, spec: BarChartSpec, style: ResolvedChartStyle): Unit =
        DefaultChartRenderer.drawBarChart(scope, spec, style)
}

internal val LocalChartRenderer = compositionLocalOf<ChartRenderer> { DefaultChartRenderer }

/** Makes [renderer] the drawing for every chart in [content]. */
@Composable
public fun CamoChartsProvider(renderer: ChartRenderer, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalChartRenderer provides renderer, content = content)
}

private const val HIGHLIGHT_PAD = 0.5f
private const val HALF_SLOT = 0.5f
private const val DOT_SCALE = 1.5f
private const val FOCUS_RING = 0.3f
private const val DIMMED = 0.55f

/** The default drawing. */
public object DefaultChartRenderer : ChartRenderer {
    override fun drawSparkline(scope: DrawScope, spec: SparklineSpec, style: ResolvedChartStyle): Unit = with(scope) {
        val inset = maxOf(style.lineWidth.toPx(), style.markerSize.toPx()) * HIGHLIGHT_PAD
        val area = Size(size.width - inset * 2, size.height - inset * 2)
        if (area.width <= 0f || area.height <= 0f) return@with
        val points = sparklinePoints(downsample(spec.values, maxOf(1, area.width.toInt())), spec.range, area, inset)
        clipRect(right = size.width * spec.progress) {
            when (spec.kind) {
                SparklineKind.Bars -> drawBars(points, spec, style, inset)
                else -> drawTrend(points, spec, style, inset)
            }
        }
        if (spec.progress >= 1f) drawHighlight(points, spec, style)
    }

    private fun DrawScope.drawTrend(points: List<Offset?>, spec: SparklineSpec, style: ResolvedChartStyle,
        inset: Float) {
        val stroke = Stroke(style.lineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val baseline = size.height - inset
        segments(points).forEach { run ->
            if (run.size == 1) {
                drawCircle(spec.color, style.lineWidth.toPx() * DOT_SCALE / 2 + 1f, run.first())
                return@forEach
            }
            val line = Path().apply {
                moveTo(run.first().x, run.first().y)
                run.drop(1).forEach { lineTo(it.x, it.y) }
            }
            if (spec.kind == SparklineKind.Area) {
                val area = Path().apply {
                    addPath(line)
                    lineTo(run.last().x, baseline)
                    lineTo(run.first().x, baseline)
                    close()
                }
                drawPath(area, spec.color.copy(alpha = style.areaOpacity))
            }
            drawPath(line, spec.color, style = stroke)
        }
    }

    private fun DrawScope.drawBars(points: List<Offset?>, spec: SparklineSpec, style: ResolvedChartStyle,
        inset: Float) {
        val gap = style.barGap.toPx()
        val slot = (size.width - inset * 2) / maxOf(1, points.size)
        val width = maxOf(1f, slot - gap)
        val baseline = size.height - inset
        points.forEachIndexed { index, point ->
            point ?: return@forEachIndexed
            val bar = Offset(inset + (index + HALF_SLOT) * slot - width / 2, point.y)
            drawRoundRect(
                spec.color, bar, Size(width, maxOf(1f, baseline - point.y)),
                androidx.compose.ui.geometry.CornerRadius(style.barRadius.toPx()),
            )
        }
    }

    private fun DrawScope.drawHighlight(points: List<Offset?>, spec: SparklineSpec, style: ResolvedChartStyle) {
        val shown = when (spec.highlight) {
            SparklineHighlight.None -> emptyList()
            SparklineHighlight.Last -> listOfNotNull(points.lastOrNull { it != null })
            SparklineHighlight.Extremes -> extremes(points)
        }
        shown.forEach { drawCircle(spec.color, style.markerSize.toPx() / 2, it) }
    }

    override fun drawLineChart(scope: DrawScope, spec: LineChartSpec, style: ResolvedChartStyle): Unit = with(scope) {
        val reveal = if (spec.progress >= 1f) size.width else spec.plot.left + spec.plot.width * spec.progress
        clipRect(left = 0f, top = 0f, right = reveal, bottom = size.height) {
            spec.series.forEach { series ->
                val lower = series.lower ?: return@forEach
                gapRuns(series.upper).filter { it.count() > 1 }.forEach { run ->
                    val top = run.map { series.upper[it]!! }
                    val bottom = run.map { lower[it]!! }.asReversed()
                    val area = Path().apply {
                        addCurve(top, spec.curve)
                        addCurve(bottom, spec.curve, connect = true)
                        close()
                    }
                    drawPath(area, series.color.copy(alpha = style.areaOpacity))
                }
            }
            val stroke = Stroke(style.lineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            spec.series.forEach { series ->
                gapRuns(series.upper).forEach { run ->
                    val points = run.map { series.upper[it]!! }
                    if (points.size == 1) {
                        drawCircle(series.color, style.lineWidth.toPx() * DOT_SCALE / 2 + 1f, points.first())
                    } else {
                        drawPath(Path().apply { addCurve(points, spec.curve) }, series.color, style = stroke)
                    }
                }
            }
            drawMarkers(spec, style)
        }
    }

    private fun DrawScope.drawMarkers(spec: LineChartSpec, style: ResolvedChartStyle) {
        val radius = style.markerSize.toPx() / 2
        spec.series.forEachIndexed { seriesIndex, series ->
            series.upper.forEachIndexed { index, point ->
                point ?: return@forEachIndexed
                val active = index == spec.activeCategory
                if (!spec.markers && !active) return@forEachIndexed
                if (active && seriesIndex == spec.focusedSeries) {
                    drawCircle(series.color.copy(alpha = FOCUS_RING), radius * 2, point)
                }
                drawCircle(series.color, radius, point)
            }
        }
    }

    override fun drawBarChart(scope: DrawScope, spec: BarChartSpec, style: ResolvedChartStyle): Unit = with(scope) {
        val radius = style.barRadius.toPx()
        spec.bars.forEachIndexed { index, bar ->
            val rect = grown(bar.rect, spec)
            val dim = spec.active != null && spec.active != index
            val color = if (dim) bar.color.copy(alpha = DIMMED) else bar.color
            val round = minOf(radius, rect.width / 2, rect.height / 2)
            if (bar.edge == null || round <= 0f) {
                drawRect(color, rect.topLeft, rect.size)
            } else {
                drawPath(Path().apply { addRoundRect(roundedAt(rect, bar.edge, round)) }, color)
            }
        }
    }

    // The rect between the baseline and its full size, by the draw-in progress.
    private fun grown(rect: Rect, spec: BarChartSpec): Rect {
        fun at(edge: Float) = spec.baseline + (edge - spec.baseline) * spec.progress
        return if (spec.vertical) {
            Rect(rect.left, at(rect.top), rect.right, at(rect.bottom))
        } else {
            Rect(at(rect.left), rect.top, at(rect.right), rect.bottom)
        }
    }

    private fun roundedAt(rect: Rect, edge: BarEdge, radius: Float): RoundRect {
        val corner = CornerRadius(radius)
        val none = CornerRadius.Zero
        return when (edge) {
            BarEdge.Top -> RoundRect(rect, corner, corner, none, none)
            BarEdge.Bottom -> RoundRect(rect, none, none, corner, corner)
            BarEdge.Left -> RoundRect(rect, corner, none, none, corner)
            BarEdge.Right -> RoundRect(rect, none, corner, corner, none)
        }
    }
}
