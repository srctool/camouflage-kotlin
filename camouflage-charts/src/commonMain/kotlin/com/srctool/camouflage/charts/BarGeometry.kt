package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Which way bars run. */
public enum class BarOrientation { Vertical, Horizontal }

/** How the series of a category sit: side by side or on top of each other. */
public enum class BarMode { Grouped, Stacked }

/** The side of a bar that is its free end (the one that gets the radius). */
public enum class BarEdge { Top, Bottom, Left, Right }

/** One bar: the [series]' [value] in [category], its [rect], and the free [end] edge (`null` inside a stack). */
internal class BarRect(
    val category: Int,
    val series: Int,
    val value: Double,
    val rect: Rect,
    val end: BarEdge?,
)

private const val MIN_BAR = 1f

// Where bars go along the category axis and the value axis, for one chart.
private class BarLayout(
    val plot: Rect,
    val vertical: Boolean,
    val categories: Int,
    val groupGap: Float,
    domain: ClosedFloatingPointRange<Double>,
) {
    val bandLength = (if (vertical) plot.width else plot.height) / categories
    val groupLength = bandLength / (1f + groupGap)
    private val valueLength = if (vertical) plot.height else plot.width
    private val start = domain.start
    private val extent = domain.endInclusive - domain.start
    val zero = position(0.0.coerceIn(domain.start, domain.endInclusive))

    // A value's distance from the plot's start of the value axis (the bottom for vertical bars).
    fun position(value: Double): Float {
        val t = if (extent == 0.0) 0.0 else (value - start) / extent
        return (t * valueLength).toFloat()
    }

    // Where a group starts along the category axis.
    fun groupStart(category: Int): Float = category * bandLength + (bandLength - groupLength) / 2f

    // A bar covering [along] on the value axis and [width] from [start] on the category axis.
    fun bar(category: Int, series: Int, value: Double, along: ClosedFloatingPointRange<Float>, start: Float,
        width: Float, free: Boolean): BarRect {
        val end = endOf(vertical, value >= 0, free)
        val low = along.start
        val high = along.endInclusive
        val rect = if (vertical) {
            Rect(plot.left + start, plot.bottom - high, plot.left + start + width, plot.bottom - low)
        } else {
            Rect(plot.left + low, plot.top + start, plot.left + high, plot.top + start + width)
        }
        return BarRect(category, series, value, rect, end)
    }
}

private fun endOf(vertical: Boolean, positive: Boolean, free: Boolean): BarEdge? = when {
    !free -> null
    vertical -> if (positive) BarEdge.Top else BarEdge.Bottom
    else -> if (positive) BarEdge.Right else BarEdge.Left
}

/**
 * The bars of a bar chart: [series] holds one list of values per visible series, one value per category, drawn in
 * [plot] against [domain]. Grouped bars sit side by side, each [barGap] apart; stacked bars pile positives up and
 * negatives down, separately. [groupGap] is the space between groups as a share of a group. Bars grow from the zero
 * baseline (or the nearest edge when zero is outside [domain]). With [rtl] the bars mirror: vertical bars run from
 * the right, horizontal bars start at the right edge.
 */
internal fun barRects(
    plot: Rect,
    categories: Int,
    series: List<List<Double?>>,
    mode: BarMode,
    orientation: BarOrientation,
    domain: ClosedFloatingPointRange<Double>,
    barGap: Float,
    groupGap: Float,
    rtl: Boolean,
): List<BarRect> {
    if (categories == 0 || series.isEmpty()) return emptyList()
    val layout = BarLayout(plot, orientation == BarOrientation.Vertical, categories, groupGap, domain)
    val bars = (0 until categories).flatMap { category ->
        if (mode == BarMode.Grouped) {
            groupedBars(layout, category, series, barGap)
        } else {
            stackedBars(layout, category, series)
        }
    }
    return if (rtl) bars.map { mirrored(it, plot) } else bars
}

private fun groupedBars(layout: BarLayout, category: Int, series: List<List<Double?>>, barGap: Float): List<BarRect> {
    val width = maxOf(MIN_BAR, (layout.groupLength - (series.size - 1) * barGap) / series.size)
    return series.mapIndexedNotNull { index, values ->
        val value = values.getOrNull(category) ?: return@mapIndexedNotNull null
        val start = layout.groupStart(category) + index * (width + barGap)
        val span = minOf(layout.zero, layout.position(value))..maxOf(layout.zero, layout.position(value))
        layout.bar(category, index, value, span, start, width, free = true)
    }
}

private fun stackedBars(layout: BarLayout, category: Int, series: List<List<Double?>>): List<BarRect> {
    val present = series.mapIndexedNotNull { index, values -> values.getOrNull(category)?.let { index to it } }
    val lastUp = present.lastOrNull { it.second >= 0 }?.first
    val lastDown = present.lastOrNull { it.second < 0 }?.first
    var up = layout.zero
    var down = layout.zero
    return present.map { (index, value) ->
        val size = layout.position(value) - layout.zero
        val from = if (value >= 0) up else down
        val to = from + size
        if (value >= 0) up = to else down = to
        val free = index == (if (value >= 0) lastUp else lastDown)
        layout.bar(category, index, value, minOf(from, to)..maxOf(from, to), layout.groupStart(category),
            layout.groupLength, free)
    }
}

private fun mirrored(bar: BarRect, plot: Rect): BarRect {
    val rect = bar.rect
    val flipped = Rect(plot.left + plot.right - rect.right, rect.top, plot.left + plot.right - rect.left, rect.bottom)
    val end = when (bar.end) {
        BarEdge.Left -> BarEdge.Right
        BarEdge.Right -> BarEdge.Left
        else -> bar.end
    }
    return BarRect(bar.category, bar.series, bar.value, flipped, end)
}

/**
 * The bar the pointer is at: the one whose column (row, for horizontal bars) is nearest to [point] across the
 * category axis, and among those the nearest along the value axis. `null` when there are no bars.
 */
internal fun nearestBar(bars: List<BarRect>, point: Offset, vertical: Boolean): BarRect? =
    bars.minWithOrNull(compareBy({ across(it.rect, point, vertical) }, { along(it.rect, point, vertical) }))

private fun across(rect: Rect, point: Offset, vertical: Boolean): Float =
    if (vertical) gap(rect.left, rect.right, point.x) else gap(rect.top, rect.bottom, point.y)

private fun along(rect: Rect, point: Offset, vertical: Boolean): Float =
    if (vertical) gap(rect.top, rect.bottom, point.y) else gap(rect.left, rect.right, point.x)

private fun gap(low: Float, high: Float, at: Float): Float = maxOf(low - at, 0f, at - high)
