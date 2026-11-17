package com.srctool.camouflage.skin.cupertino.renderer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.splitpane.SplitDividerState
import com.srctool.camouflage.core.splitpane.SplitPaneColors
import com.srctool.camouflage.core.splitpane.SplitPaneRenderer
import com.srctool.camouflage.core.splitpane.SplitPaneStyle
import com.srctool.camouflage.core.theme.CamoTheme

// Cupertino: a hairline (half a dp) in outlineVariant; outline on hover, primary while dragged.
internal object CupertinoSplitPaneRenderer : SplitPaneRenderer {
    override fun defaults(theme: CamoTheme): SplitPaneStyle {
        val c = theme.colors
        return SplitPaneStyle(
            thickness = 0.5.dp,
            hitSize = 8.dp,
            keyStep = 16.dp,
            keyStepLarge = 64.dp,
            grip = false,
            colors = SplitPaneColors(
                line = c.outlineVariant,
                hover = c.outline,
                dragging = c.primary,
                focus = c.primary,
            ),
        )
    }

    @Composable
    override fun Divider(state: SplitDividerState, theme: CamoTheme, style: SplitPaneStyle, modifier: Modifier) {
        val colors = style.colors
        val color = when {
            state.dragging -> colors.dragging
            state.hovered -> colors.hover
            else -> colors.line
        }
        val vertical = state.axis == Orientation.Horizontal // a side-by-side split has vertical lines
        Box(
            modifier.drawBehind {
                val line = style.thickness.toPx().coerceAtLeast(1f)
                if (vertical) {
                    drawRect(color, Offset((size.width - line) / 2, 0f), Size(line, size.height))
                } else {
                    drawRect(color, Offset(0f, (size.height - line) / 2), Size(size.width, line))
                }
                if (state.focused) drawRect(colors.focus, style = Stroke(FOCUS_RING.dp.toPx()))
            },
        )
    }
}

private const val FOCUS_RING = 2
