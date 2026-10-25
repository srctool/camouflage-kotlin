package com.srctool.camouflage.core.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The smallest touch target a component gets, whatever its visual size (WCAG 2.5.8, platform guidelines). */
public val MinimumTouchTarget: Dp = 48.dp

/**
 * Reports at least [size] × [size] and centers the content in it, so a smaller visual box keeps a full touch
 * target. Put it after the click handling in a behavior modifier: clicks then land anywhere in the target,
 * while the renderer's visuals keep their own size.
 */
public fun Modifier.minimumTouchTarget(size: Dp = MinimumTouchTarget): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val min = size.roundToPx()
    val width = maxOf(placeable.width, min).coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = maxOf(placeable.height, min).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) {
        placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
    }
}
