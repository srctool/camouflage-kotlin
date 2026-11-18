package com.srctool.camouflage.core.datatable

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The columns' layout as plain data the app can save (Camouflage stores nothing itself) and hand back to
 * [CamoColumnState.apply].
 */
public data class CamoColumnSnapshot(val order: List<Any>, val widths: Map<Any, Dp>, val hidden: Set<Any>)

/**
 * The layout of a `CamoDataTable`'s columns, owned by the app: their [order], the [widths] the user dragged them
 * to (a column without one keeps its own width) and which are [hidden]. [columns] are the keys of the table's
 * columns in their first order. Hand it to `CamoDataTable(columnState = …)`; save [snapshot] to restore the
 * layout after a restart.
 */
@Stable
public class CamoColumnState(columns: List<Any>) {
    private val initial: List<Any> = columns.distinct()

    /** Every column's key in the order shown (hidden ones keep their place). */
    public var order: List<Any> by mutableStateOf(initial)
        private set

    /** The widths set by the user. */
    public var widths: Map<Any, Dp> by mutableStateOf(emptyMap())
        private set

    /** The hidden columns. */
    public var hidden: Set<Any> by mutableStateOf(emptySet())
        private set

    // The widths the headers last measured, in pixels, for the keyboard and the drop target (not state, so no
    // recomposition).
    internal val measured: MutableMap<Any, Float> = mutableMapOf()

    // Where a header drag would drop the column (an index among the visible columns), `-1` when none.
    internal var dropTarget: Int by mutableIntStateOf(NONE)

    /** Sets [column]'s width to [to], at least [min]. */
    public fun resize(column: Any, to: Dp, min: Dp = DEFAULT_MIN) {
        if (column !in order) return
        widths = widths + (column to to.coerceAtLeast(min))
    }

    /** Moves [column] to position [to] among all columns. */
    public fun move(column: Any, to: Int) {
        val from = order.indexOf(column)
        if (from < 0) return
        val next = order.toMutableList().also { it.removeAt(from) }
        next.add(to.coerceIn(0, next.size), column)
        order = next
    }

    /** Moves [column] one step toward the start (`-1`) or the end (`1`) among the visible columns. */
    public fun moveBy(column: Any, step: Int) {
        val visible = order.filter { it !in hidden }
        val at = visible.indexOf(column)
        val target = visible.getOrNull(at + step) ?: return
        if (at >= 0) move(column, order.indexOf(target))
    }

    /** Hides [column]; the last visible column stays. */
    public fun hide(column: Any) {
        if (column in order && column !in hidden && order.count { it !in hidden } > 1) hidden = hidden + column
    }

    /** Shows a hidden [column]. */
    public fun show(column: Any) {
        hidden = hidden - column
    }

    /** Forgets the width the user set for [column] (it keeps its own again). */
    public fun resizeToOwn(column: Any) {
        widths = widths - column
    }

    /** Forgets every width the user set (each column keeps its own again). */
    public fun resetWidths() {
        widths = emptyMap()
    }

    /** Back to the first order and the columns' own widths, with nothing hidden. */
    public fun reset() {
        order = initial
        widths = emptyMap()
        hidden = emptySet()
    }

    /** The layout as plain data to save. */
    public fun snapshot(): CamoColumnSnapshot = CamoColumnSnapshot(order, widths, hidden)

    /** Restores a saved layout; keys the table does not have are ignored and new columns go to the end. */
    public fun apply(snapshot: CamoColumnSnapshot) {
        val known = initial.toSet()
        val saved = snapshot.order.filter { it in known }.distinct()
        order = saved + initial.filter { it !in saved }
        widths = snapshot.widths.filterKeys { it in known }
        hidden = snapshot.hidden.filter { it in known }.toSet()
            .let { if (it.size >= known.size) emptySet() else it }
    }

    internal companion object {
        const val NONE: Int = -1
        val DEFAULT_MIN: Dp = 48.dp
    }
}

/** The table's columns in the state's order, minus the hidden ones, each with the width the user set. */
internal fun <T> CamoColumnState.shown(columns: List<CamoColumn<T>>): List<CamoColumn<T>> {
    val byKey = columns.associateBy { it.key }
    val keys = order.filter { it in byKey } + columns.map { it.key }.filter { it !in order }
    return keys.filter { it !in hidden }.mapNotNull { key ->
        byKey[key]?.let { column -> widths[key]?.let { column.copy(width = ColumnWidth.Fixed(it)) } ?: column }
    }
}

/**
 * Where a dragged header of width [widths]`[from]`, moved by [offset] pixels, would drop: the index among the
 * visible columns whose span holds the dragged header's center.
 */
internal fun dropIndex(widths: List<Float>, from: Int, offset: Float): Int {
    if (from !in widths.indices) return from
    val center = widths.take(from).sum() + widths[from] / 2 + offset
    var edge = 0f
    val hit = widths.indexOfFirst { width ->
        edge += width
        center < edge
    }
    return if (hit >= 0) hit else widths.lastIndex
}
