package com.srctool.camouflage.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
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

/**
 * Draws chart marks. The default draws lines, areas and bars from the [ResolvedChartStyle]; supply your own through
 * [CamoChartsProvider] to take over the drawing completely (the frame, axes, legend and tables stay).
 */
@Stable
public interface ChartRenderer {
    /** Draws a sparkline into [scope], which is exactly the sparkline's box. */
    public fun drawSparkline(scope: DrawScope, spec: SparklineSpec, style: ResolvedChartStyle)
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
}
