package com.srctool.camouflage.core.menu

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.provider.Camouflage
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

/**
 * A menu with submenus in one popup layer as big as the window: the levels are siblings placed by their anchor,
 * so keys, hover and clicks share one focus owner (a second popup for a submenu either takes the parent's pointer
 * or can't take keys), and a tap outside every panel lands on this layer and closes the whole chain.
 */
@Composable
internal fun NestedMenuPopup(items: List<CamoMenuEntry>, scope: CoroutineScope, onClose: () -> Unit) {
    val dismiss by rememberUpdatedState(onClose)
    var anchor by remember { mutableStateOf<IntRect?>(null) }
    val focus = remember { FocusRequester() }
    val open = remember { mutableListOf<() -> Unit>() } // each open submenu's closer, the deepest last
    val provider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                if (anchor != anchorBounds) anchor = anchorBounds
                return IntOffset.Zero
            }
        }
    }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = { dismiss() },
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { dismiss() } }
                // After the levels have their say: a submenu takes Esc and the arrow back itself.
                .onKeyEvent {
                    val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                    // Esc closes the deepest open submenu first, then the menu.
                    if (esc) open.lastOrNull()?.invoke() ?: dismiss()
                    esc
                }
                .focusRequester(focus)
                .focusable(),
        ) {
            anchor?.let { MenuLevel(items, it, below = true, scope, onClose, open, onBack = null, onHover = {}) }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

private class OpenSubmenu(val index: Int, val submenu: CamoMenuSubmenu, val rowBounds: IntRect)

// One level: its panel placed at [anchor], and the submenu open in it (if any) as a sibling beside the row.
@Composable
private fun MenuLevel(
    items: List<CamoMenuEntry>,
    anchor: IntRect,
    below: Boolean,
    scope: CoroutineScope,
    onClose: () -> Unit,
    openChain: MutableList<() -> Unit>,
    onBack: (() -> Unit)?,
    onHover: (Boolean) -> Unit,
) {
    var open by remember { mutableStateOf<OpenSubmenu?>(null) }
    if (open != null) {
        DisposableEffect(open) {
            val close = { open = null }
            openChain += close
            onDispose { openChain -= close }
        }
    }
    var childHovered by remember { mutableStateOf(false) }
    val gap = with(LocalDensity.current) { Camouflage.spacing.xs.roundToPx() }
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    LaunchedEffect(hovered) { onHover(hovered) }
    Layout(
        {
            CamoMenuPanel(Modifier.hoverable(hoverSource)) {
                MenuEntries(
                    items,
                    scope,
                    onClose,
                    minWidth = 0.dp,
                    onBack = onBack,
                    open = open?.index ?: -1,
                    childHovered = childHovered,
                    onOpen = { index, submenu, bounds -> open = OpenSubmenu(index, submenu, bounds) },
                    onCloseChild = { open = null },
                )
            }
        },
    ) { measurables, constraints ->
        val window = IntSize(constraints.maxWidth, constraints.maxHeight)
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(window.width, window.height) {
            val size = IntSize(placeable.width, placeable.height)
            placeable.place(
                if (below) belowAnchor(anchor, window, layoutDirection, size, gap)
                else besideAnchor(anchor, window, layoutDirection, size),
            )
        }
    }
    open?.let { child ->
        MenuLevel(
            child.submenu.items,
            child.rowBounds,
            below = false,
            scope,
            onClose,
            openChain,
            onBack = { open = null },
            onHover = { childHovered = it },
        )
    }
}

internal fun Rect.roundToIntRect(): IntRect =
    IntRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
