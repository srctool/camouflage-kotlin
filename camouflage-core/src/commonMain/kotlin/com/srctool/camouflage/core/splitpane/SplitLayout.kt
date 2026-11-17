package com.srctool.camouflage.core.splitpane

import kotlin.math.max
import kotlin.math.min

// The split pane's layout rule as pure functions (no window needed): fractions in, pixel sizes out, and the effect
// of moving one divider. Collapsed panes have no size; the visible panes share the container by their fractions.

/** One pane's size limits in pixels. [max] is infinite when the pane has none. */
internal class PaneLimits(val min: Float, val max: Float = Float.POSITIVE_INFINITY)

private const val CLAMP_ROUNDS = 8
private const val SLACK = 0.5f

/**
 * The visible panes' sizes: each pane's share of [total] by its fraction, clamped to its limits, the excess or
 * the shortfall going to the panes that are not pinned. A container smaller than the sum of the minimums shrinks
 * every pane in proportion, so nothing is cut off. Collapsed panes are `0`.
 */
@Suppress("CyclomaticComplexMethod", "ReturnCount") // one pass of the layout rule, early exits for the edge cases
internal fun layoutSizes(
    fractions: List<Float>,
    collapsed: Set<Int>,
    total: Float,
    limits: List<PaneLimits>,
): List<Float> {
    val visible = fractions.indices.filter { it !in collapsed }
    val sizes = FloatArray(fractions.size)
    if (visible.isEmpty() || total <= 0f) return sizes.toList()
    val share = visible.sumOf { fractions[it].toDouble() }.toFloat().takeIf { it > 0f } ?: visible.size.toFloat()
    val minimums = visible.sumOf { limits[it].min.toDouble() }.toFloat()
    if (minimums >= total) {
        // Too small for the minimums: shrink them in proportion.
        visible.forEach { sizes[it] = if (minimums > 0f) limits[it].min / minimums * total else total / visible.size }
        return sizes.toList()
    }
    visible.forEach { sizes[it] = fractions[it] / share * total }
    val pinned = BooleanArray(fractions.size)
    repeat(CLAMP_ROUNDS) {
        visible.forEach { i ->
            val clamped = sizes[i].coerceIn(limits[i].min, max(limits[i].min, limits[i].max))
            if (clamped != sizes[i]) pinned[i] = true
            sizes[i] = clamped
        }
        val residual = total - visible.sumOf { sizes[it].toDouble() }.toFloat()
        if (kotlin.math.abs(residual) < SLACK) return sizes.toList()
        val free = visible.filter { !pinned[it] }
        if (free.isEmpty()) {
            // Everything is pinned: give the rest to the last visible pane.
            sizes[visible.last()] += residual
            return sizes.toList()
        }
        val freeTotal = free.sumOf { sizes[it].toDouble() }.toFloat().takeIf { it > 0f } ?: free.size.toFloat()
        free.forEach { sizes[it] += residual * (if (freeTotal > 0f) sizes[it] / freeTotal else 1f / free.size) }
    }
    return sizes.toList()
}

/** The result of moving a divider: the new fractions, and the pane it collapsed (or `null`). */
internal class Resized(val fractions: List<Float>, val collapsed: Int?)

/**
 * Moves [divider] (between panes `divider` and `divider + 1`) by [deltaPx]: space moves between those two panes
 * only, within both panes' limits. A pane in [collapsible] dragged below half its minimum collapses instead.
 * Nothing moves when a neighbour is collapsed.
 */
@Suppress("LongParameterList", "ReturnCount") // the rule's inputs, and its early exits
internal fun resize(
    fractions: List<Float>,
    collapsed: Set<Int>,
    divider: Int,
    deltaPx: Float,
    total: Float,
    limits: List<PaneLimits>,
    collapsible: Set<Int>,
): Resized {
    val a = divider
    val b = divider + 1
    if (a in collapsed || b in collapsed || total <= 0f) return Resized(fractions, null)
    val sizes = layoutSizes(fractions, collapsed, total, limits)
    val pair = sizes[a] + sizes[b]
    val wanted = sizes[a] + deltaPx
    val collapseA = a in collapsible && wanted < limits[a].min / 2
    val collapseB = b in collapsible && pair - wanted < limits[b].min / 2
    if (collapseA || collapseB) return Resized(fractions, if (collapseA) a else b)
    val low = max(limits[a].min, pair - limits[b].max)
    val high = min(limits[a].max, pair - limits[b].min)
    val newA = wanted.coerceIn(low, max(low, high))
    val share = fractions.indices.filter { it !in collapsed }.sumOf { fractions[it].toDouble() }.toFloat()
    val next = fractions.toMutableList()
    next[a] = newA / total * share
    next[b] = (pair - newA) / total * share
    return Resized(next, null)
}
