package com.srctool.camouflage.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.ButtonColors
import com.srctool.camouflage.core.button.ButtonRenderer
import com.srctool.camouflage.core.button.ButtonSlots
import com.srctool.camouflage.core.button.ButtonState
import com.srctool.camouflage.core.button.ButtonStyle
import com.srctool.camouflage.core.divider.DividerInset
import com.srctool.camouflage.core.divider.DividerRenderer
import com.srctool.camouflage.core.divider.DividerState
import com.srctool.camouflage.core.divider.DividerStyle
import com.srctool.camouflage.core.icon.IconImage
import com.srctool.camouflage.core.icon.IconRenderer
import com.srctool.camouflage.core.icon.IconState
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.iconbutton.IconButtonColors
import com.srctool.camouflage.core.iconbutton.IconButtonRenderer
import com.srctool.camouflage.core.iconbutton.IconButtonSlots
import com.srctool.camouflage.core.iconbutton.IconButtonState
import com.srctool.camouflage.core.iconbutton.IconButtonStyle
import com.srctool.camouflage.core.progress.ProgressCircleRenderer
import com.srctool.camouflage.core.progress.ProgressColors
import com.srctool.camouflage.core.progress.ProgressState
import com.srctool.camouflage.core.progress.ProgressStyle
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skin.InteractionFeedback
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.surface.SurfaceRenderer
import com.srctool.camouflage.core.surface.SurfaceSlots
import com.srctool.camouflage.core.surface.SurfaceState
import com.srctool.camouflage.core.surface.SurfaceStyle
import com.srctool.camouflage.core.surface.surfaceOf
import com.srctool.camouflage.core.text.TextRenderer
import com.srctool.camouflage.core.text.TextState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTokenSet
import com.srctool.camouflage.core.theme.CamoTypography
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.tooltip.TooltipColors
import com.srctool.camouflage.core.tooltip.TooltipRenderer
import com.srctool.camouflage.core.tooltip.TooltipState
import com.srctool.camouflage.core.tooltip.TooltipStyle
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.rememberSkinIndication

// A plain skin for core's own tests: every renderer draws from its style and theme, nothing more.

internal object Fill : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme) =
        Modifier.background(color, shape)
}

internal object NoFeedback : InteractionFeedback, IndicationNodeFactory {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme): Indication = this
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 0
}

internal object PlainText : TextRenderer {
    override fun linkStyles(theme: CamoTheme) = TextLinkStyles(SpanStyle(color = theme.colors.primary))

    @Composable
    override fun Render(state: TextState, theme: CamoTheme, modifier: Modifier) = BasicText(
        state.content, modifier, state.style.copy(color = state.color, textAlign = state.textAlign),
        maxLines = state.maxLines, overflow = state.overflow,
    )
}

internal object PlainIcon : IconRenderer {
    override fun defaults(theme: CamoTheme) = IconStyle(24.dp, theme.colors.onSurface)

    @Composable
    override fun Render(state: IconState, theme: CamoTheme, modifier: Modifier) = IconImage(state, modifier)
}

internal object PlainSurface : SurfaceRenderer {
    override fun defaults(theme: CamoTheme) = SurfaceStyle(1.dp, theme.colors.outlineVariant)

    @Composable
    override fun Render(
        state: SurfaceState,
        slots: SurfaceSlots,
        theme: CamoTheme,
        style: SurfaceStyle,
        modifier: Modifier,
    ) {
        Box(modifier.background(theme.colors.surfaceOf(state.level), theme.shapes.of(state.shape))) {
            ProvideContentStyle(theme.colors.onSurface, IconStyle(24.dp, theme.colors.onSurface)) { slots.content() }
        }
    }
}

internal object PlainDivider : DividerRenderer {
    override fun defaults(theme: CamoTheme) = DividerStyle(1.dp, theme.colors.outlineVariant, 16.dp, 16.dp)

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

internal object PlainButton : ButtonRenderer {
    override fun defaults(theme: CamoTheme): ButtonStyle {
        val c = theme.colors
        val solid = ButtonColors(c.primary, c.onPrimary)
        return ButtonStyle(
            Appearance.Solid, 40.dp, 64.dp, RectangleShape, PaddingValues(horizontal = 16.dp),
            PaddingValues(horizontal = 12.dp), theme.typography.labelLarge, 18.dp, 8.dp, 1.dp, ElevationLevel.Level1,
            solid, solid, solid, ButtonColors(Color.Transparent, c.primary, c.outline), solid, solid, 0.12f, 0.38f,
        )
    }

