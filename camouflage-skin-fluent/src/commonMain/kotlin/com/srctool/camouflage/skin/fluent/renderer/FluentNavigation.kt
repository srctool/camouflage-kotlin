package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.fab.FabColors
import com.srctool.camouflage.core.fab.FabRenderer
import com.srctool.camouflage.core.fab.FabSize
import com.srctool.camouflage.core.fab.FabSlots
import com.srctool.camouflage.core.fab.FabState
import com.srctool.camouflage.core.fab.FabStyle
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.provider.Camouflage
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
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.rememberSkinIndication

// Fluent's TabList (a short rounded brand underline, no divider) and its FAB: Fluent has none, so the skin draws a
// prominent circular brand button with depth (base Fluent Skin §3.4).

private val Hairline = 1.dp
private const val DISABLED = 0.38f

internal object FluentTabsRenderer : TabsRenderer {
    override fun defaults(theme: CamoTheme): TabsStyle {
        val c = theme.colors
        return TabsStyle(
            defaultAppearance = Appearance.Solid,
            height = 44.dp,
            heightWithIcon = 60.dp,
            indicatorHeight = 2.dp,
            indicatorWidth = IndicatorWidth.Content,
            labelStyle = theme.typography.bodyMedium,
            iconSize = 20.dp,
            iconGap = 6.dp,
            tabPadding = 12.dp,
            minTabWidth = 80.dp,
            showDivider = false,
            solidColors = TabsColors(Color.Transparent, c.onSurface, c.onSurfaceVariant, c.primary, c.outlineVariant),
            ghostColors = TabsColors(
                Color.Transparent, c.onSurface, c.onSurfaceVariant, c.onSurfaceVariant, Color.Transparent,
            ),
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
        val thickness = style.indicatorHeight
        Box(
            modifier
                .drawBehind {
                    drawRect(colors.container)
                    if (style.showDivider) {
                        val line = Hairline.toPx()
                        drawRect(colors.divider, Offset(0f, size.height - line), Size(size.width, line))
                    }
                },
        ) {
            row(
                Modifier.drawBehind {
                    val indicator = state.indicator() ?: return@drawBehind
                    val height = thickness.toPx()
                    drawRoundRect(
                        colors.indicator,
                        Offset(indicator.start, size.height - height),
                        Size(indicator.width, height),
                        CornerRadius(height / 2),
                    )
                },
            )
        }
    }

    @Composable
    override fun Tab(state: TabState, slots: TabSlots, theme: CamoTheme, style: TabsStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.appearance)
        val content = if (state.selected) colors.selected else colors.unselected
        val stacked = slots.content == null && slots.icon != null && slots.label != null
        val height = if (stacked) style.heightWithIcon else style.height
        val minWidth = if (state.scrollable) style.minTabWidth else 0.dp
        Box(
            modifier
                .fillMaxHeight()
                .heightIn(min = height)
                .widthIn(min = minWidth)
                .indication(state.interactionSource, rememberSkinIndication(content, theme.shapes.small))
                .alpha(if (state.enabled) 1f else DISABLED)
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
                        slots.label?.let {
                            BasicText(
                                it,
                                style = style.labelStyle.copy(color = content),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        slots.badge?.invoke()
                    }
                }
            }
        }
    }
}

internal object FluentFabRenderer : FabRenderer {
    override fun defaults(theme: CamoTheme): FabStyle {
        val c = theme.colors
        return FabStyle(
            defaultAppearance = Appearance.Solid,
            size = FabSize.Regular,
            sizeSmall = 40.dp,
            sizeRegular = 48.dp,
            sizeLarge = 56.dp,
            extendedHeight = 48.dp,
            shapeSmall = theme.shapes.full,
            shapeRegular = theme.shapes.full,
            shapeLarge = theme.shapes.full,
            iconSizeRegular = 24.dp,
            iconSizeLarge = 28.dp,
            iconGap = 8.dp,
            extendedPadding = 12.dp,
            textStyle = theme.typography.labelLarge,
            elevation = ElevationLevel.Level3,
            softColors = FabColors(c.surfaceContainerHigh, c.onSurface),
            solidColors = FabColors(c.primary, c.onPrimary),
            raisedColors = FabColors(c.surface, c.primary),
        )
    }

    @Composable
    override fun Render(state: FabState, slots: FabSlots, theme: CamoTheme, style: FabStyle, modifier: Modifier) {
        val colors = style.colorsFor(state, theme)
        val extended = state.extended
        val size = if (extended) {
            Modifier.heightIn(min = style.extendedHeight).widthIn(min = style.extendedHeight)
        } else {
            Modifier.size(style.containerSize)
        }
        val depth = theme.elevation.of(style.elevation)
        val border = if (state.appearance == Appearance.Raised) {
            Modifier.border(Hairline, theme.colors.outlineVariant, style.shape)
        } else {
            Modifier
        }
        val spec = tween<IntSize>(theme.motion.durationMedium)
        val fade = tween<Float>(theme.motion.durationMedium)
        val width = tween<IntSize>(theme.motion.durationMedium)
        Box(
            modifier
                .animateContentSize(spec)
                .then(size)
                .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape, depth, theme))
                .then(border)
                .indication(state.interactionSource, rememberSkinIndication(colors.content, style.shape))
                .padding(horizontal = if (extended) style.extendedPadding else 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    slots.icon()
                    AnimatedVisibility(
                        extended && state.expanded,
                        enter = fadeIn(fade) + expandHorizontally(width),
                        exit = fadeOut(fade) + shrinkHorizontally(width),
                    ) {
                        Row {
                            Box(Modifier.size(style.iconGap))
                            BasicText(
                                state.text.orEmpty(),
                                style = style.textStyle.copy(color = colors.content),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
