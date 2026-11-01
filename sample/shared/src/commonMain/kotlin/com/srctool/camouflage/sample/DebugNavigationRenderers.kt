package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.fab.FabColors
import com.srctool.camouflage.core.fab.FabRenderer
import com.srctool.camouflage.core.fab.FabSize
import com.srctool.camouflage.core.fab.FabSlots
import com.srctool.camouflage.core.fab.FabState
import com.srctool.camouflage.core.fab.FabStyle
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.tabs.IndicatorWidth
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

// The M8 tabs and FAB in the debug skin: outlined boxes, a square indicator and plain text.

object DebugTabsRenderer : TabsRenderer {
    override fun defaults(theme: CamoTheme): TabsStyle {
        val c = theme.colors
        val colors = TabsColors(c.surface, c.primary, c.onSurfaceVariant, c.primary, c.outline)
        return TabsStyle(
            Appearance.Solid, 48.dp, 64.dp, 3.dp, IndicatorWidth.Tab, theme.typography.labelLarge, 18.dp, 4.dp,
            16.dp, 80.dp, true, colors, colors,
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
        val colors = style.colorsFor(state.appearance)
        Box(modifier.background(colors.container).border(1.dp, colors.divider)) {
            row(
                Modifier.drawBehind {
                    val indicator = state.indicator() ?: return@drawBehind
                    val height = style.indicatorHeight.toPx()
                    drawRect(
                        colors.indicator,
                        Offset(indicator.start, size.height - height),
                        Size(indicator.width, height),
                    )
                },
            )
        }
    }

    @Composable
    override fun Tab(state: TabState, slots: TabSlots, theme: CamoTheme, style: TabsStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.appearance)
        val content = if (state.selected) colors.selected else colors.unselected
        Box(
            modifier
                .heightIn(min = style.height)
                .widthIn(min = if (state.scrollable) style.minTabWidth else 0.dp)
                .padding(horizontal = style.tabPadding),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                Row(
                    slots.contentModifier,
                    horizontalArrangement = Arrangement.spacedBy(style.iconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val custom = slots.content
                    if (custom != null) {
                        custom()
                    } else {
                        slots.icon?.invoke()
                        slots.label?.let { BasicText(it, style = style.labelStyle.copy(color = content)) }
                        slots.badge?.invoke()
                    }
                }
            }
        }
    }
}

object DebugFabRenderer : FabRenderer {
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
        val size = if (state.extended) {
            Modifier.heightIn(min = style.extendedHeight).widthIn(min = style.extendedHeight)
        } else {
            Modifier.size(style.containerSize)
        }
        Box(
            modifier
                .then(size)
                .background(colors.container)
                .border(1.dp, colors.content)
                .padding(horizontal = if (state.extended) style.extendedPadding else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    slots.icon()
                    if (state.extended && state.expanded) {
                        BasicText(
                            state.text.orEmpty(),
                            Modifier.padding(start = style.iconGap),
                            style = style.textStyle.copy(color = colors.content),
                        )
                    }
                }
            }
        }
    }
}
