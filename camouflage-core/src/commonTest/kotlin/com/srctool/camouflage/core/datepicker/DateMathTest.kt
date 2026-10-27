package com.srctool.camouflage.core.datepicker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

class DateMathTest {
    @Test
    fun theGridStartsOnTheFirstDayOfTheWeekAndHoldsSixWeeks() {
        // 1 October 2026 is a Thursday.
        val sunday = monthGrid(YearMonth(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(42, sunday.size)
        assertEquals(LocalDate(2026, 9, 27), sunday.first())
        val monday = monthGrid(YearMonth(2026, 10), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 9, 28), monday.first())
        // A month starting on the first day of the week starts the grid with its 1st.
        assertEquals(LocalDate(2026, 6, 1), monthGrid(YearMonth(2026, 6), DayOfWeek.MONDAY).first())
    }

    @Test
    fun leapYearsAndClampedDays() {
        assertEquals(29, YearMonth(2028, 2).numberOfDays)
        assertEquals(28, YearMonth(2100, 2).numberOfDays)
        assertEquals(LocalDate(2028, 2, 29), YearMonth(2028, 2).dayLike(LocalDate(2028, 1, 31)))
        assertEquals(LocalDate(2027, 2, 28), YearMonth(2027, 2).dayLike(LocalDate(2027, 1, 31)))
    }

    @Test
    fun weekdaysFollowTheFirstDay() {
        val names = listOf("M", "T", "W", "T", "F", "S", "S")
        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), weekdayOrder(names, DayOfWeek.SUNDAY))
        assertEquals(names, weekdayOrder(names, DayOfWeek.MONDAY))
    }

    @Test
    fun limitsAndMaxLengthDisableDays() {
        val min = LocalDate(2026, 10, 5)
        val max = LocalDate(2026, 10, 25)
        assertFalse(selectable(LocalDate(2026, 10, 4), min, max, null))
        assertTrue(selectable(LocalDate(2026, 10, 5), min, max, null))
        assertFalse(selectable(LocalDate(2026, 10, 26), min, max, null))
        val weekdays = { d: LocalDate -> d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY }
        assertFalse(selectable(LocalDate(2026, 10, 10), null, null, weekdays))
        val start = LocalDate(2026, 10, 1)
        assertTrue(selectable(LocalDate(2026, 10, 14), null, null, null, start, maxLength = 14))
        assertFalse(selectable(LocalDate(2026, 10, 15), null, null, null, start, maxLength = 14))
        // Before the start is fine: it starts a new range.
        assertTrue(selectable(LocalDate(2026, 9, 1), null, null, null, start, maxLength = 14))
    }

    @Test
    fun aRangeIsPickedStartThenEnd() {
        val a = LocalDate(2026, 10, 10)
        val b = LocalDate(2026, 10, 14)
        val start = CalendarSelection.Range(null, null).tap(a)
        assertEquals(CalendarSelection.Range(a, null), start)
        assertEquals(CalendarSelection.Range(a, b), start.tap(b))
        assertEquals(CalendarSelection.Range(LocalDate(2026, 10, 1), null), start.tap(LocalDate(2026, 10, 1)))
        assertEquals(CalendarSelection.Range(LocalDate(2026, 10, 20), null), start.tap(b).tap(LocalDate(2026, 10, 20)))
        assertEquals(RangePosition.Start, rangePosition(a, a, b))
        assertEquals(RangePosition.Middle, rangePosition(LocalDate(2026, 10, 12), a, b))
        assertEquals(RangePosition.End, rangePosition(b, a, b))
        assertEquals(RangePosition.Single, rangePosition(a, a, null))
    }

    @Test
    fun typingPatternsKeepTheLocalesOrder() {
        assertEquals("MM/dd/yyyy", typingPattern("M/d/yy"))
        assertEquals("dd/MM/yyyy", typingPattern("dd/MM/y"))
        assertEquals("dd.MM.yyyy", typingPattern("dd.MM.yy"))
        assertEquals("yyyy-MM-dd", typingPattern("y-MM-dd"))
        assertEquals("##/##/####", typingMask("dd/MM/yyyy"))
    }

    @Test
    fun typedDigitsParseOnlyRealDates() {
        assertEquals(LocalDate(2026, 10, 14), parseTyped("14102026", "dd/MM/yyyy"))
        assertEquals(LocalDate(2026, 10, 14), parseTyped("10142026", "MM/dd/yyyy"))
        assertNull(parseTyped("31022026", "dd/MM/yyyy"))
        assertNull(parseTyped("1410202", "dd/MM/yyyy"))
        assertEquals("14102026", typedDigits(LocalDate(2026, 10, 14), "dd/MM/yyyy"))
        assertEquals("20261014", typedDigits(LocalDate(2026, 10, 14), "yyyy-MM-dd"))
    }
}
