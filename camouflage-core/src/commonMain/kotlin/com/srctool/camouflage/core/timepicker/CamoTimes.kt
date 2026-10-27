package com.srctool.camouflage.core.timepicker

import com.srctool.camouflage.core.util.CamoLocaleData
import kotlinx.datetime.LocalTime

/** A wall-clock time: hour and minute, with no date or time zone. */
public typealias CamoTime = LocalTime

/** Before or after noon, in 12-hour times. */
public enum class DayPeriod { Am, Pm }

/** The part of a time being changed. */
public enum class TimePart { Hour, Minute }

/** The picker's form: typed hour and minute fields, a clock dial, or hour · minute · period wheels. */
public enum class TimePickerForm { Fields, Dial, Wheel }

private const val HALF_DAY = 12
private const val MINUTES_PER_HOUR = 60

// A time is formatted on any date; the date doesn't show.
private const val ANY_YEAR = 2000

/** The hour on a 12-hour clock: 0 → 12, 13 → 1. */
internal fun hour12(hour: Int): Int = (hour % HALF_DAY).let { if (it == 0) HALF_DAY else it }

internal fun periodOf(hour: Int): DayPeriod = if (hour < HALF_DAY) DayPeriod.Am else DayPeriod.Pm

/** The 24-hour hour for [hour12] (1–12) in [period]. */
internal fun hour24(hour12: Int, period: DayPeriod): Int =
    hour12 % HALF_DAY + if (period == DayPeriod.Pm) HALF_DAY else 0

/** [minute] on the nearest multiple of [step] that stays within the hour (58 with step 15 → 45). */
internal fun snapMinute(minute: Int, step: Int): Int {
    if (step <= 1) return minute
    val nearest = (minute + step / 2) / step * step
    return if (nearest >= MINUTES_PER_HOUR) nearest - step else nearest
}

/** Whether [time] is in the window; a window with [min] after [max] runs past midnight. */
internal fun inWindow(time: LocalTime, min: LocalTime?, max: LocalTime?): Boolean = when {
    min == null && max == null -> true
    min == null -> time <= max!!
    max == null -> time >= min
    min <= max -> time in min..max
    else -> time >= min || time <= max
}

/** [time] in the locale's short time, in 24 or 12 hours. */
internal fun formatTime(time: LocalTime, use24Hour: Boolean, locale: String): String =
    CamoLocaleData.formatSkeleton(ANY_YEAR, 1, 1, time.hour, time.minute, if (use24Hour) "Hm" else "hm", locale)

/** [time] moved by [hours] and [minutes], wrapping around the day. */
internal fun LocalTime.shift(hours: Int = 0, minutes: Int = 0): LocalTime {
    val day = HALF_DAY * 2 * MINUTES_PER_HOUR
    val total = ((hour * MINUTES_PER_HOUR + minute + hours * MINUTES_PER_HOUR + minutes) % day + day) % day
    return LocalTime(total / MINUTES_PER_HOUR, total % MINUTES_PER_HOUR)
}
