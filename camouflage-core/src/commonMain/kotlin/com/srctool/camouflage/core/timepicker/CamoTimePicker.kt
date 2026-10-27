package com.srctool.camouflage.core.timepicker

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.datepicker.PickerFieldFrame
import com.srctool.camouflage.core.datepicker.PickerPresentation
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.overlay.PickerOverlay
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.segmented.CamoSegmentedControl
import com.srctool.camouflage.core.segmented.Segment
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.CamoLocaleData
import com.srctool.camouflage.core.util.WheelColumn
import com.srctool.camouflage.core.util.currentLocaleTag
import com.srctool.camouflage.core.util.rememberSystem24Hour
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.datetime.LocalTime

/** Every color a time picker uses. */
@Immutable
public data class TimePickerColors(
    val active: Color,
    val activeContainer: Color,
    val face: Color,
    val digits: Color,
    val period: Color,
)

/** The time picker's words, for the app to translate through its theme. */
@Immutable
public data class TimePickerStrings(
    val chooseTime: String = "Choose time",
    val hour: String = "Hour",
    val minute: String = "Minute",
    val period: String = "AM or PM",
    val cancel: String = "Cancel",
    val ok: String = "OK",
)

/** Overrides for time pickers and time fields. `null` keeps the skin's default. */
@Immutable
public data class TimePickerTheme(
    val form: TimePickerForm? = null,
    val confirmButton: Boolean? = null,
    val digitStyle: TextStyle? = null,
    val dialTextStyle: TextStyle? = null,
    val wheelTextStyle: TextStyle? = null,
    val wheelItemHeight: Dp? = null,
    val fieldSize: Dp? = null,
    val dialSize: Dp? = null,
    val autoPopupMinWidth: Dp? = null,
    val contentPadding: PaddingValues? = null,
    val clockIcon: IconSource? = null,
    val colors: TimePickerColors? = null,
    val strings: TimePickerStrings? = null,
)

/** The resolved time picker style: every field set. */
@Immutable
public data class TimePickerStyle(
    val form: TimePickerForm,
    val confirmButton: Boolean,
    val digitStyle: TextStyle,
    val dialTextStyle: TextStyle,
    val wheelTextStyle: TextStyle,
    val wheelItemHeight: Dp,
    val fieldSize: Dp,
    val dialSize: Dp,
    val autoPopupMinWidth: Dp,
    val contentPadding: PaddingValues,
    val clockIcon: IconSource,
    val colors: TimePickerColors,
    val strings: TimePickerStrings,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: TimePickerTheme?): TimePickerStyle = if (overrides == null) {
        this
    } else {
        TimePickerStyle(
            form = overrides.form ?: form,
            confirmButton = overrides.confirmButton ?: confirmButton,
            digitStyle = overrides.digitStyle ?: digitStyle,
            dialTextStyle = overrides.dialTextStyle ?: dialTextStyle,
            wheelTextStyle = overrides.wheelTextStyle ?: wheelTextStyle,
            wheelItemHeight = overrides.wheelItemHeight ?: wheelItemHeight,
            fieldSize = overrides.fieldSize ?: fieldSize,
            dialSize = overrides.dialSize ?: dialSize,
            autoPopupMinWidth = overrides.autoPopupMinWidth ?: autoPopupMinWidth,
            contentPadding = overrides.contentPadding ?: contentPadding,
            clockIcon = overrides.clockIcon ?: clockIcon,
            colors = overrides.colors ?: colors,
            strings = overrides.strings ?: strings,
        )
    }
}

/** What a time picker renderer draws, for `style.form`: the hour and minute boxes, the period, a dial or wheels. */
@Immutable
public data class TimePickerState(
    val hour: Int?,
    val minute: Int?,
    val is24Hour: Boolean,
    val period: DayPeriod,
    val activePart: TimePart,
    val minuteStep: Int,
    val enabled: Boolean,
)

/**
 * The time picker's parts, built by core: the editable [hour] and [minute] (draw a box around each, active when
 * `activePart` matches), the AM/PM [period] control (12-hour only), and the [dial] or [wheels] for those forms.
 */
