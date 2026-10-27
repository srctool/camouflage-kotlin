@file:Suppress("MagicNumber") // the geometry of drawn marks: check strokes, thumb and button proportions

package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.srctool.camouflage.core.checkbox.CheckboxColors
import com.srctool.camouflage.core.checkbox.CheckboxRenderer
import com.srctool.camouflage.core.checkbox.CheckboxState
import com.srctool.camouflage.core.checkbox.CheckboxStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.radio.RadioColors
import com.srctool.camouflage.core.radio.RadioRenderer
import com.srctool.camouflage.core.radio.RadioState
import com.srctool.camouflage.core.radio.RadioStyle
import com.srctool.camouflage.core.segmented.SegmentSlots
import com.srctool.camouflage.core.segmented.SegmentState
import com.srctool.camouflage.core.segmented.SegmentedColors
import com.srctool.camouflage.core.segmented.SegmentedContainerState
import com.srctool.camouflage.core.segmented.SegmentedControlRenderer
import com.srctool.camouflage.core.segmented.SegmentedControlStyle
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.LabelPosition
import com.srctool.camouflage.core.slider.SliderColors
import com.srctool.camouflage.core.slider.SliderRenderer
import com.srctool.camouflage.core.slider.SliderState
import com.srctool.camouflage.core.slider.SliderStyle
import com.srctool.camouflage.core.stepper.StepperButton
import com.srctool.camouflage.core.stepper.StepperColors
import com.srctool.camouflage.core.stepper.StepperLayout
import com.srctool.camouflage.core.stepper.StepperRenderer
import com.srctool.camouflage.core.stepper.StepperSlots
import com.srctool.camouflage.core.stepper.StepperState
import com.srctool.camouflage.core.stepper.StepperStyle
import com.srctool.camouflage.core.switch.SwitchColors
import com.srctool.camouflage.core.switch.SwitchRenderer
import com.srctool.camouflage.core.switch.SwitchState
import com.srctool.camouflage.core.switch.SwitchStyle
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.LabelledControl
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.minimumTouchTarget
import com.srctool.camouflage.core.util.rememberSkinIndication
import com.srctool.camouflage.skin.fluent.FluentTokens

// Fluent's Checkbox, Switch, Radio, toggle-button group, Slider and SpinButton (base Fluent Skin §3.2). Hover and
// press swap to FluentTokens' steps; Subtle draws the focus.

private val Stroke1 = 1.dp
private const val DISABLED = 0.38f

internal object FluentCheckboxRenderer : CheckboxRenderer {
    override fun defaults(theme: CamoTheme): CheckboxStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        return CheckboxStyle(
            boxSize = 16.dp,
            shape = theme.shapes.extraSmall,
            borderWidth = Stroke1,
            labelStyle = theme.typography.bodyMedium,
            labelPosition = LabelPosition.End,
            labelGap = theme.spacing.s,
            descriptionStyle = theme.typography.bodySmall.copy(color = c.onSurfaceVariant),
            colors = CheckboxColors(
                checkedBox = t?.brandBackground ?: c.primary,
                checkmark = t?.neutralForegroundOnBrand ?: c.onPrimary,
                uncheckedBorder = c.outline,
                label = c.onSurface,
            ),
        )
    }

    @Composable
    override fun Render(state: CheckboxState, theme: CamoTheme, style: CheckboxStyle, modifier: Modifier) {
        val colors = style.colors
        val t = theme.tokens<FluentTokens>()
        val on by animateFloatAsState(if (state.checked) 1f else 0f, tween(theme.motion.durationShort))
        val fill = brandStep(colors.checkedBox, state.interaction, t)
        val border = when {
            state.error != null -> theme.colors.error
            state.checked -> fill
            state.interaction.hovered || state.interaction.pressed -> theme.colors.onSurface
            else -> colors.uncheckedBorder
        }
        val box = Modifier
            .size(style.boxSize)
            .background(fill.copy(alpha = fill.alpha * on), style.shape)
            .border(style.borderWidth, border, style.shape)
            .indication(state.interactionSource, rememberSkinIndication(colors.checkedBox, style.shape))
        LabelledControl(
            modifier.alpha(if (state.enabled) 1f else DISABLED),
            style.labelPosition,
            style.labelGap,
            centerControl = false,
            firstLineHeight = lineHeight(style.labelStyle),
            control = { Canvas(box) { drawCheck(colors.checkmark.copy(alpha = on), size.width) } },
            label = state.label?.let {
                { LabelText(it, state.description, style.labelStyle, colors.label, style.descriptionStyle) }
            },
            below = state.error?.let { { ErrorText(it, theme) } },
        )
    }
}

