package com.srctool.camouflage.core.stepper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.CamoLocaleData
import com.srctool.camouflage.core.util.camoError
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.currentLocaleTag
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How a stepper is laid out: − value + in a row, or a value field with ▲ ▼ at the end. */
public enum class StepperLayout { Inline, Split }

/** Every color a stepper uses. */
@Immutable
public data class StepperColors(val button: Color, val value: Color, val field: Color, val error: Color)

/** Overrides for steppers. `null` keeps the skin's default. */
@Immutable
public data class StepperTheme(
    val layout: StepperLayout? = null,
    val buttonAppearance: Appearance? = null,
    val height: Dp? = null,
    val valueMinWidth: Dp? = null,
    val valueStyle: TextStyle? = null,
    val decreaseIcon: IconSource? = null,
    val increaseIcon: IconSource? = null,
    val colors: StepperColors? = null,
)

/** The resolved stepper style: every field set. */
@Immutable
public data class StepperStyle(
    val layout: StepperLayout,
    val buttonAppearance: Appearance,
    val height: Dp,
    val valueMinWidth: Dp,
    val valueStyle: TextStyle,
    val decreaseIcon: IconSource,
    val increaseIcon: IconSource,
    val colors: StepperColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: StepperTheme?): StepperStyle = if (overrides == null) {
        this
    } else {
        StepperStyle(
            layout = overrides.layout ?: layout,
            buttonAppearance = overrides.buttonAppearance ?: buttonAppearance,
            height = overrides.height ?: height,
            valueMinWidth = overrides.valueMinWidth ?: valueMinWidth,
            valueStyle = overrides.valueStyle ?: valueStyle,
            decreaseIcon = overrides.decreaseIcon ?: decreaseIcon,
            increaseIcon = overrides.increaseIcon ?: increaseIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * One of a stepper's buttons. The renderer draws it and applies [behavior] (the click, hold-to-repeat and the
 * label) to its outermost node. It sizes the button itself: an `Inline` button adds `minimumTouchTarget()`, while
 * a `Split` layout's ▲ ▼ are half the field's height, for pointers.
 */
@Immutable
public data class StepperButton(
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
    val behavior: Modifier,
)

/** What a stepper renderer draws: the value field and the two buttons, laid out by `style.layout`. */
@Immutable
public data class StepperState(
    val valueText: String,
    val enabled: Boolean,
    val error: Boolean,
    val focused: Boolean,
    val decrease: StepperButton,
    val increase: StepperButton,
)

/** The value: core's editable field (or plain text without typing), already styled from `style.valueStyle`. */
@Stable
public class StepperSlots(public val input: @Composable () -> Unit)

/** Draws steppers. Apply `modifier` (the whole control's behavior) to the outermost node. */
@Stable
public interface StepperRenderer {
    /** The complete stepper style for [theme]. */
    public fun defaults(theme: CamoTheme): StepperStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(
        state: StepperState,
        slots: StepperSlots,
        theme: CamoTheme,
        style: StepperStyle,
        modifier: Modifier,
    )
}

/**
 * A whole-number quantity with − and + buttons: cart lines, guests. The buttons disable at the ends of [range],
 * and holding one repeats, faster over time. With [allowTyping] the value is a number field, clamped to [range]
 * and snapped to [step] when focus leaves. Screen readers get three stops: "Decrease [label]", the value, and
 * "Increase [label]"; each change is announced. [valueText] formats the value ("3 nights").
 */
@Composable
public fun CamoStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 0..Int.MAX_VALUE,
    step: Int = 1,
    label: String? = null,
    allowTyping: Boolean = true,
    valueText: ((Int) -> String)? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    val decimalRange = CamoDecimal.of(range.first.toLong())..CamoDecimal.of(range.last.toLong())
    StepperImpl(
        value = CamoDecimal.of(value.toLong()),
        onValueChange = { onValueChange(it.unscaled.toInt()) },
        modifier = modifier,
        range = decimalRange,
        step = CamoDecimal.of(step.toLong()),
        decimals = 0,
        label = label,
        allowTyping = allowTyping,
        valueText = valueText?.let { format -> { d: CamoDecimal -> format(d.unscaled.toInt()) } },
        error = error,
        enabled = enabled,
    )
}

/**
 * A decimal quantity (weights, doses), exact in fixed point: 0.1 + 0.1 + 0.1 is 0.3. [decimals] digits are
 * shown and typed, with the locale's separator; typing accepts both `.` and `,`.
 */
@Composable
public fun CamoStepper(
    value: CamoDecimal,
    onValueChange: (CamoDecimal) -> Unit,
    range: ClosedRange<CamoDecimal>,
    step: CamoDecimal,
    decimals: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    allowTyping: Boolean = true,
    valueText: ((CamoDecimal) -> String)? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    StepperImpl(value, onValueChange, modifier, range, step, decimals, label, allowTyping, valueText, error, enabled)
}

private val CursorAllowance = 2.dp
private const val REPEAT_AFTER = 400L
private const val REPEAT_FASTEST = 100L
private const val REPEAT_START = 250L
private const val PAGE_STEPS = 10L
private const val REPEAT_SPEEDUP_PERCENT = 75
private const val PERCENT = 100
private const val NUMBER_SYMBOLS = ".,-"

