package com.srctool.camouflage.core.util

import java.text.DateFormatSymbols
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.datetime.DayOfWeek

internal actual object CamoLocaleData {
    actual fun decimalSeparator(locale: String): Char =
        DecimalFormatSymbols.getInstance(Locale.forLanguageTag(locale)).decimalSeparator

    actual fun firstDayOfWeek(locale: String): DayOfWeek =
        DayOfWeek(WeekFields.of(Locale.forLanguageTag(locale)).firstDayOfWeek.value)

    actual fun monthNames(locale: String, short: Boolean): List<String> {
        val style = if (short) TextStyle.SHORT_STANDALONE else TextStyle.FULL_STANDALONE
        return java.time.Month.entries.map { it.getDisplayName(style, Locale.forLanguageTag(locale)) }
    }

    actual fun weekdayNames(locale: String, width: NameWidth): List<String> {
        val style = when (width) {
            NameWidth.Narrow -> TextStyle.NARROW_STANDALONE
            NameWidth.Short -> TextStyle.SHORT_STANDALONE
            NameWidth.Full -> TextStyle.FULL_STANDALONE
        }
        return java.time.DayOfWeek.entries.map { it.getDisplayName(style, Locale.forLanguageTag(locale)) }
    }

    // The JDK has no skeletons before 19, so core's few are mapped to the nearest localized formats.
    actual fun formatSkeleton(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        skeleton: String,
        locale: String,
    ): String {
        val loc = Locale.forLanguageTag(locale)
        val formatter = when (skeleton) {
            "yMMMM" -> DateTimeFormatter.ofPattern("LLLL yyyy", loc)
            "yMMMd" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(loc)
            "yMMMMEEEEd" -> DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(loc)
            "Hm" -> DateTimeFormatter.ofPattern("HH:mm", loc)
            else -> DateTimeFormatter.ofPattern("h:mm a", loc)
        }
        return formatter.format(LocalDateTime.of(year, month, day, hour, minute))
    }

    actual fun shortDatePattern(locale: String): String = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        FormatStyle.SHORT,
        null,
        IsoChronology.INSTANCE,
        Locale.forLanguageTag(locale),
    )

    actual fun amPm(locale: String): List<String> =
        DateFormatSymbols.getInstance(Locale.forLanguageTag(locale)).amPmStrings.toList()

    actual fun uses24Hour(locale: String): Boolean = 'a' !in DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        null,
        FormatStyle.SHORT,
        IsoChronology.INSTANCE,
        Locale.forLanguageTag(locale),
    )
}
