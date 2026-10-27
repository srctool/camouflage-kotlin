package com.srctool.camouflage.core.timepicker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalTime

class TimeMathTest {
    @Test
    fun twelveAndTwentyFourHours() {
        assertEquals(12, hour12(0))
        assertEquals(1, hour12(13))
        assertEquals(12, hour12(12))
        assertEquals(0, hour24(12, DayPeriod.Am))
        assertEquals(12, hour24(12, DayPeriod.Pm))
        assertEquals(14, hour24(2, DayPeriod.Pm))
        assertEquals(DayPeriod.Pm, periodOf(12))
        assertEquals(DayPeriod.Am, periodOf(0))
    }

    @Test
    fun minutesSnapToTheStepWithinTheHour() {
        assertEquals(15, snapMinute(14, 15))
        assertEquals(0, snapMinute(7, 15))
        assertEquals(45, snapMinute(58, 15))
        assertEquals(37, snapMinute(37, 1))
    }

    @Test
    fun windowsMayRunPastMidnight() {
        val nine = LocalTime(9, 0)
        val five = LocalTime(17, 0)
        assertTrue(inWindow(LocalTime(12, 0), nine, five))
        assertFalse(inWindow(LocalTime(20, 0), nine, five))
        val night = LocalTime(22, 0)
        val six = LocalTime(6, 0)
        assertTrue(inWindow(LocalTime(23, 30), night, six))
        assertTrue(inWindow(LocalTime(2, 0), night, six))
        assertFalse(inWindow(LocalTime(12, 0), night, six))
    }

    @Test
    fun shiftWrapsAroundTheDay() {
        assertEquals(LocalTime(0, 10), LocalTime(23, 50).shift(minutes = 20))
        assertEquals(LocalTime(23, 0), LocalTime(0, 0).shift(hours = -1))
    }
}
