package com.srctool.camouflage.skin.carbon.renderer

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.scrollbar.ScrollbarColors
import com.srctool.camouflage.core.scrollbar.ScrollbarRenderer
import com.srctool.camouflage.core.scrollbar.ScrollbarState
import com.srctool.camouflage.core.scrollbar.ScrollbarStyle
import com.srctool.camouflage.core.scrollbar.ScrollbarVisibility
import com.srctool.camouflage.core.theme.CamoTheme

// Carbon: a square 6 dp thumb that grows to 10 dp on a transparent track; shows while scrolling.
internal object CarbonScrollbarRenderer : ScrollbarRenderer {
    override fun defaults(theme: CamoTheme): ScrollbarStyle {
        val ink = theme.colors.onSurfaceVariant
        return ScrollbarStyle(
            thickness = 6.dp,
            hoverThickness = 10.dp,
            minThumb = 32.dp,
            visibility = ScrollbarVisibility.OnScroll,
            hideDelayMillis = 1200,
            colors = ScrollbarColors(
                thumb = ink.copy(alpha = 0.4f),
                thumbHover = ink.copy(alpha = 0.6f),
                thumbDrag = ink.copy(alpha = 0.8f),
                track = Color.Transparent,
            ),
        )
    }

    @Composable
    override fun Render(state: ScrollbarState, theme: CamoTheme, style: ScrollbarStyle, modifier: Modifier) {
        val active = state.hovered || state.dragging
        val color = when {
            state.dragging -> style.colors.thumbDrag
            state.hovered -> style.colors.thumbHover
            else -> style.colors.thumb
        }
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            modifier.drawBehind {
                val thin = style.thickness.toPx()
                val wide = style.hoverThickness.toPx()
                val width = if (active) wide else thin
                val vertical = state.orientation == Orientation.Vertical
                val round = 0f
                // The lane is as wide as the widest thumb; the thumb sits against the end edge.
                if (active && style.colors.track != Color.Transparent) {
                    drawRect(style.colors.track)
                }
                val lateral = if (vertical) {
                    if (rtl) 0f else size.width - width
                } else {
                    size.height - width
                }
                val topLeft = if (vertical) Offset(lateral, state.thumbStart) else Offset(state.thumbStart, lateral)
                val extent = if (vertical) Size(width, state.thumbLength) else Size(state.thumbLength, width)
                drawRoundRect(color, topLeft, extent, CornerRadius(round * width / 2, round * width / 2))
            },
        )
    }
}
