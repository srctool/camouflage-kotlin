package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.fab.FabColors
import com.srctool.camouflage.core.fab.FabRenderer
import com.srctool.camouflage.core.fab.FabSlots
import com.srctool.camouflage.core.fab.FabState
import com.srctool.camouflage.core.fab.FabSize
import com.srctool.camouflage.core.fab.FabStyle
import com.srctool.camouflage.core.tabs.IndicatorWidth
import com.srctool.camouflage.core.tabs.TabIndicator
import com.srctool.camouflage.core.tabs.TabSlots
import com.srctool.camouflage.core.tabs.TabState
import com.srctool.camouflage.core.tabs.TabsColors
import com.srctool.camouflage.core.tabs.TabsRenderer
import com.srctool.camouflage.core.tabs.TabsState
import com.srctool.camouflage.core.tabs.TabsStyle
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.icon.IconStyle

// The M8 tabs and FAB in the plain test skin: boxes, rows and text, no decoration.

/** The indicator the plain tab bar last drew, for tests to read. */
internal var lastIndicator: TabIndicator? = null

internal object PlainTabs : TabsRenderer {
    override fun defaults(theme: CamoTheme): TabsStyle {
        val c = theme.colors
        val colors = TabsColors(c.surface, c.primary, c.onSurfaceVariant, c.primary, c.outlineVariant)
        return TabsStyle(
            Appearance.Solid, 48.dp, 64.dp, 3.dp, IndicatorWidth.Tab, theme.typography.titleSmall, 24.dp, 8.dp,
            16.dp, 90.dp, true, colors, colors,
        )
    }

    @Composable
    override fun Container(
        state: TabsState,
        theme: CamoTheme,
        style: TabsStyle,
        modifier: Modifier,
        row: @Composable (Modifier) -> Unit,
    ) {
        Box(modifier) { row(Modifier.drawBehind { lastIndicator = state.indicator() }) }
    }

    @Composable
    override fun Tab(state: TabState, slots: TabSlots, theme: CamoTheme, style: TabsStyle, modifier: Modifier) {
        val color = if (state.selected) style.solidColors.selected else style.solidColors.unselected
        Box(modifier.padding(horizontal = style.tabPadding)) {
            ProvideContentStyle(color, IconStyle(style.iconSize, color)) {
                Row(slots.contentModifier) {
                    val content = slots.content
                    if (content != null) {
                        content()
                    } else {
                        slots.icon?.invoke()
                        slots.label?.let { BasicText(it) }
                        slots.badge?.invoke()
                    }
                }
            }
        }
    }
}

internal object PlainFab : FabRenderer {
    override fun defaults(theme: CamoTheme): FabStyle {
        val c = theme.colors
        return FabStyle(
            Appearance.Soft, FabSize.Regular, 40.dp, 56.dp, 96.dp, 56.dp, RectangleShape, RectangleShape,
            RectangleShape, 24.dp, 36.dp, 8.dp, 16.dp, theme.typography.labelLarge, ElevationLevel.Level3,
            FabColors(c.primaryContainer, c.onPrimaryContainer), FabColors(c.primary, c.onPrimary),
            FabColors(c.surfaceContainerHigh, c.primary),
        )
    }

    @Composable
    override fun Render(state: FabState, slots: FabSlots, theme: CamoTheme, style: FabStyle, modifier: Modifier) {
        val colors = style.colorsFor(state, theme)
        val showText = state.extended && state.expanded
        Row(
            modifier.size(style.containerSize).background(colors.container),
        ) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                slots.icon()
                if (showText) BasicText(checkNotNull(state.text))
            }
        }
    }
}
