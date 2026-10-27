package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.datepicker.CalendarSelection
import com.srctool.camouflage.core.datepicker.CamoCalendar
import com.srctool.camouflage.core.datepicker.CamoDateField
import com.srctool.camouflage.core.datepicker.CamoDateRange
import com.srctool.camouflage.core.datepicker.CamoDateRangeField
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.timepicker.CamoTimeField
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plusMonth
import kotlinx.datetime.todayIn
import kotlinx.datetime.yearMonth

private const val MAX_NIGHTS = 14
private const val SLOT_MINUTES = 15

/** The base notes' date and time samples: a birth date, weekday delivery, a hotel stay, two months, two times. */
@Composable
internal fun DatesAndTimes() {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var birth by remember { mutableStateOf<LocalDate?>(null) }
    var delivery by remember { mutableStateOf<LocalDate?>(null) }
    var stay by remember { mutableStateOf<CamoDateRange?>(null) }
    var reminder by remember { mutableStateOf<LocalTime?>(null) }
    var pickup by remember { mutableStateOf<LocalTime?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CamoDateField(birth, { birth = it }, Modifier.fillMaxWidth(), label = "Birth date", maxDate = today)
        CamoDateField(
            delivery,
            { delivery = it },
            Modifier.fillMaxWidth(),
            label = "Delivery",
            supportingText = "Weekdays only",
            minDate = today,
            isSelectable = { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY },
        )
        CamoDateRangeField(
            stay,
            { stay = it },
            Modifier.fillMaxWidth(),
            label = "Hotel stay",
            supportingText = "Up to $MAX_NIGHTS nights",
            minDate = today,
            maxLength = MAX_NIGHTS,
        )
        CamoTimeField(reminder, { reminder = it }, Modifier.fillMaxWidth(), label = "Reminder")
        CamoTimeField(
            pickup,
            { pickup = it },
            Modifier.fillMaxWidth(),
            label = "Pickup slot",
            supportingText = "Every 15 minutes, 09:00–17:00",
            minuteStep = SLOT_MINUTES,
            minTime = LocalTime.parse("09:00"),
            maxTime = LocalTime.parse("17:00"),
        )
        TwoMonths(today)
    }
}

// An inline calendar showing two months side by side, sharing one range selection.
@Composable
private fun TwoMonths(today: LocalDate) {
    var selection: CalendarSelection by remember { mutableStateOf(CalendarSelection.Range(null, null)) }
    var first by remember { mutableStateOf(today.yearMonth) }
    CamoText("Two months", style = Camouflage.typography.titleSmall)
    val gap = Arrangement.spacedBy(Camouflage.spacing.l)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = gap) {
        CamoCalendar(selection, { selection = it }, first, { first = it }, minDate = today)
        CamoCalendar(selection, { selection = it }, first.plusMonth(), { first = it.minusMonth() }, minDate = today)
    }
}
