@file:Suppress("TooManyFunctions") // the date model and the calendar math, each helper small and tested

package com.srctool.camouflage.core.datepicker

import androidx.compose.runtime.Immutable
import com.srctool.camouflage.core.util.CamoLocaleData
import com.srctool.camouflage.core.util.NameWidth
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.YearMonth
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.onDay
import kotlinx.datetime.plus

/** A calendar date: year, month and day, with no time or time zone. */
public typealias CamoDate = LocalDate

/** A month of a year, e.g. the month a calendar shows. */
public typealias CamoYearMonth = YearMonth

/** A range of dates, both ends included. */
public typealias CamoDateRange = LocalDateRange

/** What a calendar has selected: one date, or a range being picked. */
@Immutable
public sealed interface CalendarSelection {
    /** One date, or none yet. */
    public data class Single(val date: LocalDate?) : CalendarSelection

    /** A range: no ends yet, a start only, or both. */
    public data class Range(val start: LocalDate?, val end: LocalDate?) : CalendarSelection
}

/** How a date or time field shows its picker. */
public enum class PickerPresentation {
    /** A popup on a wide window (`autoPopupMinWidth`), a bottom sheet otherwise. */
    Auto,

    /** Anchored under the field. */
    Popup,

    /** A bottom sheet. */
    Sheet,
}

/** Where a day sits in a selected range. */
public enum class RangePosition { None, Start, Middle, End, Single }

/** The picker's form: a month grid, or day · month · year wheels (single dates only). */
public enum class DatePickerForm { Calendar, Wheel }

/** Whether a date field accepts typing: on desktop and web (`Auto`), everywhere, or nowhere. */
public enum class AllowTyping { Auto, Always, Never }

private const val DAYS_IN_WEEK = 7
private const val GRID_DAYS = 42

// Pattern letter counts: "yy", "MMM" / "EEE" and "MMMM" / "EEEE".
private const val SHORT_YEAR = 2
private const val CENTURY = 100
private const val SHORT_NAME = 3
private const val FULL_NAME = 4

// The 42 days a month grid shows: from the week holding the 1st, starting on [firstDayOfWeek].
internal fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val first = month.firstDay
    val lead = (first.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val start = first.minus(DatePeriod(days = lead))
    return List(GRID_DAYS) { start.plus(DatePeriod(days = it)) }
}

// The weekday names in grid order, starting on [firstDayOfWeek].
internal fun weekdayOrder(names: List<String>, firstDayOfWeek: DayOfWeek): List<String> {
    val shift = firstDayOfWeek.isoDayNumber - 1
    return names.drop(shift) + names.take(shift)
}

// [date] in the same position of [month], clamped to its length (31 Jan + 1 month = 28/29 Feb).
internal fun YearMonth.dayLike(date: LocalDate): LocalDate = onDay(minOf(date.day, numberOfDays))

internal fun LocalDate.inRange(min: LocalDate?, max: LocalDate?): Boolean =
    (min == null || this >= min) && (max == null || this <= max)

// Whether [date] can be picked with the limits and, for a range with a start, [maxLength] days.
internal fun selectable(
    date: LocalDate,
    min: LocalDate?,
    max: LocalDate?,
    isSelectable: ((LocalDate) -> Boolean)?,
    rangeStart: LocalDate? = null,
    maxLength: Int? = null,
): Boolean {
    val allowed = date.inRange(min, max) && isSelectable?.invoke(date) != false
    val withinLength = rangeStart == null || maxLength == null || date < rangeStart ||
        rangeStart.daysUntil(date) < maxLength
    return allowed && withinLength
}

internal fun rangePosition(date: LocalDate, start: LocalDate?, end: LocalDate?): RangePosition = when {
    start == null -> RangePosition.None
    end == null || start == end -> if (date == start) RangePosition.Single else RangePosition.None
    date == start -> RangePosition.Start
    date == end -> RangePosition.End
    date > start && date < end -> RangePosition.Middle
    else -> RangePosition.None
}

