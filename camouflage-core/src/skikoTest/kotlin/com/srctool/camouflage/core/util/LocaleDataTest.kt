package com.srctool.camouflage.core.util

import com.srctool.camouflage.core.datepicker.formatDatePattern
import com.srctool.camouflage.core.datepicker.typingPattern
import com.srctool.camouflage.core.timepicker.formatTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// The platforms' locale data (Android's android.icu is a stub in host tests, so these run on desktop, iOS and web).
class LocaleDataTest {
    @Test
    fun localesKnowTheirFirstDayAndNames() {
        assertEquals(DayOfWeek.SUNDAY, CamoLocaleData.firstDayOfWeek("en-US"))
        assertEquals(DayOfWeek.MONDAY, CamoLocaleData.firstDayOfWeek("de-DE")) // CLDR starts id-ID on Sunday
        assertEquals("October", CamoLocaleData.monthNames("en-US", short = false)[9])
        assertEquals("Oktober", CamoLocaleData.monthNames("id-ID", short = false)[9])
        assertEquals("MM/dd/yyyy", typingPattern(CamoLocaleData.shortDatePattern("en-US")))
        assertEquals("dd/MM/yyyy", typingPattern(CamoLocaleData.shortDatePattern("id-ID")))
        assertTrue(CamoLocaleData.formatSkeleton(2026, 10, 1, 0, 0, "yMMMM", "en-US").contains("October 2026"))
    }

    @Test
    fun patternsFormatWithTheLocalesNames() {
        val date = LocalDate(2026, 10, 14)
        assertEquals("14/10/2026", formatDatePattern(date, "dd/MM/yyyy", "en-US"))
        assertEquals("14 Oct 2026", formatDatePattern(date, "d MMM yyyy", "en-US"))
        assertEquals("Wednesday, 14 October", formatDatePattern(date, "EEEE, d MMMM", "en-US"))
        assertEquals("14 de Oct", formatDatePattern(date, "d 'de' MMM", "en-US"))
    }

    @Test
    fun localesKnowTheirHourFormat() {
        assertFalse(CamoLocaleData.uses24Hour("en-US"))
        assertTrue(CamoLocaleData.uses24Hour("id-ID"))
        assertEquals("14.30", formatTime(LocalTime(14, 30), use24Hour = true, "id-ID").replace(':', '.'))
        assertTrue(formatTime(LocalTime(14, 30), use24Hour = false, "en-US").startsWith("2:30"))
    }
}
