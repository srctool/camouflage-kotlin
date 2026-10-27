package com.srctool.camouflage.core.util

import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.DayOfWeek

/** Locale facts the platform knows and Compose doesn't expose. `locale` is a BCP 47 tag ("id-ID"). */
internal expect object CamoLocaleData {
    /** The decimal separator: '.' for en-US, ',' for id-ID. */
    fun decimalSeparator(locale: String): Char

    /** The first day of the week: Sunday for en-US, Monday for id-ID. */
    fun firstDayOfWeek(locale: String): DayOfWeek

    /** The twelve standalone month names, January first: full ("October") or [short] ("Oct"). */
    fun monthNames(locale: String, short: Boolean): List<String>

    /** The seven standalone weekday names, Monday first, at [width]. */
    fun weekdayNames(locale: String, width: NameWidth): List<String>

    /**
     * The date and time formatted with a CLDR [skeleton] in the locale's own order and words. Core uses
     * `yMMMM` (month header), `yMMMd` (the field), `yMMMMEEEEd` (a day's full name), `Hm` and `hm` (times).
     */
    fun formatSkeleton(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        skeleton: String,
        locale: String,
    ): String

    /** The locale's short numeric date pattern as the platform gives it ("M/d/yy", "dd/MM/y"). */
    fun shortDatePattern(locale: String): String

    /** The AM and PM markers. */
    fun amPm(locale: String): List<String>

    /** Whether the locale writes times in 24 hours ("14:30") rather than 12 ("2:30 PM"). */
    fun uses24Hour(locale: String): Boolean
}

/** How long a weekday name is: "T", "Tue" or "Tuesday". */
internal enum class NameWidth { Narrow, Short, Full }

/** The current locale's BCP 47 tag. */
internal fun currentLocaleTag(): String = Locale.current.toLanguageTag()
