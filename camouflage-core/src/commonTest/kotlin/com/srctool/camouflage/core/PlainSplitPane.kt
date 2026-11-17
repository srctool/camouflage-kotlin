package com.srctool.camouflage.core

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

// The split pane's test renderer: a gray line, and a record of the last divider state it was asked to draw.
internal object PlainSplitPane : SplitPaneRenderer {
    var lastDivider: SplitDividerState? = null

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
        lastDivider = state
        Box(modifier.background(style.colors.line))
    }
}
