package com.srctool.camouflage.skin.carbon.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.popover.PopoverColors
import com.srctool.camouflage.core.popover.PopoverPlacement
import com.srctool.camouflage.core.popover.PopoverRenderer
import com.srctool.camouflage.core.popover.PopoverSlots
import com.srctool.camouflage.core.popover.PopoverState
import com.srctool.camouflage.core.popover.PopoverStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.sidesheet.SideSheetColors
import com.srctool.camouflage.core.sidesheet.SideSheetRenderer
import com.srctool.camouflage.core.sidesheet.SideSheetSlots
import com.srctool.camouflage.core.sidesheet.SideSheetState
import com.srctool.camouflage.core.sidesheet.SideSheetStyle
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.of

// Carbon's popover and side sheet (base Carbon Skin §3.4): a hairline panel, no shadow, no arrow by default.

private val Hairline = 1.dp
private const val SCRIM = 0.4f
private const val ARROW_INSET = 12

internal object CarbonPopoverRenderer : PopoverRenderer {
    override fun defaults(theme: CamoTheme) = PopoverStyle(
        shape = theme.shapes.none,
        maxWidth = 320.dp,
        padding = theme.spacing.l,
        showArrow = true, // a caret
        arrowSize = DpSize(16.dp, 8.dp),
        elevation = ElevationLevel.Level3,
        titleStyle = theme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        gap = theme.spacing.xs,
        colors = PopoverColors(
            theme.colors.surfaceContainerLow, theme.colors.onSurface, theme.colors.onSurfaceVariant,
            theme.colors.outlineVariant,
        ),
    )

    @Composable
    override fun Render(
        state: PopoverState,
        slots: PopoverSlots,
        theme: CamoTheme,
        style: PopoverStyle,
        modifier: Modifier,
    ) = PaintPopover(state, slots, theme, style, modifier)
}

/** A popover: the arrow (when the style asks) and the panel with the title, the content and the actions. */
@Composable
internal fun PaintPopover(
    state: PopoverState,
    slots: PopoverSlots,
    theme: CamoTheme,
    style: PopoverStyle,
    modifier: Modifier,
) {
    val arrow = if (style.showArrow) style.arrowSize else DpSize.Zero
    val edges = when (state.placement) {
        PopoverPlacement.Top -> PaddingValues(bottom = arrow.height)
        PopoverPlacement.End -> PaddingValues(start = arrow.height)
        PopoverPlacement.Start -> PaddingValues(end = arrow.height)
        else -> PaddingValues(top = arrow.height)
    }
    val c = style.colors
    Box(
        modifier
            .then(if (style.showArrow) Modifier.drawBehind { drawArrow(state, style, c) } else Modifier)
            .padding(edges),
    ) {
        val depth = theme.elevation.of(style.elevation)
        val treatment = Camouflage.skin.surfaceTreatment.modifier(c.container, style.shape, depth, theme)
        Column(
            Modifier
                .widthIn(max = style.maxWidth)
                .then(treatment)
                .then(c.border?.let { Modifier.border(Hairline, it, style.shape) } ?: Modifier)
                .clip(style.shape)
                .padding(style.padding),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.s),
        ) {
            slots.title?.let { CamoText(it, style = style.titleStyle, color = c.title) }
            Column(verticalArrangement = Arrangement.spacedBy(theme.spacing.s)) { slots.content(this) }
            slots.actions?.let {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(theme.spacing.s, Alignment.End)) { it() }
            }
        }
    }
}

// The arrow on the panel's edge that faces the anchor, centered on `arrowOffset` and kept off the corners.
private fun DrawScope.drawArrow(state: PopoverState, style: PopoverStyle, colors: PopoverColors) {
    val base = style.arrowSize.width.toPx()
    val height = style.arrowSize.height.toPx()
    val inset = ARROW_INSET.dp.toPx() + base / 2
    val at = state.arrowOffset.toPx()
    val ltr = layoutDirection == LayoutDirection.Ltr
    val path = Path()
    when (state.placement) {
        PopoverPlacement.Top -> {
            val x = at.coerceIn(inset, size.width - inset)
            path.moveTo(x - base / 2, size.height - height)
            path.lineTo(x, size.height)
            path.lineTo(x + base / 2, size.height - height)
        }
        PopoverPlacement.Start, PopoverPlacement.End -> {
            val y = at.coerceIn(inset, size.height - inset)
            val onLeft = (state.placement == PopoverPlacement.End) == ltr
            val edge = if (onLeft) height else size.width - height
            val tip = if (onLeft) 0f else size.width
            path.moveTo(edge, y - base / 2)
            path.lineTo(tip, y)
            path.lineTo(edge, y + base / 2)
        }
        else -> {
            val x = at.coerceIn(inset, size.width - inset)
            path.moveTo(x - base / 2, height)
            path.lineTo(x, 0f)
            path.lineTo(x + base / 2, height)
        }
    }
    path.close()
    drawPath(path, colors.container)
    colors.border?.let { drawPath(path, it, style = Stroke(Hairline.toPx())) }
}

internal object CarbonSideSheetRenderer : SideSheetRenderer {
    override fun defaults(theme: CamoTheme): SideSheetStyle {
        val c = theme.colors
        return SideSheetStyle(
            width = 320.dp,
            modalShape = RectangleShape,
            padding = theme.spacing.l,
            titleStyle = theme.typography.headlineMedium,
            showDivider = true,
            showCloseButton = true,
            closeIcon = IconSource.Vector(ClearIcon),
            scrimAlpha = SCRIM,
            colors = SideSheetColors(c.surfaceContainerLow, c.surfaceContainerLow, c.onSurface, c.outlineVariant),
        )
    }

    @Composable
    override fun Render(
        state: SideSheetState,
        slots: SideSheetSlots,
        theme: CamoTheme,
        style: SideSheetStyle,
        modifier: Modifier,
    ) = PaintSideSheet(state, slots, theme, style, modifier)
}

/** A side sheet: the container with its start-edge line, the title row with the close button, content, actions. */
@Composable
internal fun PaintSideSheet(
    state: SideSheetState,
    slots: SideSheetSlots,
    theme: CamoTheme,
    style: SideSheetStyle,
    modifier: Modifier,
) {
    val c = style.colors
    val shape = if (state.modal) style.modalShape else RectangleShape
    val frame = if (state.modal) Modifier.fillMaxSize() else Modifier.fillMaxHeight().width(style.width)
    val close = style.showCloseButton && state.onClose != null
    val container = if (state.modal) c.modalContainer else c.container
    Column(
        modifier
            .then(frame)
            .then(Camouflage.skin.surfaceTreatment.modifier(container, shape, 0.dp, theme))
            .clip(shape)
            .drawBehind {
                if (style.showDivider) {
                    val x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - Hairline.toPx()
                    drawRect(c.divider, Offset(x, 0f), Size(Hairline.toPx(), size.height))
                }
            },
    ) {
        if (state.title != null || close) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = style.padding, top = style.padding / 2, end = style.padding / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    state.title?.let { CamoText(it, style = style.titleStyle, color = c.title) }
                }
                if (close) {
                    CamoIconButton({ CamoIcon(style.closeIcon, null) }, "Close", checkNotNull(state.onClose))
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(style.padding),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.m),
        ) { slots.content(this) }
        slots.actions?.let {
            Row(
                Modifier.fillMaxWidth().padding(style.padding),
                horizontalArrangement = Arrangement.spacedBy(theme.spacing.s, Alignment.End),
            ) { it() }
        }
    }
}
