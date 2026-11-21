package com.srctool.camouflage.core.dock

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.splitpane.CamoPane
import com.srctool.camouflage.core.splitpane.CamoSplitPane
import com.srctool.camouflage.core.splitpane.CamoSplitState
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.theme.SurfaceLevel

private val DefaultStripe = 36.dp
private val DefaultMinDock = 160.dp
private val MinStacked = 80.dp
private const val MIN_CENTER_SHARE = 0.1f

/**
 * IDE-style tool windows around [center]: side and bottom docks that open from icon stripes on the window's edges.
 * A stripe button toggles its tool window; several open windows on one edge stack in a split of their own. Docks
 * resize through the split pane's divider (keys and screen reader included), a window's header hides, maximizes and
 * moves it to another edge, and `state.focus(id)` opens a window and moves keyboard focus into it; Esc inside one
 * returns focus to the center. The [state] is the app's; it can be saved and restored. Left and right mean start
 * and end, so they swap in right-to-left layouts.
 */
@Composable
public fun CamoDockLayout(
    state: CamoDockState,
    toolWindows: List<CamoToolWindow>,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit,
) {
    val windows = remember(toolWindows) { toolWindows.associateBy { it.id } }
    require(state.ids.all { it in windows }) { "CamoDockLayout: the state has tool windows that were not passed in." }
    val o = Camouflage.theme.components.dock
    val strings = o?.strings ?: DockStrings()
    val stripe = o?.stripeSize ?: DefaultStripe
    val focus = remember { DockFocus() }
    FocusEffect(state, focus)
    fun stripeOf(edge: DockEdge): @Composable () -> Unit = {
        val onEdge = state.windowsOn(edge).map { windows.getValue(it) }
        if (onEdge.isNotEmpty()) Stripe(edge, onEdge, state, stripe, strings)
    }
    Row(modifier.fillMaxSize()) {
        stripeOf(DockEdge.Left)()
        Column(Modifier.weight(1f)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val maximized = state.maximized?.let { windows[it] }
                if (maximized != null) {
                    ToolWindowPanel(maximized, state, strings, focus)
                } else {
                    Docks(state, windows, strings, focus, o?.minDock ?: DefaultMinDock, center)
                }
            }
            stripeOf(DockEdge.Bottom)()
        }
        stripeOf(DockEdge.Right)()
    }
}

// The focus requesters: one per tool window and one for the center.
private class DockFocus {
    val center = FocusRequester()
    private val windows = mutableMapOf<Any, FocusRequester>()
    fun of(id: Any): FocusRequester = windows.getOrPut(id) { FocusRequester() }
}

// Moves focus into the window `state.focus` named, once it has been composed.
@Composable
private fun FocusEffect(state: CamoDockState, focus: DockFocus) {
    LaunchedEffect(state.focusCount) {
        val target = state.focusTarget
        if (state.focusCount > 0 && target != null) {
            withFrameNanos { }
            withFrameNanos { }
            runCatching { focus.of(target).requestFocus() }
        }
    }
}

// One resizable slot: [edge] is the dock it holds, or `null` for the rest (the center, or the docks above).
private class Slot(val edge: DockEdge?, val minSize: Dp, val content: @Composable () -> Unit)

@Composable
private fun Docks(
    state: CamoDockState,
    windows: Map<Any, CamoToolWindow>,
    strings: DockStrings,
    focus: DockFocus,
    minDock: Dp,
    center: @Composable () -> Unit,
) {
    fun stack(edge: DockEdge): (@Composable () -> Unit)? {
        val open = state.openOn(edge).map { windows.getValue(it) }
        return if (open.isEmpty()) null else ({ Stack(edge, open, state, strings, focus) })
    }
    val centerSlot: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().focusRequester(focus.center).focusGroup()) { center() }
    }
    val upper = listOfNotNull(
        stack(DockEdge.Left)?.let { Slot(DockEdge.Left, minDock, it) },
        Slot(null, minDock, centerSlot),
        stack(DockEdge.Right)?.let { Slot(DockEdge.Right, minDock, it) },
    )
    val bottom = stack(DockEdge.Bottom)
    val whole = listOfNotNull(
        Slot(null, minDock) { EdgeSplit(Orientation.Horizontal, state, upper) },
        bottom?.let { Slot(DockEdge.Bottom, MinStacked, it) },
    )
    EdgeSplit(Orientation.Vertical, state, whole)
}

// A split of [slots] whose docks take the sizes in the state and write the dragged sizes back.
@Composable
private fun EdgeSplit(axis: Orientation, state: CamoDockState, slots: List<Slot>) {
    if (slots.size == 1) {
        slots.single().content()
        return
    }
    val edges = slots.map { it.edge }
    val split = remember(edges, state.generation) {
        val docks = edges.filterNotNull().sumOf { state.sizeOf(it).toDouble() }.toFloat()
        CamoSplitState(edges.map { it?.let(state::sizeOf) ?: (1f - docks).coerceAtLeast(MIN_CENTER_SHARE) })
    }
    LaunchedEffect(split) {
        snapshotFlow { split.fractions }.collect { fractions ->
            edges.forEachIndexed { index, edge -> edge?.let { state.setSize(it, fractions[index]) } }
        }
    }
    CamoSplitPane(axis, slots.map { CamoPane(minSize = it.minSize, content = it.content) }, split)
}

// The open windows of one edge: one, or a split of their own along the dock's length.
@Composable
private fun Stack(
    edge: DockEdge,
    open: List<CamoToolWindow>,
    state: CamoDockState,
    strings: DockStrings,
    focus: DockFocus,
) {
    if (open.size == 1) {
        ToolWindowPanel(open.single(), state, strings, focus)
        return
    }
    val ids = open.map { it.id }
    val split = remember(ids) { CamoSplitState(List(ids.size) { 1f }) }
    CamoSplitPane(
        if (edge == DockEdge.Bottom) Orientation.Horizontal else Orientation.Vertical,
        open.map { window -> CamoPane(minSize = MinStacked) { ToolWindowPanel(window, state, strings, focus) } },
        split,
    )
}

// Esc inside a window: focus goes to the center, or just out of the window when the center has nothing to focus.
private fun leaveToCenter(focus: DockFocus, focusManager: FocusManager) {
    val moved = runCatching { focus.center.requestFocus() }.getOrDefault(false)
    if (!moved) focusManager.clearFocus()
}

@Composable
private fun ToolWindowPanel(window: CamoToolWindow, state: CamoDockState, strings: DockStrings, focus: DockFocus) {
    val focusManager = LocalFocusManager.current
    CamoSurface(level = SurfaceLevel.Surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                // After the window's own content: a text field that takes Esc keeps it.
                .onKeyEvent {
                    val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                    if (esc) leaveToCenter(focus, focusManager)
                    esc
                },
        ) {
            ToolWindowHeader(window, state.edgeOf(window.id) ?: window.defaultEdge, state, strings)
            // `focus(id)` lands in the content, not on the header's buttons.
            Box(Modifier.weight(1f).fillMaxWidth().focusRequester(focus.of(window.id)).focusGroup()) {
                window.content()
            }
        }
    }
}
