@file:Suppress("TooManyFunctions") // the two fields with their shared frame, typing and panel

package com.srctool.camouflage.core.datepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.overlay.PickerOverlay
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.textfield.CamoDisplayFormat
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.textfield.CamoFieldFrame
import com.srctool.camouflage.core.textfield.CamoInputFilter
import com.srctool.camouflage.core.textfield.CamoTextField
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.CamoLocaleData
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.currentLocaleTag
import com.srctool.camouflage.core.util.prefersTyping
import com.srctool.camouflage.core.util.WheelColumn
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth

/**
 * A date field: the text-field frame showing [value] in `DatePickerTheme.displayFormat` (or the locale's
 * medium date), opening a calendar (or wheels, per `DatePickerTheme.form`) in a popup on wide windows and a
 * bottom sheet otherwise ([presentation]). Single dates confirm with Cancel/OK unless
 * `DatePickerTheme.confirmButton` is off, when a tap picks and closes.
 *
 * On desktop and web (`DatePickerTheme.allowTyping = Auto`) the field also takes typing in the locale's short
 * date pattern; a complete, valid date in range calls [onValueChange], anything else shows "Enter a valid date"
 * (`DatePickerStrings`) and leaves [value] alone.
 */
@Composable
public fun CamoDateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isSelectable: ((LocalDate) -> Boolean)? = null,
    presentation: PickerPresentation = PickerPresentation.Auto,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val style = datePickerStyle("CamoDateField")
    val locale = currentLocaleTag()
    val typing = enabled && when (style.allowTyping) {
        AllowTyping.Auto -> prefersTyping
        AllowTyping.Always -> true
        AllowTyping.Never -> false
    }
    var open by remember { mutableStateOf(false) }
    val opener = remember { FocusRequester() }
    val keyboard = LocalInputModeManager.current
    val close = {
        open = false
        if (keyboard.inputMode == InputMode.Keyboard) opener.requestFocus()
    }
    Box(modifier) {
        if (typing) {
            TypedDateField(
                value, onValueChange, label, placeholder, supportingText, error, minDate, maxDate, isSelectable,
                style, locale, opener, onFocusChanged,
            ) { open = true }
        } else {
            PickerFieldFrame(
                text = value?.let { displayDate(it, style, locale) },
                label = label,
                placeholder = placeholder,
                supportingText = supportingText,
                error = error,
                icon = style.calendarIcon,
                open = open,
                enabled = enabled,
                focusRequester = opener,
                onFocusChanged = onFocusChanged,
            ) { open = true }
        }
        if (open) {
            PickerOverlay(presentation, style.autoPopupMinWidth, label, close) {
                DatePanel(
                    initial = CalendarSelection.Single(value),
                    minDate = minDate,
                    maxDate = maxDate,
                    isSelectable = isSelectable,
                    maxRangeLength = null,
                    style = style,
                    onCommit = { selection ->
                        (selection as CalendarSelection.Single).date?.let(onValueChange)
                        close()
                    },
                    onCancel = close,
                )
            }
        }
    }
}

/**
 * A date range field: like [CamoDateField] (without typing), showing "start – end" and opening a calendar
 * where the first tap sets the start and the second the end; Done confirms and Clear starts over. With
 * [maxLength], days more than [maxLength] days after a picked start are disabled.
 */
@Composable
public fun CamoDateRangeField(
    value: CamoDateRange?,
    onValueChange: (CamoDateRange) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isSelectable: ((LocalDate) -> Boolean)? = null,
    maxLength: Int? = null,
    presentation: PickerPresentation = PickerPresentation.Auto,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val style = datePickerStyle("CamoDateRangeField")
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
            text = value?.let { displayRange(it, style, locale) },
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            error = error,
            icon = style.calendarIcon,
            open = open,
            enabled = enabled,
            focusRequester = opener,
            onFocusChanged = onFocusChanged,
        ) { open = true }
        if (open) {
            PickerOverlay(presentation, style.autoPopupMinWidth, label, close) {
                DatePanel(
                    initial = CalendarSelection.Range(value?.start, value?.endInclusive),
                    minDate = minDate,
                    maxDate = maxDate,
                    isSelectable = isSelectable,
                    maxRangeLength = maxLength,
                    style = style,
                    onCommit = { selection ->
                        val range = selection as CalendarSelection.Range
                        if (range.start != null && range.end != null) onValueChange(range.start..range.end)
                        close()
                    },
                    onCancel = close,
                )
            }
        }
    }
}

@Composable
internal fun datePickerStyle(component: String): DatePickerStyle {
    val skin = requireSkin(component)
    val theme = requireTheme(component)
    return remember(skin, theme) { skin.datePicker.defaults(theme).merge(theme.components.datePicker) }
}

/** [range] as the range field shows it: "start – end". */
internal fun displayRange(range: CamoDateRange, style: DatePickerStyle, locale: String): String =
    "${displayDate(range.start, style, locale)} – ${displayDate(range.endInclusive, style, locale)}"