// The next range after tapping [date]: a start, then an end; a tap before the start, or after a full
// range, starts again.
internal fun CalendarSelection.Range.tap(date: LocalDate): CalendarSelection.Range = when {
    start == null || end != null || date < start -> CalendarSelection.Range(date, null)
    else -> CalendarSelection.Range(start, date)
}

/**
 * Formats [date] with a pattern: `d`/`dd` day, `M`/`MM` month number, `MMM`/`MMMM` month name, `yy`/`yyyy`
 * year, `EEE`/`EEEE` weekday name, text in quotes kept as is.
 */
internal fun formatDatePattern(date: LocalDate, pattern: String, locale: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        var n = 1
        while (i + n < pattern.length && pattern[i + n] == c) n++
        when (c) {
            'd' -> out.append(date.day.toString().padStart(n, '0'))
            'M', 'L' -> out.append(monthText(date, n, locale))
            'y' -> {
                val year = if (n == SHORT_YEAR) date.year % CENTURY else date.year
                out.append(year.toString().padStart(n, '0'))
            }
            'E' -> {
                val width = if (n >= FULL_NAME) NameWidth.Full else NameWidth.Short
                out.append(CamoLocaleData.weekdayNames(locale, width)[date.dayOfWeek.isoDayNumber - 1])
            }
            '\'' -> {
                val end = pattern.indexOf('\'', i + 1).takeIf { it > i } ?: pattern.length
                out.append(pattern, i + 1, end)
                n = end - i + 1
            }
            else -> repeat(n) { out.append(c) }
        }
        i += n
    }
    return out.toString()
}

private fun monthText(date: LocalDate, n: Int, locale: String): String = when {
    n >= FULL_NAME -> CamoLocaleData.monthNames(locale, short = false)[date.month.number - 1]
    n == SHORT_NAME -> CamoLocaleData.monthNames(locale, short = true)[date.month.number - 1]
    else -> date.month.number.toString().padStart(n, '0')
}

/**
 * A typing pattern from the locale's short date pattern, with two-digit day and month and a four-digit year in
 * the locale's order and separator: "M/d/yy" → "MM/dd/yyyy", "dd.MM.y" → "dd.MM.yyyy".
 */
internal fun typingPattern(shortPattern: String): String {
    val order = shortPattern.filter { it == 'd' || it == 'M' || it == 'y' }.toList().distinct()
    val separator = shortPattern.firstOrNull { !it.isLetter() && !it.isWhitespace() && it != '\'' } ?: '/'
    val fields = order.ifEmpty { listOf('M', 'd', 'y') }.map {
        when (it) {
            'd' -> "dd"
            'M' -> "MM"
            else -> "yyyy"
        }
    }
    return fields.joinToString(separator.toString())
}

/** A typing pattern's mask for `CamoDisplayFormat.pattern`: "dd/MM/yyyy" → "##/##/####". */
internal fun typingMask(pattern: String): String = pattern.map { if (it.isLetter()) '#' else it }.joinToString("")

/** The date in [digits] (exactly the pattern's eight), or `null` when they aren't a real date. */
internal fun parseTyped(digits: String, pattern: String): LocalDate? {
    val letters = pattern.filter { it.isLetter() }
    if (digits.length != letters.length || !digits.all { it.isDigit() }) return null
    fun field(c: Char) = letters.indices.filter { letters[it] == c }.joinToString("") { digits[it].toString() }.toInt()
    return runCatching { LocalDate(field('y'), field('M'), field('d')) }.getOrNull()
}

/** [date] as the digits a typing [pattern] holds: 2026-10-14 with "dd/MM/yyyy" → "14102026". */
internal fun typedDigits(date: LocalDate, pattern: String): String {
    val letters = pattern.filter { it.isLetter() }
    val out = StringBuilder()
    var i = 0
    while (i < letters.length) {
        val c = letters[i]
        var n = 1
        while (i + n < letters.length && letters[i + n] == c) n++
        val value = when (c) {
            'd' -> date.day
            'M' -> date.month.number
            else -> date.year
        }
        out.append(value.toString().padStart(n, '0').takeLast(n))
        i += n
    }
    return out.toString()
}
