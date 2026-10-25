package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.ButtonColors
import com.srctool.camouflage.core.button.ButtonRenderer
import com.srctool.camouflage.core.button.ButtonSlots
import com.srctool.camouflage.core.button.ButtonState
import com.srctool.camouflage.core.button.ButtonStyle
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
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication
import com.srctool.camouflage.skin.fluent.FluentTokens

// Fluent's Button, icon-only Button and Spinner (base Fluent Skin §3.1 and §3.2). Medium density by default;
// fluentTheme(density = …) changes the sizes through the component themes.

private val Stroke1 = 1.dp
private const val DISABLED_CONTAINER = 0.12f
private const val DISABLED_CONTENT = 0.38f

// Status tones have no hover/pressed tokens in Fluent: their fills step toward the content color instead.
private const val HOVER_STEP = 0.08f
private const val PRESSED_STEP = 0.16f

internal object FluentButtonRenderer : ButtonRenderer {
    override fun defaults(theme: CamoTheme): ButtonStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        val stroke = t?.neutralStroke1 ?: c.outlineVariant
        val secondary = ButtonColors(c.surface, c.onSurface, border = stroke)
        return ButtonStyle(
            defaultAppearance = Appearance.Solid,
            height = 32.dp,
            minWidth = 96.dp,
            shape = theme.shapes.small,
            padding = PaddingValues(horizontal = theme.spacing.m),
            paddingWithIcon = PaddingValues(horizontal = theme.spacing.m),
            textStyle = theme.typography.labelLarge,
            iconSize = 20.dp,
            iconGap = 6.dp,
            borderWidth = Stroke1,
            raisedElevation = ElevationLevel.Level0,
            solidColors = ButtonColors(t?.brandBackground ?: c.primary, t?.neutralForegroundOnBrand ?: c.onPrimary),
            softColors = secondary,
            raisedColors = secondary,
            outlineColors = ButtonColors(Color.Transparent, c.onSurface, border = stroke),
            ghostColors = ButtonColors(Color.Transparent, c.onSurfaceVariant),
            linkColors = ButtonColors(Color.Transparent, c.primary),
            disabledContainerAlpha = DISABLED_CONTAINER,
            disabledContentAlpha = DISABLED_CONTENT,
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
        val rest = style.colorsFor(state.appearance, state.tone, theme)
        val colors = if (state.enabled) {
            stepped(rest, state.appearance, state.tone, state.interaction, theme.tokens())
        } else {
            disabledColors(rest, theme.colors.onSurface, style.disabledContainerAlpha, style.disabledContentAlpha)
        }
        val link = state.appearance == Appearance.Link
        val box = Modifier
            .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape, 0.dp, theme))
            .then(colors.border?.let { Modifier.border(style.borderWidth, it, style.shape) } ?: Modifier)
            .indication(state.interactionSource, rememberSkinIndication(colors.content, style.shape))
        val size = if (link) {
            Modifier
        } else {
            Modifier
                .heightIn(min = style.height)
                .widthIn(min = style.minWidth)
                .padding(padding(style, slots, LocalLayoutDirection.current))
        }
        Box(modifier.then(box).then(size), contentAlignment = Alignment.Center) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                Row(
                    Modifier.alpha(if (state.loading) 0f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(style.iconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    slots.prefixIcon?.invoke()
                    BasicText(
                        state.text,
                        style = style.textStyle.copy(
                            color = colors.content,
                            textDecoration = if (link && state.interaction.hovered) TextDecoration.Underline else null,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    slots.suffixIcon?.invoke()
                }
                if (state.loading) Spinner(colors.content, style.iconSize, theme)
            }
        }
    }

    // Fluent's state tokens for the default tone; a step toward the content color for status tones.
    private fun stepped(
        rest: ButtonColors,
        appearance: Appearance,
        tone: Tone,
        i: InteractionState,
        t: FluentTokens?,
    ): ButtonColors = when {
        !i.hovered && !i.pressed -> rest
        t == null || tone != Tone.Default -> rest.steppedToward(i)
        else -> tokenStep(rest, appearance, i, t)
    }

    private fun tokenStep(
        rest: ButtonColors,
        appearance: Appearance,
        i: InteractionState,
        t: FluentTokens,
    ): ButtonColors {
        fun pick(hover: Color, pressed: Color) = if (i.pressed) pressed else hover
        return when (appearance) {
            Appearance.Solid -> rest.copy(container = pick(t.brandBackgroundHover, t.brandBackgroundPressed))
            Appearance.Soft, Appearance.Raised -> rest.copy(
                container = pick(t.neutralBackground1Hover, t.neutralBackground1Pressed),
                border = pick(t.neutralStroke1Hover, t.neutralStroke1Pressed),
            )
            Appearance.Outline -> rest.copy(border = pick(t.neutralStroke1Hover, t.neutralStroke1Pressed))
            Appearance.Ghost -> rest.copy(
                container = pick(t.subtleBackgroundHover, t.subtleBackgroundPressed),
                content = t.neutralForeground2Hover,
            )
            Appearance.Link -> rest.copy(content = pick(t.brandForegroundLinkHover, t.brandForegroundLinkPressed))
        }
    }
}

