package com.srctool.camouflage.sample

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.progress.ProgressCircleRenderer
import com.srctool.camouflage.core.progress.ProgressColors
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.progress.ProgressState
import com.srctool.camouflage.core.progress.ProgressStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.surface.SurfaceRenderer
import com.srctool.camouflage.core.surface.SurfaceSlots
import com.srctool.camouflage.core.surface.SurfaceState
import com.srctool.camouflage.core.surface.SurfaceStyle
import com.srctool.camouflage.core.surface.surfaceOf
import com.srctool.camouflage.core.text.TextRenderer
import com.srctool.camouflage.core.text.TextState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.tooltip.TooltipColors
import com.srctool.camouflage.core.tooltip.TooltipRenderer
import com.srctool.camouflage.core.tooltip.TooltipState
import com.srctool.camouflage.core.tooltip.TooltipStyle
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication

// DebugSkin's renderers: the contract's reference implementations (Skin Contract §3.5). They draw plain,
// labelled boxes from the style and theme, plus a magenta border while focused, so a skin's behavior can be
// checked before any real dialect exists.

internal val DebugFocus = Color.Magenta

object DebugTextRenderer : TextRenderer {
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

object DebugIconRenderer : IconRenderer {
    override fun defaults(theme: CamoTheme) = IconStyle(24.dp, theme.colors.onSurface)

    @Composable
    override fun Render(state: IconState, theme: CamoTheme, modifier: Modifier) = IconImage(state, modifier)
}

object DebugSurfaceRenderer : SurfaceRenderer {
    override fun defaults(theme: CamoTheme) = SurfaceStyle(1.dp, theme.colors.outlineVariant)

    @Composable
    override fun Render(
        state: SurfaceState,
        slots: SurfaceSlots,
        theme: CamoTheme,
        style: SurfaceStyle,
        modifier: Modifier,
    ) {
        val shape = theme.shapes.of(state.shape)
        Box(
            modifier
                .then(
                    Camouflage.skin.surfaceTreatment.modifier(
                        theme.colors.surfaceOf(state.level),
                        shape,
                        theme.elevation.of(state.elevation),
                        theme,
                    ),
                )
                .then(if (state.border) Modifier.border(style.borderWidth, style.borderColor, shape) else Modifier)
                .clip(shape),
        ) {
            ProvideContentStyle(theme.colors.onSurface, Camouflage.iconStyle.copy(color = theme.colors.onSurface)) {
                slots.content()
            }
        }
    }
}

object DebugDividerRenderer : DividerRenderer {
    override fun defaults(theme: CamoTheme) = DividerStyle(1.dp, theme.colors.outlineVariant, 16.dp, 16.dp)

