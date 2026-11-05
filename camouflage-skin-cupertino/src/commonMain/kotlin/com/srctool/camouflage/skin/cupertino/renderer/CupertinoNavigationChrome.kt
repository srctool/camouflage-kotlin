package com.srctool.camouflage.skin.cupertino.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.badge.CamoBadgedBox
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.navigation.DrawerColors
import com.srctool.camouflage.core.navigation.DrawerState
import com.srctool.camouflage.core.navigation.LabelVisibility
import com.srctool.camouflage.core.navigation.NavBarState
import com.srctool.camouflage.core.navigation.NavColors
import com.srctool.camouflage.core.navigation.NavIndicator
import com.srctool.camouflage.core.navigation.NavItemSlots
import com.srctool.camouflage.core.navigation.NavItemState
import com.srctool.camouflage.core.navigation.NavPlacement
import com.srctool.camouflage.core.navigation.NavRailState
import com.srctool.camouflage.core.navigation.NavigationBarRenderer
import com.srctool.camouflage.core.navigation.NavigationBarStyle
import com.srctool.camouflage.core.navigation.NavigationDrawerRenderer
import com.srctool.camouflage.core.navigation.NavigationDrawerStyle
import com.srctool.camouflage.core.navigation.NavigationRailRenderer
import com.srctool.camouflage.core.navigation.NavigationRailStyle
import com.srctool.camouflage.core.navigation.RailAlignment
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.rememberSkinIndication

// Cupertino's navigation chrome (base Cupertino Skin §3.2): a tab bar on a blur material (a capsule when floating)
// with no indicator, the selected item in the accent; a compact sidebar for the rail; a sidebar list for the drawer.

private val Hairline = 0.5.dp
private const val DISABLED = 0.38f
private val Gap = 4.dp
private val RowGap = 12.dp
private val LineWidth = 2.dp

// Which edge of an item a `Line` indicator runs along: the top (the bar) or the start (the rail and drawer).
private enum class Edge { Top, Start }

// How one item is painted, from whichever style the chrome has.
private class Paint(
    val indicator: NavIndicator,
    val selectedIcon: Color,
    val selectedLabel: Color,
    val icon: Color,
    val label: Color,
    val indicatorColor: Color,
    val iconSize: Dp,
    val labelStyle: TextStyle,
    val indicatorSize: DpSize,
    val indicatorShape: Shape,
    val row: Boolean,
    val rowHeight: Dp,
    val edge: Edge,
    val lineColor: Color,
)

private fun DrawScope.drawLine(edge: Edge, color: Color, thickness: Float) {
    when (edge) {
        Edge.Top -> drawRect(color, Offset.Zero, Size(size.width, thickness))
        Edge.Start -> {
            val x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - thickness
            drawRect(color, Offset(x, 0f), Size(thickness, size.height))
        }
    }
}

