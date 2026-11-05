package com.srctool.camouflage.skin.cupertino.renderer

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
import androidx.compose.ui.text.font.FontWeight
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

// Cupertino's buttons, icon buttons and activity indicator (base Cupertino Skin §3.1 and §3.2): a prominent button
// is the accent filled, Soft is the accent at 15%, Raised and Outline fold into Soft, Ghost and Link are plain accent
// text (Apple has no outlined or underlined buttons). With Liquid Glass the shape is a capsule and Soft is glass.

private const val DISABLED_CONTAINER = 0.12f
private const val DISABLED_CONTENT = 0.38f

internal class CupertinoButtonRenderer(private val glass: Boolean) : ButtonRenderer {
    override fun defaults(theme: CamoTheme): ButtonStyle {
        val c = theme.colors
        return ButtonStyle(
            defaultAppearance = Appearance.Solid,
            height = 44.dp,
            minWidth = 44.dp,
            shape = if (glass) theme.shapes.full else theme.shapes.medium,
            padding = PaddingValues(horizontal = 16.dp),
            paddingWithIcon = PaddingValues(horizontal = theme.spacing.m),
            textStyle = theme.typography.labelLarge,
            iconSize = 20.dp,
            iconGap = theme.spacing.s,
            borderWidth = 0.dp,
            raisedElevation = ElevationLevel.Level0,
            solidColors = ButtonColors(c.primary, c.onPrimary),
            softColors = ButtonColors(c.primaryContainer, c.primary),
            raisedColors = ButtonColors(c.primaryContainer, c.primary),
            outlineColors = ButtonColors(c.primaryContainer, c.primary),
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
        val depth = buttonDepth(glass, state.appearance, colors.container)
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
                            fontWeight = if (state.appearance == Appearance.Solid) FontWeight.SemiBold else null,
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

internal class CupertinoIconButtonRenderer(private val glass: Boolean) : IconButtonRenderer {
    override fun defaults(theme: CamoTheme): IconButtonStyle {
        val c = theme.colors
        return IconButtonStyle(
            defaultAppearance = Appearance.Ghost,
            containerSize = 44.dp,
            shape = if (glass) theme.shapes.full else theme.shapes.medium,
            iconSize = 22.dp,
            borderWidth = 0.dp,
            solidColors = IconButtonColors(c.primary, c.onPrimary),
            softColors = IconButtonColors(c.primaryContainer, c.primary),
            outlineColors = IconButtonColors(c.primaryContainer, c.primary),
            ghostColors = IconButtonColors(Color.Transparent, c.primary),
            checkedColors = IconButtonColors(c.primaryContainer, c.primary),
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
                .then(
                    Camouflage.skin.surfaceTreatment.modifier(
                        container, style.shape, glassDepth(glass, container, theme), theme,
                    ),
                )
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

internal object CupertinoProgressCircleRenderer : ProgressCircleRenderer {
    override fun defaults(theme: CamoTheme) = ProgressStyle(
        circleStroke = 3.dp,
        circleSizeSmall = 20.dp, // UIActivityIndicator regular
        circleSizeMedium = 28.dp,
        circleSizeLarge = 37.dp, // UIActivityIndicator large
        colors = ProgressColors(theme.colors.primary, theme.colors.secondaryContainer),
        // The bar: a 4 dp capsule on a gray fill, no gap, no stop dot (UIProgressView).
        barHeight = 4.dp,
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
private const val HALF_TURN = 180f
private const val TWELVE_O_CLOCK = -90f
private const val SPOKES = 12
private const val SPIN_MILLIS = 1000
private const val PULSE_MILLIS = 1200
private const val PULSE_LOW = 0.4f
private const val TRAIL = 0.8f
private const val SPOKE_START = 0.55f

// Determinate: a ring. Indeterminate: the activity indicator, twelve spokes lit in turn (reduced motion: a pulse).
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
    val step by transition.animateFloat(
        0f,
        SPOKES.toFloat(),
        infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing)),
    )
    // Reduced motion: a slow opacity pulse instead of movement (base CamoProgress §3).
    val pulse by transition.animateFloat(
        PULSE_LOW,
        1f,
        infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
    )
    val value by animateFloatAsState(state.progress ?: 0f, tween(theme.motion.durationMedium))
    Canvas(modifier.size(diameter).alpha(if (reduced && state.progress == null) pulse else 1f)) {
        if (state.progress == null) {
            val head = if (reduced) 0 else step.toInt() % SPOKES
            val radius = size.minDimension / 2
            val width = radius * 0.2f
            for (i in 0 until SPOKES) {
                val fade = ((head - i + SPOKES) % SPOKES) / SPOKES.toFloat()
                val angle = (i * FULL_TURN / SPOKES + TWELVE_O_CLOCK) * (kotlin.math.PI / HALF_TURN).toFloat()
                val dx = kotlin.math.cos(angle)
                val dy = kotlin.math.sin(angle)
                val center = Offset(size.width / 2, size.height / 2)
                drawLine(
                    colors.active.copy(alpha = colors.active.alpha * (1f - fade * TRAIL)),
                    center + Offset(dx, dy) * (radius * SPOKE_START),
                    center + Offset(dx, dy) * (radius - width / 2),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )
            }
        } else {
            val line = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            val inset = stroke.toPx() / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(colors.track, 0f, FULL_TURN, false, topLeft, arcSize, style = line)
            drawArc(colors.active, TWELVE_O_CLOCK, value * FULL_TURN, false, topLeft, arcSize, style = line)
        }
    }
}

// The glass depth of a tinted control: Level2 (a material) with Liquid Glass, flat otherwise.
internal fun glassDepth(glass: Boolean, container: Color, theme: CamoTheme) =
    theme.elevation.of(if (glass && container != Color.Transparent) ElevationLevel.Level2 else ElevationLevel.Level0)

// Liquid Glass: the tinted buttons (Soft, and Raised and Outline folded into it) are glass, a material at Level2; the
// prominent one and the plain text ones stay as they are. Classic buttons are always flat.
private fun buttonDepth(glass: Boolean, appearance: Appearance, container: Color): ElevationLevel {
    val tinted = appearance == Appearance.Soft || appearance == Appearance.Raised || appearance == Appearance.Outline
    return if (glass && tinted && container != Color.Transparent) ElevationLevel.Level2 else ElevationLevel.Level0
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
