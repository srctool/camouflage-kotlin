package com.srctool.camouflage.core.dock

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp

/** An edge of a [CamoDockLayout]. `Left` and `Right` mean start and end: they swap in right-to-left layouts. */
public enum class DockEdge { Left, Right, Bottom }

/** A saved dock layout: where each tool window is (stripe order), which are open, the sizes and the maximized one. */
@Immutable
public data class CamoDockSnapshot(
    val order: List<Any>,
    val edges: Map<Any, DockEdge>,
    val open: Set<Any>,
    val sizes: Map<DockEdge, Float>,
    val maximized: Any? = null,
)

/**
 * The layout of a `CamoDockLayout`, owned by the app so it survives restarts ([snapshot] and [apply]): the edge each
 * tool window sits on and its place on that edge's stripe, which windows are open, how big each dock is (a
 * **fraction** of the window, so a saved layout restores at another size) and which window is maximized.
 * [defaults] gives every tool window's id and starting edge, in stripe order; [open] the ones that start open.
 * Several open windows on one edge stack in a split of their own.
 */
@Suppress("TooManyFunctions") // one cohesive state: the rules of a dock
@Stable
public class CamoDockState(defaults: Map<Any, DockEdge>, open: Set<Any> = emptySet()) {
    private val first = defaults.toMap()
    private var order by mutableStateOf(first.keys.toList())
    private var edges by mutableStateOf(first)
    private var opened by mutableStateOf(open.filter { it in first }.toSet())
    private var sizes by mutableStateOf(DEFAULT_SIZES)

    /** The id of the maximized tool window, or `null`. */
    public var maximized: Any? by mutableStateOf(null)
        private set

    // Bumped by [focus]; the layout moves focus into [focusTarget] when it changes.
    internal var focusCount by mutableIntStateOf(0)
        private set
    internal var focusTarget: Any? = null
        private set

    // Bumped by [apply]: the layout rebuilds its splits from the restored sizes.
    internal var generation by mutableIntStateOf(0)
        private set

    /** Every tool window's id in stripe order. */
    public val ids: List<Any> get() = order

    /** The edge [id] sits on. */
    public fun edgeOf(id: Any): DockEdge? = edges[id]

    /** The windows on [edge], in stripe order. */
    public fun windowsOn(edge: DockEdge): List<Any> = order.filter { edges[it] == edge }

    /** The open windows on [edge], in stripe order. */
    public fun openOn(edge: DockEdge): List<Any> = windowsOn(edge).filter { it in opened }

    /** Whether [id] is open. */
    public fun isOpen(id: Any): Boolean = id in opened

    /** The size of the dock on [edge] as a fraction of the window. */
    public fun sizeOf(edge: DockEdge): Float = sizes.getValue(edge)

    internal fun setSize(edge: DockEdge, fraction: Float) {
        if (fraction > 0f && fraction < 1f) sizes = sizes + (edge to fraction)
    }

    /** Opens [id] if it is closed, closes it if it is open. */
    public fun toggle(id: Any) {
        if (isOpen(id)) close(id) else open(id)
    }

    /** Opens [id]. Another window's maximize ends. */
    public fun open(id: Any) {
        if (id !in edges) return
        opened = opened + id
        if (maximized != null && maximized != id) maximized = null
    }

    /** Hides [id]; it keeps its place on the stripe. */
    public fun close(id: Any) {
        opened = opened - id
        if (maximized == id) maximized = null
    }

    /** Moves [id] to the end of [edge]'s stripe; it stays open if it was. */
    public fun move(id: Any, edge: DockEdge) {
        if (id !in edges || edges[id] == edge) return
        edges = edges + (id to edge)
        order = order - id + id
    }

    /** Opens [id] and has it fill the window: the other docks and the center hide, the stripes stay. */
    public fun maximize(id: Any) {
        if (id !in edges) return
        opened = opened + id
        maximized = id
    }

    /** Brings back what [maximize] hid. */
    public fun restore() {
        maximized = null
    }

    /** Opens [id] and moves keyboard focus into it (an app's "Show Git" shortcut). */
    public fun focus(id: Any) {
        if (id !in edges) return
        open(id)
        focusTarget = id
        focusCount++
    }

    /** The layout to save. */
    public fun snapshot(): CamoDockSnapshot = CamoDockSnapshot(order, edges, opened, sizes, maximized)

    /**
     * Restores a saved layout. Windows it doesn't know keep their place at the end; ids that are gone are
     * dropped.
     */
    public fun apply(snapshot: CamoDockSnapshot) {
        val known = snapshot.order.filter { it in first }
        order = known + first.keys.filter { it !in known }
        edges = first + snapshot.edges.filterKeys { it in first }
        opened = snapshot.open.filter { it in first }.toSet()
        sizes = DEFAULT_SIZES + snapshot.sizes.filterValues { it > 0f && it < 1f }
        maximized = snapshot.maximized?.takeIf { it in opened }
        generation++
    }

    private companion object {
        const val SIDE = 0.22f
        const val BOTTOM = 0.3f
        val DEFAULT_SIZES = mapOf(DockEdge.Left to SIDE, DockEdge.Right to SIDE, DockEdge.Bottom to BOTTOM)
    }
}

/** Overrides for dock layouts. `null` keeps the default. */
@Immutable
public data class CamoDockTheme(
    val stripeSize: Dp? = null,
    val minDock: Dp? = null,
    val strings: DockStrings? = null,
)

/** The words a dock says; set them to translate. */
@Immutable
public data class DockStrings(
    val stripe: (DockEdge) -> String = { "${it.name} tool windows" },
    val moveTo: String = "Move to",
    val edge: (DockEdge) -> String = { it.name },
    val hide: String = "Hide",
    val maximize: String = "Maximize",
    val restore: String = "Restore",
    val more: String = "Tool window options",
)