internal object FluentSwitchRenderer : SwitchRenderer {
    override fun defaults(theme: CamoTheme): SwitchStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        return SwitchStyle(
            showThumbIcons = false,
            thumbIcons = null,
            trackWidth = 40.dp,
            trackHeight = 20.dp,
            thumbSizeOff = 14.dp,
            thumbSizeOn = 14.dp,
            thumbSizePressed = 14.dp,
            labelStyle = theme.typography.bodyMedium,
            labelPosition = LabelPosition.End,
            labelGap = theme.spacing.s,
            descriptionStyle = theme.typography.bodySmall.copy(color = c.onSurfaceVariant),
            colors = SwitchColors(
                trackOn = t?.brandBackground ?: c.primary,
                trackOff = Color.Transparent,
                thumbOn = t?.neutralForegroundOnBrand ?: c.onPrimary,
                thumbOff = c.outline,
                trackOffBorder = c.outline,
                label = c.onSurface,
            ),
        )
    }

    @Composable
    override fun Render(state: SwitchState, theme: CamoTheme, style: SwitchStyle, modifier: Modifier) {
        LabelledControl(
            modifier.alpha(if (state.enabled) 1f else DISABLED),
            style.labelPosition,
            style.labelGap,
            centerControl = true,
            firstLineHeight = 0.dp,
            control = { SwitchTrack(state, theme, style) },
            label = state.label?.let {
                { LabelText(it, state.description, style.labelStyle, style.colors.label, style.descriptionStyle) }
            },
            below = state.error?.let { { ErrorText(it, theme) } },
        )
    }
}

internal object FluentRadioRenderer : RadioRenderer {
    override fun defaults(theme: CamoTheme): RadioStyle {
        val c = theme.colors
        return RadioStyle(
            size = 16.dp,
            dotSize = 10.dp,
            borderWidth = Stroke1,
            labelStyle = theme.typography.bodyMedium,
            descriptionStyle = theme.typography.bodySmall.copy(color = c.onSurfaceVariant),
            labelPosition = LabelPosition.End,
            labelGap = theme.spacing.s,
            colors = RadioColors(selected = c.primary, unselected = c.outline, error = c.error, label = c.onSurface),
        )
    }

    @Composable
    override fun Render(state: RadioState, theme: CamoTheme, style: RadioStyle, modifier: Modifier) {
        val colors = style.colors
        val on by animateFloatAsState(if (state.selected) 1f else 0f, tween(theme.motion.durationShort))
        val active = state.interaction.hovered || state.interaction.pressed
        val ring = when {
            state.error -> colors.error
            state.selected -> colors.selected
            active -> theme.colors.onSurface
            else -> colors.unselected
        }
        val circle = Modifier
            .size(style.size)
            .indication(state.interactionSource, rememberSkinIndication(colors.selected, CircleShape))
        LabelledControl(
            modifier.alpha(if (state.enabled) 1f else DISABLED),
            style.labelPosition,
            style.labelGap,
            centerControl = false,
            firstLineHeight = lineHeight(style.labelStyle),
            control = {
                Canvas(circle) {
                    val stroke = style.borderWidth.toPx()
                    drawCircle(ring, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
                    if (on > 0f) drawCircle(colors.selected, radius = style.dotSize.toPx() / 2 * on)
                }
            },
            label = state.label?.let {
                { LabelText(it, state.description, style.labelStyle, colors.label, style.descriptionStyle) }
            },
        )
    }
}

internal object FluentSegmentedControlRenderer : SegmentedControlRenderer {
    override fun defaults(theme: CamoTheme): SegmentedControlStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        return SegmentedControlStyle(
            height = 32.dp,
            shape = theme.shapes.small,
            borderWidth = Stroke1,
            textStyle = theme.typography.bodyMedium,
            iconSize = 20.dp,
            showCheckIcon = false,
            colors = SegmentedColors(
                container = c.surface,
                border = t?.neutralStroke1 ?: c.outlineVariant,
                selectedContainer = c.surfaceContainerHighest,
                selectedContent = c.onSurface,
                content = c.onSurface,
            ),
        )
    }