    @Composable
    override fun Render(
        state: ButtonState,
        slots: ButtonSlots,
        theme: CamoTheme,
        style: ButtonStyle,
        modifier: Modifier,
    ) {
        val colors = style.colorsFor(state.appearance, state.tone, theme)
        Box(
            modifier
                .background(colors.container, style.shape)
                .indication(state.interactionSource, rememberSkinIndication(colors.content, style.shape))
                .heightIn(min = style.height)
                .widthIn(min = style.minWidth)
                .padding(style.padding),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    slots.prefixIcon?.invoke()
                    BasicText(state.text, style = style.textStyle.copy(color = colors.content), maxLines = 2)
                    slots.suffixIcon?.invoke()
                }
            }
        }
    }
}

internal object PlainIconButton : IconButtonRenderer {
    override fun defaults(theme: CamoTheme): IconButtonStyle {
        val c = theme.colors
        val ghost = IconButtonColors(Color.Transparent, c.onSurfaceVariant)
        return IconButtonStyle(
            Appearance.Ghost, 40.dp, RectangleShape, 24.dp, 1.dp,
            IconButtonColors(c.primary, c.onPrimary), ghost, ghost, ghost, IconButtonColors(c.primary, c.onPrimary),
            0.12f, 0.38f,
        )
    }

    @Composable
    override fun Render(
        state: IconButtonState,
        slots: IconButtonSlots,
        theme: CamoTheme,
        style: IconButtonStyle,
        modifier: Modifier,
    ) {
        val colors = style.colorsFor(state.appearance, state.tone, state.checked == true, theme)
        Box(modifier.size(style.containerSize).background(colors.container, style.shape), Alignment.Center) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                Box(Modifier.clearAndSetSemantics {}) { slots.icon() }
            }
        }
    }
}

internal object PlainProgressCircle : ProgressCircleRenderer {
    override fun defaults(theme: CamoTheme) =
        ProgressStyle(4.dp, 24.dp, 48.dp, 64.dp, ProgressColors(theme.colors.primary, theme.colors.outlineVariant))

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
        Canvas(modifier.size(style.circleSizeOf(state.size))) { drawCircle(style.colors.active) }
    }
}

internal object PlainTooltip : TooltipRenderer {
    override fun defaults(theme: CamoTheme) = TooltipStyle(
        RectangleShape, PaddingValues(4.dp), 8.dp, theme.typography.bodySmall, 500, 1500, 200.dp,
        TooltipColors(theme.colors.inverseSurface, theme.colors.inverseOnSurface),
    )

    @Composable
    override fun Render(state: TooltipState, theme: CamoTheme, style: TooltipStyle, modifier: Modifier) =
        BasicText(state.text, modifier.background(style.colors.container), style.textStyle)
}

internal data class Brand(val accent: Color) : CamoTokenSet<Brand> {
    override fun lerp(to: Brand, fraction: Float): Brand = this
}

internal val skinA = CamoSkin(
    "a", Fill, NoFeedback, PlainText, PlainIcon, PlainSurface, PlainDivider, PlainButton, PlainIconButton,
    PlainProgressCircle, PlainTooltip, PlainCheckbox, PlainSwitch, PlainRadio, PlainSegmented, PlainSlider,
    PlainStepper, PlainTextField, PlainSearchField, PlainMenu, PlainBottomSheet, PlainDatePicker, PlainTimePicker,
)
internal val skinB = skinA.copy(name = "b")
internal val themeT = CamoTheme(
    colors = CamoColors.fromSeed(primary = Color(0xFF0B57D0)),
    typography = CamoTypography.system(),
)
internal val themeU = themeT.copy(colors = CamoColors.fromSeed(primary = Color(0xFFB3261E)))
