package com.srctool.camouflage.core.datepicker

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.CamoLocaleData
import com.srctool.camouflage.core.util.NameWidth
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.currentLocaleTag
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.minusMonth
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth

/** Every color a date picker uses. */
@Immutable
public data class DatePickerColors(
    val selected: Color,
    val onSelected: Color,
    val range: Color,
    val onRange: Color,
    val today: Color,
    val day: Color,
    val weekday: Color,
    val disabled: Color,
)

/** The date picker's words, for the app to translate through its theme. */
@Immutable
public data class DatePickerStrings(
    val chooseDate: String = "Choose date",
    val cancel: String = "Cancel",
    val ok: String = "OK",
    val done: String = "Done",
    val clear: String = "Clear",
    val previousMonth: String = "Previous month",
    val nextMonth: String = "Next month",
    val chooseYear: String = "Choose year",
    val today: String = "today",
    val unavailable: String = "unavailable",
    val rangeStart: String = "start of range",
    val rangeEnd: String = "end of range",
    val inRange: String = "in range",
    val day: String = "Day",
    val month: String = "Month",
    val year: String = "Year",
    val invalidDate: String = "Enter a valid date",
    val outOfRange: String = "Enter a date within the allowed range",
)

/** Overrides for date pickers and date fields. `null` keeps the skin's default. */
@Immutable
public data class DatePickerTheme(
    val form: DatePickerForm? = null,
    val firstDayOfWeek: DayOfWeek? = null,
    val displayFormat: String? = null,
    val confirmButton: Boolean? = null,
    val allowTyping: AllowTyping? = null,
    val showOutsideDays: Boolean? = null,
    val daySize: Dp? = null,
    val autoPopupMinWidth: Dp? = null,
    val contentPadding: PaddingValues? = null,
    val headerStyle: TextStyle? = null,
    val weekdayStyle: TextStyle? = null,
    val dayStyle: TextStyle? = null,
    val wheelTextStyle: TextStyle? = null,
    val wheelItemHeight: Dp? = null,
    val calendarIcon: IconSource? = null,
    val previousIcon: IconSource? = null,
    val nextIcon: IconSource? = null,
    val colors: DatePickerColors? = null,
    val strings: DatePickerStrings? = null,
)

/**
 * The resolved date picker style. [firstDayOfWeek] and [displayFormat] stay `null` to follow the locale;
 * [displayFormat] is a pattern like `"d MMM yyyy"` (see the date field).
 */
@Immutable
public data class DatePickerStyle(
    val form: DatePickerForm,
    val firstDayOfWeek: DayOfWeek?,
    val displayFormat: String?,
    val confirmButton: Boolean,
    val allowTyping: AllowTyping,
    val showOutsideDays: Boolean,
    val daySize: Dp,
    val autoPopupMinWidth: Dp,
    val contentPadding: PaddingValues,
    val headerStyle: TextStyle,
    val weekdayStyle: TextStyle,
    val dayStyle: TextStyle,
    val wheelTextStyle: TextStyle,
    val wheelItemHeight: Dp,
    val calendarIcon: IconSource,
    val previousIcon: IconSource,
    val nextIcon: IconSource,
    val colors: DatePickerColors,
    val strings: DatePickerStrings,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: DatePickerTheme?): DatePickerStyle = if (overrides == null) {
        this
    } else {
        DatePickerStyle(
            form = overrides.form ?: form,
            firstDayOfWeek = overrides.firstDayOfWeek ?: firstDayOfWeek,
            displayFormat = overrides.displayFormat ?: displayFormat,
            confirmButton = overrides.confirmButton ?: confirmButton,
            allowTyping = overrides.allowTyping ?: allowTyping,
            showOutsideDays = overrides.showOutsideDays ?: showOutsideDays,
            daySize = overrides.daySize ?: daySize,
            autoPopupMinWidth = overrides.autoPopupMinWidth ?: autoPopupMinWidth,
            contentPadding = overrides.contentPadding ?: contentPadding,
            headerStyle = overrides.headerStyle ?: headerStyle,
            weekdayStyle = overrides.weekdayStyle ?: weekdayStyle,
            dayStyle = overrides.dayStyle ?: dayStyle,
            wheelTextStyle = overrides.wheelTextStyle ?: wheelTextStyle,
            wheelItemHeight = overrides.wheelItemHeight ?: wheelItemHeight,
            calendarIcon = overrides.calendarIcon ?: calendarIcon,
            previousIcon = overrides.previousIcon ?: previousIcon,
            nextIcon = overrides.nextIcon ?: nextIcon,
            colors = overrides.colors ?: colors,
            strings = overrides.strings ?: strings,
        )
    }
}