@Suppress("LongMethod", "CyclomaticComplexMethod") // the stepper's three stops share its state
@Composable
private fun StepperImpl(
    value: CamoDecimal,
    onValueChange: (CamoDecimal) -> Unit,
    modifier: Modifier,
    range: ClosedRange<CamoDecimal>,
    step: CamoDecimal,
    decimals: Int,
    label: String?,
    allowTyping: Boolean,
    valueText: ((CamoDecimal) -> String)?,
    error: String?,
    enabled: Boolean,
) {
    val skin = requireSkin("CamoStepper")
    val theme = requireTheme("CamoStepper")
    val style = remember(skin, theme) { skin.stepper.defaults(theme).merge(theme.components.stepper) }
    val separator = remember { CamoLocaleData.decimalSeparator(currentLocaleTag()) }
    val latest by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val canDecrease = enabled && value > range.start
    val canIncrease = enabled && value < range.endInclusive
    val shown = valueText?.invoke(value) ?: value.withScale(decimals).format(separator)

    fun stepBy(steps: Long): Boolean {
        val next = (latest + step * steps).coerceIn(range.start, range.endInclusive)
        if (next.compareTo(latest) == 0) return false
        change(next)
        return true
    }

    val decrease = stepperButton(canDecrease, "Decrease${label?.let { " $it" } ?: ""}") { stepBy(-1) }
    val increase = stepperButton(canIncrease, "Increase${label?.let { " $it" } ?: ""}") { stepBy(1) }

    var focused by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<String?>(null) }
    val keys = Modifier.onPreviewKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> stepBy(1)
            Key.DirectionDown -> stepBy(-1)
            Key.PageUp -> stepBy(PAGE_STEPS)
            Key.PageDown -> stepBy(-PAGE_STEPS)
            Key.MoveHome -> change(range.start).let { true }
            Key.MoveEnd -> change(range.endInclusive).let { true }
            else -> return@onPreviewKeyEvent false
        }
        draft = null
        true
    }
    val valueSemantics = Modifier.semantics {
        if (label != null) contentDescription = label
        liveRegion = LiveRegionMode.Polite
    }
    val textStyle = style.valueStyle.copy(
        color = style.colors.value,
        textAlign = if (style.layout == StepperLayout.Inline) TextAlign.Center else TextAlign.Start,
    )
    val text = draft ?: value.withScale(decimals).format(separator)
    // BasicTextField is ten characters wide by default; the value is as wide as its text, at least valueMinWidth.
    val measurer = rememberTextMeasurer()
    val measured = with(LocalDensity.current) { measurer.measure(text, textStyle).size.width.toDp() }
    val fieldWidth = Modifier.width(maxOf(measured + CursorAllowance, style.valueMinWidth))
    val input: @Composable () -> Unit = if (allowTyping) {
        {
            BasicTextField(
                value = text,
                onValueChange = { text ->
                    if (text.all { it.isDigit() || it in NUMBER_SYMBOLS }) draft = text
                },
                modifier = fieldWidth.then(valueSemantics).then(keys).onFocusChanged { state ->
                    if (focused && !state.isFocused) {
                        draft?.let { commit(it, latest, range, step, decimals, change) }
                        draft = null
                    }
                    focused = state.isFocused
                },
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(style.colors.value),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (decimals == 0) KeyboardType.Number else KeyboardType.Decimal,
                ),
            )
        }
    } else {
        {
            BasicText(
                shown,
                valueSemantics.then(keys).onFocusChanged { focused = it.isFocused }.focusable(enabled),
                style = textStyle,
            )
        }
    }
    skin.stepper.Render(
        StepperState(shown, enabled, error != null, focused, decrease, increase),
        StepperSlots(input),
        theme,
        style,
        modifier.camoError(error),
    )
}

// A typed value, clamped and snapped; an empty or unreadable draft restores the last valid value.
private fun commit(
    text: String,
    current: CamoDecimal,
    range: ClosedRange<CamoDecimal>,
    step: CamoDecimal,
    decimals: Int,
    onValueChange: (CamoDecimal) -> Unit,
) {
    val typed = CamoDecimal.parseOrNull(text) ?: return
    val clamped = typed.coerceIn(range.start, range.endInclusive)
    val snapped = snapToStep(clamped, range.start, step).coerceIn(range.start, range.endInclusive).withScale(decimals)
    if (snapped.compareTo(current) != 0) onValueChange(snapped)
}

// The nearest value start + n × step.
internal fun snapToStep(value: CamoDecimal, start: CamoDecimal, step: CamoDecimal): CamoDecimal {
    val s = maxOf(value.scale, start.scale, step.scale)
    val unit = step.rescaled(s).takeIf { it > 0 } ?: return value
    val offset = (value - start).rescaled(s)
    val half = unit / 2
    val n = if (offset >= 0) (offset + half) / unit else (offset - half) / unit
    return start + step * n
}

// A button's behavior: a click steps once; holding steps again after 400 ms, then faster down to 10 per second.
@Composable
private fun stepperButton(enabled: Boolean, label: String, onStep: () -> Boolean): StepperButton {
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val step by rememberUpdatedState(onStep)
    var repeated by remember { mutableStateOf(false) }
    LaunchedEffect(source, enabled) {
        var job: Job? = null
        source.interactions.collect { event ->
            when (event) {
                is PressInteraction.Press -> if (enabled) {
                    repeated = false
                    job = launch {
                        delay(REPEAT_AFTER)
                        var pause = REPEAT_START
                        while (step()) {
                            repeated = true
                            delay(pause)
                            pause = maxOf(REPEAT_FASTEST, pause * REPEAT_SPEEDUP_PERCENT / PERCENT)
                        }
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> job?.cancel()
            }
        }
    }
    val behavior = Modifier
        .semantics { contentDescription = label }
        .clickable(source, indication = null, enabled = enabled, role = Role.Button) {
            if (repeated) repeated = false else step()
        }
    return StepperButton(enabled, interaction, source, behavior)
}
