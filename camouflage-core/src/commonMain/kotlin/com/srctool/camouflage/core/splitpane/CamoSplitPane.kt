package com.srctool.camouflage.core.splitpane

import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import kotlin.math.roundToInt

/**
 * One pane of a split: [content] between [minSize] and [maxSize] (dp; the state keeps fractions). A [collapsible]
 * pane dragged below half its minimum collapses and leaves the composition; its divider stays so it restores.
 */
@Immutable
public data class CamoPane(
    val minSize: Dp = 80.dp,
    val maxSize: Dp? = null,
    val collapsible: Boolean = false,
    val content: @Composable () -> Unit,
)

/** Every color a split pane's divider uses. */
@Immutable
public data class SplitPaneColors(val line: Color, val hover: Color, val dragging: Color, val focus: Color)

/** The words a divider says to a screen reader. */
@Immutable
public data class SplitPaneStrings(
    val label: String = "Resize panel",
    val percent: (Int) -> String = { "$it percent" },
)

/** Overrides for split panes. `null` keeps the skin's default. */
@Immutable
public data class SplitPaneTheme(
    /** The drawn line. */
    val thickness: Dp? = null,
    /** The grab area, centred on the line; a touch input grows it to 24 dp. */
    val hitSize: Dp? = null,
    /** Arrow keys move the divider this far; Shift moves it [keyStepLarge]. */
    val keyStep: Dp? = null,
    val keyStepLarge: Dp? = null,
    /** A small grip in the middle of the divider (some skins). */
    val grip: Boolean? = null,
    val colors: SplitPaneColors? = null,
    val strings: SplitPaneStrings? = null,
)

/** The resolved split pane style: every field set. */
@Immutable
public data class SplitPaneStyle(
    val thickness: Dp,
    val hitSize: Dp,
    val keyStep: Dp,
    val keyStepLarge: Dp,
    val grip: Boolean,
    val colors: SplitPaneColors,
    val strings: SplitPaneStrings = SplitPaneStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SplitPaneTheme?): SplitPaneStyle = if (overrides == null) {
        this
    } else {
        SplitPaneStyle(
            thickness = overrides.thickness ?: thickness,
            hitSize = overrides.hitSize ?: hitSize,
            keyStep = overrides.keyStep ?: keyStep,
            keyStepLarge = overrides.keyStepLarge ?: keyStepLarge,
            grip = overrides.grip ?: grip,
            colors = overrides.colors ?: colors,
            strings = overrides.strings ?: strings,
        )
    }
}

/**
 * What a divider renderer draws: the line along [axis] (a vertical line for a `Horizontal` split), in the idle,
 * hover, dragging or focus color, with a focus ring when [focused], and a grip if the style asks. The renderer
 * draws inside the hit area it is given and keeps the line centred; [collapsedBefore] and [collapsedAfter] say a
 * neighbouring pane is collapsed (a skin may draw a handle then).
 */
@Immutable
public data class SplitDividerState(
    val axis: Orientation,
    val hovered: Boolean,
    val focused: Boolean,
    val dragging: Boolean,
    val collapsedBefore: Boolean,
    val collapsedAfter: Boolean,
)

/** Draws the divider between a split's panes; core owns the layout, the hit area and the interaction. */
@Stable
public interface SplitPaneRenderer {
    /** The complete split pane style for [theme]. */
    public fun defaults(theme: CamoTheme): SplitPaneStyle

    /** Draws one divider in `modifier`'s bounds (the hit area); apply `modifier` to the outermost node. */
    @Composable
    public fun Divider(state: SplitDividerState, theme: CamoTheme, style: SplitPaneStyle, modifier: Modifier)
}

private const val LARGE_STEP_KEY_RANGE = 1_000_000f
private const val PROGRESS_STEPS = 19

private class PaneId(val index: Int)
private class HandleId(val index: Int)