/** A day in the month grid. Days of the next or previous month are `inMonth = false`. */
@Immutable
public data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val enabled: Boolean,
    val selected: Boolean,
    val rangePosition: RangePosition,
    val isToday: Boolean,
)

/** What a calendar cell is: a day of the grid, or a year of the year list. */
public enum class CalendarCellKind { Day, Year }

/**
 * One cell, drawn by the renderer: its [label] ("14", "2026"), its state, and the [behavior] core attaches (the
 * click, focus and the full-date announcement), which the renderer applies outermost.
 */
@Immutable
public data class CalendarCell(
    val label: String,
    val kind: CalendarCellKind,
    val inMonth: Boolean,
    val enabled: Boolean,
    val selected: Boolean,
    val rangePosition: RangePosition,
    val isToday: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
    val behavior: Modifier,
)

/**
 * What a calendar renderer draws: the header ([monthLabel] in core's [CalendarSlots.header] button, previous
 * and next), then either the weekday row and the six weeks of [days] (seven each, in order), or, while
 * [choosingYear], the year list.
 */
@Immutable
public data class CalendarState(
    val visibleMonth: YearMonth,
    val monthLabel: String,
    val weekdayLabels: List<String>,
    val days: List<CalendarDay>,
    val showOutsideDays: Boolean,
    val choosingYear: Boolean,
)

/** The calendar's parts, built by core: the header buttons, one day cell, and the year list. */
@Stable
public class CalendarSlots(
    public val header: @Composable () -> Unit,
    public val previous: @Composable () -> Unit,
    public val next: @Composable () -> Unit,
    public val day: @Composable (CalendarDay) -> Unit,
    public val years: @Composable () -> Unit,
)

/** Draws date pickers: the calendar, its cells, and the wheels. Apply each `modifier` outermost. */
@Stable
public interface DatePickerRenderer {
    /** The complete date picker style for [theme]. */
    public fun defaults(theme: CamoTheme): DatePickerStyle

    /** The month: header, weekday row and grid (or the year list), from [slots]. */
    @Composable
    public fun Calendar(
        state: CalendarState,
        slots: CalendarSlots,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    )

    /** One day or year cell; an `inMonth = false` day is blank unless `style.showOutsideDays`. */
    @Composable
    public fun Cell(cell: CalendarCell, theme: CamoTheme, style: DatePickerStyle, modifier: Modifier)

    /** The wheels side by side, with the band behind the middle row (`WHEEL_ROWS` rows of `wheelItemHeight`). */
    @Composable
    public fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    )
}

private const val YEARS_BACK = 120
private const val YEARS_AHEAD = 50
private const val YEAR_COLUMNS = 3
private const val DAYS_IN_WEEK = 7

/**
 * A month calendar (ADR-30): for booking screens and custom pickers, and inside the date fields. Everything is
 * controlled: [selection] and the [visibleMonth] come from the caller, which a booking screen can use to show
 * two months side by side. Days outside [minDate]…[maxDate], refused by [isSelectable] or, for a range with a
 * start, longer than [maxRangeLength] days, are shown but disabled. A single date's tap selects it; a range's
 * first tap sets the start and the second the end (a tap before the start starts again).
 *
 * Keyboard: arrows move by day and week (mirrored right to left), PageUp/PageDown by month, with Shift by year,
 * Home/End to the week's ends, Enter or Space selects. The header opens a year list.
 */
@Composable
public fun CamoCalendar(
    selection: CalendarSelection,
    onSelectionChange: (CalendarSelection) -> Unit,
    visibleMonth: YearMonth,
    onVisibleMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isSelectable: ((LocalDate) -> Boolean)? = null,
    maxRangeLength: Int? = null,
) {
    CalendarView(selection, onSelectionChange, visibleMonth, onVisibleMonthChange, modifier, minDate, maxDate,
        isSelectable, maxRangeLength, autoFocus = false)
}

