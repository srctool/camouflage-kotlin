package com.srctool.camouflage.core.menu

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

// Below the anchor, start-aligned; above it when it doesn't fit below; otherwise against the bottom edge. The same
// rule as `CamoAnchoredPopup`, for a menu that lays out its own levels in one window-sized layer.
internal fun belowAnchor(
    anchor: IntRect,
    window: IntSize,
    direction: LayoutDirection,
    content: IntSize,
    gap: Int,
): IntOffset {
    val start = if (direction == LayoutDirection.Ltr) anchor.left else anchor.right - content.width
    val x = start.coerceIn(0, maxOf(0, window.width - content.width))
    val below = anchor.bottom + gap
    val above = anchor.top - gap - content.height
    val y = when {
        below + content.height <= window.height -> below
        above >= 0 -> above
        else -> maxOf(0, window.height - content.height)
    }
    return IntOffset(x, y)
}

// Beside the anchor on its end side with its top at the anchor's top (a submenu next to its row); the start side
// when it doesn't fit; shifted up near the bottom edge and never past the window's edges.
internal fun besideAnchor(
    anchor: IntRect,
    window: IntSize,
    direction: LayoutDirection,
    content: IntSize,
): IntOffset {
    val rtl = direction == LayoutDirection.Rtl
    val atEnd = if (rtl) anchor.left - content.width else anchor.right
    val atStart = if (rtl) anchor.right else anchor.left - content.width
    val endFits = atEnd >= 0 && atEnd + content.width <= window.width
    val startFits = atStart >= 0 && atStart + content.width <= window.width
    val x = when {
        endFits -> atEnd
        startFits -> atStart
        else -> atEnd.coerceIn(0, maxOf(0, window.width - content.width))
    }
    val y = anchor.top.coerceIn(0, maxOf(0, window.height - content.height))
    return IntOffset(x, y)
}