    @Composable
    override fun Container(
        state: SegmentedContainerState,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        Row(
            modifier
                .alpha(if (state.enabled) 1f else DISABLED)
                .heightIn(min = style.height)
                .background(style.colors.container, style.shape)
                .border(style.borderWidth, style.colors.border, style.shape)
                .clip(style.shape),
            content = content,
        )
    }

    @Composable
    override fun Segment(
        state: SegmentState,
        slots: SegmentSlots,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
    ) {
        val colors = style.colors
        val t = theme.tokens<FluentTokens>()
        val i = state.interaction
        val fill = when {
            state.selected -> colors.selectedContainer
            i.pressed -> t?.subtleBackgroundPressed ?: colors.selectedContainer
            i.hovered -> t?.subtleBackgroundHover ?: colors.selectedContainer.copy(alpha = HOVER_ALPHA)
            else -> Color.Transparent
        }
        val content = if (state.selected) colors.selectedContent else colors.content
        val shape = RectangleShape
        val divider = colors.border
        val width = style.borderWidth
        Box(
            modifier
                .fillMaxHeight()
                .drawBehind {
                    if (!state.position.isFirst) {
                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f
                        drawLine(divider, Offset(x, 0f), Offset(x, size.height), width.toPx())
                    }
                }
                .background(fill, shape)
                .indication(state.interactionSource, rememberSkinIndication(content, shape))
                .alpha(if (state.enabled) 1f else DISABLED)
                .padding(horizontal = theme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            SegmentContent(slots, state.selected && style.showCheckIcon, content, style, theme)
        }
    }
}

internal object FluentSliderRenderer : SliderRenderer {
    override fun defaults(theme: CamoTheme): SliderStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        return SliderStyle(
            trackHeight = 4.dp,
            thumbSize = DpSize(20.dp, 20.dp),
            showStopMarks = false,
            showValueLabel = false,
            valueLabelStyle = theme.typography.bodySmall,
            colors = SliderColors(
                activeTrack = t?.brandBackground ?: c.primary,
                inactiveTrack = c.outline,
                thumb = t?.brandBackground ?: c.primary,
                stopMark = c.surface,
                valueLabel = c.inverseSurface,
                valueLabelText = c.inverseOnSurface,
            ),
        )
    }