// The calendar; with [autoFocus] its focus day takes focus when it appears (inside a picker's popup or sheet).
@Suppress("LongMethod", "CyclomaticComplexMethod") // the grid, keys, header and years share the focus date
@Composable
internal fun CalendarView(
    selection: CalendarSelection,
    onSelectionChange: (CalendarSelection) -> Unit,
    visibleMonth: YearMonth,
    onVisibleMonthChange: (YearMonth) -> Unit,
    modifier: Modifier,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isSelectable: ((LocalDate) -> Boolean)?,
    maxRangeLength: Int?,
    autoFocus: Boolean,
) {
    val skin = requireSkin("CamoCalendar")
    val theme = requireTheme("CamoCalendar")
    val style = remember(skin, theme) { skin.datePicker.defaults(theme).merge(theme.components.datePicker) }
    val locale = currentLocaleTag()
    val firstDay = style.firstDayOfWeek ?: remember(locale) { CamoLocaleData.firstDayOfWeek(locale) }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val rangeStart = (selection as? CalendarSelection.Range)?.let { if (it.end == null) it.start else null }
    fun enabled(date: LocalDate) = selectable(date, minDate, maxDate, isSelectable, rangeStart, maxRangeLength)

    var choosingYear by rememberSaveable { mutableStateOf(false) }
    var focusDate by remember { mutableStateOf(initialFocus(selection, visibleMonth, today)) }
    var keyboardMoved by remember { mutableStateOf(autoFocus) }
    if (focusDate.yearMonth != visibleMonth) focusDate = visibleMonth.dayLike(focusDate)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusDate, keyboardMoved) {
        if (keyboardMoved) {
            focusRequester.requestFocus()
            keyboardMoved = false
        }
    }

    fun select(date: LocalDate) {
        if (!enabled(date)) return
        focusDate = date
        onSelectionChange(
            when (selection) {
                is CalendarSelection.Single -> CalendarSelection.Single(date)
                is CalendarSelection.Range -> selection.tap(date)
            },
        )
    }

    fun moveTo(date: LocalDate) {
        focusDate = date
        keyboardMoved = true
        if (date.yearMonth != visibleMonth) onVisibleMonthChange(date.yearMonth)
    }

    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val days = monthGrid(visibleMonth, firstDay).map { date ->
        val (selected, position) = when (selection) {
            is CalendarSelection.Single -> (date == selection.date) to RangePosition.None
            is CalendarSelection.Range -> rangePosition(date, selection.start, selection.end).let {
                (it != RangePosition.None && it != RangePosition.Middle) to it
            }
        }
        CalendarDay(date, date.yearMonth == visibleMonth, enabled(date), selected, position, date == today)
    }
    val weekdays = remember(locale, firstDay) {
        weekdayOrder(CamoLocaleData.weekdayNames(locale, NameWidth.Narrow), firstDay)
    }
    val monthLabel = remember(visibleMonth, locale) {
        CamoLocaleData.formatSkeleton(visibleMonth.year, visibleMonth.month.number, 1, 0, 0, "yMMMM", locale)
    }
    val strings = style.strings
    val canGoBack = minDate == null || visibleMonth > minDate.yearMonth
    val canGoOn = maxDate == null || visibleMonth < maxDate.yearMonth
    val slots = CalendarSlots(
        header = {
            CamoButton(
                text = monthLabel,
                onClick = { choosingYear = !choosingYear },
                appearance = Appearance.Ghost,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    stateDescription = strings.chooseYear
                },
            )
        },
        previous = {
            CamoIconButton(
                icon = { CamoIcon(if (rtl) style.nextIcon else style.previousIcon, null) },
                contentDescription = strings.previousMonth,
                onClick = { onVisibleMonthChange(visibleMonth.minusMonth()) },
                appearance = Appearance.Ghost,
                enabled = canGoBack && !choosingYear,
            )
        },
        next = {
            CamoIconButton(
                icon = { CamoIcon(if (rtl) style.previousIcon else style.nextIcon, null) },
                contentDescription = strings.nextMonth,
                onClick = { onVisibleMonthChange(visibleMonth.plusMonth()) },
                appearance = Appearance.Ghost,
                enabled = canGoOn && !choosingYear,
            )
        },
        day = { day ->
            DayCell(day, day.date == focusDate, focusRequester, locale, style, theme, ::select) {
                if (it) focusDate = day.date
            }
        },
        years = {
            YearList(visibleMonth, today, minDate, maxDate, style, theme) {
                onVisibleMonthChange(YearMonth(it, visibleMonth.month))
                choosingYear = false
            }
        },
    )
    skin.datePicker.Calendar(
        CalendarState(visibleMonth, monthLabel, weekdays, days, style.showOutsideDays, choosingYear),
        slots,
        theme,
        style,
        modifier.onPreviewKeyEvent { event ->
            val target = if (choosingYear) null else keyTarget(event, focusDate, firstDay, rtl)
            if (target != null) moveTo(target)
            target != null
        },
    )
}

private fun initialFocus(selection: CalendarSelection, month: YearMonth, today: LocalDate): LocalDate {
    val selected = when (selection) {
        is CalendarSelection.Single -> selection.date
        is CalendarSelection.Range -> selection.start
    }
    return when {
        selected?.yearMonth == month -> selected
        today.yearMonth == month -> today
        else -> month.firstDay
    }
}

