package com.srctool.camouflage.core.util

import kotlinx.datetime.DayOfWeek
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName

internal actual object CamoLocaleData {
    private const val DAYS_IN_WEEK = 7

    actual fun decimalSeparator(locale: String): Char {
        val formatter = NSNumberFormatter().apply { this.locale = NSLocale(localeIdentifier = locale) }
        return formatter.decimalSeparator.firstOrNull() ?: '.'
    }

    // NSCalendar counts Sunday as 1; ISO counts Monday as 1.
    actual fun firstDayOfWeek(locale: String): DayOfWeek {
        val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
        calendar.locale = NSLocale(localeIdentifier = locale)
        val first = calendar.firstWeekday.toInt()
        return DayOfWeek((first + DAYS_IN_WEEK - 2) % DAYS_IN_WEEK + 1)
    }

    actual fun monthNames(locale: String, short: Boolean): List<String> {
        val formatter = formatter(locale)
        val names = if (short) formatter.shortStandaloneMonthSymbols else formatter.standaloneMonthSymbols
        return names.orEmpty().map { it.toString() }
    }

    // Foundation's weekday arrays start on Sunday.
    actual fun weekdayNames(locale: String, width: NameWidth): List<String> {
        val formatter = formatter(locale)
        val names = when (width) {
            NameWidth.Narrow -> formatter.veryShortStandaloneWeekdaySymbols
            NameWidth.Short -> formatter.shortStandaloneWeekdaySymbols
            NameWidth.Full -> formatter.standaloneWeekdaySymbols
        }.orEmpty().map { it.toString() }
        return names.drop(1) + names.take(1)
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
        val formatter = formatter(locale)
        formatter.dateFormat = NSDateFormatter.dateFormatFromTemplate(skeleton, 0u, NSLocale(localeIdentifier = locale))
            ?: skeleton
        val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
        calendar.timeZone = utc()
        val parts = NSDateComponents().apply {
            this.year = year.toLong()
            this.month = month.toLong()
            this.day = day.toLong()
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }
        return calendar.dateFromComponents(parts)?.let { formatter.stringFromDate(it) }.orEmpty()
    }

    actual fun shortDatePattern(locale: String): String =
        NSDateFormatter.dateFormatFromTemplate("yMd", 0u, NSLocale(localeIdentifier = locale)) ?: "M/d/y"

    actual fun amPm(locale: String): List<String> {
        val formatter = formatter(locale)
        return listOf(formatter.AMSymbol, formatter.PMSymbol)
    }

    // The "j" template is the locale's preferred hour; it carries "a" when that is 12-hour.
    actual fun uses24Hour(locale: String): Boolean =
        NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale(localeIdentifier = locale))?.contains('a') != true

    private fun formatter(locale: String) = NSDateFormatter().apply {
        this.locale = NSLocale(localeIdentifier = locale)
        timeZone = utc()
    }

    private fun utc(): NSTimeZone = NSTimeZone.timeZoneWithName("UTC")!!
}
