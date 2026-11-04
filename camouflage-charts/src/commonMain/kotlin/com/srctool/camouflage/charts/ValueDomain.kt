package com.srctool.camouflage.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

/** The value axis of a chart: nice [ticks] and the [low]..[high] range they (or the axis' own bounds) give. */
internal class ValueDomain(val ticks: List<Double>, val low: Double, val high: Double) {
    val range: ClosedFloatingPointRange<Double> get() = low..high
}

/**
 * The value axis for data between [dataLow] and [dataHigh] (`null` for no data): the bounds [axis] fixes, else nice
 * ticks around the data, widened to include zero when [includeZero].
 */
internal fun valueDomain(dataLow: Double?, dataHigh: Double?, includeZero: Boolean, axis: ChartAxis): ValueDomain {
    var low = dataLow ?: 0.0
    var high = dataHigh ?: 1.0
    if (includeZero) {
        low = minOf(low, 0.0)
        high = maxOf(high, 0.0)
    }
    val ticks = niceTicks(axis.min ?: low, axis.max ?: high, axis.tickCount)
    val from = axis.min ?: ticks.first()
    val to = axis.max ?: ticks.last()
    return ValueDomain(ticks.filter { it in from..to }, from, to)
}

/** The color of series [index]: its own, else the style's palette entry. */
internal fun seriesColors(series: List<ChartSeries>, style: ResolvedChartStyle): List<Color> =
    series.mapIndexed { index, item -> item.color.takeOrElse { style.seriesColor(index) } }

/** [this] with [index] removed when present, added when not. */
internal fun Set<Int>.toggled(index: Int): Set<Int> = if (index in this) this - index else this + index

/** True when there is nothing to draw: no categories, or every value is a gap. */
internal fun noChartData(categories: List<String>, series: List<ChartSeries>): Boolean =
    categories.isEmpty() || series.all { item -> item.values.all { it == null } }

/**
 * The screen reader text of a chart: "Line chart, 2 series, 12 categories", then each series' trend through
 * [sparklineSummary] ("Sales: from 3 to 9, up 200%").
 */
internal fun chartSummary(kind: String, categories: List<String>, series: List<ChartSeries>): String {
    val noun = if (categories.size == 1) "category" else "categories"
    val head = "$kind, ${series.size} series, ${categories.size} $noun"
    if (series.isEmpty()) return head
    return head + ". " + series.joinToString("; ") { sparklineSummary(it.values, it.name) }
}
