package com.srctool.camouflage.core.popover

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel

/** Where a popover opens relative to its anchor. `Start` and `End` follow the reading direction. */
public enum class PopoverPlacement { Auto, Bottom, Top, Start, End }

/** Every color a popover uses. [border] is drawn only when set. */
@Immutable
public data class PopoverColors(val container: Color, val title: Color, val content: Color, val border: Color? = null)

/** Overrides for popovers. `null` keeps the skin's default. */
@Immutable
public data class PopoverTheme(
    val shape: Shape? = null,
    val maxWidth: Dp? = null,
    val padding: Dp? = null,
    val showArrow: Boolean? = null,
    val arrowSize: DpSize? = null,
    val elevation: ElevationLevel? = null,
    val titleStyle: TextStyle? = null,
    val gap: Dp? = null,
    val colors: PopoverColors? = null,
)

/** The resolved popovers style: every field set. */
@Immutable
public data class PopoverStyle(
    val shape: Shape,
    val maxWidth: Dp,
    val padding: Dp,
    val showArrow: Boolean,
    val arrowSize: DpSize,
    val elevation: ElevationLevel,
    val titleStyle: TextStyle,
    val gap: Dp,
    val colors: PopoverColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: PopoverTheme?): PopoverStyle = if (overrides == null) {
        this
    } else {
        PopoverStyle(
            shape = overrides.shape ?: shape,
            maxWidth = overrides.maxWidth ?: maxWidth,
            padding = overrides.padding ?: padding,
            showArrow = overrides.showArrow ?: showArrow,
            arrowSize = overrides.arrowSize ?: arrowSize,
            elevation = overrides.elevation ?: elevation,
            titleStyle = overrides.titleStyle ?: titleStyle,
            gap = overrides.gap ?: gap,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What the popover renderer draws. [placement] is the side it opened on after any flipping (never `Auto`);
 * [arrowOffset] is where the anchor's center is along the panel's edge, measured from the panel's top (for
 * `Start` and `End`) or its left edge (for `Bottom` and `Top`), so an arrow can point at the anchor.
 */
@Immutable
public data class PopoverState(val placement: PopoverPlacement, val arrowOffset: Dp)

/** A popover content: the optional [title], the [content] and the [actions] at the bottom end. */
@Stable
public class PopoverSlots(
    public val title: String?,
    public val content: @Composable ColumnScope.() -> Unit,
    public val actions: (@Composable RowScope.() -> Unit)?,
)

/** Draws popovers. Apply `modifier` (the semantics) to the outermost node. */
@Stable
public interface PopoverRenderer {
    /** The complete popover style for [theme]. */
    public fun defaults(theme: CamoTheme): PopoverStyle

    /** Draws the panel (and its arrow, when the style asks for one) with [slots]. */
    @Composable
    public fun Render(
        state: PopoverState,
        slots: PopoverSlots,
        theme: CamoTheme,
        style: PopoverStyle,
        modifier: Modifier,
    )
}

/** Where a popover ended up: the side, the top-left corner of its panel and where the arrow points. */
internal data class PopoverLayout(val placement: PopoverPlacement, val offset: IntOffset, val arrow: Int)

/**
 * Places a panel of [size] next to [anchor] inside [window]: on the [requested] side when it fits, else the opposite
 * one, else the other axis; centered on the anchor and kept inside the window. `Auto` is below, then above.
 * `Start` and `End` are left and right in a left-to-right layout.
 */
internal fun placePopover(
    anchor: IntRect,
    size: IntSize,
    window: IntSize,
    requested: PopoverPlacement,
    gap: Int,
    ltr: Boolean,
): PopoverLayout {
    val order = when (requested) {
        PopoverPlacement.Auto, PopoverPlacement.Bottom -> listOf(PopoverPlacement.Bottom, PopoverPlacement.Top)
        PopoverPlacement.Top -> listOf(PopoverPlacement.Top, PopoverPlacement.Bottom)
        PopoverPlacement.Start -> listOf(PopoverPlacement.Start, PopoverPlacement.End, PopoverPlacement.Bottom)
        PopoverPlacement.End -> listOf(PopoverPlacement.End, PopoverPlacement.Start, PopoverPlacement.Bottom)
    }
    val side = order.firstOrNull { fits(it, anchor, size, window, gap, ltr) } ?: order.first()
    val maxX = maxOf(0, window.width - size.width)
    val maxY = maxOf(0, window.height - size.height)
    if (side == PopoverPlacement.Bottom || side == PopoverPlacement.Top) {
        val x = (anchor.center.x - size.width / 2).coerceIn(0, maxX)
        val y = if (side == PopoverPlacement.Bottom) anchor.bottom + gap else anchor.top - gap - size.height
        return PopoverLayout(side, IntOffset(x, y.coerceIn(0, maxY)), (anchor.center.x - x).coerceIn(0, size.width))
    }
    val left = (side == PopoverPlacement.Start) == ltr
    val x = if (left) anchor.left - gap - size.width else anchor.right + gap
    val y = (anchor.center.y - size.height / 2).coerceIn(0, maxY)
    return PopoverLayout(side, IntOffset(x.coerceIn(0, maxX), y), (anchor.center.y - y).coerceIn(0, size.height))
}

// Whether a panel of [size] fits on [side] of [anchor]; Start and End are left and right in a left-to-right layout.
private fun fits(side: PopoverPlacement, anchor: IntRect, size: IntSize, window: IntSize, gap: Int, ltr: Boolean) =
    when (side) {
        PopoverPlacement.Bottom -> anchor.bottom + gap + size.height <= window.height
        PopoverPlacement.Top -> anchor.top - gap - size.height >= 0
        else -> {
            val onRight = (side == PopoverPlacement.End) == ltr
            if (onRight) anchor.right + gap + size.width <= window.width else anchor.left - gap - size.width >= 0
        }
    }

// Positions the popup from the layout the composable computes, and reports the result back.
private class PopoverPosition(
    private val requested: PopoverPlacement,
    private val gap: Int,
    private val onLayout: (PopoverLayout) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val layout = placePopover(
            anchorBounds, popupContentSize, windowSize, requested, gap, layoutDirection == LayoutDirection.Ltr,
        )
        onLayout(layout)
        return layout.offset
    }
}

