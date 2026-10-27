package com.srctool.camouflage.core.overlay

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.provider.Camouflage

/**
 * A popup attached to the layout it's placed in (its anchor): below it and aligned to its start edge, above it
 * when there's no room below, and never past the window's edges. It's visible while it's in the composition,
 * takes focus (its content may move it on, as the date picker does to its day), and turns an outside tap, back
 * and Esc into [onDismissRequest]. Returning focus to the anchor after it closes is the caller's (the anchor
 * knows what to focus).
 *
 * The building block under menus and the date and time fields' popups (ADR-30); put the content in a
 * `CamoMenuPanel` for the skin's popup surface.
 */
@Composable
public fun CamoAnchoredPopup(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismissRequest)
    val gap = with(LocalDensity.current) { Camouflage.spacing.xs.roundToPx() }
    val focus = remember { FocusRequester() }
    Popup(
        popupPositionProvider = remember(gap) { BelowAnchor(gap) },
        onDismissRequest = { dismiss() },
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier
                .onPreviewKeyEvent {
                    val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                    if (esc) dismiss()
                    esc
                }
                .focusRequester(focus)
                .focusable(),
        ) { content() }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

// Below the anchor, start-aligned; above it when it doesn't fit below; otherwise against the bottom edge.
private class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        val start = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left else anchorBounds.right - width
        val x = start.coerceIn(0, maxOf(0, windowSize.width - width))
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - height
        val y = when {
            below + height <= windowSize.height -> below
            above >= 0 -> above
            else -> maxOf(0, windowSize.height - height)
        }
        return IntOffset(x, y)
    }
}
