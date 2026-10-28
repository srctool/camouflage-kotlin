package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.dialog.DialogActionsLayout
import com.srctool.camouflage.core.dialog.DialogColors
import com.srctool.camouflage.core.dialog.DialogRenderer
import com.srctool.camouflage.core.dialog.DialogSlots
import com.srctool.camouflage.core.dialog.DialogState
import com.srctool.camouflage.core.dialog.DialogStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.overlay.StackedScrim
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.of

// Fluent's dialog (base Fluent Skin §3): radius 8, shadow64, a 20 semibold title, actions at the end.

private const val SMOKE_LIGHT = 0.4f
private const val SMOKE_DARK = 0.5f
private const val DARK_SURFACE = 0.5f

/**
 * Fluent's Smoke, the dimming layer behind modal dialogs and sheets: `colors.scrim` (black) at Fluent 2's
 * `colorBackgroundOverlay` alpha, 40% in light and 50% in dark.
 */
internal fun fluentSmoke(theme: CamoTheme): Float =
    if (theme.colors.surface.luminance() < DARK_SURFACE) SMOKE_DARK else SMOKE_LIGHT

internal object FluentDialogRenderer : DialogRenderer {
    override fun defaults(theme: CamoTheme): DialogStyle {
        val c = theme.colors
        return DialogStyle(
            shape = theme.shapes.medium,
            minWidth = 280.dp,
            maxWidth = 560.dp,
            padding = PaddingValues(theme.spacing.xl),
            titleStyle = theme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            messageStyle = theme.typography.bodyMedium,
            titleAlign = TitleAlign.Start,
            actionsLayout = DialogActionsLayout.End,
            actionGap = theme.spacing.s,
            sectionGap = theme.spacing.l,
            showCloseButton = false,
            closeIcon = IconSource.Vector(DismissIcon),
            elevation = ElevationLevel.Level5,
            scrimAlpha = fluentSmoke(theme),
            stackedScrim = StackedScrim.Each,
            colors = DialogColors(c.surface, c.onSurface, c.onSurface),
        )
    }

    @Composable
    override fun Render(
        state: DialogState,
        slots: DialogSlots,
        theme: CamoTheme,
        style: DialogStyle,
        modifier: Modifier,
    ) =
        PaintDialog(state, slots, theme, style, modifier, null)
}

/**
 * A dialog: the container (with a hairline in [border] when set), the title row with the optional close button,
 * the message and content scrolling between them, and the actions laid out by `actionsLayout`.
 */
@Composable
internal fun PaintDialog(
    state: DialogState,
    slots: DialogSlots,
    theme: CamoTheme,
    style: DialogStyle,
    modifier: Modifier,
    border: Color?,
) {
    val treatment = Camouflage.skin.surfaceTreatment.modifier(
        style.colors.container,
        style.shape,
        theme.elevation.of(style.elevation),
        theme,
    )
    val outline = if (border != null) Modifier.border(Hairline, border, style.shape) else Modifier
    val align = if (style.titleAlign == TitleAlign.Center) TextAlign.Center else TextAlign.Start
    val close = style.showCloseButton && state.dismissible
    Column(
        modifier.then(treatment).then(outline).clip(style.shape).padding(style.padding),
        verticalArrangement = Arrangement.spacedBy(style.sectionGap),
    ) {
        if (state.title != null || close) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) {
                    state.title?.let {
                        CamoText(
                            it,
                            Modifier.fillMaxWidth(),
                            style = style.titleStyle.copy(textAlign = align),
                            color = style.colors.title,
                        )
                    }
                }
                if (close) {
                    CamoIconButton(
                        icon = { CamoIcon(style.closeIcon, null) },
                        contentDescription = "Close",
                        onClick = state.onDismissRequest,
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(style.sectionGap),
        ) {
            CamoText(
                state.message,
                Modifier.fillMaxWidth(),
                style = style.messageStyle.copy(textAlign = align),
                color = style.colors.message,
            )
            slots.content?.invoke(this)
        }
        DialogActions(slots, style.actionsLayout, style.actionGap)
    }
}

/**
 * `confirm` and `dismiss` arranged by [layout]: `dismiss` first in a row (confirm at the end), confirm on top when
 * stacked.
 */
@Composable
internal fun DialogActions(slots: DialogSlots, layout: DialogActionsLayout, gap: Dp) {
    val dismiss = slots.dismiss
    when (layout) {
        DialogActionsLayout.End, DialogActionsLayout.Center -> Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                gap,
                if (layout == DialogActionsLayout.End) Alignment.End else Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dismiss?.invoke()
            slots.confirm()
        }
        DialogActionsLayout.FullWidthRow -> Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
            if (dismiss != null) Box(Modifier.weight(1f), propagateMinConstraints = true) { dismiss() }
            Box(Modifier.weight(1f), propagateMinConstraints = true) { slots.confirm() }
        }
        DialogActionsLayout.Stacked -> Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(gap)) {
            Box(Modifier.fillMaxWidth(), propagateMinConstraints = true) { slots.confirm() }
            if (dismiss != null) Box(Modifier.fillMaxWidth(), propagateMinConstraints = true) { dismiss() }
        }
    }
}

private val Hairline = 1.dp