internal object FluentIconButtonRenderer : IconButtonRenderer {
    override fun defaults(theme: CamoTheme): IconButtonStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        val stroke = t?.neutralStroke1 ?: c.outlineVariant
        return IconButtonStyle(
            defaultAppearance = Appearance.Ghost,
            containerSize = 32.dp,
            shape = theme.shapes.small,
            iconSize = 20.dp,
            borderWidth = Stroke1,
            solidColors = IconButtonColors(t?.brandBackground ?: c.primary, t?.neutralForegroundOnBrand ?: c.onPrimary),
            softColors = IconButtonColors(c.surface, c.onSurface, border = stroke),
            outlineColors = IconButtonColors(Color.Transparent, c.onSurface, border = stroke),
            ghostColors = IconButtonColors(Color.Transparent, c.onSurfaceVariant),
            checkedColors = IconButtonColors(c.surfaceContainerHighest, c.onSurface, border = stroke),
            disabledContainerAlpha = DISABLED_CONTAINER,
            disabledContentAlpha = DISABLED_CONTENT,
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
        val colors = colorsOf(state, style, theme)
        Box(
            modifier
                .size(style.containerSize)
                .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape, 0.dp, theme))
                .then(colors.border?.let { Modifier.border(style.borderWidth, it, style.shape) } ?: Modifier)
                .indication(state.interactionSource, rememberSkinIndication(colors.content, style.shape)),
            contentAlignment = Alignment.Center,
        ) {
            if (state.loading) {
                Spinner(colors.content, style.iconSize, theme)
            } else {
                ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                    Box(Modifier.clearAndSetSemantics {}) { slots.icon() }
                }
            }
        }
    }
}

// The icon button's colors for its state: Fluent's token steps for the default tone, like buttons.
private fun colorsOf(state: IconButtonState, style: IconButtonStyle, theme: CamoTheme): ButtonColors {
    val rest = style.colorsFor(state.appearance, state.tone, state.checked == true, theme)
        .let { ButtonColors(it.container, it.content, it.border) }
    val i = state.interaction
    val t = theme.tokens<FluentTokens>()
    return when {
        !state.enabled ->
            disabledColors(rest, theme.colors.onSurface, style.disabledContainerAlpha, style.disabledContentAlpha)
        !i.hovered && !i.pressed -> rest
        t == null || state.tone != Tone.Default || state.checked == true -> rest.steppedToward(i)
        else -> iconTokenStep(rest, state.appearance, i, t)
    }
}

private fun iconTokenStep(rest: ButtonColors, appearance: Appearance, i: InteractionState, t: FluentTokens) = when {
    appearance == Appearance.Solid ->
        rest.copy(container = if (i.pressed) t.brandBackgroundPressed else t.brandBackgroundHover)
    rest.container == Color.Transparent && rest.border == null -> rest.copy(
        container = if (i.pressed) t.subtleBackgroundPressed else t.subtleBackgroundHover,
        content = t.neutralForeground2Hover,
    )
    else -> rest.copy(
        container = when {
            rest.container == Color.Transparent -> rest.container
            i.pressed -> t.neutralBackground1Pressed
            else -> t.neutralBackground1Hover
        },
        border = if (i.pressed) t.neutralStroke1Pressed else t.neutralStroke1Hover,
    )
}

