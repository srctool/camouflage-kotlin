@file:Suppress("MagicNumber") // the geometry of drawn marks: check strokes, thumb and button proportions

package com.srctool.camouflage.skin.cupertino.renderer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
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

// Cupertino's selection controls, slider and stepper (base Cupertino Skin §3.2): accent when on (the switch too,
// decided 2026-09-27), a round check, a 51 x 31 switch, a checkmark radio, a segmented track with a white thumb,
// a 28 thumb with a shadow, a gray -/+ pill.

private val Hairline = 0.5.dp
private val ControlBorder = 1.5.dp
private const val DISABLED = 0.38f

internal object CupertinoCheckboxRenderer : CheckboxRenderer {
    override fun defaults(theme: CamoTheme): CheckboxStyle {
        val c = theme.colors
        return CheckboxStyle(
            boxSize = 22.dp,
            shape = theme.shapes.full,
            borderWidth = ControlBorder,
            labelStyle = theme.typography.bodyLarge,
            labelPosition = LabelPosition.End,
            labelGap = theme.spacing.m,
            descriptionStyle = theme.typography.bodySmall.copy(color = c.onSurfaceVariant),
            colors = CheckboxColors(c.primary, c.onPrimary, c.outline, c.onSurface),
        )
    }

    @Composable
    override fun Render(state: CheckboxState, theme: CamoTheme, style: CheckboxStyle, modifier: Modifier) {
        val colors = style.colors
        val on by animateFloatAsState(if (state.checked) 1f else 0f, tween(theme.motion.durationShort))
        val border = when {
            state.error != null -> theme.colors.error
            state.checked -> colors.checkedBox
            else -> colors.uncheckedBorder
        }
        val box = Modifier
            .size(style.boxSize)
            .background(colors.checkedBox.copy(alpha = colors.checkedBox.alpha * on), style.shape)
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

internal object CupertinoSwitchRenderer : SwitchRenderer {
    override fun defaults(theme: CamoTheme): SwitchStyle {
        val c = theme.colors
        return SwitchStyle(
            showThumbIcons = false,
            thumbIcons = null,
            trackWidth = 51.dp,
            trackHeight = 31.dp,
            thumbSizeOff = 27.dp,
            thumbSizeOn = 27.dp,
            thumbSizePressed = 27.dp,
            labelStyle = theme.typography.bodyLarge,
            labelPosition = LabelPosition.Start,
            labelGap = theme.spacing.l,
            descriptionStyle = theme.typography.bodySmall.copy(color = c.onSurfaceVariant),
            colors = SwitchColors(
                trackOn = c.primary,
                trackOff = c.surfaceContainerHigh,
                thumbOn = Color.White,
                thumbOff = Color.White,
                trackOffBorder = Color.Transparent,
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

internal object CupertinoRadioRenderer : RadioRenderer {
    override fun defaults(theme: CamoTheme): RadioStyle {
        val c = theme.colors
        return RadioStyle(
            size = 22.dp,
            dotSize = 8.dp,
            borderWidth = Hairline,
            labelStyle = theme.typography.bodyLarge,
            descriptionStyle = theme.typography.bodySmall.copy(color = c.onSurfaceVariant),
            labelPosition = LabelPosition.Start,
            labelGap = theme.spacing.m,
            colors = RadioColors(c.primary, c.outline, c.error, c.onSurface),
        )
    }

    @Composable
    override fun Render(state: RadioState, theme: CamoTheme, style: RadioStyle, modifier: Modifier) {
        val colors = style.colors
        val on by animateFloatAsState(if (state.selected) 1f else 0f, tween(theme.motion.durationShort))
        val ring = when {
            state.error -> colors.error
            state.selected -> colors.selected
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
                // iOS list style: a check mark on the selected row. An unselected option keeps a faint ring, so a
                // horizontal group (core doesn't tell the renderer) still reads as options.
                Canvas(circle) {
                    val stroke = style.borderWidth.toPx()
                    if (on < 1f) {
                        val faint = ring.copy(alpha = ring.alpha * (1f - on))
                        drawCircle(faint, (size.minDimension - stroke) / 2, style = Stroke(stroke))
                    }
                    drawCheck(colors.selected.copy(alpha = on), size.width)
                }
            },
            label = state.label?.let {
                { LabelText(it, state.description, style.labelStyle, colors.label, style.descriptionStyle) }
            },
        )
    }
}

internal object CupertinoSegmentedControlRenderer : SegmentedControlRenderer {
    override fun defaults(theme: CamoTheme): SegmentedControlStyle {
        val c = theme.colors
        return SegmentedControlStyle(
            height = 32.dp,
            shape = theme.shapes.small,
            borderWidth = 0.dp,
            textStyle = theme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            iconSize = 16.dp,
            showCheckIcon = false,
            colors = SegmentedColors(
                container = c.secondaryContainer,
                border = Color.Transparent,
                selectedContainer = thumbColor(theme),
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
                .padding(theme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.xxs),
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
        val shape = theme.shapes.extraSmall
        val fill by animateColorAsState(
            if (state.selected) colors.selectedContainer else colors.selectedContainer.copy(alpha = 0f),
            tween(theme.motion.durationShort),
        )
        val content = if (state.selected) colors.selectedContent else colors.content
        Box(
            modifier
                .fillMaxHeight()
                .then(if (state.selected) Modifier.shadow(1.dp, shape) else Modifier)
                .background(fill, shape)
                .indication(state.interactionSource, rememberSkinIndication(content, shape))
                .alpha(if (state.enabled) 1f else DISABLED)
                .padding(horizontal = theme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            SegmentContent(slots, state.selected && style.showCheckIcon, content, style)
        }
    }
}

internal object CupertinoSliderRenderer : SliderRenderer {
    override fun defaults(theme: CamoTheme): SliderStyle {
        val c = theme.colors
        return SliderStyle(
            trackHeight = 4.dp,
            thumbSize = DpSize(28.dp, 28.dp),
            showStopMarks = false,
            showValueLabel = true,
            valueLabelStyle = theme.typography.labelMedium,
            colors = SliderColors(
                activeTrack = c.primary,
                inactiveTrack = c.secondaryContainer,
                thumb = Color.White,
                stopMark = c.onSurfaceVariant,
                valueLabel = c.inverseSurface,
                valueLabelText = c.inverseOnSurface,
            ),
        )
    }

    @Composable
    override fun Render(state: SliderState, theme: CamoTheme, style: SliderStyle, modifier: Modifier) {
        val thumbShape = if (style.thumbSize.width == style.thumbSize.height) CircleShape else theme.shapes.full
        SliderLayout(state, theme, style, modifier.alpha(if (state.enabled) 1f else DISABLED)) {
            Box(
                Modifier
                    .size(style.thumbSize)
                    .shadow(3.dp, thumbShape)
                    .background(style.colors.thumb, thumbShape)
                    .indication(state.interactionSource, rememberSkinIndication(theme.colors.onSurface, thumbShape)),
            )
        }
    }
}

internal object CupertinoStepperRenderer : StepperRenderer {
    override fun defaults(theme: CamoTheme): StepperStyle {
        val c = theme.colors
        return StepperStyle(
            layout = StepperLayout.Inline,
            buttonAppearance = Appearance.Soft,
            height = 32.dp,
            valueMinWidth = 40.dp,
            valueStyle = theme.typography.titleMedium,
            decreaseIcon = IconSource.Vector(Minus),
            increaseIcon = IconSource.Vector(Plus),
            colors = StepperColors(
                button = c.onSurface,
                value = c.onSurface,
                field = c.secondaryContainer,
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
        val border = if (state.error) style.colors.error else style.colors.field
        val shape = theme.shapes.small
        when (style.layout) {
            // The value beside a gray pill with the - and the + in halves (UIStepper).
            StepperLayout.Inline -> Row(
                modifier.alpha(if (state.enabled) 1f else DISABLED),
                horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.widthIn(min = style.valueMinWidth), contentAlignment = Alignment.Center) { slots.input() }
                Row(
                    Modifier
                        .background(style.colors.field, shape)
                        .then(if (state.error) Modifier.border(Hairline, border, shape) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepButton(state.decrease, style.decreaseIcon, style, theme, Modifier.minimumTouchTarget())
                    Box(Modifier.width(Hairline).height(style.height / 2).background(theme.colors.outline))
                    StepButton(state.increase, style.increaseIcon, style, theme, Modifier.minimumTouchTarget())
                }
            }
            StepperLayout.Split -> Row(
                modifier
                    .alpha(if (state.enabled) 1f else DISABLED)
                    .height(IntrinsicSize.Min)
                    .heightIn(min = style.height)
                    .border(
                        if (state.focused) ControlBorder else Hairline,
                        if (state.focused) theme.colors.primary else border,
                        shape,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.widthIn(min = style.valueMinWidth).padding(horizontal = theme.spacing.m)) { slots.input() }
                Column(Modifier.fillMaxHeight()) {
                    SplitButton(state.increase, style.increaseIcon, style)
                    SplitButton(state.decrease, style.decreaseIcon, style)
                }
            }
        }
    }

    @Composable
    private fun StepButton(
        button: StepperButton,
        icon: IconSource,
        style: StepperStyle,
        theme: CamoTheme,
        target: Modifier,
    ) {
        val shape = theme.shapes.small
        val content = style.colors.button
        Box(
            button.behavior
                .then(target)
                .size(width = StepWidth, height = style.height)
                .indication(button.interactionSource, rememberSkinIndication(content, shape))
                .alpha(if (button.enabled) 1f else DISABLED),
            contentAlignment = Alignment.Center,
        ) {
            CamoIcon(icon, null, size = StepIconSize, tint = content)
        }
    }

    @Composable
    private fun ColumnScope.SplitButton(button: StepperButton, icon: IconSource, style: StepperStyle) {
        Box(
            button.behavior
                .weight(1f)
                .width(style.height * 3 / 4)
                .indication(button.interactionSource, rememberSkinIndication(style.colors.button, RectangleShapeOf))
                .alpha(if (button.enabled) 1f else DISABLED),
            contentAlignment = Alignment.Center,
        ) {
            CamoIcon(icon, null, size = SplitIconSize, tint = style.colors.button)
        }
    }
}

private val StepWidth = 47.dp
private val StepIconSize = 16.dp
private val SplitIconSize = 12.dp
private val RectangleShapeOf = RectangleShape

// The label and its description line.
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

// The message under a control. Core announces it, so the text hides its own semantics.
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

// A check mark inside a box of side `side`, in `color`.
private fun DrawScope.drawCheck(color: Color, side: Float) {
    if (color.alpha == 0f) return
    val path = Path().apply {
        moveTo(side * 0.24f, side * 0.52f)
        lineTo(side * 0.42f, side * 0.70f)
        lineTo(side * 0.77f, side * 0.32f)
    }
    drawPath(path, color, style = Stroke(side / 9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun SwitchTrack(state: SwitchState, theme: CamoTheme, style: SwitchStyle) {
    val colors = style.colors
    val motion = tween<Float>(theme.motion.durationShort, easing = theme.motion.easingStandard)
    val animated by animateFloatAsState(if (state.checked) 1f else 0f, motion)
    val on = state.dragFraction ?: animated
    val thumb by animateDpAsState(
        when {
            state.interaction.pressed || state.interaction.dragged -> style.thumbSizePressed
            state.checked -> style.thumbSizeOn
            else -> style.thumbSizeOff
        },
        tween(theme.motion.durationShort),
    )
    val center = style.trackHeight / 2 + (style.trackWidth - style.trackHeight) * on
    val off = 1f - on
    Box(
        Modifier
            .size(style.trackWidth, style.trackHeight)
            .background(lerp(colors.trackOff, colors.trackOn, on), CircleShape)
            .border(Hairline, colors.trackOffBorder.copy(alpha = colors.trackOffBorder.alpha * off), CircleShape)
            .indication(state.interactionSource, rememberSkinIndication(colors.trackOn, CircleShape)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = center - thumb / 2)
                .size(thumb)
                .shadow(2.dp, CircleShape)
                .background(lerp(colors.thumbOff, colors.thumbOn, on), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val icons = style.thumbIcons
            val icon = if (state.checked) icons?.checked else icons?.unchecked
            if (style.showThumbIcons && icon != null) {
                val tint = if (state.checked) colors.trackOn else colors.trackOff
                CamoIcon(icon, null, size = thumb * 2 / 3, tint = tint)
            }
        }
    }
}

// The segment's check icon (when asked), icon and one-line label.
@Composable
private fun SegmentContent(slots: SegmentSlots, check: Boolean, color: Color, style: SegmentedControlStyle) {
    ProvideContentStyle(color, IconStyle(style.iconSize, color)) {
        if (slots.content != null) {
            slots.content!!.invoke()
            return@ProvideContentStyle
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (check) Canvas(Modifier.size(style.iconSize)) { drawCheck(color, size.width) }
            slots.icon?.invoke()
            slots.label?.let {
                BasicText(
                    it,
                    style = style.textStyle.copy(color = color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// The track across the width (inset by half a thumb at each end, where core maps touches), the stops, the
// thumb at the value and, while dragging, the value label above it.
@Composable
private fun SliderLayout(
    state: SliderState,
    theme: CamoTheme,
    style: SliderStyle,
    modifier: Modifier,
    thumb: @Composable () -> Unit,
) {
    val colors = style.colors
    val height = maxOf(style.thumbSize.height, style.trackHeight)
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.fillMaxWidth().height(style.trackHeight)) {
            val inset = style.thumbSize.width.toPx() / 2
            val rtl = layoutDirection == LayoutDirection.Rtl
            val travel = size.width - 2 * inset
            fun x(f: Float) = if (rtl) size.width - inset - f * travel else inset + f * travel
            val y = size.height / 2
            val line = Stroke(size.height, cap = StrokeCap.Round)
            drawLine(colors.inactiveTrack, Offset(x(0f), y), Offset(x(1f), y), line.width, StrokeCap.Round)
            drawLine(colors.activeTrack, Offset(x(0f), y), Offset(x(state.fraction), y), line.width, StrokeCap.Round)
            if (style.showStopMarks && state.steps > 0) {
                for (i in 0..state.steps + 1) {
                    val f = i / (state.steps + 1f)
                    drawCircle(colors.stopMark, radius = size.height / 4, center = Offset(x(f), y))
                }
            }
        }
        Box(Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints())
            val travel = constraints.maxWidth - placeable.width
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative((travel * state.fraction).toInt(), 0)
            }
        }) { thumb() }
        if (style.showValueLabel && state.interaction.dragged && state.valueText != null) {
            BasicText(
                state.valueText!!,
                Modifier
                    .layout { measurable, constraints ->
                        val label = measurable.measure(Constraints())
                        val thumbWidth = style.thumbSize.width.roundToPx()
                        val travel = constraints.maxWidth - thumbWidth
                        val x = (travel * state.fraction).toInt() + thumbWidth / 2 - label.width / 2
                        val above = label.height + theme.spacing.s.roundToPx() + height.roundToPx() / 2
                        layout(0, 0) { label.placeRelative(x, -above) }
                    }
                    .background(colors.valueLabel, theme.shapes.small)
                    .padding(horizontal = theme.spacing.s, vertical = theme.spacing.xxs),
                style = style.valueLabelStyle.copy(color = colors.valueLabelText),
            )
        }
    }
}

internal val Minus: ImageVector = ImageVector.Builder("minus", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(5f, 12f); lineTo(19f, 12f)
    }
    .build()

internal val Plus: ImageVector = ImageVector.Builder("plus", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(5f, 12f); lineTo(19f, 12f); moveTo(12f, 5f); lineTo(12f, 19f)
    }
    .build()
