package com.srctool.camouflage.core.tabs

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A saved tab layout: the order of the document ids and which are pinned. */
@Immutable
public data class CamoDocumentTabsSnapshot(val order: List<Any>, val pinned: Set<Any>)

/**
 * The order and the pinned set of a [CamoDocumentTabs] row, owned by the app so a layout survives restarts
 * (`snapshot()` and `apply()`). Pinned tabs always sit at the start and reorder among themselves; the others
 * follow. The row adds new documents at the end and forgets closed ones.
 */
@Stable
public class CamoDocumentTabsState(order: List<Any> = emptyList(), pinned: Set<Any> = emptySet()) {
    private var layout by mutableStateOf(normalized(order, pinned))

    /** The ids in tab order, pinned first. */
    public val order: List<Any> get() = layout.order

    /** The pinned ids. */
    public val pinned: Set<Any> get() = layout.pinned

    /** Whether [id] is pinned. */
    public fun isPinned(id: Any): Boolean = id in layout.pinned

    /** Moves [id] to position [to], staying inside its group (pinned tabs among pinned, others among others). */
    public fun move(id: Any, to: Int) {
        layout = layout.moved(id, to)
    }

    /** Moves [id] [delta] places along the order (negative: toward the start), staying inside its group. */
    public fun moveBy(id: Any, delta: Int) {
        val at = layout.order.indexOf(id)
        if (at >= 0) move(id, at + delta)
    }

    /** Pins [id]: it moves to the end of the pinned tabs. */
    public fun pin(id: Any) {
        layout = layout.pinned(id, true)
    }

    /** Unpins [id]: it moves to the start of the other tabs. */
    public fun unpin(id: Any) {
        layout = layout.pinned(id, false)
    }

    /** The layout to save. */
    public fun snapshot(): CamoDocumentTabsSnapshot = CamoDocumentTabsSnapshot(layout.order, layout.pinned)

    /** Restores a saved [snapshot]. */
    public fun apply(snapshot: CamoDocumentTabsSnapshot) {
        layout = normalized(snapshot.order, snapshot.pinned)
    }

    // Adds ids the state doesn't know at the end and drops ids that are gone.
    internal fun sync(ids: List<Any>) {
        val next = layout.synced(ids)
        if (next != layout) layout = next
    }

    // [ids] in this state's order; unknown ids at the end in their own order.
    internal fun sorted(ids: List<Any>): List<Any> = layout.synced(ids).order
}

// The pure ordering rules: [order] with the pinned ids first.
internal data class TabLayout(val order: List<Any>, val pinned: Set<Any>) {
    private val pinnedCount: Int get() = order.count { it in pinned }

    fun moved(id: Any, to: Int): TabLayout {
        val from = order.indexOf(id)
        val range = if (id in pinned) 0 until pinnedCount else pinnedCount until order.size
        val target = to.coerceIn(range.first, maxOf(range.first, range.last))
        if (from < 0 || target == from) return this
        val next = order.toMutableList()
        next.removeAt(from)
        next.add(target, id)
        return copy(order = next)
    }

    fun pinned(id: Any, value: Boolean): TabLayout {
        if (id !in order || (id in pinned) == value) return this
        val rest = order - id
        val pins = if (value) pinned + id else pinned - id
        val count = rest.count { it in pinned }
        return TabLayout(rest.take(count) + id + rest.drop(count), pins)
    }

    fun synced(ids: List<Any>): TabLayout {
        val known = ids.toSet()
        val kept = order.filter { it in known }
        val added = ids.filter { it !in kept }
        val pins = pinned.filter { it in known }.toSet()
        return normalized(kept + added, pins)
    }
}

private fun normalized(order: List<Any>, pinned: Set<Any>): TabLayout {
    val distinct = order.distinct()
    val pins = pinned.filter { it in distinct }.toSet()
    return TabLayout(distinct.filter { it in pins } + distinct.filter { it !in pins }, pins)
}
