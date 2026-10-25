package com.srctool.camouflage.core.tooltip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** A tooltip's colors. */
@Immutable
public data class TooltipColors(val container: Color, val text: Color)

/** Overrides for tooltips. `null` keeps the skin's default. Delays are in milliseconds. */
@Immutable
public data class TooltipTheme(
    val shape: Shape? = null,
    val padding: PaddingValues? = null,
    val offset: Dp? = null,
    val textStyle: TextStyle? = null,
    val showDelayMillis: Int? = null,
    val hideDelayMillis: Int? = null,
    val maxWidth: Dp? = null,
    val colors: TooltipColors? = null,
)

/** The resolved tooltip style: every field set. [hideDelayMillis] applies after a long press on touch. */
@Immutable
public data class TooltipStyle(
    val shape: Shape,
    val padding: PaddingValues,
    val offset: Dp,
    val textStyle: TextStyle,
    val showDelayMillis: Int,
    val hideDelayMillis: Int,
    val maxWidth: Dp,
    val colors: TooltipColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: TooltipTheme?): TooltipStyle = if (overrides == null) {
        this
    } else {
        TooltipStyle(
            shape = overrides.shape ?: shape,
            padding = overrides.padding ?: padding,
            offset = overrides.offset ?: offset,
            textStyle = overrides.textStyle ?: textStyle,
            showDelayMillis = overrides.showDelayMillis ?: showDelayMillis,
            hideDelayMillis = overrides.hideDelayMillis ?: hideDelayMillis,
            maxWidth = overrides.maxWidth ?: maxWidth,
            colors = overrides.colors ?: colors,
        )
    }
}

/** What a tooltip renderer draws. Core owns when it shows and where. */
@Immutable
public data class TooltipState(val text: String, val visible: Boolean)

/** Draws a tooltip's bubble. Core places it above its anchor (below at the top edge) and never focuses it. */
@Stable
public interface TooltipRenderer {
    /** The complete tooltip style for [theme]. */
    public fun defaults(theme: CamoTheme): TooltipStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: TooltipState, theme: CamoTheme, style: TooltipStyle, modifier: Modifier)
}

/**
 * Shows [text] next to [content] on hover (after a delay), on keyboard focus, and on a long press on touch.
 * It never takes focus, and Esc hides it. On a disabled anchor it still shows on hover.
 */
@Composable
public fun CamoTooltip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipBox(text, enabled, anchorLabel = null, modifier, content)
}

// The pointer may move from the anchor onto the bubble without the bubble disappearing (WCAG 1.4.13).
private const val HOVER_GRACE_MILLIS = 150L

@Composable
internal fun TooltipBox(
    text: String,
    enabled: Boolean,
    anchorLabel: String?,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoTooltip")
    val theme = requireTheme("CamoTooltip")
    val style = remember(skin, theme) { skin.tooltip.defaults(theme).merge(theme.components.tooltip) }
    val scope = rememberCoroutineScope()
    val inputMode = LocalInputModeManager.current
    val visibility = remember { TooltipVisibility() }
    fun after(millis: Long, show: Boolean) {
        visibility.timer?.cancel()
        visibility.timer = scope.launch {
            delay(millis)
            visibility.visible = show
        }
    }

    val triggers = if (enabled) {
        modifier.tooltipTriggers(
            style = style,
            keyboard = { inputMode.inputMode == InputMode.Keyboard },
            visible = { visibility.visible },
            after = ::after,
            showNow = {
                visibility.timer?.cancel()
                visibility.visible = true
            },
        )
    } else {
        modifier
    }
    val described = if (!enabled || text == anchorLabel) {
        triggers
    } else {
        triggers.semantics {
            onLongClick(label = text) {
                visibility.visible = true
                true
            }
        }
    }
    Box(described) {
        content()
        if (visibility.visible) {
            TooltipPopup(
                text,
                style,
                theme,
                onHover = { visibility.timer?.cancel() },
                onLeave = { after(HOVER_GRACE_MILLIS, false) },
            )
        }
    }
}

private class TooltipVisibility {
    var visible by mutableStateOf(false)
    var timer: Job? = null
}

// Hover (after the delay), keyboard focus, and a long press on touch; Esc hides. Nothing is consumed, so the
// anchor's own clicks still work.
private fun Modifier.tooltipTriggers(
    style: TooltipStyle,
    keyboard: () -> Boolean,
    visible: () -> Boolean,
    after: (Long, Boolean) -> Unit,
    showNow: () -> Unit,
): Modifier = this
    .pointerInput(style) {
        awaitPointerEventScope {
            while (true) {
                when (awaitPointerEvent(PointerEventPass.Initial).type) {
                    PointerEventType.Enter -> after(style.showDelayMillis.toLong(), true)
                    PointerEventType.Exit -> after(HOVER_GRACE_MILLIS, false)
                }
            }
        }
    }
    .pointerInput(style) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type == PointerType.Touch) {
                val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    waitForUpOrCancellation(PointerEventPass.Initial)
                }
                if (up == null) {
                    showNow()
                    waitForUpOrCancellation(PointerEventPass.Initial)
                    after(style.hideDelayMillis.toLong(), false)
                }
            }
        }
    }
    .onFocusChanged {
        when {
            it.hasFocus && keyboard() -> after(0, true)
            !it.hasFocus -> after(0, false)
        }
    }
    .onPreviewKeyEvent {
        val hide = visible() && it.key == Key.Escape
        if (hide) after(0, false)
        hide
    }

@Composable
private fun TooltipPopup(
    text: String,
    style: TooltipStyle,
    theme: CamoTheme,
    onHover: () -> Unit,
    onLeave: () -> Unit,
) {
    val offset = with(LocalDensity.current) { style.offset.roundToPx() }
    Popup(
        popupPositionProvider = remember(offset) { AboveAnchor(offset) },
        properties = PopupProperties(focusable = false, dismissOnClickOutside = false),
    ) {
        requireSkin("CamoTooltip").tooltip.Render(
            TooltipState(text, visible = true),
            theme,
            style,
            Modifier
                .pointerInput(style) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> onHover()
                                PointerEventType.Exit -> onLeave()
                            }
                        }
                    }
                }
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

// Centered above the anchor; below it when there's no room above. Kept inside the window horizontally.
private class AboveAnchor(private val offset: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2)
            .coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val above = anchorBounds.top - offset - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.bottom + offset
        return IntOffset(x, y)
    }
}