@Stable
public class TimePickerSlots(
    public val hour: @Composable () -> Unit,
    public val minute: @Composable () -> Unit,
    public val period: (@Composable () -> Unit)?,
    public val dial: (@Composable () -> Unit)?,
    public val wheels: List<@Composable () -> Unit>?,
)

/**
 * The dial's face: [labels] around the outer ring (12 at the top), [innerLabels] on an inner ring for 24-hour
 * hours, the hand at [handAngle] degrees clockwise from the top, and the label at [selectedIndex] (on the inner
 * ring when [selectedInner]) highlighted; -1 when the hand sits between labels (minute 7).
 */
@Immutable
public data class DialState(
    val labels: List<String>,
    val innerLabels: List<String>?,
    val selectedIndex: Int,
    val selectedInner: Boolean,
    val handAngle: Float,
    val part: TimePart,
)

/** Draws time pickers: the layout, the dial face and the wheels. Apply each `modifier` outermost. */
@Stable
public interface TimePickerRenderer {
    /** The complete time picker style for [theme]. */
    public fun defaults(theme: CamoTheme): TimePickerStyle

    /** Lays out [slots] for `style.form`. */
    @Composable
    public fun Render(
        state: TimePickerState,
        slots: TimePickerSlots,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    )

    /** The dial face, filling `style.dialSize`; core turns touches into values (inner ring below 62% of the radius). */
    @Composable
    public fun Dial(state: DialState, theme: CamoTheme, style: TimePickerStyle, modifier: Modifier)

    /** The wheels side by side, with the band behind the middle row. */
    @Composable
    public fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    )
}

/** On a 24-hour dial, a touch nearer the middle than this share of the radius picks the inner ring (12–23). */
public const val DIAL_INNER_RING: Float = 0.62f

private const val HOURS_ON_DIAL = 12
private const val MINUTES = 60
private const val MINUTE_LABEL_STEP = 5
private const val DEGREES_PER_HOUR = 30f
private const val DEGREES_PER_MINUTE = 6f

/**
 * The inline time input (ADR-30) in `TimePickerTheme.form`: two digit fields with AM/PM (`Fields`), the same
 * above a clock dial (`Dial`), or wheels (`Wheel`). [use24Hour] `null` follows the device. Minutes move by
 * [minuteStep] and snap to it when the minute field is left. Screen readers get the hour and minute as
 * adjustable fields in every form; Up and Down change them by 1 hour or [minuteStep].
 */
@Suppress("LongMethod") // the three forms' parts share the value, the active part and the hour format
@Composable
public fun CamoTimeInput(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    minuteStep: Int = 1,
    use24Hour: Boolean? = null,
    enabled: Boolean = true,
) {
    val skin = requireSkin("CamoTimeInput")
    val theme = requireTheme("CamoTimeInput")
    val style = remember(skin, theme) { skin.timePicker.defaults(theme).merge(theme.components.timePicker) }
    val is24 = use24Hour ?: rememberSystem24Hour()
    val locale = currentLocaleTag()
    var active by remember { mutableStateOf(TimePart.Hour) }
    val change by rememberUpdatedState(onValueChange)
    val time = value ?: LocalTime(0, 0)
    val period = periodOf(time.hour)
    val strings = style.strings
    val hourField: @Composable () -> Unit = {
        DigitField(
            value = value?.let { if (is24) it.hour else hour12(it.hour) },
            range = if (is24) 0..23 else 1..HOURS_ON_DIAL,
            step = 1,
            name = strings.hour,
            enabled = enabled,
            style = style,
            onFocus = { active = TimePart.Hour },
        ) { h -> change(LocalTime(if (is24) h else hour24(h, period), time.minute)) }
    }
    val minuteField: @Composable () -> Unit = {
        DigitField(
            value = value?.minute,
            range = 0 until MINUTES,
            step = minuteStep,
            name = strings.minute,
            enabled = enabled,
            style = style,
            onFocus = { active = TimePart.Minute },
        ) { m -> change(LocalTime(time.hour, m)) }
    }
    val amPm = remember(locale) { CamoLocaleData.amPm(locale) }
    val periodControl: (@Composable () -> Unit)? = if (is24) {
        null
    } else {
        {
            CamoSegmentedControl(
                options = listOf(Segment(DayPeriod.Am, amPm[0]), Segment(DayPeriod.Pm, amPm[1])),
                selected = period,
                onSelectionChange = { change(LocalTime(hour24(hour12(time.hour), it), time.minute)) },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = strings.period },
            )
        }
    }
    val dial: (@Composable () -> Unit)? = if (style.form != TimePickerForm.Dial) {
        null
    } else {
        { TimeDial(time, active, is24, minuteStep, style, theme, enabled, { active = it }) { change(it) } }
    }
    val wheels = if (style.form != TimePickerForm.Wheel) {
        null
    } else {
        timeWheels(time, is24, minuteStep, amPm, style) { change(it) }
    }
    skin.timePicker.Render(
        TimePickerState(value?.hour, value?.minute, is24, period, active, minuteStep, enabled),
        TimePickerSlots(hourField, minuteField, periodControl, dial, wheels),
        theme,
        style,
        modifier,
    )
}

