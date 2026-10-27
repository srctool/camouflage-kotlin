package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.checkbox.CheckboxColors
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

// The M5 inputs in the plain test skin: boxes and text, laid out from the style.

private val Square = IconSource.Vector(
    ImageVector.Builder("square", 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black)) { moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); close() }
        .build(),
)

@Composable
private fun LabelRow(
    modifier: Modifier,
    control: @Composable () -> Unit,
    label: List<CamoSpan>?,
    description: String?,
    error: String?,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            control()
            if (label != null) {
                Column {
                    CamoText(label)
                    description?.let { CamoText(it) }
                }
            }
        }
        error?.let { CamoText(it, Modifier.clearAndSetSemantics {}) }
    }
}

internal object PlainCheckbox : CheckboxRenderer {
    override fun defaults(theme: CamoTheme) = CheckboxStyle(
        18.dp, RectangleShape, 2.dp, theme.typography.bodyLarge, LabelPosition.End, 12.dp, theme.typography.bodySmall,
        CheckboxColors(theme.colors.primary, theme.colors.onPrimary, theme.colors.outline, theme.colors.onSurface),
    )

    @Composable
    override fun Render(state: CheckboxState, theme: CamoTheme, style: CheckboxStyle, modifier: Modifier) =
        LabelRow(modifier, {
            Box(Modifier.size(style.boxSize).background(if (state.checked) style.colors.checkedBox else Color.White))
        }, state.label, state.description, state.error)
}

internal object PlainSwitch : SwitchRenderer {
    override fun defaults(theme: CamoTheme) = SwitchStyle(
        false, null, 52.dp, 32.dp, 16.dp, 24.dp, 28.dp, theme.typography.bodyLarge, LabelPosition.Start, 16.dp,
        theme.typography.bodySmall,
        SwitchColors(theme.colors.primary, theme.colors.surface, theme.colors.onPrimary, theme.colors.outline,
            theme.colors.outline, theme.colors.onSurface),
    )

    @Composable
    override fun Render(state: SwitchState, theme: CamoTheme, style: SwitchStyle, modifier: Modifier) =
        LabelRow(modifier, {
            Box(Modifier.size(style.trackWidth, style.trackHeight).background(style.colors.trackOff))
        }, state.label, state.description, state.error)
}

internal object PlainRadio : RadioRenderer {
    override fun defaults(theme: CamoTheme) = RadioStyle(
        20.dp, 10.dp, 2.dp, theme.typography.bodyLarge, theme.typography.bodyMedium, LabelPosition.End, 12.dp,
        RadioColors(theme.colors.primary, theme.colors.outline, theme.colors.error, theme.colors.onSurface),
    )

    @Composable
    override fun Render(state: RadioState, theme: CamoTheme, style: RadioStyle, modifier: Modifier) =
        LabelRow(modifier, { Box(Modifier.size(style.size).background(style.colors.unselected)) }, state.label,
            state.description, null)
}

internal object PlainSegmented : SegmentedControlRenderer {
    override fun defaults(theme: CamoTheme) = SegmentedControlStyle(
        40.dp, RectangleShape, 1.dp, theme.typography.labelLarge, 18.dp, false,
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
    ) = Row(modifier.heightIn(min = style.height), content = content)

    @Composable
    override fun Segment(
        state: SegmentState,
        slots: SegmentSlots,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
    ) {
        Box(modifier.border(style.borderWidth, style.colors.border), Alignment.Center) {
            slots.content?.invoke() ?: slots.label?.let { BasicText(it, maxLines = 1) } ?: slots.icon?.invoke()
        }
    }
}

internal object PlainSlider : SliderRenderer {
    override fun defaults(theme: CamoTheme) = SliderStyle(
        4.dp, DpSize(20.dp, 20.dp), false, false, theme.typography.labelLarge,
        SliderColors(theme.colors.primary, theme.colors.outline, theme.colors.primary, theme.colors.onPrimary,
            theme.colors.inverseSurface, theme.colors.inverseOnSurface),
    )

    @Composable
    override fun Render(state: SliderState, theme: CamoTheme, style: SliderStyle, modifier: Modifier) =
        Box(modifier.fillMaxWidth().height(style.thumbSize.height).background(style.colors.inactiveTrack))
}

internal object PlainStepper : StepperRenderer {
    override fun defaults(theme: CamoTheme) = StepperStyle(
        StepperLayout.Inline, Appearance.Outline, 40.dp, 40.dp, theme.typography.titleMedium, Square, Square,
        StepperColors(theme.colors.outline, theme.colors.onSurface, theme.colors.outline, theme.colors.error),
    )

    @Composable
    override fun Render(
        state: StepperState,
        slots: StepperSlots,
        theme: CamoTheme,
        style: StepperStyle,
        modifier: Modifier,
    ) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Box(state.decrease.behavior.size(style.height).background(style.colors.button))
            Box(Modifier.widthIn(min = style.valueMinWidth)) { slots.input() }
            Box(state.increase.behavior.size(style.height).background(style.colors.button))
        }
    }
}