@Composable
private fun NavItemPaint(state: NavItemState, slots: NavItemSlots, paint: Paint, modifier: Modifier) {
    val iconColor = if (state.selected) paint.selectedIcon else paint.icon
    val labelColor = if (state.selected) paint.selectedLabel else paint.label
    val marked = state.selected && state.indicator != NavIndicator.None
    val shape = paint.indicatorShape
    val line = marked && state.indicator == NavIndicator.Line
    val icon: @Composable () -> Unit = {
        ProvideContentStyle(iconColor, IconStyle(paint.iconSize, iconColor)) {
            val badge = slots.badge
            if (badge != null) CamoBadgedBox(badge = badge) { slots.icon() } else slots.icon()
        }
    }
    val base = modifier
        .alpha(if (state.enabled) 1f else DISABLED)
        .then(if (line) Modifier.drawBehind { drawLine(paint.edge, paint.lineColor, LineWidth.toPx()) } else Modifier)
        .indication(state.interactionSource, rememberSkinIndication(iconColor, shape))
    if (paint.row) {
        Row(
            base
                .fillMaxWidth()
                .heightIn(min = paint.rowHeight)
                .then(if (marked) Modifier.background(paint.indicatorColor, shape) else Modifier)
                .padding(horizontal = RowGap),
            horizontalArrangement = Arrangement.spacedBy(RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            BasicText(
                slots.label,
                Modifier.weight(1f),
                style = paint.labelStyle.copy(color = labelColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Column(
            base.widthIn(min = paint.indicatorSize.width).padding(vertical = Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            Box(
                if (marked && state.indicator == NavIndicator.Pill) {
                    Modifier.size(paint.indicatorSize).background(paint.indicatorColor, paint.indicatorShape)
                } else {
                    Modifier
                },
                contentAlignment = Alignment.Center,
            ) { icon() }
            if (state.showLabel) {
                BasicText(
                    slots.label,
                    style = paint.labelStyle.copy(color = labelColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun navColors(theme: CamoTheme): NavColors {
    val c = theme.colors
    return NavColors(
        c.surfaceContainerLow, c.primaryContainer, c.primary, c.primary, c.onSurfaceVariant, c.onSurfaceVariant,
    )
}

private fun NavColors.paint(
    indicator: NavIndicator,
    iconSize: Dp,
    labelStyle: TextStyle,
    size: DpSize,
    shape: Shape,
    edge: Edge,
) = Paint(
    indicator, selectedIcon, selectedLabel, icon, label, this.indicator, iconSize, labelStyle, size, shape,
    row = false, rowHeight = 0.dp, edge = edge, lineColor = selectedIcon,
)

internal object CupertinoNavigationBarRenderer : NavigationBarRenderer {
    override fun defaults(theme: CamoTheme) = NavigationBarStyle(
        placement = NavPlacement.Pinned,
        floatingMargin = 12.dp,
        floatingShape = theme.shapes.full,
        floatingElevation = ElevationLevel.Level3,
        indicator = NavIndicator.None,
        showLabels = LabelVisibility.Always,
        height = 56.dp,
        indicatorSize = DpSize(48.dp, 28.dp),
        indicatorShape = theme.shapes.full,
        labelStyle = theme.typography.labelSmall,
        iconSize = 24.dp,
        colors = navColors(theme),
    )

    @Composable
    override fun Container(
        state: NavBarState,
        theme: CamoTheme,
        style: NavigationBarStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        val floating = style.placement == NavPlacement.Floating
        val shape = if (floating) style.floatingShape else RectangleShape
        val depth = theme.elevation.of(if (floating) style.floatingElevation else ElevationLevel.Level2)
        val line = theme.colors.outlineVariant
        Box(
            modifier
                .then(if (floating) Modifier.padding(style.floatingMargin) else Modifier)
                .then(Camouflage.skin.surfaceTreatment.modifier(style.colors.container, shape, depth, theme))
                .clip(shape)
                .drawBehind {
                    if (!floating) drawRect(line, Offset.Zero, Size(size.width, Hairline.toPx()))
                }
                .windowInsetsPadding(state.insets),
        ) {
            Row(
                Modifier.fillMaxWidth().height(style.height),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }

    @Composable
    override fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationBarStyle,
        modifier: Modifier,
    ) = NavItemPaint(
        state,
        slots,
        style.colors.paint(
            style.indicator, style.iconSize, style.labelStyle, style.indicatorSize, style.indicatorShape, Edge.Top,
        ),
        modifier,
    )
}

internal object CupertinoNavigationRailRenderer : NavigationRailRenderer {
    override fun defaults(theme: CamoTheme) = NavigationRailStyle(
        indicator = NavIndicator.None,
        alignment = RailAlignment.Top,
        showLabels = LabelVisibility.Always,
        width = 72.dp,
        itemSpacing = 8.dp,
        indicatorSize = DpSize(48.dp, 28.dp),
        indicatorShape = theme.shapes.full,
        labelStyle = theme.typography.labelSmall,
        iconSize = 22.dp,
        colors = navColors(theme),
    )

    @Composable
    override fun Container(
        state: NavRailState,
        theme: CamoTheme,
        style: NavigationRailStyle,
        modifier: Modifier,
        header: (@Composable () -> Unit)?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val line = theme.colors.outlineVariant
        Column(
            modifier
                .fillMaxHeight()
                .width(style.width)
                .then(
                    Camouflage.skin.surfaceTreatment.modifier(
                        style.colors.container, RectangleShape, theme.elevation.of(ElevationLevel.Level2), theme,
                    ),
                )
                .drawBehind {
                    val x = if (layoutDirection == LayoutDirection.Ltr) size.width - Hairline.toPx() else 0f
                    drawRect(line, Offset(x, 0f), Size(Hairline.toPx(), size.height))
                }
                .windowInsetsPadding(state.insets)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header?.let { Box(Modifier.padding(bottom = 12.dp)) { it() } }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = if (style.alignment == RailAlignment.Center) {
                    Arrangement.spacedBy(style.itemSpacing, Alignment.CenterVertically)
                } else {
                    Arrangement.spacedBy(style.itemSpacing)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }

    @Composable
    override fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationRailStyle,
        modifier: Modifier,
    ) = NavItemPaint(
        state,
        slots,
        style.colors.paint(
            style.indicator, style.iconSize, style.labelStyle, style.indicatorSize, style.indicatorShape, Edge.Start,
        ),
        modifier,
    )
}

internal object CupertinoNavigationDrawerRenderer : NavigationDrawerRenderer {
    override fun defaults(theme: CamoTheme): NavigationDrawerStyle {
        val c = theme.colors
        return NavigationDrawerStyle(
            indicator = NavIndicator.Fill,
            width = 320.dp,
            itemHeight = 44.dp,
            iconSize = 22.dp,
            itemShape = theme.shapes.medium,
            modalShape = RectangleShape,
            labelStyle = theme.typography.bodyLarge,
            sectionTitleStyle = theme.typography.titleSmall,
            scrimAlpha = 0.4f,
            colors = DrawerColors(
                c.surfaceContainerLow, c.surfaceContainerLow, c.primaryContainer, c.primary, c.onSurface,
                c.onSurfaceVariant,
            ),
        )
    }

    @Composable
    override fun Container(
        state: DrawerState,
        theme: CamoTheme,
        style: NavigationDrawerStyle,
        modifier: Modifier,
        header: (@Composable () -> Unit)?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val line = theme.colors.outlineVariant
        Column(
            modifier
                .fillMaxHeight()
                .width(style.width)
                .then(
                    Camouflage.skin.surfaceTreatment.modifier(
                        if (state.modal) style.colors.modalContainer else style.colors.container,
                        style.modalShape,
                        theme.elevation.of(ElevationLevel.Level2),
                        theme,
                    ),
                )
                .drawBehind {
                    val x = if (layoutDirection == LayoutDirection.Ltr) size.width - Hairline.toPx() else 0f
                    drawRect(line, Offset(x, 0f), Size(Hairline.toPx(), size.height))
                }
                .padding(12.dp),
        ) {
            header?.let { Box(Modifier.padding(bottom = 12.dp)) { it() } }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                Arrangement.spacedBy(2.dp),
                content = content,
            )
        }
    }

    @Composable
    override fun SectionTitle(title: String, theme: CamoTheme, style: NavigationDrawerStyle, modifier: Modifier) {
        BasicText(
            title,
            modifier.padding(start = RowGap, top = 16.dp, bottom = 8.dp),
            style = style.sectionTitleStyle.copy(color = style.colors.sectionTitle),
            maxLines = 1,
        )
    }

    @Composable
    override fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationDrawerStyle,
        modifier: Modifier,
    ) {
        val c = style.colors
        NavItemPaint(
            state,
            slots,
            Paint(
                state.indicator, c.selectedContent, c.selectedContent, c.content, c.content, c.indicator,
                style.iconSize, style.labelStyle, DpSize(style.width, style.itemHeight), style.itemShape,
                row = true, rowHeight = style.itemHeight, edge = Edge.Start, lineColor = c.selectedContent,
            ),
            modifier,
        )
    }
}