// Fluent's Spinner: a brand arc on a light brand track, 3 dp.
internal object FluentProgressCircleRenderer : ProgressCircleRenderer {
    override fun defaults(theme: CamoTheme) = ProgressStyle(
        circleStroke = 3.dp,
        circleSizeSmall = 20.dp,
        circleSizeMedium = 32.dp,
        circleSizeLarge = 40.dp,
        colors = ProgressColors(theme.colors.primary, theme.colors.primaryContainer),
    )

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
        val colors = if (state.tone == Tone.Default) {
            style.colors
        } else {
            val f = theme.colors.family(state.tone)
            ProgressColors(f.strong, f.container)
        }
        DrawSpinner(state.progress, theme, style.circleSizeOf(state.size), style.circleStroke, colors, modifier)
    }
}

private fun ButtonColors.steppedToward(i: InteractionState): ButtonColors {
    if (container == Color.Transparent) return this
    return copy(container = lerp(container, content, if (i.pressed) PRESSED_STEP else HOVER_STEP))
}

private fun disabledColors(rest: ButtonColors, onSurface: Color, containerAlpha: Float, contentAlpha: Float) =
    ButtonColors(
        container = if (rest.container == Color.Transparent) Color.Transparent else onSurface.disabled(containerAlpha),
        content = onSurface.disabled(contentAlpha),
        border = rest.border?.let { onSurface.disabled(containerAlpha) },
    )

@Composable
private fun Spinner(color: Color, size: Dp, theme: CamoTheme) {
    val style = Camouflage.skin.progressCircle.defaults(theme).merge(theme.components.progress)
    DrawSpinner(null, theme, size, style.circleStroke, ProgressColors(color, Color.Transparent), Modifier)
}

private const val FULL_TURN = 360f
private const val ARC = 90f
private const val SPIN_CYCLES = 5
private const val PULSE_MILLIS = 1200
private const val PULSE_LOW = 0.4f

@Composable
private fun DrawSpinner(
    progress: Float?,
    theme: CamoTheme,
    diameter: Dp,
    stroke: Dp,
    colors: ProgressColors,
    modifier: Modifier,
) {
    val reduced = theme.motion.durationLong == 0
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        0f,
        FULL_TURN,
        infiniteRepeatable(tween((theme.motion.durationLong * SPIN_CYCLES).coerceAtLeast(1), easing = LinearEasing)),
    )
    val pulse by transition.animateFloat(PULSE_LOW, 1f, infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse))
    val value by animateFloatAsState(progress ?: 0f, tween(theme.motion.durationMedium))
    Canvas(modifier.size(diameter).alpha(if (reduced && progress == null) pulse else 1f)) {
        val line = Stroke(stroke.toPx(), cap = StrokeCap.Round)
        val inset = stroke.toPx() / 2
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        if (colors.track.alpha > 0f) drawArc(colors.track, 0f, FULL_TURN, false, topLeft, arcSize, style = line)
        when {
            progress != null -> drawArc(colors.active, -ARC, value * FULL_TURN, false, topLeft, arcSize, style = line)
            reduced -> drawArc(colors.active, 0f, FULL_TURN, false, topLeft, arcSize, style = line)
            else -> drawArc(colors.active, angle, ARC, false, topLeft, arcSize, style = line)
        }
    }
}

private fun padding(style: ButtonStyle, slots: ButtonSlots, direction: LayoutDirection) = PaddingValues(
    start = (if (slots.prefixIcon != null) style.paddingWithIcon else style.padding).calculateStartPadding(direction),
    top = style.padding.calculateTopPadding(),
    end = (if (slots.suffixIcon != null) style.paddingWithIcon else style.padding).calculateEndPadding(direction),
    bottom = style.padding.calculateBottomPadding(),
)
