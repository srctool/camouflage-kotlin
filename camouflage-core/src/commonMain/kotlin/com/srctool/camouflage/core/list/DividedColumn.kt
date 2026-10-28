package com.srctool.camouflage.core.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme

/**
 * A column that draws the skin's divider line between consecutive children, never after the last: edge to edge
 * for `Full`, from [insetStart] for `Inset`, none for `None`. Shared by list sections and accordion groups.
 */
@Composable
internal fun DividedColumn(
    dividers: ListDividers,
    insetStart: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoListSection")
    val theme = requireTheme("CamoListSection")
    val line = remember(skin, theme) { skin.divider.defaults(theme).merge(theme.components.divider) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The y of each gap between children, written in placement and read in drawing (same frame, after layout).
    val gaps = remember { FloatArrayList() }
    val draw = if (dividers == ListDividers.None) {
        Modifier
    } else {
        Modifier.drawWithContent {
            drawContent()
            val thickness = line.thickness.toPx()
            val inset = if (dividers == ListDividers.Inset) insetStart.toPx() else 0f
            val start = if (rtl) 0f else inset
            val end = if (rtl) size.width - inset else size.width
            for (i in 0 until gaps.size) {
                val y = gaps[i] - thickness / 2
                drawLine(line.color, Offset(start, y), Offset(end, y), thickness)
            }
        }
    }
    Layout(content, modifier.then(draw)) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val width = placeables.maxOfOrNull { it.width }?.coerceAtLeast(constraints.minWidth) ?: constraints.minWidth
        val height = placeables.sumOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            gaps.clear()
            var y = 0
            placeables.forEachIndexed { i, p ->
                if (i > 0) gaps.add(y.toFloat())
                p.placeRelative(0, y)
                y += p.height
            }
        }
    }
}

/** A growable list of floats without boxing. */
internal class FloatArrayList {
    private var values = FloatArray(INITIAL_CAPACITY)

    var size: Int = 0
        private set

    operator fun get(i: Int): Float = values[i]

    fun add(v: Float) {
        if (size == values.size) values = values.copyOf(size * 2)
        values[size++] = v
    }

    fun clear() {
        size = 0
    }

    private companion object {
        const val INITIAL_CAPACITY = 8
    }
}