    @Composable
    override fun Render(state: SliderState, theme: CamoTheme, style: SliderStyle, modifier: Modifier) {
        val t = theme.tokens<FluentTokens>()
        val dot = brandStep(style.colors.thumb, state.interaction, t)
        val ring = t?.neutralStroke1 ?: theme.colors.outlineVariant
        val colors = style.colors
        val height = maxOf(style.thumbSize.height, style.trackHeight)
        Box(
            modifier.alpha(if (state.enabled) 1f else DISABLED).fillMaxWidth().height(height),
            contentAlignment = Alignment.CenterStart,
        ) {
            SliderTrack(state, style)
            Box(
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints())
                    val travel = constraints.maxWidth - placeable.width
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative((travel * state.fraction).toInt(), 0)
                    }
                },
            ) {
                // Fluent's thumb: a neutral ring around a brand dot.
                Canvas(
                    Modifier
                        .size(style.thumbSize)
                        .indication(state.interactionSource, rememberSkinIndication(dot, CircleShape)),
                ) {
                    val r = size.minDimension / 2
                    drawCircle(theme.colors.surface, r)
                    drawCircle(ring, r - Stroke1.toPx() / 2, style = Stroke(Stroke1.toPx()))
                    drawCircle(dot, r * THUMB_DOT)
                }
            }
            if (style.showValueLabel && state.interaction.dragged && state.valueText != null) {
                BasicText(
                    state.valueText!!,
                    Modifier
                        .layout { measurable, constraints ->
                            val label = measurable.measure(Constraints())
                            val thumbWidth = style.thumbSize.width.roundToPx()
                            val x = ((constraints.maxWidth - thumbWidth) * state.fraction).toInt() +
                                thumbWidth / 2 - label.width / 2
                            layout(0, 0) { label.placeRelative(x, -label.height - height.roundToPx()) }
                        }
                        .background(colors.valueLabel, theme.shapes.small)
                        .padding(horizontal = theme.spacing.s, vertical = theme.spacing.xxs),
                    style = style.valueLabelStyle.copy(color = colors.valueLabelText),
                )
            }
        }
    }
}

internal object FluentStepperRenderer : StepperRenderer {
    override fun defaults(theme: CamoTheme): StepperStyle {
        val c = theme.colors
        val t = theme.tokens<FluentTokens>()
        return StepperStyle(
            layout = StepperLayout.Split,
            buttonAppearance = Appearance.Ghost,
            height = 32.dp,
            valueMinWidth = 64.dp,
            valueStyle = theme.typography.bodyMedium,
            decreaseIcon = IconSource.Vector(ChevronDown),
            increaseIcon = IconSource.Vector(ChevronUp),
            colors = StepperColors(
                button = c.onSurfaceVariant,
                value = c.onSurface,
                field = t?.neutralStroke1 ?: c.outlineVariant,
                error = c.error,
            ),
        )
    }

    @Composable
    override fun Render(
        state: StepperState,
        slots: StepperSlots,
        theme: CamoTheme,
        style: StepperStyle,
        modifier: Modifier,
    ) {
        val shape = theme.shapes.small
        val border = if (state.error) style.colors.error else style.colors.field
        when (style.layout) {
            StepperLayout.Split -> SplitField(state, slots, theme, style, modifier, border)
            StepperLayout.Inline -> Row(
                modifier.alpha(if (state.enabled) 1f else DISABLED),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InlineButton(state.decrease, style.decreaseIcon, style, theme, border)
                Box(Modifier.widthIn(min = style.valueMinWidth), contentAlignment = Alignment.Center) { slots.input() }
                InlineButton(state.increase, style.increaseIcon, style, theme, border)
            }
        }
    }

    // Fluent's SpinButton: the value field with ▲ ▼ at the end.
    @Composable
    private fun SplitField(
        state: StepperState,
        slots: StepperSlots,
        theme: CamoTheme,
        style: StepperStyle,
        modifier: Modifier,
        border: Color,
    ) {
        val shape = theme.shapes.small
        // Fluent's input: a stroke all around, a stronger bottom edge that turns brand (2 dp) on focus.
        val accent = when {
            state.error -> style.colors.error
            state.focused -> theme.colors.primary
            else -> theme.colors.outline
        }
        val accentWidth = if (state.focused) 2.dp else Stroke1
        Row(
            modifier
                .alpha(if (state.enabled) 1f else DISABLED)
                .height(IntrinsicSize.Min)
                .heightIn(min = style.height)
                .background(theme.colors.surface, shape)
                .border(Stroke1, border, shape)
                .drawBehind {
                    val w = accentWidth.toPx()
                    drawRect(accent, Offset(0f, size.height - w), size.copy(height = w))
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.widthIn(min = style.valueMinWidth).padding(horizontal = theme.spacing.s)) {
                slots.input()
            }
            Column(Modifier.fillMaxHeight().padding(vertical = Stroke1)) {
                SplitButton(state.increase, style.increaseIcon, style, theme)
                SplitButton(state.decrease, style.decreaseIcon, style, theme)
            }
        }
    }

