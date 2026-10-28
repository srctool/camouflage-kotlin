package com.srctool.camouflage.skin.minimal.renderer

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
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.progress.ProgressState
import com.srctool.camouflage.core.progress.ProgressStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication

// Minimal's buttons, icon buttons and progress circle (base Minimal Skin §3.3 and §3.4).

private val Hairline = 1.dp
private const val DISABLED_CONTAINER = 0.12f
private const val DISABLED_CONTENT = 0.38f

internal object MinimalButtonRenderer : ButtonRenderer {
    override fun defaults(theme: CamoTheme): ButtonStyle {
        val c = theme.colors
        return ButtonStyle(
            defaultAppearance = Appearance.Solid,
            height = 40.dp,
            minWidth = 64.dp,
            shape = theme.shapes.medium,
            padding = PaddingValues(horizontal = theme.spacing.l),
            paddingWithIcon = PaddingValues(horizontal = theme.spacing.m),
            textStyle = theme.typography.labelLarge,
            iconSize = 16.dp,
            iconGap = theme.spacing.s,
            borderWidth = Hairline,
            raisedElevation = ElevationLevel.Level0,
            solidColors = ButtonColors(c.primary, c.onPrimary),
            softColors = ButtonColors(c.surfaceContainerHigh, c.onSurface),
            raisedColors = ButtonColors(c.surface, c.onSurface, border = c.outline),
            outlineColors = ButtonColors(Color.Transparent, c.primary, border = c.outlineVariant),
            ghostColors = ButtonColors(Color.Transparent, c.primary),
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
        val colors = style.colorsFor(state.appearance, state.tone, theme)
        val onSurface = theme.colors.onSurface
        val link = state.appearance == Appearance.Link
        val container = when {
            colors.container == Color.Transparent -> Color.Transparent
            state.enabled -> colors.container
            else -> onSurface.disabled(style.disabledContainerAlpha)
        }
        val content = if (state.enabled) colors.content else onSurface.disabled(style.disabledContentAlpha)
        val border = colors.border?.let { if (state.enabled) it else onSurface.disabled(style.disabledContainerAlpha) }
        val depth = if (state.appearance == Appearance.Raised) style.raisedElevation else ElevationLevel.Level0
        val box = Modifier
            .then(Camouflage.skin.surfaceTreatment.modifier(container, style.shape, theme.elevation.of(depth), theme))
            .then(if (border != null) Modifier.border(style.borderWidth, border, style.shape) else Modifier)
            .indication(state.interactionSource, rememberSkinIndication(content, style.shape))
        val size = if (link) {
            Modifier
        } else {
            Modifier
                .heightIn(min = style.height)
                .widthIn(min = style.minWidth)
                .padding(buttonPadding(style, slots, LocalLayoutDirection.current))
        }
        Box(modifier.then(box).then(size), contentAlignment = Alignment.Center) {
            ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                Row(
                    Modifier.alpha(if (state.loading) 0f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(style.iconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    slots.prefixIcon?.invoke()
                    BasicText(
                        state.text,
                        style = style.textStyle.copy(
                            color = content,
                            textDecoration = if (link) TextDecoration.Underline else null,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    slots.suffixIcon?.invoke()
                }
                if (state.loading) Spinner(content, style.iconSize, theme)
            }
        }
    }
}

internal object MinimalIconButtonRenderer : IconButtonRenderer {
    override fun defaults(theme: CamoTheme): IconButtonStyle {
        val c = theme.colors
        return IconButtonStyle(
            defaultAppearance = Appearance.Ghost,
            containerSize = 36.dp,
            shape = theme.shapes.medium,
            iconSize = 20.dp,
            borderWidth = Hairline,
            solidColors = IconButtonColors(c.primary, c.onPrimary),
            softColors = IconButtonColors(c.surfaceContainerHigh, c.onSurface),
            outlineColors = IconButtonColors(Color.Transparent, c.onSurfaceVariant, border = c.outlineVariant),
            ghostColors = IconButtonColors(Color.Transparent, c.onSurfaceVariant),
            checkedColors = IconButtonColors(c.surfaceContainerHigh, c.primary),
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
        val colors = style.colorsFor(state.appearance, state.tone, state.checked == true, theme)
        val onSurface = theme.colors.onSurface
        val container = when {
            colors.container == Color.Transparent -> Color.Transparent
            state.enabled -> colors.container
            else -> onSurface.disabled(style.disabledContainerAlpha)
        }
        val content = if (state.enabled) colors.content else onSurface.disabled(style.disabledContentAlpha)
        val border = colors.border?.let { if (state.enabled) it else onSurface.disabled(style.disabledContainerAlpha) }
        Box(
            modifier
                .size(style.containerSize)
                .then(Camouflage.skin.surfaceTreatment.modifier(container, style.shape, 0.dp, theme))
                .then(if (border != null) Modifier.border(style.borderWidth, border, style.shape) else Modifier)
                .indication(state.interactionSource, rememberSkinIndication(content, style.shape)),
            contentAlignment = Alignment.Center,
        ) {
            if (state.loading) {
                Spinner(content, style.iconSize, theme)
            } else {
                ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                    Box(Modifier.clearAndSetSemantics {}) { slots.icon() }
                }
            }
        }
    }
}

internal object MinimalProgressCircleRenderer : ProgressCircleRenderer {
    override fun defaults(theme: CamoTheme) = ProgressStyle(
        circleStroke = 2.dp,
        circleSizeSmall = 16.dp,
        circleSizeMedium = 32.dp,
        circleSizeLarge = 48.dp,
        colors = ProgressColors(theme.colors.primary, theme.colors.outlineVariant),
        // A 2 px ink bar on a hairline track, no gap, no stop dot.
        barHeight = 2.dp,
        trackGap = 0.dp,
        showStopIndicator = false,
    )

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
        val colors = if (state.tone == Tone.Default) {
            style.colors
        } else {
            ProgressColors(theme.colors.family(state.tone).strong, style.colors.track)
        }
        DrawCircle(state, theme, style.circleSizeOf(state.size), style.circleStroke, colors, modifier)
    }
}

// The loading indicator inside a button: an indeterminate circle in the button's content color, at its icon size.
@Composable
private fun Spinner(color: Color, size: Dp, theme: CamoTheme) {
    val style = Camouflage.skin.progressCircle.defaults(theme).merge(theme.components.progress)
    DrawCircle(
        ProgressState(progress = null, tone = Tone.Default, size = ProgressSize.Small),
        theme,
        size,
        style.circleStroke,
        ProgressColors(color, Color.Transparent),
        Modifier,
    )
}

private const val FULL_TURN = 360f
private const val TWELVE_O_CLOCK = -90f
private const val ARC = 270f
private const val SPIN_CYCLES = 3
private const val PULSE_MILLIS = 1200
private const val PULSE_LOW = 0.4f

@Composable
private fun DrawCircle(
    state: ProgressState,
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
    // Reduced motion: a slow opacity pulse instead of movement (base CamoProgress §3).
    val pulse by transition.animateFloat(
        PULSE_LOW,
        1f,
        infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
    )
    val value by animateFloatAsState(state.progress ?: 0f, tween(theme.motion.durationMedium))
    Canvas(modifier.size(diameter).alpha(if (reduced && state.progress == null) pulse else 1f)) {
        val line = Stroke(stroke.toPx(), cap = StrokeCap.Round)
        val inset = stroke.toPx() / 2
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        if (colors.track.alpha > 0f) drawArc(colors.track, 0f, FULL_TURN, false, topLeft, arcSize, style = line)
        if (state.progress == null) {
            val sweep = if (reduced) FULL_TURN else ARC
            drawArc(colors.active, if (reduced) 0f else angle, sweep, false, topLeft, arcSize, style = line)
        } else {
            drawArc(colors.active, TWELVE_O_CLOCK, value * FULL_TURN, false, topLeft, arcSize, style = line)
        }
    }
}

// Padding beside an icon comes from paddingWithIcon, elsewhere from padding.
internal fun buttonPadding(
    style: ButtonStyle,
    slots: ButtonSlots,
    direction: LayoutDirection,
): PaddingValues = PaddingValues(
    start = (if (slots.prefixIcon != null) style.paddingWithIcon else style.padding).calculateStartPadding(direction),
    top = style.padding.calculateTopPadding(),
    end = (if (slots.suffixIcon != null) style.paddingWithIcon else style.padding).calculateEndPadding(direction),
    bottom = style.padding.calculateBottomPadding(),
)