    @Composable
    override fun Render(state: DividerState, theme: CamoTheme, style: DividerStyle, modifier: Modifier) {
        val inset = when (state.inset) {
            DividerInset.None -> PaddingValues(0.dp)
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

object DebugButtonRenderer : ButtonRenderer {
    override fun defaults(theme: CamoTheme): ButtonStyle {
        val c = theme.colors
        return ButtonStyle(
            defaultAppearance = Appearance.Solid,
            height = 40.dp,
            minWidth = 64.dp,
            shape = theme.shapes.small,
            padding = PaddingValues(horizontal = theme.spacing.l),
            paddingWithIcon = PaddingValues(horizontal = theme.spacing.m),
            textStyle = theme.typography.labelLarge,
            iconSize = 18.dp,
            iconGap = theme.spacing.s,
            borderWidth = 1.dp,
            raisedElevation = ElevationLevel.Level1,
            solidColors = ButtonColors(c.primary, c.onPrimary),
            softColors = ButtonColors(c.secondaryContainer, c.onSecondaryContainer),
            raisedColors = ButtonColors(c.surfaceContainerLow, c.primary, c.outlineVariant),
            outlineColors = ButtonColors(Color.Transparent, c.primary, c.outline),
            ghostColors = ButtonColors(Color.Transparent, c.primary),
            linkColors = ButtonColors(Color.Transparent, c.primary),
            disabledContainerAlpha = 0.12f,
            disabledContentAlpha = 0.38f,
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
        val onSurface = theme.colors.onSurface
        val container = if (state.enabled) colors.container else onSurface.disabled(style.disabledContainerAlpha)
        val content = if (state.enabled) colors.content else onSurface.disabled(style.disabledContentAlpha)
        val padding = PaddingValues(
            start = if (slots.prefixIcon != null) style.paddingWithIcon.startOf() else style.padding.startOf(),
            end = if (slots.suffixIcon != null) style.paddingWithIcon.endOf() else style.padding.endOf(),
        )
        Box(
            modifier
                .then(Camouflage.skin.surfaceTreatment.modifier(container, style.shape, 0.dp, theme))
                .then(colors.border?.let { Modifier.border(style.borderWidth, it, style.shape) } ?: Modifier)
                .indication(state.interactionSource, rememberSkinIndication(content, style.shape))
                .border(if (state.interaction.focused) 2.dp else 0.dp, DebugFocus, style.shape)
                .heightIn(min = style.height)
                .widthIn(min = style.minWidth)
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                Row(
                    Modifier.alpha(if (state.loading) 0f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(style.iconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    slots.prefixIcon?.invoke()
                    BasicText(
                        "[${state.appearance}] ${state.text}",
                        style = style.textStyle.copy(
                            color = content,
                            textDecoration = TextDecoration.Underline.takeIf { state.appearance == Appearance.Link },
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    slots.suffixIcon?.invoke()
                }
                if (state.loading) CamoProgressCircle(size = ProgressSize.Small, tone = state.tone)
            }
        }
    }
}

object DebugIconButtonRenderer : IconButtonRenderer {
    override fun defaults(theme: CamoTheme): IconButtonStyle {
        val c = theme.colors
        return IconButtonStyle(
            defaultAppearance = Appearance.Ghost,
            containerSize = 40.dp,
            shape = theme.shapes.full,
            iconSize = 24.dp,
            borderWidth = 1.dp,
            solidColors = IconButtonColors(c.primary, c.onPrimary),
            softColors = IconButtonColors(c.secondaryContainer, c.onSecondaryContainer),
            outlineColors = IconButtonColors(Color.Transparent, c.onSurfaceVariant, c.outline),
            ghostColors = IconButtonColors(Color.Transparent, c.onSurfaceVariant),
            checkedColors = IconButtonColors(c.primary, c.onPrimary),
            disabledContainerAlpha = 0.12f,
            disabledContentAlpha = 0.38f,
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
        val onSurface = theme.colors.onSurface
        val container = if (state.enabled) colors.container else onSurface.disabled(style.disabledContainerAlpha)
        val content = if (state.enabled) colors.content else onSurface.disabled(style.disabledContentAlpha)
        Box(
            modifier
                .size(style.containerSize)
                .then(Camouflage.skin.surfaceTreatment.modifier(container, style.shape, 0.dp, theme))
                .then(colors.border?.let { Modifier.border(style.borderWidth, it, style.shape) } ?: Modifier)
                .indication(state.interactionSource, rememberSkinIndication(content, style.shape))
                .border(if (state.interaction.focused) 2.dp else 0.dp, DebugFocus, style.shape),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                if (state.loading) {
                    CamoProgressCircle(size = ProgressSize.Small, tone = state.tone)
                } else {
                    Box(Modifier.clearAndSetSemantics {}) { slots.icon() }
                }
            }
        }
    }
}

object DebugProgressCircleRenderer : ProgressCircleRenderer {
    override fun defaults(theme: CamoTheme) = ProgressStyle(
        circleStroke = 4.dp,
        circleSizeSmall = 18.dp,
        circleSizeMedium = 40.dp,
        circleSizeLarge = 56.dp,
        colors = ProgressColors(theme.colors.primary, theme.colors.outlineVariant),
    )

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
        val active = if (state.tone == Tone.Default) {
            style.colors.active
        } else {
            theme.colors.family(state.tone).strong
        }
        val turn = rememberInfiniteTransition()
        val angle by turn.animateFloat(
            0f,
            FULL_TURN,
            infiniteRepeatable(
                tween(theme.motion.durationLong.coerceAtLeast(1) * SPIN_SLOWDOWN, easing = LinearEasing),
            ),
        )
        Canvas(modifier.size(style.circleSizeOf(state.size))) {
            val stroke = Stroke(style.circleStroke.toPx(), cap = StrokeCap.Round)
            drawArc(style.colors.track, 0f, FULL_TURN, false, style = stroke)
            val sweep = state.progress?.let { it * FULL_TURN } ?: QUARTER_TURN
            val start = if (state.progress == null) angle else -QUARTER_TURN
            drawArc(active, start, sweep, false, style = stroke)
        }
    }

    private const val FULL_TURN = 360f
    private const val QUARTER_TURN = 90f
    private const val SPIN_SLOWDOWN = 3
}

object DebugTooltipRenderer : TooltipRenderer {
    override fun defaults(theme: CamoTheme) = TooltipStyle(
        shape = RoundedCornerShape(4.dp),
        padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        offset = 8.dp,
        textStyle = theme.typography.bodySmall,
        showDelayMillis = 500,
        hideDelayMillis = 1500,
        maxWidth = 200.dp,
        colors = TooltipColors(theme.colors.inverseSurface, theme.colors.inverseOnSurface),
    )

    @Composable
    override fun Render(state: TooltipState, theme: CamoTheme, style: TooltipStyle, modifier: Modifier) {
        BasicText(
            "[tooltip] ${state.text}",
            modifier
                .widthIn(max = style.maxWidth)
                .background(style.colors.container, style.shape)
                .padding(style.padding),
            style = style.textStyle.copy(color = style.colors.text),
        )
    }
}

private fun PaddingValues.startOf() = calculateLeftPadding(LayoutDirection.Ltr)
private fun PaddingValues.endOf() = calculateRightPadding(LayoutDirection.Ltr)
