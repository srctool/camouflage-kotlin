@file:Suppress("MagicNumber") // calendar constants: minutes, hours, days and the Gregorian cycle

package com.srctool.camouflage.charts

// A time axis over epoch milliseconds: ticks fall on calendar steps (minutes, hours, days, weeks, months, years) in
// the zone given as a UTC offset. No time library: core and charts stay free of one, and the app passes the offset.

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val DAY = 24 * HOUR
private const val WEEKDAY_SHIFT = 4L // 1970-01-01 was a Thursday, so Monday is day 4 (mod 7)

/** A calendar step between two ticks. */
internal enum class TimeStep(val millis: Long, val months: Int = 0) {
    Minute(MINUTE), FiveMinutes(5 * MINUTE), FifteenMinutes(15 * MINUTE), ThirtyMinutes(30 * MINUTE),
    Hour(HOUR), ThreeHours(3 * HOUR), SixHours(6 * HOUR), TwelveHours(12 * HOUR),
    Day(DAY), TwoDays(2 * DAY), Week(7 * DAY),
    Month(0, 1), Quarter(0, 3), HalfYear(0, 6), Year(0, 12), TwoYears(0, 24), FiveYears(0, 60), TenYears(0, 120),
}

/** The ticks of a time axis and the step between them. */
internal class TimeTicks(val ticks: List<Long>, val step: TimeStep)

/** The days since 1970-01-01 of a civil date (proleptic Gregorian; Howard Hinnant's algorithm). */
internal fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = y.floorDiv(400)
    val yearOfEra = y - era * 400
    val shifted = if (month > 2) month - 3 else month + 9
    val dayOfYear = (153 * shifted + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097L + dayOfEra - 719_468L
}

/** The civil date (year, month 1..12, day 1..31) of [days] since 1970-01-01. */
internal fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val z = days + 719_468L
    val era = z.floorDiv(146_097L)
    val dayOfEra = (z - era * 146_097L).toInt()
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val shifted = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * shifted + 2) / 5 + 1
    val month = if (shifted < 10) shifted + 3 else shifted - 9
    val year = (yearOfEra + era * 400).toInt() + if (month <= 2) 1 else 0
    return Triple(year, month, day)
}

private fun epochMillisOfMonth(year: Int, month: Int): Long = daysFromCivil(year, month, 1) * DAY

/**
 * Ticks for [start]..[end] (epoch milliseconds) at the finest calendar step that gives no more than [maxTicks],
 * as local times at [offsetMinutes] from UTC; the values are still epoch milliseconds.
 */
internal fun timeTicks(start: Long, end: Long, maxTicks: Int, offsetMinutes: Int = 0): TimeTicks {
    require(maxTicks >= 2) { "A time axis needs at least 2 ticks" }
    val offset = offsetMinutes * MINUTE
    val low = minOf(start, end) + offset
    val high = maxOf(start, end) + offset
    var chosen = TimeStep.TenYears
    var ticks = emptyList<Long>()
    for (step in TimeStep.entries) {
        val candidate = ticksAt(low, high, step)
        if (candidate.size <= maxTicks) {
            chosen = step
            ticks = candidate
            break
        }
        chosen = step
        ticks = candidate
    }
    return TimeTicks(ticks.map { it - offset }, chosen)
}

private fun ticksAt(low: Long, high: Long, step: TimeStep): List<Long> {
    if (step.months == 0) {
        val anchor = if (step == TimeStep.Week) WEEKDAY_SHIFT * DAY else 0L
        val first = (low - anchor + step.millis - 1).floorDiv(step.millis) * step.millis + anchor
        return generateSequence(first) { it + step.millis }.takeWhile { it <= high }.toList()
    }
    val (year, month, _) = civilFromDays(low.floorDiv(DAY))
    var monthIndex = year * 12 + (month - 1)
    // Quarters, years and multiples of them start on a multiple of the step: Jan Apr Jul Oct, then 2020 2025 2030.
    if (step.months > 1) monthIndex = monthIndex.floorDiv(step.months) * step.months
    val ticks = ArrayList<Long>()
    while (true) {
        val time = epochMillisOfMonth(monthIndex.floorDiv(12), monthIndex.mod(12) + 1)
        if (time > high) break
        if (time >= low) ticks += time
        monthIndex += step.months
    }
    return ticks
}

private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** A tick's label for [step]: a time of day, a short date, a month (with the year in January) or a year. */
internal fun formatChartTime(epochMillis: Long, step: TimeStep, offsetMinutes: Int = 0): String {
    val local = epochMillis + offsetMinutes * MINUTE
    val (year, month, day) = civilFromDays(local.floorDiv(DAY))
    val minuteOfDay = local.mod(DAY) / MINUTE
    return when {
        step.millis in 1 until DAY -> "${two(minuteOfDay / 60)}:${two(minuteOfDay % 60)}"
        step.millis >= DAY -> "${MonthNames[month - 1]} $day"
        step.months >= 12 -> "$year"
        month == 1 -> "$year"
        else -> MonthNames[month - 1]
    }
}

private fun two(value: Long): String = if (value < 10) "0$value" else "$value"
