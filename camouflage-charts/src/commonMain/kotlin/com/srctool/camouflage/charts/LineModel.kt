package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

/** The points of a line chart, ready to draw: a [LineSeriesSpec] per visible series over the positions [xs]. */
internal class LineModel(
    val domain: ValueDomain,
    val yScale: LinearScale,
    val xs: List<Float>,
    val specs: List<LineSeriesSpec>,
)

internal fun validTimes(times: List<Long>?, count: Int): List<Long>? =
    times?.takeIf { it.size == count && count > 1 && it.zipWithNext().all { (a, b) -> a < b } }

/** The x position of each category: evenly spread, or by [times] (epoch milliseconds, strictly ascending). */
internal fun categoryPositions(plot: Rect, count: Int, times: List<Long>?): List<Float> {
    val ascending = validTimes(times, count)
    return when {
        count == 0 -> emptyList()
        ascending != null -> {
            val scale = LinearScale(ascending.first().toDouble(), ascending.last().toDouble(), plot.left, plot.right)
            ascending.map { scale.map(it.toDouble()) }
        }
        count == 1 -> listOf(plot.center.x)
        else -> List(count) { plot.left + plot.width * it / (count - 1) }
    }
}

/** The value axis for [values] (one list per visible series) drawn as [kind], stacked into [bands] when given. */
internal fun lineDomain(
    kind: LineKind,
    values: List<List<Double?>>,
    bands: List<List<StackBand?>>?,
    axis: ChartAxis,
): ValueDomain {
    val flat = bands?.flatMap { row -> row.mapNotNull { it?.top } } ?: values.flatMap { it.filterNotNull() }
    return valueDomain(flat.minOrNull(), flat.maxOrNull(), includeZero = kind != LineKind.Line, axis = axis)
}

/** The drawing model: [values] per visible series placed on [xs] and the [domain] in [plotArea]. */
internal fun lineModel(
    kind: LineKind,
    plotArea: Rect,
    values: List<List<Double?>>,
    bands: List<List<StackBand?>>?,
    colors: List<Color>,
    domain: ValueDomain,
    xs: List<Float>,
): LineModel {
    val yScale = LinearScale(domain.low, domain.high, plotArea.bottom, plotArea.top)
    val baseline = yScale.map(0.0.coerceIn(domain.low, domain.high))
    val specs = values.indices.map { s ->
        val upper = xs.indices.map { c ->
            val top = if (bands != null) bands[s][c]?.top else values[s].getOrNull(c)
            top?.let { Offset(xs[c], yScale.map(it)) }
        }
        val lower = when (kind) {
            LineKind.Line -> null
            LineKind.Area -> upper.map { point -> point?.let { Offset(it.x, baseline) } }
            LineKind.StackedArea -> xs.indices.map { c -> bands!![s][c]?.let { Offset(xs[c], yScale.map(it.base)) } }
        }
        LineSeriesSpec(colors[s], upper, lower)
    }
    return LineModel(domain, yScale, xs, specs)
}
