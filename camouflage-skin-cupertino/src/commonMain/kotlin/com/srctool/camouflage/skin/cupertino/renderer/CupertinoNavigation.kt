package com.srctool.camouflage.skin.cupertino.renderer

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
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

// Cupertino's tabs and FAB (base Cupertino Skin §3.2): tabs are a segmented control (a gray track with a white thumb
// that slides to the selected tab; Solid and Ghost look the same); `scrollable` falls back to an underline row, since a
// segmented control can't scroll. The FAB is a circular prominent button.

private val Hairline = 0.5.dp
private const val DISABLED = 0.38f
private const val SEGMENT_RADIUS = 6
private val TRACK_PADDING = 2.dp

internal object CupertinoTabsRenderer : TabsRenderer {
    override fun defaults(theme: CamoTheme): TabsStyle {
        val c = theme.colors
        val segmented = TabsColors(c.secondaryContainer, c.onSurface, c.onSurface, thumbColor(theme), c.outlineVariant)
        return TabsStyle(
            defaultAppearance = Appearance.Solid,
            height = 32.dp,
            heightWithIcon = 48.dp,
            indicatorHeight = 2.dp,
            indicatorWidth = IndicatorWidth.Tab,
            labelStyle = theme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            iconSize = 16.dp,
            iconGap = 6.dp,
            tabPadding = 12.dp,
            minTabWidth = 72.dp,
            showDivider = true,
            solidColors = segmented,
            ghostColors = segmented,
        )
    }

    // The underline row a scrollable tab list falls back to (a segmented control can't scroll).
    private fun underline(theme: CamoTheme) = TabsColors(
        Color.Transparent, theme.colors.primary, theme.colors.onSurfaceVariant, theme.colors.primary,
        theme.colors.outlineVariant,
    )

    @Composable
    override fun Container(
        state: TabsState,
        theme: CamoTheme,
        style: TabsStyle,
        modifier: Modifier,
        row: @Composable (Modifier) -> Unit,
    ) {
        val colors = style.colorsFor(state.appearance)
        if (state.scrollable) {
            val lines = underline(theme)
            Box(
                modifier.drawBehind {
                    if (style.showDivider) {
                        val line = Hairline.toPx()
                        drawRect(lines.divider, Offset(0f, size.height - line), Size(size.width, line))
                    }
                },
            ) {
                row(
                    Modifier.drawBehind {
                        val indicator = state.indicator() ?: return@drawBehind
                        val height = style.indicatorHeight.toPx()
                        val at = Offset(indicator.start, size.height - height)
                        drawRect(lines.indicator, at, Size(indicator.width, height))
                    },
                )
            }
        } else {
            // The tabs fill the track (the whole node takes the press); the thumb is inset 2 inside the selected tab.
            Box(modifier.background(colors.container, theme.shapes.small)) {
                row(
                    Modifier.drawBehind {
                        val indicator = state.indicator() ?: return@drawBehind
                        val inset = TRACK_PADDING.toPx()
                        val topLeft = Offset(indicator.start + inset, inset)
                        val box = Size(indicator.width - inset * 2, size.height - inset * 2)
                        val corner = CornerRadius(SEGMENT_RADIUS.dp.toPx())
                        drawRoundRect(colors.indicator, topLeft, box, corner)
                        drawRoundRect(colors.divider, topLeft, box, corner, style = Stroke(Hairline.toPx()))
                    },
                )
            }
        }
    }

    @Composable
    override fun Tab(state: TabState, slots: TabSlots, theme: CamoTheme, style: TabsStyle, modifier: Modifier) {
        val colors = if (state.scrollable) underline(theme) else style.colorsFor(state.appearance)
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

internal object CupertinoFabRenderer : FabRenderer {
    override fun defaults(theme: CamoTheme): FabStyle {
        val c = theme.colors
        return FabStyle(
            defaultAppearance = Appearance.Solid,
            size = FabSize.Regular,
            sizeSmall = 44.dp,
            sizeRegular = 56.dp,
            sizeLarge = 64.dp,
            extendedHeight = 50.dp,
            shapeSmall = theme.shapes.full,
            shapeRegular = theme.shapes.full,
            shapeLarge = theme.shapes.full,
            iconSizeRegular = 24.dp,
            iconSizeLarge = 28.dp,
            iconGap = 8.dp,
            extendedPadding = 20.dp,
            textStyle = theme.typography.labelLarge,
            elevation = ElevationLevel.Level2,
            softColors = FabColors(c.surfaceContainerHigh, c.onSurface),
            solidColors = FabColors(c.primary, c.onPrimary),
            raisedColors = FabColors(c.primaryContainer, c.primary),
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
        val depth = if (state.appearance == Appearance.Raised) theme.elevation.of(style.elevation) else 0.dp
        val border = Modifier
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