    @Composable
    private fun ColumnScope.SplitButton(
        button: StepperButton,
        icon: IconSource,
        style: StepperStyle,
        theme: CamoTheme,
    ) {
        val t = theme.tokens<FluentTokens>()
        val fill = when {
            !button.enabled -> Color.Transparent
            button.interaction.pressed -> t?.subtleBackgroundPressed ?: Color.Transparent
            button.interaction.hovered -> t?.subtleBackgroundHover ?: Color.Transparent
            else -> Color.Transparent
        }
        Box(
            button.behavior
                .weight(1f)
                .width(style.height * 3 / 4)
                .background(fill)
                .indication(button.interactionSource, rememberSkinIndication(style.colors.button, RectangleShape))
                .alpha(if (button.enabled) 1f else DISABLED),
            contentAlignment = Alignment.Center,
        ) {
            CamoIcon(icon, null, size = SplitIcon, tint = style.colors.button)
        }
    }

    @Composable
    private fun InlineButton(
        button: StepperButton,
        icon: IconSource,
        style: StepperStyle,
        theme: CamoTheme,
        border: Color,
    ) {
        val shape = theme.shapes.small
        val outlined = style.buttonAppearance == Appearance.Outline || style.buttonAppearance == Appearance.Soft
        Box(
            button.behavior
                .minimumTouchTarget()
                .size(style.height)
                .then(if (outlined) Modifier.border(Stroke1, border, shape) else Modifier)
                .indication(button.interactionSource, rememberSkinIndication(style.colors.button, shape))
                .alpha(if (button.enabled) 1f else DISABLED),
            contentAlignment = Alignment.Center,
        ) {
            CamoIcon(icon, null, size = InlineIcon, tint = style.colors.button)
        }
    }
}

private val SplitIcon = 12.dp
private val InlineIcon = 16.dp
private const val THUMB_DOT = 0.6f
private const val HOVER_ALPHA = 0.5f

// The rail across the width, inset by half a thumb at each end, the active part and the stops.
@Composable
private fun SliderTrack(state: SliderState, style: SliderStyle) {
    val colors = style.colors
    Canvas(Modifier.fillMaxWidth().height(style.trackHeight)) {
        val inset = style.thumbSize.width.toPx() / 2
        val rtl = layoutDirection == LayoutDirection.Rtl
        val travel = size.width - 2 * inset
        fun x(f: Float) = if (rtl) size.width - inset - f * travel else inset + f * travel
        val y = size.height / 2
        drawLine(colors.inactiveTrack, Offset(x(0f), y), Offset(x(1f), y), size.height, StrokeCap.Round)
        val end = Offset(x(state.fraction), y)
        drawLine(colors.activeTrack, Offset(x(0f), y), end, size.height, StrokeCap.Round)
        if (style.showStopMarks && state.steps > 0) {
            for (i in 0..state.steps + 1) {
                drawCircle(colors.stopMark, size.height / 4, Offset(x(i / (state.steps + 1f)), y))
            }
        }
    }
}

// The brand fill's hover and pressed steps from FluentTokens; the fill itself under another theme.
private fun brandStep(rest: Color, i: InteractionState, t: FluentTokens?): Color = when {
    t == null || rest != t.brandBackground -> rest
    i.pressed -> t.brandBackgroundPressed
    i.hovered -> t.brandBackgroundHover
    else -> rest
}