// A two-digit field for the hour or the minute: typing a whole number in [range] reports it; Up and Down step
// it (wrapping); leaving it snaps to [step] and shows two digits again. Adjustable for screen readers.
@Composable
private fun DigitField(
    value: Int?,
    range: IntRange,
    step: Int,
    name: String,
    enabled: Boolean,
    style: TimePickerStyle,
    onFocus: () -> Unit,
    onChange: (Int) -> Unit,
) {
    fun shown(v: Int?) = v?.toString()?.padStart(2, '0').orEmpty()
    var text by remember { mutableStateOf(shown(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value, focused) { if (!focused) text = shown(value) }
    fun stepBy(direction: Int) {
        val size = range.last - range.first + 1
        val from = value ?: range.first
        onChange(((from - range.first + direction * step) % size + size) % size + range.first)
    }
    BasicTextField(
        value = text,
        onValueChange = { typed ->
            val digits = typed.filter(Char::isDigit).takeLast(2)
            text = digits
            digits.toIntOrNull()?.takeIf { it in range }?.let(onChange)
        },
        enabled = enabled,
        singleLine = true,
        textStyle = style.digitStyle.copy(color = style.colors.digits, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(style.colors.active),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .width(style.fieldSize)
            .onFocusChanged {
                if (it.isFocused) onFocus()
                val left = focused && !it.isFocused
                if (left && value != null) snapMinute(value, step).takeIf { s -> s != value }?.let(onChange)
                focused = it.isFocused
            }
            .stepKeys(::stepBy)
            .semantics {
                contentDescription = name
                val span = range.first.toFloat()..range.last.toFloat()
                progressBarRangeInfo = ProgressBarRangeInfo((value ?: range.first).toFloat(), span)
                setProgress { target ->
                    onChange(target.roundToInt().coerceIn(range))
                    true
                }
            },
    )
}

// Up and Down step the value (by [onStep]'s direction), and are taken from the text field's cursor keys.
private fun Modifier.stepKeys(onStep: (Int) -> Unit): Modifier = onPreviewKeyEvent {
    val direction = when (it.key) {
        Key.DirectionUp -> 1
        Key.DirectionDown -> -1
        else -> 0
    }
    if (direction != 0 && it.type == KeyEventType.KeyDown) onStep(direction)
    direction != 0
}

// The time at a dial point: the hour (inner ring 12–23 on a 24-hour dial) or the minute, snapped to [minuteStep].
private fun dialTime(
    at: Offset,
    size: Float,
    now: LocalTime,
    part: TimePart,
    is24: Boolean,
    minuteStep: Int,
): LocalTime {
    val center = size / 2
    val dx = at.x - center
    val dy = at.y - center
    val angle = ((atan2(dx, -dy) * 180 / PI).toFloat() + 360f) % 360f
    if (part == TimePart.Minute) {
        return LocalTime(now.hour, snapMinute((angle / DEGREES_PER_MINUTE).roundToInt() % MINUTES, minuteStep))
    }
    val index = (angle / DEGREES_PER_HOUR).roundToInt() % HOURS_ON_DIAL
    val hour = when {
        !is24 -> hour24(if (index == 0) HOURS_ON_DIAL else index, periodOf(now.hour))
        hypot(dx, dy) < center * DIAL_INNER_RING -> index + HOURS_ON_DIAL
        else -> index
    }
    return LocalTime(hour, now.minute)
}

// The clock dial: a tap or drag picks the hour (then moves on to the minute) or the minute. Screen readers use
// the digit fields instead, so the dial is hidden from them.
@Composable
private fun TimeDial(
    time: LocalTime,
    part: TimePart,
    is24: Boolean,
    minuteStep: Int,
    style: TimePickerStyle,
    theme: CamoTheme,
    enabled: Boolean,
    onPart: (TimePart) -> Unit,
    onChange: (LocalTime) -> Unit,
) {
    val skin = requireSkin("CamoTimeInput")
    val current by rememberUpdatedState(time)
    val currentPart by rememberUpdatedState(part)
    fun pick(at: Offset, size: Float, release: Boolean) {
        onChange(dialTime(at, size, current, currentPart, is24, minuteStep))
        if (release && currentPart == TimePart.Hour) onPart(TimePart.Minute)
    }
    val state = if (part == TimePart.Hour) {
        val index = time.hour % HOURS_ON_DIAL
        DialState(
            labels = List(HOURS_ON_DIAL) { if (it != 0) it.toString() else if (is24) "00" else "12" },
            innerLabels = if (is24) List(HOURS_ON_DIAL) { (it + HOURS_ON_DIAL).toString() } else null,
            selectedIndex = index,
            selectedInner = is24 && time.hour >= HOURS_ON_DIAL,
            handAngle = index * DEGREES_PER_HOUR,
            part = TimePart.Hour,
        )
    } else {
        DialState(
            labels = List(HOURS_ON_DIAL) { (it * MINUTE_LABEL_STEP).toString().padStart(2, '0') },
            innerLabels = null,
            selectedIndex = if (time.minute % MINUTE_LABEL_STEP == 0) time.minute / MINUTE_LABEL_STEP else -1,
            selectedInner = false,
            handAngle = time.minute * DEGREES_PER_MINUTE,
            part = TimePart.Minute,
        )
    }
    val behavior = if (!enabled) {
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier
            .clearAndSetSemantics {}
            .pointerInput(is24, minuteStep) {
                detectTapGestures { pick(it, size.width.toFloat(), release = true) }
            }
            .pointerInput(is24, minuteStep) {
                var last = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        last = it
                        pick(it, size.width.toFloat(), release = false)
                    },
                    onDragEnd = { pick(last, size.width.toFloat(), release = true) },
                ) { change, _ ->
                    last = change.position
                    pick(change.position, size.width.toFloat(), release = false)
                }
            }
    }
    Box(Modifier.size(style.dialSize)) {
        skin.timePicker.Dial(state, theme, style, behavior.size(style.dialSize))
    }
}

// Hour, minute (in steps) and, for 12 hours, period wheels.
@Composable
private fun timeWheels(
    time: LocalTime,
    is24: Boolean,
    minuteStep: Int,
    amPm: List<String>,
    style: TimePickerStyle,
    onChange: (LocalTime) -> Unit,
): List<@Composable () -> Unit> {
    val step = minuteStep.coerceAtLeast(1)
    val colors = style.colors
    val strings = style.strings
    val hour: @Composable () -> Unit = {
        if (is24) {
            WheelColumn(24, time.hour, { onChange(LocalTime(it, time.minute)) }, { it.toString().padStart(2, '0') },
                strings.hour, style.wheelItemHeight, style.wheelTextStyle, colors.digits, colors.period)
        } else {
            WheelColumn(HOURS_ON_DIAL, hour12(time.hour) - 1,
                { onChange(LocalTime(hour24(it + 1, periodOf(time.hour)), time.minute)) }, { (it + 1).toString() },
                strings.hour, style.wheelItemHeight, style.wheelTextStyle, colors.digits, colors.period)
        }
    }
    val minute: @Composable () -> Unit = {
        WheelColumn(MINUTES / step, snapMinute(time.minute, step) / step, { onChange(LocalTime(time.hour, it * step)) },
            { (it * step).toString().padStart(2, '0') }, strings.minute, style.wheelItemHeight, style.wheelTextStyle,
            colors.digits, colors.period)
    }
    val period: @Composable () -> Unit = {
        WheelColumn(2, periodOf(time.hour).ordinal,
            { onChange(LocalTime(hour24(hour12(time.hour), DayPeriod.entries[it]), time.minute)) }, { amPm[it] },
            strings.period, style.wheelItemHeight, style.wheelTextStyle, colors.digits, colors.period)
    }
    return if (is24) listOf(hour, minute) else listOf(hour, minute, period)
}

/**
 * A time field: the text-field frame showing [value] in the locale's short time (24 or 12 hours per
 * [use24Hour], `null` following the device), opening a [CamoTimeInput] in a popup on wide windows and a bottom
 * sheet otherwise. Cancel/OK confirm by default (`TimePickerTheme.confirmButton`); OK is disabled outside
 * [minTime]…[maxTime], a window that may run past midnight.
 */
@Composable
public fun CamoTimeField(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    minuteStep: Int = 1,
    use24Hour: Boolean? = null,
    minTime: LocalTime? = null,
    maxTime: LocalTime? = null,
    presentation: PickerPresentation = PickerPresentation.Auto,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val skin = requireSkin("CamoTimeField")
    val theme = requireTheme("CamoTimeField")
    val style = remember(skin, theme) { skin.timePicker.defaults(theme).merge(theme.components.timePicker) }
    val is24 = use24Hour ?: rememberSystem24Hour()
    val locale = currentLocaleTag()
    var open by remember { mutableStateOf(false) }
    val opener = remember { FocusRequester() }
    val keyboard = LocalInputModeManager.current
    val close = {
        open = false
        if (keyboard.inputMode == InputMode.Keyboard) opener.requestFocus()
    }
    Box(modifier) {
        PickerFieldFrame(
            text = value?.let { formatTime(it, is24, locale) },
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            error = error,
            icon = style.clockIcon,
            open = open,
            enabled = enabled,
            focusRequester = opener,
            onFocusChanged = onFocusChanged,
        ) { open = true }
        if (open) {
            PickerOverlay(presentation, style.autoPopupMinWidth, label, close) {
                TimePanel(value, onValueChange, minuteStep, is24, minTime, maxTime, style, theme, close)
            }
        }
    }
}

// What the popup or sheet holds: the time input on a draft, then Cancel/OK unless `confirmButton` is off, when
// each change in the window is reported at once.
@Composable
private fun TimePanel(
    value: LocalTime?,
    onValueChange: (LocalTime) -> Unit,
    minuteStep: Int,
    is24: Boolean,
    minTime: LocalTime?,
    maxTime: LocalTime?,
    style: TimePickerStyle,
    theme: CamoTheme,
    close: () -> Unit,
) {
    var draft by remember { mutableStateOf(value ?: defaultTime(minTime, minuteStep)) }
    Column(
        Modifier.padding(style.contentPadding).width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CamoTimeInput(
            value = draft,
            onValueChange = {
                draft = it
                if (!style.confirmButton && inWindow(it, minTime, maxTime)) onValueChange(it)
            },
            minuteStep = minuteStep,
            use24Hour = is24,
        )
        if (style.confirmButton) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.spacing.s, Alignment.End)) {
                CamoButton(style.strings.cancel, close, appearance = Appearance.Ghost)
                CamoButton(
                    style.strings.ok,
                    {
                        onValueChange(draft)
                        close()
                    },
                    appearance = Appearance.Ghost,
                    enabled = inWindow(draft, minTime, maxTime),
                )
            }
        }
    }
}

private fun defaultTime(min: LocalTime?, step: Int): LocalTime =
    min ?: LocalTime(HOURS_ON_DIAL, snapMinute(0, step))