/** [date] as the field shows it: `displayFormat`, else the locale's medium date. */
internal fun displayDate(date: LocalDate, style: DatePickerStyle, locale: String): String =
    style.displayFormat?.let { formatDatePattern(date, it, locale) }
        ?: CamoLocaleData.formatSkeleton(date.year, date.month.number, date.day, 0, 0, "yMMMd", locale)

/**
 * A read-only field that opens a picker: the text-field frame (focused while [open]) with [text] or the
 * [placeholder], the picker's [icon] at the end, and the whole field one button announced as a pop-up.
 */
@Composable
internal fun PickerFieldFrame(
    text: String?,
    label: String?,
    placeholder: String?,
    supportingText: String?,
    error: String?,
    icon: IconSource,
    open: Boolean,
    enabled: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: ((Boolean) -> Unit)?,
    prefixIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val skin = requireSkin("CamoDateField")
    val theme = requireTheme("CamoDateField")
    val fieldStyle = remember(skin, theme) { skin.textField.defaults(theme).merge(theme.components.textField) }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val focusChanged by rememberUpdatedState(onFocusChanged)
    LaunchedEffect(interaction.focused) { focusChanged?.invoke(interaction.focused) }
    CamoFieldFrame(
        focused = interaction.focused || open,
        isEmpty = text == null,
        modifier = Modifier
            .focusRequester(focusRequester)
            .clickable(source, indication = null, enabled = enabled, role = Role.DropdownList, onClick = onOpen)
            .then(modifier),
        label = label,
        supportingText = supportingText,
        error = error,
        prefixIcon = prefixIcon,
        suffixIcon = { CamoIcon(icon, null) },
        enabled = enabled,
    ) {
        // A floating label rests where the placeholder would be, so the placeholder waits for focus there.
        val floating = fieldStyle.labelPlacement == LabelPlacement.Floating && label != null
        val resting = floating && !interaction.focused && !open
        val shown = text ?: placeholder.takeUnless { resting }.orEmpty()
        BasicText(
            shown,
            style = fieldStyle.textStyle.copy(
                color = if (text == null) fieldStyle.colors.placeholder else fieldStyle.colors.text,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// The typed form of the date field: the locale's short pattern as a mask, the calendar behind the icon.
@Composable
private fun TypedDateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String?,
    placeholder: String?,
    supportingText: String?,
    error: String?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isSelectable: ((LocalDate) -> Boolean)?,
    style: DatePickerStyle,
    locale: String,
    focusRequester: FocusRequester,
    onFocusChanged: ((Boolean) -> Unit)?,
    onOpen: () -> Unit,
) {
    val pattern = remember(locale) { typingPattern(CamoLocaleData.shortDatePattern(locale)) }
    val length = pattern.count { it.isLetter() }
    var digits by remember { mutableStateOf(value?.let { typedDigits(it, pattern) }.orEmpty()) }
    var left by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (parseTyped(digits, pattern) != value) digits = value?.let { typedDigits(it, pattern) }.orEmpty()
    }
    val strings = style.strings
    val typedError = typedError(digits, pattern, left, strings) { selectable(it, minDate, maxDate, isSelectable) }
    CamoTextField(
        value = digits,
        onValueChange = { typed ->
            digits = typed
            val date = parseTyped(typed, pattern)
            if (date != null && selectable(date, minDate, maxDate, isSelectable)) onValueChange(date)
        },
        modifier = Modifier.focusRequester(focusRequester),
        label = label,
        placeholder = placeholder ?: pattern.lowercase(),
        supportingText = supportingText,
        error = error ?: typedError,
        suffixIcon = {
            CamoFieldAction(
                icon = { CamoIcon(style.calendarIcon, null) },
                contentDescription = strings.chooseDate,
                onClick = onOpen,
            )
        },
        onFocusChanged = {
            if (!it && digits.isNotEmpty()) left = true
            onFocusChanged?.invoke(it)
        },
        inputFilter = CamoInputFilter.digitsOnly(length),
        displayFormat = CamoDisplayFormat.pattern(typingMask(pattern)),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

// The typed date's message: none while empty or still being typed, else invalid or out of range.
private fun typedError(
    digits: String,
    pattern: String,
    left: Boolean,
    strings: DatePickerStrings,
    allowed: (LocalDate) -> Boolean,
): String? {
    val parsed = parseTyped(digits, pattern)
    return when {
        digits.isEmpty() -> null
        digits.length < pattern.count { it.isLetter() } -> if (left) strings.invalidDate else null
        parsed == null -> strings.invalidDate
        !allowed(parsed) -> strings.outOfRange
        else -> null
    }
}

/**
 * What the popup or sheet holds: the calendar (or wheels for a single date in the `Wheel` form) on a draft of
 * the selection, then Cancel/OK (single, with `confirmButton`) or Clear/Done (range).
 */
@Composable
private fun DatePanel(
    initial: CalendarSelection,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isSelectable: ((LocalDate) -> Boolean)?,
    maxRangeLength: Int?,
    style: DatePickerStyle,
    onCommit: (CalendarSelection) -> Unit,
    onCancel: () -> Unit,
) {
    val theme = requireTheme("CamoDateField")
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var draft by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(startMonth(initial, minDate, maxDate, today)) }
    val single = initial is CalendarSelection.Single
    val strings = style.strings
    Column(
        Modifier.padding(style.contentPadding).width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s),
    ) {
        if (single && style.form == DatePickerForm.Wheel) {
            val start = (draft as CalendarSelection.Single).date ?: today.clampTo(minDate, maxDate)
            DateWheels(start, minDate, maxDate, style, theme) { draft = CalendarSelection.Single(it) }
        } else {
            CalendarView(
                selection = draft,
                onSelectionChange = {
                    draft = it
                    if (single && !style.confirmButton) onCommit(it)
                },
                visibleMonth = month,
                onVisibleMonthChange = { month = it },
                minDate = minDate,
                maxDate = maxDate,
                isSelectable = isSelectable,
                maxRangeLength = maxRangeLength,
                modifier = Modifier,
                autoFocus = true,
            )
        }
        val wheel = single && style.form == DatePickerForm.Wheel
        if (!single || style.confirmButton || wheel) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(theme.spacing.s, Alignment.End)) {
                val commit = {
                    onCommit(if (wheel) CalendarSelection.Single(wheelDate(draft, today, minDate, maxDate)) else draft)
                }
                PanelActions(draft, wheel, strings, commit, onCancel) { draft = CalendarSelection.Range(null, null) }
            }
        }
    }
}

