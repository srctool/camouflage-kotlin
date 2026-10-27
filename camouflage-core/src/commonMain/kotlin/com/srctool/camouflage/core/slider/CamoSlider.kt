package com.srctool.camouflage.core.slider

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget
import kotlin.math.roundToInt

/** Every color a slider uses. */
@Immutable
public data class SliderColors(
    val activeTrack: Color,
    val inactiveTrack: Color,
    val thumb: Color,
    val stopMark: Color,
    val valueLabel: Color,
    val valueLabelText: Color,
)

/** Overrides for sliders. `null` keeps the skin's default. */
@Immutable
public data class SliderTheme(
    val trackHeight: Dp? = null,
    val thumbSize: DpSize? = null,
    val showStopMarks: Boolean? = null,
    val showValueLabel: Boolean? = null,
    val valueLabelStyle: TextStyle? = null,
    val colors: SliderColors? = null,
)

/** The resolved slider style: every field set. */
@Immutable
public data class SliderStyle(
    val trackHeight: Dp,
    val thumbSize: DpSize,
    val showStopMarks: Boolean,
    val showValueLabel: Boolean,
    val valueLabelStyle: TextStyle,
    val colors: SliderColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SliderTheme?): SliderStyle = if (overrides == null) {
        this
    } else {
        SliderStyle(
            trackHeight = overrides.trackHeight ?: trackHeight,
            thumbSize = overrides.thumbSize ?: thumbSize,
            showStopMarks = overrides.showStopMarks ?: showStopMarks,
            showValueLabel = overrides.showValueLabel ?: showValueLabel,
            valueLabelStyle = overrides.valueLabelStyle ?: valueLabelStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a slider renderer draws. [fraction] is the value's place in the range (0 to 1, from the start in the
 * reading direction); [steps] stops lie between the ends. The renderer fills the width it's given, and its
 * track runs from half a thumb width in from each end, which is where core maps touches. [valueText] labels the
 * thumb while `interaction.dragged`.
 */
@Immutable
public data class SliderState(
    val fraction: Float,
    val steps: Int,
    val enabled: Boolean,
    val valueText: String?,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** Draws sliders. Apply `modifier` (the component's behavior) to the outermost node. */
@Stable
public interface SliderRenderer {
    /** The complete slider style for [theme]. */
    public fun defaults(theme: CamoTheme): SliderStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: SliderState, theme: CamoTheme, style: SliderStyle, modifier: Modifier)
}

private const val KEY_STEP_CONTINUOUS = 0.01f
private const val PAGE_STEP = 0.1f

/**
 * A value from a continuous or stepped [range]: volume, a price filter. A tap jumps to the touched value and a
 * drag follows the finger; [steps] stops lie between the ends (0 = continuous). [onValueChangeFinished] fires
 * once per gesture or key press, so an app can save once. [label] names it for screen readers ("Volume"), and
 * [valueLabel] gives the value's text, shown on the thumb while dragging. The parent sets the width.
 */
@Composable
public fun CamoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueLabel: ((Float) -> String)? = null,
    label: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = requireSkin("CamoSlider")
    val theme = requireTheme("CamoSlider")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.slider.defaults(theme).merge(theme.components.slider) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value.coerceIn(range) - range.start) / span).coerceIn(0f, 1f)
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val toValue = { f: Float -> range.start + snap(f.coerceIn(0f, 1f), steps) * span }
    val keyStep = if (steps > 0) 1f / (steps + 1) else KEY_STEP_CONTINUOUS
    val gestures = sliderGestures(source, enabled, rtl, style.thumbSize.width) { f, last ->
        change(toValue(f))
        if (last) finished?.invoke()
    }
    val keys = Modifier.onKeyEvent { event ->
        val pressed = enabled && event.type == KeyEventType.KeyDown
        val target = if (pressed) keyTarget(event.key, fraction, keyStep, rtl) else null
        target?.let {
            change(toValue(it))
            finished?.invoke()
        }
        target != null
    }
    val behavior = modifier
        .semantics {
            if (label != null) contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(range), range, steps)
            valueLabel?.let { stateDescription = it(value) }
            if (enabled) {
                setProgress { target ->
                    change(toValue((target - range.start) / span))
                    finished?.invoke()
                    true
                }
            } else {
                disabled()
            }
        }
        .then(keys)
        .focusable(enabled, source)
        .then(gestures)
        .minimumTouchTarget()
    skin.slider.Render(
        SliderState(fraction, steps, enabled, valueLabel?.invoke(value), interaction, source),
        theme,
        style,
        behavior,
    )
}

// The fraction a key moves to: arrows one step (Left and Right in reading order), Page ten percent, Home/End.
private fun keyTarget(key: Key, fraction: Float, step: Float, rtl: Boolean): Float? {
    val forward = if (rtl) -1 else 1
    val page = maxOf(PAGE_STEP, step)
    return when (key) {
        Key.DirectionRight -> fraction + step * forward
        Key.DirectionLeft -> fraction - step * forward
        Key.DirectionUp -> fraction + step
        Key.DirectionDown -> fraction - step
        Key.PageUp -> fraction + page
        Key.PageDown -> fraction - page
        Key.MoveHome -> 0f
        Key.MoveEnd -> 1f
        else -> null
    }
}

// A press jumps to the touched fraction and a drag follows it; `onFraction(f, last)` gets `last = true` on release.
// The track runs from half a thumb in from each end. Press and drag interactions go to `source`.
private fun sliderGestures(
    source: MutableInteractionSource,
    enabled: Boolean,
    rtl: Boolean,
    thumbWidth: Dp,
    onFraction: (Float, Boolean) -> Unit,
): Modifier = if (!enabled) {
    Modifier
} else {
    Modifier.pointerInput(rtl, thumbWidth) {
        val inset = thumbWidth.toPx() / 2
        fun fractionAt(x: Float): Float {
            val f = (x - inset) / (size.width - inset * 2).coerceAtLeast(1f)
            return if (rtl) 1f - f else f
        }
        awaitEachGesture {
            val down = awaitFirstDown()
            val press = PressInteraction.Press(down.position)
            val start = DragInteraction.Start()
            source.tryEmit(press)
            var at = fractionAt(down.position.x)
            onFraction(at, false)
            var dragged = false
            horizontalDrag(down.id) { move ->
                if (!dragged) {
                    dragged = true
                    source.tryEmit(start)
                }
                move.consume()
                at = fractionAt(move.position.x)
                onFraction(at, false)
            }
            if (dragged) source.tryEmit(DragInteraction.Stop(start))
            source.tryEmit(PressInteraction.Release(press))
            onFraction(at, true)
        }
    }
}

// Snaps a fraction to the nearest of steps + 2 stops (both ends included); continuous when steps is 0.
internal fun snap(fraction: Float, steps: Int): Float =
    if (steps <= 0) fraction else (fraction * (steps + 1)).roundToInt() / (steps + 1f)
