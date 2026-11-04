package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private const val FULL_TURN = 360f
private const val HALF_TURN = 180.0

/** A slice of a pie: its [label], [value] and optional [color]. A negative value counts as zero. */
public data class PieSlice(
    val label: String,
    val value: Double,
    val color: Color = Color.Unspecified,
)

/**
 * The (start, sweep) angle in degrees of each value, clockwise from [startAngle] (0 is 3 o'clock, so -90 is the top).
 * Negative values count as zero; with a total of zero every sweep is zero; one value gets the full turn.
 */
internal fun sliceAngles(values: List<Double>, startAngle: Float): List<Pair<Float, Float>> {
    val total = values.sumOf { maxOf(0.0, it) }
    var start = startAngle
    return values.map { value ->
        val sweep = if (total <= 0.0) 0f else (maxOf(0.0, value) / total * FULL_TURN).toFloat()
        (start to sweep).also { start += sweep }
    }
}

/** The index of the slice at [point], between [inner] and [outer] radius around [center]; `null` when none. */
internal fun sliceAt(
    point: Offset,
    center: Offset,
    outer: Float,
    inner: Float,
    angles: List<Pair<Float, Float>>,
): Int? {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val radius = hypot(dx, dy)
    if (radius < inner || radius > outer) return null
    val angle = degrees(atan2(dy.toDouble(), dx.toDouble()))
    return angles.indexOfFirst { (start, sweep) ->
        val relative = (((angle - start) % FULL_TURN) + FULL_TURN) % FULL_TURN
        sweep > 0f && relative < sweep
    }.takeIf { it >= 0 }
}

/** The point at [radius] from [center] in direction [angle] degrees. */
internal fun polar(center: Offset, radius: Float, angle: Float): Offset {
    val radians = angle * PI / HALF_TURN
    return Offset(center.x + (cos(radians) * radius).toFloat(), center.y + (sin(radians) * radius).toFloat())
}

internal fun degrees(radians: Double): Float = (radians * HALF_TURN / PI).toFloat()

private const val PERCENT = 100.0

/** A share as a person reads it: "45%", or "<1%" for a sliver. */
internal fun shareText(share: Double): String {
    val percent = share * PERCENT
    return if (percent > 0.0 && percent < 1.0) "<1%" else "${percent.roundToInt()}%"
}

/** The screen reader text of a pie: "Pie chart, 4 slices, largest: Search 45%". */
internal fun pieSummary(kind: String, slices: List<PieSlice>): String {
    val noun = if (slices.size == 1) "slice" else "slices"
    val head = "$kind, ${slices.size} $noun"
    val total = slices.sumOf { maxOf(0.0, it.value) }
    val largest = slices.maxByOrNull { it.value }
    if (largest == null || total <= 0.0) return head
    return "$head, largest: ${largest.label} ${shareText(maxOf(0.0, largest.value) / total)}"
}
