package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.scrollbar.ScrollbarColors
import com.srctool.camouflage.core.scrollbar.ScrollbarRenderer
import com.srctool.camouflage.core.scrollbar.ScrollbarState
import com.srctool.camouflage.core.scrollbar.ScrollbarStyle
import com.srctool.camouflage.core.scrollbar.ScrollbarVisibility
import com.srctool.camouflage.core.theme.CamoTheme

/** The debug skin's scrollbar: an always-visible gray bar the full width of its lane. */
object DebugScrollbarRenderer : ScrollbarRenderer {
    override fun defaults(theme: CamoTheme) = ScrollbarStyle(
        thickness = 6.dp,
        hoverThickness = 10.dp,
        minThumb = 32.dp,
        visibility = ScrollbarVisibility.Always,
        hideDelayMillis = 1200,
        colors = ScrollbarColors(Color.Gray, Color.DarkGray, Color.Blue, Color.Transparent),
    )

    @Composable
    override fun Render(state: ScrollbarState, theme: CamoTheme, style: ScrollbarStyle, modifier: Modifier) {
        Box(modifier.background(Color.Gray.copy(alpha = 0.3f)))
    }
}
