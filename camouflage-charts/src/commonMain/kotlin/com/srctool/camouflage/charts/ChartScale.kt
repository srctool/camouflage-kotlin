package com.srctool.camouflage.charts

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val ROUND_ONE = 1.5
private const val ROUND_TWO = 3.0
private const val ROUND_FIVE = 7.0
private const val CEIL_ONE = 1.0
private const val CEIL_TWO = 2.0
private const val CEIL_FIVE = 5.0
private const val TEN = 10.0
private const val WIDEN = 0.1

// The nearest "nice" step (1, 2, 5 times a power of ten) to [range].
private fun niceNumber(range: Double, round: Boolean): Double {
    val exponent = floor(log10(range))
    val fraction = range / TEN.pow(exponent)
    val nice = if (round) {
        when {
            fraction < ROUND_ONE -> 1.0
            fraction < ROUND_TWO -> 2.0
            fraction < ROUND_FIVE -> 5.0
            else -> TEN
        }
    } else {
        when {
            fraction <= CEIL_ONE -> 1.0
            fraction <= CEIL_TWO -> 2.0
            fraction <= CEIL_FIVE -> 5.0
            else -> TEN
        }
    }
    return nice * TEN.pow(exponent)
}

/**
 * Tick values for an axis over [min]..[max], about [count] of them, at steps of 1, 2 or 5 times a power of ten.
 * The first tick is at or below [min] and the last at or above [max]. A zero-height range widens around its value.
 */
public fun niceTicks(min: Double, max: Double, count: Int = DEFAULT_TICK_COUNT): List<Double> {
    require(count >= 2) { "A scale needs at least 2 ticks" }
    var low = minOf(min, max)
    var high = maxOf(min, max)
    if (low == high) {
        val pad = if (low == 0.0) 1.0 else abs(low) * WIDEN
        low -= pad
        high += pad
    }
    val range = niceNumber(high - low, round = false)
    val step = niceNumber(range / (count - 1), round = true)
    val first = floor(low / step) * step
    val last = ceil(high / step) * step
    val ticks = ArrayList<Double>()
    var index = 0
    while (first + index * step <= last + step / TEN) {
        // Multiplying (not adding) keeps 0.1 + 0.2 style drift out of the ticks.
        ticks += first + index * step
        index++
    }
    return ticks
}

/** A linear map from a data domain to a pixel range; the range may run backwards (a y axis does). */
public class LinearScale(
    private val domainStart: Double,
    private val domainEnd: Double,
    private val rangeStart: Float,
    private val rangeEnd: Float,
) {
    /** The pixel position of [value]. */
    public fun map(value: Double): Float {
        val span = domainEnd - domainStart
        val t = if (span == 0.0) 0.0 else (value - domainStart) / span
        return (rangeStart + (rangeEnd - rangeStart) * t).toFloat()
    }

    /** The data value at pixel [position]. */
    public fun invert(position: Float): Double {
        val span = rangeEnd - rangeStart
        val t = if (span == 0f) 0.0 else ((position - rangeStart) / span).toDouble()
        return domainStart + (domainEnd - domainStart) * t
    }
}