/**
 * Panes side by side ([axis] `Horizontal`, first pane at the start) or stacked, with a draggable, keyboard and
 * screen reader accessible divider between each pair. The [state] is the app's: fractions of the container and
 * the collapsed panes, which it can [snapshot][CamoSplitState.snapshot] and restore. A divider moves space between
 * its two neighbours only, within their `minSize` and `maxSize`. Arrow keys resize (Shift for a larger step), Home
 * and End go to a limit, Enter collapses or restores a collapsible neighbour, a double-click resets the two
 * neighbours; a screen reader adjusts it like a slider. Right to left mirrors a horizontal split.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod") // the layout pass of one component
@Composable
public fun CamoSplitPane(
    axis: Orientation,
    panes: List<CamoPane>,
    state: CamoSplitState,
    modifier: Modifier = Modifier,
) {
    require(panes.size == state.fractions.size) {
        "CamoSplitPane: ${panes.size} panes for ${state.fractions.size} fractions."
    }
    val skin = requireSkin("CamoSplitPane")
    val theme = requireTheme("CamoSplitPane")
    val style = remember(skin, theme) { skin.splitPane.defaults(theme).merge(theme.components.splitPane) }
    val horizontal = axis == Orientation.Horizontal
    val mirrored = horizontal && LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val dividers = panes.size - 1
    Layout(
        content = {
            panes.forEachIndexed { index, pane ->
                if (index !in state.collapsed) Box(Modifier.layoutId(PaneId(index)).clipToBounds()) { pane.content() }
            }
            for (index in 0 until dividers) {
                Box(Modifier.layoutId(HandleId(index))) {
                    SplitDivider(index, axis, mirrored, state, style, theme, skin.splitPane)
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val main = if (horizontal) constraints.maxWidth else constraints.maxHeight
        val cross = if (horizontal) constraints.maxHeight else constraints.maxWidth
        val thickness = with(density) { style.thickness.roundToPx() }
        val available = (main - thickness * dividers).coerceAtLeast(0)
        state.total = available.toFloat()
        state.collapsible = panes.indices.filter { panes[it].collapsible }.toSet()
        state.limits = panes.map {
            PaneLimits(
                min = with(density) { it.minSize.toPx() },
                max = it.maxSize?.let { max -> with(density) { max.toPx() } } ?: Float.POSITIVE_INFINITY,
            )
        }
        val sizes = layoutSizes(state.fractions, state.collapsed, available.toFloat(), state.limits)
        val hit = with(density) { maxOf(style.hitSize.roundToPx(), thickness) }
        val paneAt = measurables.filter { it.layoutId is PaneId }.associateBy { (it.layoutId as PaneId).index }
        val handleAt = measurables.filter { it.layoutId is HandleId }.associateBy { (it.layoutId as HandleId).index }
        // Whole pixels along the main axis; the last pane takes the rounding.
        val lengths = sizes.map { it.roundToInt() }.toMutableList()
        val visible = panes.indices.filter { it !in state.collapsed }
        if (visible.isNotEmpty()) lengths[visible.last()] += available - lengths.sum()
        val placeablePanes = paneAt.mapValues { (index, m) ->
            m.measure(
                if (horizontal) Constraints.fixed(lengths[index], cross) else Constraints.fixed(cross, lengths[index]),
            )
        }
        val placeableHandles = handleAt.mapValues { (index, m) ->
            m.measure(if (horizontal) Constraints.fixed(hit, cross) else Constraints.fixed(cross, hit))
        }
        layout(if (horizontal) main else cross, if (horizontal) cross else main) {
            var position = 0
            panes.indices.forEach { index ->
                placeablePanes[index]?.let { placeable ->
                    val at = if (mirrored) main - position - lengths[index] else position
                    placeable.place(if (horizontal) IntOffset(at, 0) else IntOffset(0, at))
                }
                position += lengths[index]
                if (index < dividers) {
                    val handle = placeableHandles.getValue(index)
                    val extent = if (horizontal) handle.width else handle.height
                    // The line is centred in the handle, which overlaps both neighbours.
                    val line = if (mirrored) main - position - thickness else position
                    val at = line + (thickness - extent) / 2
                    handle.place(if (horizontal) IntOffset(at, 0) else IntOffset(0, at))
                    position += thickness
                }
            }
        }
    }
}

@Composable
private fun SplitDivider(
    index: Int,
    axis: Orientation,
    mirrored: Boolean,
    state: CamoSplitState,
    style: SplitPaneStyle,
    theme: CamoTheme,
    renderer: SplitPaneRenderer,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    var dragging by remember { mutableStateOf(false) }
    val sign = if (mirrored) -1f else 1f
    val density = LocalDensity.current
    val drag = rememberDraggableState { state.resize(index, it * sign) }
    // Pane [index]'s share of the visible panes, from the (observable) fractions.
    val visible = state.fractions.indices.filter { it !in state.collapsed }
    val shown = visible.sumOf { state.fractions[it].toDouble() }.toFloat()
    val share = if (shown > 0f) state.fractions[index] / shown else 0f
    val percent = (share * 100).roundToInt()
    renderer.Divider(
        SplitDividerState(
            axis,
            hovered,
            focused,
            dragging,
            collapsedBefore = index in state.collapsed,
            collapsedAfter = index + 1 in state.collapsed,
        ),
        theme,
        style,
        Modifier
            .fillMaxSize() // the whole grab area, whatever the renderer draws
            .resizeCursor(axis == Orientation.Horizontal)
            .draggable(
                drag,
                axis,
                onDragStarted = { dragging = true },
                onDragStopped = { dragging = false },
            )
            .hoverable(source)
            .focusable(interactionSource = source)
            .pointerInput(index) { detectTapGestures(onDoubleTap = { state.reset(index) }) }
            .onKeyEvent {
                val step = with(density) { style.keyStep.toPx() }
                val large = with(density) { style.keyStepLarge.toPx() }
                splitKey(it, axis, index, state, mirrored, step, large)
            }
            .semantics {
                contentDescription = style.strings.label
                stateDescription = style.strings.percent(percent)
                progressBarRangeInfo = ProgressBarRangeInfo(share, 0f..1f, PROGRESS_STEPS)
                setProgress { target ->
                    val total = state.total
                    state.resize(index, (target * total - state.sizeOf(index)) * sign, allowCollapse = false)
                    true
                }
            },
    )
}

// Arrow keys keep their visual meaning (right moves the divider right, also in right-to-left).
@Suppress("LongParameterList", "CyclomaticComplexMethod", "ReturnCount")
private fun splitKey(
    event: KeyEvent,
    axis: Orientation,
    index: Int,
    state: CamoSplitState,
    mirrored: Boolean,
    step: Float,
    largeStep: Float,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val horizontal = axis == Orientation.Horizontal
    val toward = if (horizontal) Key.DirectionRight else Key.DirectionDown
    val away = if (horizontal) Key.DirectionLeft else Key.DirectionUp
    val sign = if (mirrored) -1f else 1f
    val amount = if (event.isShiftPressed) largeStep else step
    when (event.key) {
        toward -> state.resize(index, amount * sign, allowCollapse = false)
        away -> state.resize(index, -amount * sign, allowCollapse = false)
        Key.MoveHome -> state.resize(index, -LARGE_STEP_KEY_RANGE * sign, allowCollapse = false)
        Key.MoveEnd -> state.resize(index, LARGE_STEP_KEY_RANGE * sign, allowCollapse = false)
        Key.Enter, Key.NumPadEnter -> toggleCollapse(state, index)
        else -> return false
    }
    return true
}

// Enter restores a collapsed neighbour, or collapses a collapsible one.
private fun toggleCollapse(state: CamoSplitState, divider: Int) {
    val neighbours = listOf(divider, divider + 1)
    val collapsed = neighbours.firstOrNull { it in state.collapsed }
    when {
        collapsed != null -> state.restore(collapsed)
        else -> neighbours.firstOrNull { it in state.collapsible }?.let { state.collapse(it) }
    }
}
