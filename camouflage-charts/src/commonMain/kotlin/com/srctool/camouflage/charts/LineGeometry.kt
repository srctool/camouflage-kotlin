package com.srctool.camouflage.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.sqrt

/** The form of a line chart. */
public enum class LineKind { Line, Area, StackedArea }

/** How a line connects its points: straight, smoothed (monotone cubic or Catmull-Rom) or as steps. */
public enum class LineCurve {
    Linear,

    /** Monotone cubic: smooth, and never overshoots the data between two points. */
    Smooth,

    /** Catmull-Rom: smooth through every point; it can overshoot a sharp peak. */
    CatmullRom,

    /** Horizontal to the next x, then vertical. */
    Step,
}

/** When a line chart marks its points. */
public enum class MarkerVisibility { Always, OnHover, Never }

/** One series' band in a stack: from [base] up to [top]. */
internal data class StackBand(val base: Double, val top: Double)

/**
 * Stacks [series] (one list of values per series, one value per category) in order. A `null` value leaves a gap in
 * that series; a negative value adds nothing (stacked areas treat negatives as zero).
 */
internal fun stack(series: List<List<Double?>>): List<List<StackBand?>> {
    val categories = series.maxOfOrNull { it.size } ?: 0
    val running = DoubleArray(categories)
    return series.map { values ->
        List(categories) { category ->
            val value = values.getOrNull(category) ?: return@List null
            val add = maxOf(0.0, value)
            StackBand(running[category], running[category] + add).also { running[category] += add }
        }
    }
}

/** The index of the entry of [positions] nearest to [x], or `null` for an empty list. */
internal fun nearestIndex(positions: List<Float>, x: Float): Int? =
    positions.indices.minByOrNull { abs(positions[it] - x) }

/** The index ranges of [points] between gaps. */
internal fun gapRuns(points: List<Offset?>): List<IntRange> {
    val runs = ArrayList<IntRange>()
    var start = -1
    points.forEachIndexed { index, point ->
        if (point == null) {
            if (start >= 0) runs += start until index
            start = -1
        } else if (start < 0) {
            start = index
        }
    }
    if (start >= 0) runs += start until points.size
    return runs
}

/** One cubic piece of a smoothed line: two control points and where it ends. */
internal class Cubic(val control1: Offset, val control2: Offset, val end: Offset)

private const val THIRD = 1f / 3f
private const val SIXTH = 1f / 6f
private const val MONOTONE_LIMIT = 9.0

/** The cubic pieces joining [points] (x strictly increasing) for a smoothed [curve]; none for the others. */
internal fun curvePieces(points: List<Offset>, curve: LineCurve): List<Cubic> = when {
    points.size < 2 -> emptyList()
    curve == LineCurve.Smooth -> monotonePieces(points)
    curve == LineCurve.CatmullRom -> catmullRomPieces(points)
    else -> emptyList()
}

private fun catmullRomPieces(points: List<Offset>): List<Cubic> = (0 until points.size - 1).map { index ->
    val before = points[maxOf(0, index - 1)]
    val from = points[index]
    val to = points[index + 1]
    val after = points[minOf(points.lastIndex, index + 2)]
    Cubic(from + (to - before) * SIXTH, to - (after - from) * SIXTH, to)
}

// Fritsch-Carlson: tangents are limited so the curve between two points stays between their values.
private fun monotonePieces(points: List<Offset>): List<Cubic> {
    val count = points.size
    val width = DoubleArray(count - 1) { (points[it + 1].x - points[it].x).toDouble() }
    val secant = DoubleArray(count - 1) { if (width[it] == 0.0) 0.0 else (points[it + 1].y - points[it].y) / width[it] }
    val slope = DoubleArray(count) { index ->
        when (index) {
            0 -> secant[0]
            count - 1 -> secant[count - 2]
            else -> if (secant[index - 1] * secant[index] <= 0.0) 0.0 else (secant[index - 1] + secant[index]) / 2
        }
    }
    for (index in 0 until count - 1) {
        if (secant[index] == 0.0) {
            slope[index] = 0.0
            slope[index + 1] = 0.0
            continue
        }
        val a = slope[index] / secant[index]
        val b = slope[index + 1] / secant[index]
        val sum = a * a + b * b
        if (sum > MONOTONE_LIMIT) {
            val scale = 3.0 / sqrt(sum)
            slope[index] = scale * a * secant[index]
            slope[index + 1] = scale * b * secant[index]
        }
    }
    return (0 until count - 1).map { index ->
        val from = points[index]
        val to = points[index + 1]
        val third = width[index].toFloat() * THIRD
        Cubic(
            Offset(from.x + third, from.y + slope[index].toFloat() * third),
            Offset(to.x - third, to.y - slope[index + 1].toFloat() * third),
            to,
        )
    }
}

/** The points of a step line through [points]: each step goes along first, then across. */
internal fun stepPoints(points: List<Offset>): List<Offset> {
    if (points.isEmpty()) return points
    val result = ArrayList<Offset>()
    result += points.first()
    for (index in 1 until points.size) {
        result += Offset(points[index].x, points[index - 1].y)
        result += points[index]
    }
    return result
}

/** Adds [points] shaped by [curve] to this path: it continues from the current point when [connect], else moves. */
internal fun Path.addCurve(points: List<Offset>, curve: LineCurve, connect: Boolean = false) {
    if (points.isEmpty()) return
    val first = points.first()
    if (connect) lineTo(first.x, first.y) else moveTo(first.x, first.y)
    when (curve) {
        LineCurve.Linear -> points.drop(1).forEach { lineTo(it.x, it.y) }
        LineCurve.Step -> stepPoints(points).drop(1).forEach { lineTo(it.x, it.y) }
        else -> curvePieces(points, curve).forEach { piece ->
            cubicTo(piece.control1.x, piece.control1.y, piece.control2.x, piece.control2.y, piece.end.x, piece.end.y)
        }
    }
}
