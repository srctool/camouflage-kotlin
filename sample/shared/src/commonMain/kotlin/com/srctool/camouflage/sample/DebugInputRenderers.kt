package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.checkbox.CheckboxColors
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.checkbox.CheckboxRenderer
import com.srctool.camouflage.core.checkbox.CheckboxState
import com.srctool.camouflage.core.checkbox.CheckboxStyle
import com.srctool.camouflage.core.icon.IconSource
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
import com.srctool.camouflage.sample.resources.Res
import com.srctool.camouflage.sample.resources.ic_star

// DebugSkin's M5 inputs: squares and text, no animation, a focus border. They prove the contract, not a look.

private const val DISABLED = 0.38f

@Composable
private fun DebugLabel(label: List<CamoSpan>?, description: String?) = label?.let {
    @Composable {
        Column {
            CamoText(it)
            description?.let { d -> CamoText(d, color = Camouflage.colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun DebugError(error: String?) = error?.let {
    @Composable { CamoText(it, Modifier.clearAndSetSemantics {}, color = Camouflage.colors.error) }
}

private fun Modifier.debugFocus(focused: Boolean) = border(if (focused) 2.dp else 0.dp, DebugFocus)

object DebugCheckboxRenderer : CheckboxRenderer {
    override fun defaults(theme: CamoTheme) = CheckboxStyle(
        18.dp, theme.shapes.none, 2.dp, theme.typography.bodyLarge, LabelPosition.End, theme.spacing.m,
        theme.typography.bodySmall,
        CheckboxColors(theme.colors.primary, theme.colors.onPrimary, theme.colors.outline, theme.colors.onSurface),
    )

    @Composable
    override fun Render(state: CheckboxState, theme: CamoTheme, style: CheckboxStyle, modifier: Modifier) {
        val box = Modifier
            .size(style.boxSize)
            .background(if (state.checked) style.colors.checkedBox else Color.Transparent)
            .border(style.borderWidth, if (state.error != null) theme.colors.error else style.colors.uncheckedBorder)
            .debugFocus(state.interaction.focused)
        LabelledControl(
            modifier.alphaIf(state.enabled), style.labelPosition, style.labelGap, centerControl = true, 0.dp,
            control = { Box(box) }, label = DebugLabel(state.label, state.description), below = DebugError(state.error),
        )
    }
}

object DebugSwitchRenderer : SwitchRenderer {
    override fun defaults(theme: CamoTheme) = SwitchStyle(
        false, null, 44.dp, 24.dp, 20.dp, 20.dp, 20.dp, theme.typography.bodyLarge, LabelPosition.Start,
        theme.spacing.l, theme.typography.bodySmall,
        SwitchColors(theme.colors.primary, theme.colors.surfaceContainerHighest, theme.colors.onPrimary,
            theme.colors.outline, theme.colors.outline, theme.colors.onSurface),
    )

    @Composable
    override fun Render(state: SwitchState, theme: CamoTheme, style: SwitchStyle, modifier: Modifier) {
        val c = style.colors
        val on = state.dragFraction?.let { it > HALF } ?: state.checked
        val track = Modifier
            .size(style.trackWidth, style.trackHeight)
            .background(if (on) c.trackOn else c.trackOff)
            .border(1.dp, c.trackOffBorder)
            .debugFocus(state.interaction.focused)
        LabelledControl(
            modifier.alphaIf(state.enabled), style.labelPosition, style.labelGap, centerControl = true, 0.dp,
            control = {
                Box(track, contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
                    Box(Modifier.padding(2.dp).size(style.thumbSizeOn).background(if (on) c.thumbOn else c.thumbOff))
                }
            },
            label = DebugLabel(state.label, state.description),
            below = DebugError(state.error),
        )
    }
}

object DebugRadioRenderer : RadioRenderer {
    override fun defaults(theme: CamoTheme) = RadioStyle(
        18.dp, 8.dp, 2.dp, theme.typography.bodyLarge, theme.typography.bodySmall, LabelPosition.End, theme.spacing.m,
        RadioColors(theme.colors.primary, theme.colors.outline, theme.colors.error, theme.colors.onSurface),
    )

    @Composable
    override fun Render(state: RadioState, theme: CamoTheme, style: RadioStyle, modifier: Modifier) {
        val ring = if (state.error) style.colors.error else style.colors.unselected
        LabelledControl(
            modifier.alphaIf(state.enabled), style.labelPosition, style.labelGap, centerControl = true, 0.dp,
            control = {
                Box(
                    Modifier.size(style.size).border(style.borderWidth, ring).debugFocus(state.interaction.focused),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.selected) Box(Modifier.size(style.dotSize).background(style.colors.selected))
                }
            },
            label = DebugLabel(state.label, state.description),
        )
    }
}

object DebugSegmentedControlRenderer : SegmentedControlRenderer {
    override fun defaults(theme: CamoTheme) = SegmentedControlStyle(
        40.dp, theme.shapes.none, 1.dp, theme.typography.labelLarge, 18.dp, false,
        SegmentedColors(Color.Transparent, theme.colors.outline, theme.colors.primary, theme.colors.onPrimary,
            theme.colors.onSurface),
    )

    @Composable
    override fun Container(
        state: SegmentedContainerState,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) = Row(modifier.alphaIf(state.enabled).heightIn(min = style.height), content = content)

    @Composable
    override fun Segment(
        state: SegmentState,
        slots: SegmentSlots,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
    ) {
        val c = style.colors
        val content = if (state.selected) c.selectedContent else c.content
        Box(
            modifier
                .fillMaxHeight()
                .background(if (state.selected) c.selectedContainer else c.container)
                .border(style.borderWidth, c.border)
                .debugFocus(state.interaction.focused)
                .padding(horizontal = theme.spacing.s),
            contentAlignment = Alignment.Center,
        ) {
            slots.content?.invoke() ?: slots.label?.let {
                BasicText(it, style = style.textStyle.copy(color = content), maxLines = 1)
            } ?: slots.icon?.invoke()
        }
    }
}

object DebugSliderRenderer : SliderRenderer {
    override fun defaults(theme: CamoTheme) = SliderStyle(
        2.dp, DpSize(16.dp, 16.dp), true, true, theme.typography.labelMedium,
        SliderColors(theme.colors.primary, theme.colors.outline, theme.colors.primary, theme.colors.onSurface,
            theme.colors.inverseSurface, theme.colors.inverseOnSurface),
    )

    @Composable
    override fun Render(state: SliderState, theme: CamoTheme, style: SliderStyle, modifier: Modifier) {
        Box(
            modifier.alphaIf(state.enabled).fillMaxWidth().height(style.thumbSize.height),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(style.trackHeight).background(style.colors.inactiveTrack))
            Box(
                Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(Constraints())
                    layout(constraints.maxWidth, p.height) {
                        p.placeRelative(((constraints.maxWidth - p.width) * state.fraction).toInt(), 0)
                    }
                },
            ) {
                Box(Modifier.size(style.thumbSize).background(style.colors.thumb).debugFocus(state.interaction.focused))
            }
        }
    }
}

object DebugStepperRenderer : StepperRenderer {
    override fun defaults(theme: CamoTheme) = StepperStyle(
        StepperLayout.Inline, Appearance.Outline, 40.dp, 48.dp, theme.typography.titleMedium,
        IconSource.Resource(Res.drawable.ic_star), IconSource.Resource(Res.drawable.ic_star),
        StepperColors(theme.colors.primary, theme.colors.onSurface, theme.colors.outline, theme.colors.error),
    )

    @Composable
    override fun Render(
        state: StepperState,
        slots: StepperSlots,
        theme: CamoTheme,
        style: StepperStyle,
        modifier: Modifier,
    ) {
        val border = if (state.error) style.colors.error else style.colors.field
        Row(modifier.alphaIf(state.enabled), verticalAlignment = Alignment.CenterVertically) {
            Box(
                state.decrease.behavior.size(style.height).border(1.dp, border)
                    .debugFocus(state.decrease.interaction.focused),
                contentAlignment = Alignment.Center,
            ) { BasicText("−", style = style.valueStyle.copy(color = style.colors.button)) }
            Box(
                Modifier.widthIn(min = style.valueMinWidth).debugFocus(state.focused).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { slots.input() }
            Box(
                state.increase.behavior.size(style.height).border(1.dp, border)
                    .debugFocus(state.increase.interaction.focused),
                contentAlignment = Alignment.Center,
            ) { BasicText("+", style = style.valueStyle.copy(color = style.colors.button)) }
        }
    }
}

private const val HALF = 0.5f

private fun Modifier.alphaIf(enabled: Boolean) = alpha(if (enabled) 1f else DISABLED)
