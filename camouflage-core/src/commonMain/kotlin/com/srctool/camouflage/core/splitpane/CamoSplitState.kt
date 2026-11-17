package com.srctool.camouflage.core.splitpane

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A split's layout as plain data the app can save (Camouflage stores nothing itself) and hand back to
 * [CamoSplitState.apply].
 */
public data class CamoSplitSnapshot(val fractions: List<Float>, val collapsed: Set<Int>)

/**
 * The layout of a `CamoSplitPane`, owned by the app: each pane's size as a **fraction** of the container (so a
 * saved layout restores on a different window size) and which panes are collapsed. [initial] are the fractions of
 * the panes (they are scaled to sum to 1). The pane limits (`minSize`, `maxSize`, in dp) clamp what the layout
 * draws and what [resize] does; they reach the state when the pane has been measured.
 */
@Stable
public class CamoSplitState(initial: List<Float>) {
    private val start: List<Float> = normalized(initial)

    /** Each pane's share of the container, summing to 1 across all panes. */
    public var fractions: List<Float> by mutableStateOf(start)
        private set

    /** The collapsed panes: they leave the composition, and their divider stays so they can be restored. */
    public var collapsed: Set<Int> by mutableStateOf(emptySet())
        private set

    // What the layout last measured: the container's length and each pane's limits, in pixels.
    internal var total: Float = 0f
    internal var limits: List<PaneLimits> = emptyList()
    internal var collapsible: Set<Int> = emptySet()

    /** Moves [divider] (between panes `divider` and `divider + 1`) by [deltaPx], within both panes' limits. */
    public fun resize(divider: Int, deltaPx: Float) {
        resize(divider, deltaPx, allowCollapse = true)
    }

    // [allowCollapse] `false` clamps at the minimum (Home and End never collapse a pane).
    internal fun resize(divider: Int, deltaPx: Float, allowCollapse: Boolean) {
        if (divider !in 0 until fractions.size - 1 || limits.size != fractions.size) return
        val collapsing = if (allowCollapse) collapsible else emptySet()
        val result = resize(fractions, collapsed, divider, deltaPx, total, limits, collapsing)
        result.collapsed?.let { collapse(it) } ?: run { fractions = result.fractions }
    }

    // The pane's size in pixels from the last measure.
    internal fun sizeOf(pane: Int): Float =
        if (limits.size == fractions.size) layoutSizes(fractions, collapsed, total, limits)[pane] else 0f

    /** Collapses [pane]; it restores to its previous fraction. */
    public fun collapse(pane: Int) {
        if (pane in fractions.indices && pane !in collapsed && collapsed.size < fractions.size - 1) {
            collapsed = collapsed + pane
        }
    }

    /** Brings a collapsed [pane] back at its previous fraction. */
    public fun restore(pane: Int) {
        collapsed = collapsed - pane
    }

    /** Back to the initial fractions, with nothing collapsed. */
    public fun reset() {
        fractions = start
        collapsed = emptySet()
    }

    /** Puts the two panes beside [divider] back to their initial sizes, keeping the space they share. */
    public fun reset(divider: Int) {
        if (divider !in 0 until fractions.size - 1) return
        val shared = fractions[divider] + fractions[divider + 1]
        val initial = start[divider] + start[divider + 1]
        if (initial <= 0f) return
        fractions = fractions.toMutableList().also {
            it[divider] = start[divider] / initial * shared
            it[divider + 1] = start[divider + 1] / initial * shared
        }
        collapsed = collapsed - divider - (divider + 1)
    }

    /** The layout as plain data to save. */
    public fun snapshot(): CamoSplitSnapshot = CamoSplitSnapshot(fractions, collapsed)

    /** Restores a saved layout; one for a different number of panes is ignored. */
    public fun apply(snapshot: CamoSplitSnapshot) {
        if (snapshot.fractions.size != start.size) return
        fractions = normalized(snapshot.fractions)
        collapsed = snapshot.collapsed.filter { it in start.indices }.toSet()
    }
}

private fun normalized(fractions: List<Float>): List<Float> {
    require(fractions.size >= 2) { "CamoSplitState: a split has two or more panes." }
    require(fractions.all { it > 0f }) { "CamoSplitState: every pane's fraction must be positive." }
    val sum = fractions.sum()
    return fractions.map { it / sum }
}