@Composable
private fun SwitchTrack(state: SwitchState, theme: CamoTheme, style: SwitchStyle) {
    val colors = style.colors
    val t = theme.tokens<FluentTokens>()
    val animated by animateFloatAsState(
        if (state.checked) 1f else 0f,
        tween(theme.motion.durationShort, easing = theme.motion.easingStandard),
    )
    val on = state.dragFraction ?: animated
    val off = 1f - on
    val active = state.interaction.hovered || state.interaction.pressed
    val trackOn = brandStep(colors.trackOn, state.interaction, t)
    val offStroke = if (active) theme.colors.onSurface else colors.trackOffBorder
    val thumbOff = if (active) theme.colors.onSurface else colors.thumbOff
    val thumb = if (state.checked) style.thumbSizeOn else style.thumbSizeOff
    val inset = (style.trackHeight - thumb) / 2
    val x = inset + (style.trackWidth - thumb - inset * 2) * on
    Box(
        Modifier
            .size(style.trackWidth, style.trackHeight)
            .background(lerp(colors.trackOff, trackOn, on), CircleShape)
            .border(Stroke1, offStroke.copy(alpha = offStroke.alpha * off), CircleShape)
            .indication(state.interactionSource, rememberSkinIndication(colors.trackOn, CircleShape)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(thumb)
                .background(lerp(thumbOff, colors.thumbOn, on), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val icon = if (state.checked) style.thumbIcons?.checked else style.thumbIcons?.unchecked
            if (style.showThumbIcons && icon != null) {
                CamoIcon(icon, null, size = thumb * 2 / 3, tint = if (state.checked) trackOn else theme.colors.surface)
            }
        }
    }
}

@Composable
private fun LabelText(
    label: List<CamoSpan>,
    description: String?,
    labelStyle: TextStyle,
    labelColor: Color,
    descriptionStyle: TextStyle,
) {
    Column {
        CamoText(label, style = labelStyle, color = labelColor)
        description?.let { CamoText(it, style = descriptionStyle, color = descriptionStyle.color) }
    }
}

@Composable
private fun ErrorText(message: String, theme: CamoTheme) {
    CamoText(
        message,
        Modifier.clearAndSetSemantics {}.padding(top = theme.spacing.xs),
        style = theme.typography.bodySmall,
        color = theme.colors.error,
    )
}

@Composable
private fun lineHeight(style: TextStyle): Dp = with(LocalDensity.current) {
    if (style.lineHeight.isSpecified) style.lineHeight.toDp() else (style.fontSize * LINE_FACTOR).toDp()
}

private const val LINE_FACTOR = 1.4f

private fun DrawScope.drawCheck(color: Color, side: Float) {
    if (color.alpha == 0f) return
    val path = Path().apply {
        moveTo(side * 0.22f, side * 0.52f)
        lineTo(side * 0.42f, side * 0.72f)
        lineTo(side * 0.80f, side * 0.30f)
    }
    drawPath(path, color, style = Stroke(side / 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal val ChevronUp: ImageVector = ImageVector.Builder("chevron-up", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round) {
        moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f)
    }
    .build()

internal val ChevronDown: ImageVector = ImageVector.Builder("chevron-down", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round) {
        moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
    }
    .build()

// A segment's check (when asked), icon and one-line label, or a building block's own content.
@Composable
private fun SegmentContent(
    slots: SegmentSlots,
    check: Boolean,
    color: Color,
    style: SegmentedControlStyle,
    theme: CamoTheme,
) {
    ProvideContentStyle(color, IconStyle(style.iconSize, color)) {
        val custom = slots.content
        if (custom != null) {
            custom()
            return@ProvideContentStyle
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (check) Canvas(Modifier.size(style.iconSize)) { drawCheck(color, size.width) }
            slots.icon?.invoke()
            slots.label?.let {
                BasicText(
                    it,
                    Modifier.padding(start = if (slots.icon != null) theme.spacing.xs else 0.dp),
                    style = style.textStyle.copy(color = color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
