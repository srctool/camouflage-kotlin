package com.srctool.camouflage.charts

import kotlin.test.Test
import kotlin.test.assertEquals

class TimeAxisTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun utc(year: Int, month: Int, dayOfMonth: Int, hours: Int = 0) =
        daysFromCivil(year, month, dayOfMonth) * day + hours * hour

    @Test
    fun civilDatesRoundTripThroughDays() {
        assertEquals(0L, daysFromCivil(1970, 1, 1))
        assertEquals(11_017L, daysFromCivil(2000, 3, 1))
        assertEquals(Triple(2000, 3, 1), civilFromDays(11_017L))
        assertEquals(Triple(2024, 2, 29), civilFromDays(daysFromCivil(2024, 2, 29)))
        assertEquals(Triple(1969, 12, 31), civilFromDays(-1L))
        for (days in -800L..800L step 7) {
            val (y, m, d) = civilFromDays(days * 40)
            assertEquals(days * 40, daysFromCivil(y, m, d))
        }
    }

    @Test
    fun threeDaysGetDailyTicks() {
        val ticks = timeTicks(utc(2026, 3, 1), utc(2026, 3, 4), 5)
        assertEquals(TimeStep.Day, ticks.step)
        assertEquals(listOf(utc(2026, 3, 1), utc(2026, 3, 2), utc(2026, 3, 3), utc(2026, 3, 4)), ticks.ticks)
    }

    @Test
    fun aDaysTicksFallOnHours() {
        val ticks = timeTicks(utc(2026, 3, 1), utc(2026, 3, 2), 9)
        assertEquals(TimeStep.ThreeHours, ticks.step)
        assertEquals(9, ticks.ticks.size)
    }

    @Test
    fun aYearGetsQuartersAlignedToJanuary() {
        val ticks = timeTicks(utc(2026, 1, 1), utc(2026, 12, 31), 6)
        assertEquals(TimeStep.Quarter, ticks.step)
        assertEquals(listOf(utc(2026, 1, 1), utc(2026, 4, 1), utc(2026, 7, 1), utc(2026, 10, 1)), ticks.ticks)
        val mid = timeTicks(utc(2026, 2, 10), utc(2026, 12, 31), 6)
        assertEquals(utc(2026, 4, 1), mid.ticks.first())
    }

    @Test
    fun yearsStepOnRoundYears() {
        val ticks = timeTicks(utc(2019, 6, 1), utc(2031, 6, 1), 4)
        assertEquals(TimeStep.FiveYears, ticks.step)
        assertEquals(listOf(utc(2020, 1, 1), utc(2025, 1, 1), utc(2030, 1, 1)), ticks.ticks)
    }

    @Test
    fun ticksFollowTheLocalDayOfTheZone() {
        val ticks = timeTicks(utc(2026, 3, 1), utc(2026, 3, 4), 5, offsetMinutes = 60)
        assertEquals(utc(2026, 3, 1, 23), ticks.ticks.first())
    }

    @Test
    fun weeksStartOnMonday() {
        val ticks = timeTicks(utc(2026, 3, 1), utc(2026, 3, 29), 5)
        assertEquals(TimeStep.Week, ticks.step)
        assertEquals(utc(2026, 3, 2), ticks.ticks.first())
    }

    @Test
    fun labelsReadAsTimesDatesMonthsAndYears() {
        assertEquals("14:30", formatChartTime(14 * hour + hour / 2, TimeStep.FifteenMinutes))
        assertEquals("Mar 5", formatChartTime(utc(2026, 3, 5), TimeStep.Day))
        assertEquals("Mar", formatChartTime(utc(2026, 3, 1), TimeStep.Quarter))
        assertEquals("2026", formatChartTime(utc(2026, 1, 1), TimeStep.Month))
        assertEquals("2030", formatChartTime(utc(2030, 1, 1), TimeStep.FiveYears))
        assertEquals("01:00", formatChartTime(0L, TimeStep.Hour, offsetMinutes = 60))
    }
}