// Cancel/OK for a single date (OK needs a date, except on wheels, which always show one), Clear/Done for a range.
@Composable
private fun PanelActions(
    draft: CalendarSelection,
    wheel: Boolean,
    strings: DatePickerStrings,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
) {
    when (draft) {
        is CalendarSelection.Single -> {
            CamoButton(strings.cancel, onCancel, appearance = Appearance.Ghost)
            CamoButton(strings.ok, onCommit, appearance = Appearance.Ghost, enabled = wheel || draft.date != null)
        }
        is CalendarSelection.Range -> {
            CamoButton(strings.clear, onClear, appearance = Appearance.Ghost, enabled = draft.start != null)
            CamoButton(strings.done, onCommit, appearance = Appearance.Ghost, enabled = draft.end != null)
        }
    }
}

private fun wheelDate(draft: CalendarSelection, today: LocalDate, min: LocalDate?, max: LocalDate?): LocalDate =
    (draft as CalendarSelection.Single).date ?: today.clampTo(min, max)

// The month a picker opens on: the value's, else today's within the limits.
private fun startMonth(initial: CalendarSelection, min: LocalDate?, max: LocalDate?, today: LocalDate): YearMonth {
    val selected = when (initial) {
        is CalendarSelection.Single -> initial.date
        is CalendarSelection.Range -> initial.start
    }
    return (selected ?: today.clampTo(min, max)).yearMonth
}

internal fun LocalDate.clampTo(min: LocalDate?, max: LocalDate?): LocalDate = when {
    min != null && this < min -> min
    max != null && this > max -> max
    else -> this
}

// Day, month and year wheels in the locale's order; a change keeps the date real (31 → 30) and within limits.
@Composable
private fun DateWheels(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    style: DatePickerStyle,
    theme: CamoTheme,
    onChange: (LocalDate) -> Unit,
) {
    val skin = requireSkin("CamoDateField")
    val locale = currentLocaleTag()
    val order = remember(locale) {
        typingPattern(CamoLocaleData.shortDatePattern(locale)).filter { it.isLetter() }.toList().distinct()
    }
    val months = remember(locale) { CamoLocaleData.monthNames(locale, short = true) }
    val firstYear = minDate?.year ?: (date.year - 120)
    val lastYear = maxDate?.year ?: (date.year + 50)
    val ym = date.yearMonth
    fun emit(year: Int, month: Int, day: Int) {
        val target = YearMonth(year, month)
        onChange(target.onDayClamped(day).clampTo(minDate, maxDate))
    }
    val strings = style.strings
    val colors = style.colors
    val wheels: List<@Composable () -> Unit> = order.map { part ->
        when (part) {
            'd' -> {
                {
                    WheelColumn(
                        ym.numberOfDays, date.day - 1, { emit(date.year, date.month.number, it + 1) },
                        { (it + 1).toString() }, strings.day, style.wheelItemHeight, style.wheelTextStyle,
                        colors.day, colors.weekday,
                    )
                }
            }
            'M' -> {
                {
                    WheelColumn(
                        Month.entries.size, date.month.number - 1, { emit(date.year, it + 1, date.day) },
                        { months[it] }, strings.month, style.wheelItemHeight, style.wheelTextStyle,
                        colors.day, colors.weekday,
                    )
                }
            }
            else -> {
                {
                    WheelColumn(
                        lastYear - firstYear + 1,
                        date.year - firstYear,
                        { emit(firstYear + it, date.month.number, date.day) },
                        { (firstYear + it).toString() }, strings.year, style.wheelItemHeight, style.wheelTextStyle,
                        colors.day, colors.weekday,
                    )
                }
            }
        }
    }
    skin.datePicker.Wheels(wheels, theme, style, Modifier)
}

private fun YearMonth.onDayClamped(day: Int): LocalDate = LocalDate(year, month, minOf(day, numberOfDays))
