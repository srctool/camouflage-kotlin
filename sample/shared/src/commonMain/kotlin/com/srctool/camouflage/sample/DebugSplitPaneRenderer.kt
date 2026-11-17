package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.splitpane.SplitDividerState
import com.srctool.camouflage.core.splitpane.SplitPaneColors
import com.srctool.camouflage.core.splitpane.SplitPaneRenderer
import com.srctool.camouflage.core.splitpane.SplitPaneStyle
import com.srctool.camouflage.core.theme.CamoTheme

/** The debug skin's divider: a flat gray bar that turns blue while dragged. */
object DebugSplitPaneRenderer : SplitPaneRenderer {
    override fun defaults(theme: CamoTheme) = SplitPaneStyle(
        thickness = 2.dp,
        hitSize = 8.dp,
        keyStep = 16.dp,
        keyStepLarge = 64.dp,
        grip = false,
        colors = SplitPaneColors(Color.Gray, Color.DarkGray, Color.Blue, Color.Red),
    )

    @Composable
    override fun Divider(state: SplitDividerState, theme: CamoTheme, style: SplitPaneStyle, modifier: Modifier) {
        Box(modifier.background(if (state.dragging) style.colors.dragging else style.colors.line))
    }
}