// The date a key moves the focus to, or null when the key isn't the calendar's.
private fun keyTarget(event: KeyEvent, from: LocalDate, firstDay: DayOfWeek, rtl: Boolean): LocalDate? {
    if (event.type != KeyEventType.KeyDown) return null
    val forward = if (rtl) -1 else 1
    return when (event.key) {
        Key.DirectionLeft -> from.minus(DatePeriod(days = forward))
        Key.DirectionRight -> from.plus(DatePeriod(days = forward))
        Key.DirectionUp -> from.minus(DatePeriod(days = DAYS_IN_WEEK))
        Key.DirectionDown -> from.plus(DatePeriod(days = DAYS_IN_WEEK))
        Key.PageUp -> from.minus(if (event.isShiftPressed) DatePeriod(years = 1) else DatePeriod(months = 1))
        Key.PageDown -> from.plus(if (event.isShiftPressed) DatePeriod(years = 1) else DatePeriod(months = 1))
        Key.MoveHome -> from.minus(DatePeriod(days = dayInWeek(from, firstDay)))
        Key.MoveEnd -> from.plus(DatePeriod(days = DAYS_IN_WEEK - 1 - dayInWeek(from, firstDay)))
        else -> null
    }
}

// How far into its week [date] is, counting from [firstDay] (0 on the first day).
private fun dayInWeek(date: LocalDate, firstDay: DayOfWeek): Int =
    (date.dayOfWeek.isoDayNumber - firstDay.isoDayNumber + DAYS_IN_WEEK) % DAYS_IN_WEEK

// A day's cell: the one Tab stop when it holds the focus date, a button announced with its full date and state.
@Suppress("LongMethod") // the announcement and the behavior belong together
@Composable
private fun DayCell(
    day: CalendarDay,
    focusTarget: Boolean,
    focusRequester: FocusRequester,
    locale: String,
    style: DatePickerStyle,
    theme: CamoTheme,
    onSelect: (LocalDate) -> Unit,
    onFocus: (Boolean) -> Unit,
) {
    val skin = requireSkin("CamoCalendar")
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val shown = day.inMonth || style.showOutsideDays
    val name = remember(day.date, locale) {
        CamoLocaleData.formatSkeleton(day.date.year, day.date.month.number, day.date.day, 0, 0, "yMMMMEEEEd", locale)
    }
    val strings = style.strings
    val states = buildList {
        if (day.isToday) add(strings.today)
        when (day.rangePosition) {
            RangePosition.Start -> add(strings.rangeStart)
            RangePosition.End -> add(strings.rangeEnd)
            RangePosition.Middle -> add(strings.inRange)
            else -> Unit
        }
        if (!day.enabled) add(strings.unavailable)
    }
    val behavior = if (!shown) {
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier
            .then(if (focusTarget) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties { canFocus = focusTarget }
            .onFocusChanged { if (it.isFocused) onFocus(true) }
            .selectable(
                selected = day.selected,
                interactionSource = source,
                indication = null,
                enabled = day.enabled,
                role = Role.Button,
                onClick = { onSelect(day.date) },
            )
            .clearAndSetSemantics {
                contentDescription = name
                role = Role.Button
                selected = day.selected
                if (states.isNotEmpty()) stateDescription = states.joinToString(", ")
                if (!day.enabled) disabled()
                onClick {
                    onSelect(day.date)
                    true
                }
            }
    }
    skin.datePicker.Cell(
        CalendarCell(
            label = day.date.day.toString(),
            kind = CalendarCellKind.Day,
            inMonth = day.inMonth,
            enabled = day.enabled,
            selected = day.selected,
            rangePosition = day.rangePosition,
            isToday = day.isToday,
            interaction = interaction,
            interactionSource = source,
            behavior = behavior,
        ),
        theme,
        style,
        Modifier,
    )
}

// The years to choose from, the visible year first in view; years outside the limits are disabled.
@Composable
private fun YearList(
    month: YearMonth,
    today: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    style: DatePickerStyle,
    theme: CamoTheme,
    onPick: (Int) -> Unit,
) {
    val skin = requireSkin("CamoCalendar")
    val first = minDate?.year ?: (month.year - YEARS_BACK)
    val last = maxDate?.year ?: (month.year + YEARS_AHEAD)
    val years = (first..last).toList()
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = maxOf(0, years.indexOf(month.year) - YEAR_COLUMNS))
    LazyVerticalGrid(GridCells.Fixed(YEAR_COLUMNS), state = state) {
        items(years, key = { it }) { year ->
            val source = remember { MutableInteractionSource() }
            skin.datePicker.Cell(
                CalendarCell(
                    label = year.toString(),
                    kind = CalendarCellKind.Year,
                    inMonth = true,
                    enabled = true,
                    selected = year == month.year,
                    rangePosition = RangePosition.None,
                    isToday = year == today.year,
                    interaction = source.collectInteractionState(),
                    interactionSource = source,
                    behavior = Modifier.selectable(
                        selected = year == month.year,
                        interactionSource = source,
                        indication = null,
                        role = Role.Button,
                        onClick = { onPick(year) },
                    ),
                ),
                theme,
                style,
                Modifier,
            )
        }
    }
}
