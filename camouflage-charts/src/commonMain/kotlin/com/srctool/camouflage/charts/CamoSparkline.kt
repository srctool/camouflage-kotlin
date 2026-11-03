package com.srctool.camouflage.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import kotlin.math.abs
import kotlin.math.roundToLong

/** The form of a sparkline. */
public enum class SparklineKind { Line, Area, Bars }

/** Which points of a sparkline get a dot. */
public enum class SparklineHighlight { None, Last, Extremes }

private val DefaultWidth = 100.dp
private val DefaultHeight = 28.dp
private const val PERCENT = 100.0

/**
 * A small trend with no axes, grid or legend, for a row or a card. It sizes to [modifier] (100 x 28 dp when that
 * doesn't say), scales to the data or to [range], and breaks the line at `null` values. A sparkline is one image
 * node whose text is [sparklineSummary]: [description] names it ("Signups, last 30 days") and the text adds the
 * first and last values and the change, so the direction is never only a color. It is not focusable.
 */
@Composable
public fun CamoSparkline(
    values: List<Double?>,
    modifier: Modifier = Modifier,
    kind: SparklineKind = SparklineKind.Line,
    range: ClosedFloatingPointRange<Double>? = null,
    highlight: SparklineHighlight = SparklineHighlight.Last,
    tone: Tone = Tone.Default,
    description: String? = null,
    style: ChartStyle? = null,
) {
    val resolved = rememberChartStyle(style)
    val renderer = LocalChartRenderer.current
    val color = Camouflage.colors.family(tone).strong
    val motion = Camouflage.motion
    val progress = remember { Animatable(0f) }
    val animate = resolved.animate && motion.durationLong > 0
    LaunchedEffect(values, animate) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(motion.durationLong, easing = motion.easingStandard))
        } else {
            progress.snapTo(1f)
        }
    }
    val domain = range ?: dataRange(values, includeZero = kind == SparklineKind.Bars)
    val summary = sparklineSummary(values, description)
    Spacer(
        modifier
            .semantics { role = Role.Image; contentDescription = summary }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = if (constraints.hasFixedWidth) constraints.maxWidth else {
                    constraints.constrainWidth(DefaultWidth.roundToPx())
                }
                val height = if (constraints.hasFixedHeight) constraints.maxHeight else {
                    constraints.constrainHeight(DefaultHeight.roundToPx())
                }
                layout(width, height) { placeable.place(0, 0) }
            }
            .drawBehind {
                val spec = SparklineSpec(values, kind, domain, highlight, color, progress.value)
                renderer.drawSparkline(this, spec, resolved)
            },
    )
}

// The data's own extent; bars always include zero so they grow from a baseline.
internal fun dataRange(values: List<Double?>, includeZero: Boolean = false): ClosedFloatingPointRange<Double> {
    val present = values.filterNotNull()
    if (present.isEmpty()) return 0.0..1.0
    val low = present.min().let { if (includeZero) minOf(it, 0.0) else it }
    val high = present.max().let { if (includeZero) maxOf(it, 0.0) else it }
    return low..high
}

/** The text a screen reader gets for a sparkline: "Trend from 3 to 9, up 200%", led by [description] when given. */
public fun sparklineSummary(values: List<Double?>, description: String? = null): String {
    val present = values.filterNotNull()
    val body = when {
        present.isEmpty() -> "no data"
        present.size == 1 -> "at ${formatChartNumber(present.first())}"
        else -> "from ${formatChartNumber(present.first())} to ${formatChartNumber(present.last())}, " +
            changeText(present.first(), present.last())
    }
    return if (description == null) "Trend $body" else "$description: $body"
}

private fun changeText(first: Double, last: Double): String = when {
    last == first -> "flat"
    else -> {
        val direction = if (last > first) "up" else "down"
        if (first == 0.0) {
            "$direction by ${formatChartNumber(abs(last - first))}"
        } else {
            "$direction ${(abs(last - first) / abs(first) * PERCENT).roundToLong()}%"
        }
    }
}

/**
 * Reduces [values] to about [buckets] columns, keeping each column's lowest and highest value (in their original
 * order) so a peak survives; a column of only gaps stays a gap.
 */
internal fun downsample(values: List<Double?>, buckets: Int): List<Double?> {
    if (values.size <= buckets * 2) return values
    val result = ArrayList<Double?>()
    for (bucket in 0 until buckets) {
        val from = bucket * values.size / buckets
        val to = (bucket + 1) * values.size / buckets
        val slice = (from until to).filter { values[it] != null }
        if (slice.isEmpty()) {
            result += null
            continue
        }
        val low = slice.minBy { values[it]!! }
        val high = slice.maxBy { values[it]!! }
        listOf(low, high).distinct().sorted().forEach { result += values[it] }
    }
    return result
}

/** The pixel position of each value inside [area] (inset by [inset]); `null` stays a gap. */
internal fun sparklinePoints(
    values: List<Double?>,
    range: ClosedFloatingPointRange<Double>,
    area: Size,
    inset: Float,
): List<Offset?> {
    val span = range.endInclusive - range.start
    return values.mapIndexed { index, value ->
        value ?: return@mapIndexed null
        val x = if (values.size == 1) inset + area.width / 2 else inset + area.width * index / (values.size - 1)
        val t = if (span == 0.0) 0.5 else (value - range.start) / span
        Offset(x, (inset + area.height * (1 - t)).toFloat())
    }
}

/** The runs of points between gaps. */
internal fun segments(points: List<Offset?>): List<List<Offset>> {
    val runs = ArrayList<List<Offset>>()
    var current = ArrayList<Offset>()
    points.forEach { point ->
        if (point == null) {
            if (current.isNotEmpty()) runs += current
            current = ArrayList()
        } else {
            current += point
        }
    }
    if (current.isNotEmpty()) runs += current
    return runs
}

/** The highest and the lowest point (one when they're the same). */
internal fun extremes(points: List<Offset?>): List<Offset> {
    val present = points.filterNotNull()
    if (present.isEmpty()) return emptyList()
    return listOf(present.minBy { it.y }, present.maxBy { it.y }).distinct()
}
