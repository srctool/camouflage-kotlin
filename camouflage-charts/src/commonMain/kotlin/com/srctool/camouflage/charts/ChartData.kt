package com.srctool.camouflage.charts

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToLong

/** One data series: a [name] for the legend, one value per category (`null` is a gap) and an optional [color]. */
@Immutable
public data class ChartSeries(
    val name: String,
    val values: List<Double?>,
    val color: Color = Color.Unspecified,
)

/** An axis: an optional [title], a fixed [min] or [max] (`null` reads the data), [tickCount] and tick [format]. */
@Immutable
public data class ChartAxis(
    val title: String? = null,
    val min: Double? = null,
    val max: Double? = null,
    val tickCount: Int = DEFAULT_TICK_COUNT,
    val format: (Double) -> String = ::formatChartNumber,
)

/** One legend entry; the legend toggles [visible] through the frame's callback. */
@Immutable
public data class LegendItem(val label: String, val color: Color, val visible: Boolean = true)

/** How many ticks an axis aims for unless it says otherwise. */
public const val DEFAULT_TICK_COUNT: Int = 5

private const val TENTH = 10.0
private const val WHOLE_LIMIT = 1e15

/** A number as a person reads it: whole numbers without a decimal point, otherwise one decimal. */
public fun formatChartNumber(value: Double): String = when {
    value.isNaN() || value.isInfinite() -> "$value"
    abs(value) >= WHOLE_LIMIT -> value.toLong().toString()
    else -> {
        val tenths = (abs(value) * TENTH).roundToLong()
        val sign = if (value < 0 && tenths != 0L) "-" else ""
        val whole = tenths / TENTH.toLong()
        val fraction = tenths % TENTH.toLong()
        if (fraction == 0L) "$sign$whole" else "$sign$whole.$fraction"
    }
}