/**
 * A non-modal panel anchored to [anchor] for content richer than a menu and lighter than a dialog: a few lines,
 * a small form, one or two [actions]. Its visibility is state ([open]), like every controlled component: the
 * app's button toggles it. It opens on the [placement] side (below by default, above when there is no room), and
 * an outside tap, back and Esc all call [onDismissRequest]. Focus moves into it when it opens and back to the
 * anchor when it closes. The anchor should be one control (a button): it announces that it has a popup.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod") // the anchor, the popup, the focus and the key share state
@Composable
public fun CamoPopover(
    open: Boolean,
    onDismissRequest: () -> Unit,
    anchor: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    placement: PopoverPlacement = PopoverPlacement.Auto,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = requireSkin("CamoPopover")
    val theme = requireTheme("CamoPopover")
    val style = remember(skin, theme) { skin.popover.defaults(theme).merge(theme.components.popover) }
    val dismiss by rememberUpdatedState(onDismissRequest)
    val opener = remember { FocusRequester() }
    val panel = remember { FocusRequester() }
    var layout by remember { mutableStateOf<PopoverLayout?>(null) }
    val gap = with(LocalDensity.current) { style.gap.roundToPx() }
    var wasOpen by remember { mutableStateOf(open) }
    LaunchedEffect(open) {
        if (wasOpen && !open) runCatching { opener.requestFocus() }
        wasOpen = open
        if (!open) layout = null
    }
    Box(
        modifier
            .focusRequester(opener)
            .focusGroup()
            .semantics(mergeDescendants = true) { stateDescription = if (open) "Expanded" else "Collapsed" },
    ) {
        anchor()
        if (open) {
            Popup(
                popupPositionProvider = remember(placement, gap) {
                    PopoverPosition(placement, gap) { if (layout != it) layout = it }
                },
                onDismissRequest = { dismiss() },
                properties = PopupProperties(focusable = true),
            ) {
                val resolved = layout
                val density = LocalDensity.current
                Box(
                    Modifier
                        .alpha(if (resolved == null) 0f else 1f)
                        .onPreviewKeyEvent {
                            val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                            if (esc) dismiss()
                            esc
                        }
                        .focusRequester(panel)
                        .focusable(),
                ) {
                    skin.popover.Render(
                        PopoverState(
                            resolved?.placement ?: PopoverPlacement.Bottom,
                            with(density) { (resolved?.arrow ?: 0).toDp() },
                        ),
                        PopoverSlots(title, content, actions),
                        theme,
                        style,
                        Modifier.semantics { if (title != null) paneTitle = title },
                    )
                }
                LaunchedEffect(Unit) { panel.requestFocus() }
            }
        }
    }
}
