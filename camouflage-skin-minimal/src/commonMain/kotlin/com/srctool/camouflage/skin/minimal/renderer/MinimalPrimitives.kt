package com.srctool.camouflage.skin.minimal.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.divider.DividerInset
import com.srctool.camouflage.core.divider.DividerRenderer
import com.srctool.camouflage.core.divider.DividerState
import com.srctool.camouflage.core.divider.DividerStyle
import com.srctool.camouflage.core.icon.IconImage
import com.srctool.camouflage.core.icon.IconRenderer
import com.srctool.camouflage.core.icon.IconState
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.surface.SurfaceRenderer
import com.srctool.camouflage.core.surface.SurfaceSlots
import com.srctool.camouflage.core.surface.SurfaceState
import com.srctool.camouflage.core.surface.SurfaceStyle
import com.srctool.camouflage.core.surface.surfaceOf
import com.srctool.camouflage.core.text.TextRenderer
import com.srctool.camouflage.core.text.TextState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.tooltip.TooltipColors
import com.srctool.camouflage.core.tooltip.TooltipRenderer
import com.srctool.camouflage.core.tooltip.TooltipState
import com.srctool.camouflage.core.tooltip.TooltipStyle
import com.srctool.camouflage.core.util.ProvideContentStyle

// Minimal's defaults for text, icons, surfaces, dividers and tooltips (base Minimal Skin §3.4).

private val Hairline = 1.dp
private val IconSize = 20.dp

internal object MinimalTextRenderer : TextRenderer {
    override fun linkStyles(theme: CamoTheme) =
        TextLinkStyles(SpanStyle(color = theme.colors.primary, textDecoration = TextDecoration.Underline))

    @Composable
    override fun Render(state: TextState, theme: CamoTheme, modifier: Modifier) {
        BasicText(
            state.content,
            modifier,
            style = state.style.copy(color = state.color, textAlign = state.textAlign),
            maxLines = state.maxLines,
            overflow = state.overflow,
        )
    }
}

internal object MinimalIconRenderer : IconRenderer {
    override fun defaults(theme: CamoTheme) = IconStyle(IconSize, theme.colors.onSurface)

    @Composable
    override fun Render(state: IconState, theme: CamoTheme, modifier: Modifier) = IconImage(state, modifier)
}

internal object MinimalSurfaceRenderer : SurfaceRenderer {
    override fun defaults(theme: CamoTheme) = SurfaceStyle(Hairline, theme.colors.outlineVariant)

    @Composable
    override fun Render(
        state: SurfaceState,
        slots: SurfaceSlots,
        theme: CamoTheme,
        style: SurfaceStyle,
        modifier: Modifier,
    ) {
        val skin = Camouflage.skin
        val shape = theme.shapes.of(state.shape)
        val treatment = skin.surfaceTreatment.modifier(
            theme.colors.surfaceOf(state.level),
            shape,
            theme.elevation.of(state.elevation),
            theme,
        )
        val border = if (state.border) Modifier.border(style.borderWidth, style.borderColor, shape) else Modifier
        val icons = skin.icon.defaults(theme).merge(theme.components.icon)
        Box(modifier.then(treatment).then(border).clip(shape)) {
            ProvideContentStyle(theme.colors.onSurface, icons.copy(color = theme.colors.onSurface)) {
                slots.content()
            }
        }
    }
}

internal object MinimalDividerRenderer : DividerRenderer {
    override fun defaults(theme: CamoTheme) =
        DividerStyle(Hairline, theme.colors.outlineVariant, theme.spacing.l, theme.spacing.l)

    @Composable
    override fun Render(state: DividerState, theme: CamoTheme, style: DividerStyle, modifier: Modifier) {
        val inset = when (state.inset) {
            DividerInset.None -> PaddingValues()
            DividerInset.Start -> PaddingValues(start = style.startInset)
            DividerInset.Middle -> PaddingValues(start = style.startInset, end = style.endInset)
        }
        val line = if (state.orientation == Orientation.Horizontal) {
            Modifier.fillMaxWidth().padding(inset).height(style.thickness)
        } else {
            Modifier.fillMaxHeight().padding(inset).width(style.thickness)
        }
        Box(modifier.then(line).background(style.color).clearAndSetSemantics {})
    }
}

internal object MinimalTooltipRenderer : TooltipRenderer {
    override fun defaults(theme: CamoTheme) = TooltipStyle(
        shape = theme.shapes.small,
        padding = PaddingValues(horizontal = theme.spacing.s, vertical = theme.spacing.xs),
        offset = theme.spacing.s,
        textStyle = theme.typography.bodySmall,
        showDelayMillis = 500,
        hideDelayMillis = 1500,
        maxWidth = 200.dp,
        colors = TooltipColors(theme.colors.inverseSurface, theme.colors.inverseOnSurface),
    )

    @Composable
    override fun Render(state: TooltipState, theme: CamoTheme, style: TooltipStyle, modifier: Modifier) {
        BasicText(
            state.text,
            modifier
                .widthIn(max = style.maxWidth)
                .background(style.colors.container, style.shape)
                .padding(style.padding),
            style = style.textStyle.copy(color = style.colors.text),
        )
    }
}
