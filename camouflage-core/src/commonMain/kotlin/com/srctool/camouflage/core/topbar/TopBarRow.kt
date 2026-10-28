package com.srctool.camouflage.core.topbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import com.srctool.camouflage.core.skin.TitleAlign

private const val NAVIGATION = "navigation"
private const val TITLE = "title"
private const val ACTIONS = "actions"

/**
 * The top bar's row, for renderers: [navigation] at the start, [actions] at the end ([actionGap] apart), and
 * [title] at the start between them or centered in the bar's width. A centered title shrinks only when a side
 * would overlap it. Children are centered vertically; the row is at least [minHeight] tall and grows with the
 * title (large font scales).
 */
@Composable
public fun TopBarRow(
    titleAlign: TitleAlign,
    minHeight: Dp,
    actionGap: Dp,
    navigation: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            if (navigation != null) Box(Modifier.layoutId(NAVIGATION)) { navigation() }
            Box(Modifier.layoutId(TITLE)) { title() }
            Row(
                Modifier.layoutId(ACTIONS),
                horizontalArrangement = Arrangement.spacedBy(actionGap),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val nav = measurables.firstOrNull { it.layoutId == NAVIGATION }?.measure(loose)
        val act = measurables.first { it.layoutId == ACTIONS }.measure(loose)
        val navWidth = nav?.width ?: 0
        val titleMeasurable = measurables.first { it.layoutId == TITLE }
        // Unbounded (in a horizontal scroll): as wide as the parts.
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            navWidth + act.width + titleMeasurable.maxIntrinsicWidth(constraints.maxHeight)
        }
        val free = (width - navWidth - act.width).coerceAtLeast(0)
        val titleMax = if (titleAlign == TitleAlign.Center) {
            // Centered in the full width: as wide as the narrower gap allows on both sides.
            (minOf(width / 2 - navWidth, width / 2 - act.width) * 2).coerceIn(0, free)
        } else {
            free
        }
        val ttl = titleMeasurable.measure(loose.copy(maxWidth = titleMax))
        val minPx = minHeight.roundToPx()
        val height = constraints.constrainHeight(maxOf(minPx, nav?.height ?: 0, act.height, ttl.height))
        layout(width, height) {
            nav?.placeRelative(0, (height - nav.height) / 2)
            val titleX = if (titleAlign == TitleAlign.Center) (width - ttl.width) / 2 else navWidth
            ttl.placeRelative(titleX, (height - ttl.height) / 2)
            act.placeRelative(width - act.width, (height - act.height) / 2)
        }
    }
}
