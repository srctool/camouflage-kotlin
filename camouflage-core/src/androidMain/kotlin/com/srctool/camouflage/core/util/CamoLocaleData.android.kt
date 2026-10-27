package com.srctool.camouflage.core.util

import android.icu.text.DateFormat
import android.icu.text.DateFormatSymbols
import android.icu.text.DateTimePatternGenerator
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.datetime.DayOfWeek

// android.icu (API 24) rather than java.time, which needs API 26 or the app's desugaring.
internal actual object CamoLocaleData {
    private const val DAYS_IN_WEEK = 7

    actual fun decimalSeparator(locale: String): Char =
        DecimalFormatSymbols.getInstance(Locale.forLanguageTag(locale)).decimalSeparator

    // ICU counts Sunday as 1; ISO counts Monday as 1.
    actual fun firstDayOfWeek(locale: String): DayOfWeek {
        val icu = Calendar.getInstance(ULocale.forLanguageTag(locale)).firstDayOfWeek
        return DayOfWeek((icu + DAYS_IN_WEEK - 2) % DAYS_IN_WEEK + 1)
    }

    actual fun monthNames(locale: String, short: Boolean): List<String> =
        DateFormatSymbols(ULocale.forLanguageTag(locale)).getMonths(
            DateFormatSymbols.STANDALONE,
            if (short) DateFormatSymbols.ABBREVIATED else DateFormatSymbols.WIDE,
        ).toList()

    // ICU's weekday arrays start with an empty slot, then Sunday.
    actual fun weekdayNames(locale: String, width: NameWidth): List<String> {
        val icuWidth = when (width) {
            NameWidth.Narrow -> DateFormatSymbols.NARROW
            NameWidth.Short -> DateFormatSymbols.ABBREVIATED
            NameWidth.Full -> DateFormatSymbols.WIDE
        }
        val symbols = DateFormatSymbols(ULocale.forLanguageTag(locale))
        val names = symbols.getWeekdays(DateFormatSymbols.STANDALONE, icuWidth)
        return (2..DAYS_IN_WEEK).map { names[it] } + names[1]
    }

    actual fun formatSkeleton(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        skeleton: String,
        locale: String,
    ): String {
        val utc = TimeZone.getTimeZone("UTC")
        val format = DateFormat.getInstanceForSkeleton(skeleton, ULocale.forLanguageTag(locale))
        format.timeZone = utc
        val calendar = Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }
        return format.format(calendar.time)
    }

    actual fun shortDatePattern(locale: String): String =
        DateTimePatternGenerator.getInstance(ULocale.forLanguageTag(locale)).getBestPattern("yMd")

    actual fun amPm(locale: String): List<String> =
        DateFormatSymbols(ULocale.forLanguageTag(locale)).amPmStrings.toList()

    actual fun uses24Hour(locale: String): Boolean =
        'a' !in DateTimePatternGenerator.getInstance(ULocale.forLanguageTag(locale)).getBestPattern("jm")
}
