@file:Suppress("MatchingDeclarationName", "UnusedParameter") // the actual object plus its JavaScript helpers, whose
// parameters are read by the JavaScript

package com.srctool.camouflage.core.util

import kotlinx.datetime.DayOfWeek

internal actual object CamoLocaleData {
    actual fun decimalSeparator(locale: String): Char = jsDecimalSeparator(locale).firstOrNull() ?: '.'

    actual fun firstDayOfWeek(locale: String): DayOfWeek = DayOfWeek(jsFirstDay(locale))

    actual fun monthNames(locale: String, short: Boolean): List<String> =
        jsMonthNames(locale, if (short) "short" else "long").split(SEPARATOR)

    actual fun weekdayNames(locale: String, width: NameWidth): List<String> {
        val js = when (width) {
            NameWidth.Narrow -> "narrow"
            NameWidth.Short -> "short"
            NameWidth.Full -> "long"
        }
        return jsWeekdayNames(locale, js).split(SEPARATOR)
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
        val options = when (skeleton) {
            "yMMMM" -> """{"year":"numeric","month":"long"}"""
            "yMMMd" -> """{"year":"numeric","month":"short","day":"numeric"}"""
            "yMMMMEEEEd" -> """{"weekday":"long","year":"numeric","month":"long","day":"numeric"}"""
            "Hm" -> """{"hour":"2-digit","minute":"2-digit","hourCycle":"h23"}"""
            else -> """{"hour":"numeric","minute":"2-digit","hourCycle":"h12"}"""
        }
        return jsFormat(year, month, day, hour, minute, options, locale)
    }

    actual fun shortDatePattern(locale: String): String = jsShortPattern(locale)

    actual fun amPm(locale: String): List<String> = jsAmPm(locale).split(SEPARATOR)

    actual fun uses24Hour(locale: String): Boolean = jsUses24Hour(locale)

    private const val SEPARATOR = "\u0000"
}

private fun jsDecimalSeparator(locale: String): String = js(
    "(new Intl.NumberFormat(locale).formatToParts(1.5)" +
        ".find(function (p) { return p.type === 'decimal' }) || { value: '.' }).value",
)

// Intl.Locale's week info (1 = Monday … 7 = Sunday); Monday where the browser doesn't have it.
private fun jsFirstDay(locale: String): Int = js(
    "(function () { try { var l = new Intl.Locale(locale);" +
        " var w = l.getWeekInfo ? l.getWeekInfo() : l.weekInfo;" +
        " return w ? w.firstDay : 1 } catch (e) { return 1 } })()",
)

private fun jsMonthNames(locale: String, width: String): String = js(
    "(function () { var f = new Intl.DateTimeFormat(locale, { month: width, timeZone: 'UTC' }); var r = [];" +
        " for (var m = 0; m < 12; m++) r.push(f.format(new Date(Date.UTC(2021, m, 1)))); return r.join('\\u0000') })()",
)

// 2024-01-01 was a Monday.
private fun jsWeekdayNames(locale: String, width: String): String = js(
    "(function () { var f = new Intl.DateTimeFormat(locale, { weekday: width, timeZone: 'UTC' }); var r = [];" +
        " for (var d = 1; d <= 7; d++) r.push(f.format(new Date(Date.UTC(2024, 0, d)))); return r.join('\\u0000') })()",
)

private fun jsFormat(year: Int, month: Int, day: Int, hour: Int, minute: Int, options: String, locale: String): String =
    js(
        "(function () { var o = JSON.parse(options); o.timeZone = 'UTC';" +
            " var d = new Date(Date.UTC(2000, 0, 1, hour, minute)); d.setUTCFullYear(year, month - 1, day);" +
            " return new Intl.DateTimeFormat(locale, o).format(d) })()",
    )

// The numeric date's parts in the locale's order, as a pattern ("MM/dd/yyyy").
private fun jsShortPattern(locale: String): String = js(
    "new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'numeric', day: 'numeric', timeZone: 'UTC' })" +
        ".formatToParts(new Date(Date.UTC(2033, 10, 22))).map(function (p) {" +
        " return p.type === 'day' ? 'd' : p.type === 'month' ? 'M' : p.type === 'year' ? 'y' : p.value }).join('')",
)

private fun jsAmPm(locale: String): String = js(
    "(function () { var f = new Intl.DateTimeFormat(locale, { hour: 'numeric', hourCycle: 'h12', timeZone: 'UTC' });" +
        " function p(h) { return (f.formatToParts(new Date(Date.UTC(2024, 0, 1, h))).find(function (x) {" +
        " return x.type === 'dayPeriod' }) || { value: h < 12 ? 'AM' : 'PM' }).value }" +
        " return p(9) + '\\u0000' + p(21) })()",
)

private fun jsUses24Hour(locale: String): Boolean = js(
    "(function () { var c = new Intl.DateTimeFormat(locale, { hour: 'numeric' }).resolvedOptions().hourCycle;" +
        " return c === 'h23' || c === 'h24' })()",
)
