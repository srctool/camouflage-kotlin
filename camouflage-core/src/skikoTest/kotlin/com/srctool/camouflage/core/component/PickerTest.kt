package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.bottomsheet.CamoSheetFrame
import com.srctool.camouflage.core.datepicker.AllowTyping
import com.srctool.camouflage.core.datepicker.CalendarSelection
import com.srctool.camouflage.core.datepicker.CamoCalendar
import com.srctool.camouflage.core.datepicker.CamoDateField
import com.srctool.camouflage.core.datepicker.DatePickerTheme
import com.srctool.camouflage.core.datepicker.PickerPresentation
import com.srctool.camouflage.core.datepicker.PickerPresentation.Popup
import com.srctool.camouflage.core.datepicker.typingPattern
import com.srctool.camouflage.core.form.CamoDateFormField
import com.srctool.camouflage.core.form.CamoForm
import com.srctool.camouflage.core.form.CamoFormState
import com.srctool.camouflage.core.form.CamoRules
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.timepicker.CamoTimeField
import com.srctool.camouflage.core.timepicker.CamoTimeInput
import com.srctool.camouflage.core.util.CamoLocaleData
import com.srctool.camouflage.core.util.currentLocaleTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

// The batch-3 pickers' tests from their Kotlin notes: the calendar, the date and time fields and the sheet frame.
@OptIn(ExperimentalTestApi::class)
class PickerTest {
    private fun ComposeUiTest.camo(theme: CamoTheme = themeT, content: @Composable () -> Unit) =
        setContent { CamouflageProvider(skinA, theme, content) }

    // A day cell is named by its full date in the current locale.
    private fun dayName(date: LocalDate): String =
        CamoLocaleData.formatSkeleton(date.year, date.month.number, date.day, 0, 0, "yMMMMEEEEd", currentLocaleTag())

    private fun ComposeUiTest.day(date: LocalDate) = onNodeWithContentDescription(dayName(date))

    private val october = YearMonth(2026, 10)

    private fun announces(state: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

    private fun invalid(message: String) = SemanticsMatcher.expectValue(SemanticsProperties.Error, message)

    // CamoCalendar

    @Test
    fun aTapSelectsADayAndDaysOutsideTheLimitsAreDisabled() = runComposeUiTest {
        var selection: CalendarSelection by mutableStateOf(CalendarSelection.Single(null))
        camo {
            CamoCalendar(selection, { selection = it }, october, {}, minDate = LocalDate(2026, 10, 5))
        }
        day(LocalDate(2026, 10, 14)).performClick()
        assertEquals(CalendarSelection.Single(LocalDate(2026, 10, 14)), selection)
        day(LocalDate(2026, 10, 14)).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        day(LocalDate(2026, 10, 4)).assertIsNotEnabled()
    }

    @Test
    fun aRangeIsPickedStartThenEndAndTheEndsAreAnnounced() = runComposeUiTest {
        var selection: CalendarSelection by mutableStateOf(CalendarSelection.Range(null, null))
        camo { CamoCalendar(selection, { selection = it }, october, {}) }
        day(LocalDate(2026, 10, 10)).performClick()
        day(LocalDate(2026, 10, 14)).performClick()
        assertEquals(CalendarSelection.Range(LocalDate(2026, 10, 10), LocalDate(2026, 10, 14)), selection)
        day(LocalDate(2026, 10, 10)).assert(announces("start of range"))
        day(LocalDate(2026, 10, 12)).assert(announces("in range"))
    }

    @Test
    fun arrowsMoveTheFocusedDayAcrossMonthsAndEnterSelects() = runComposeUiTest {
        var month by mutableStateOf(october)
        var selection: CalendarSelection by mutableStateOf(CalendarSelection.Single(LocalDate(2026, 10, 30)))
        camo { CamoCalendar(selection, { selection = it }, month, { month = it }) }
        day(LocalDate(2026, 10, 30)).requestFocus()
        day(LocalDate(2026, 10, 30)).performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        day(LocalDate(2026, 10, 31)).assertIsFocused()
        day(LocalDate(2026, 10, 31)).performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()
        assertEquals(YearMonth(2026, 11), month)
        day(LocalDate(2026, 11, 7)).assertIsFocused()
        day(LocalDate(2026, 11, 7)).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(CalendarSelection.Single(LocalDate(2026, 11, 7)), selection)
    }

    // CamoDateField

    @Test
    fun aDateFieldConfirmsWithOk() = runComposeUiTest {
        var value by mutableStateOf<LocalDate?>(LocalDate(2026, 10, 1))
        camo { CamoDateField(value, { value = it }, label = "Birth date", presentation = PickerPresentation.Popup) }
        onNodeWithContentDescription("Birth date").performClick()
        day(LocalDate(2026, 10, 14)).performClick()
        assertEquals(LocalDate(2026, 10, 1), value)
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(LocalDate(2026, 10, 14), value)
        day(LocalDate(2026, 10, 14)).assertDoesNotExist()
    }

    @Test
    fun cancelAndEscCloseTheSheetWithoutAChange() = runComposeUiTest {
        var value by mutableStateOf<LocalDate?>(null)
        camo { CamoDateField(value, { value = it }, label = "Delivery", presentation = PickerPresentation.Sheet) }
        onNodeWithContentDescription("Delivery").performClick()
        waitForIdle()
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithContentDescription("Delivery").performClick()
        waitForIdle()
        onNodeWithText("Cancel").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithText("Cancel").assertDoesNotExist()
        assertNull(value)
    }

    @Test
    fun typingAValidDateSetsItAndAnInvalidOneShowsTheError() = runComposeUiTest {
        var value by mutableStateOf<LocalDate?>(null)
        val always = DatePickerTheme(allowTyping = AllowTyping.Always)
        val typing = themeT.copy(components = CamoComponentThemes(datePicker = always))
        camo(typing) { CamoDateField(value, { value = it }, label = "Due") }
        val pattern = typingPattern(CamoLocaleData.shortDatePattern(currentLocaleTag()))
        fun digits(day: String, month: String, year: String) = pattern.filter { it.isLetter() }.toList().distinct()
            .joinToString("") { if (it == 'd') day else if (it == 'M') month else year }
        onNodeWithText("Due").performTextInput(digits("14", "10", "2026"))
        waitForIdle()
        assertEquals(LocalDate(2026, 10, 14), value)
        onNodeWithText("Due").performTextReplacement(digits("31", "02", "2026"))
        waitForIdle()
        assertEquals(LocalDate(2026, 10, 14), value)
        onNodeWithText("Due").assert(invalid("Enter a valid date"))
    }

    @Test
    fun aRequiredDateFormFieldFailsUntilPicked() = runComposeUiTest {
        val form = CamoFormState()
        camo {
            CamoForm(form) {
                Column {
                    CamoDateFormField("start", label = "Start", rules = listOf(CamoRules.required("Pick a date")),
                        presentation = PickerPresentation.Popup)
                }
            }
        }
        runOnIdle { assertEquals(false, form.validate()) }
        waitForIdle()
        onNodeWithContentDescription("Start").assert(invalid("Pick a date"))
    }

    // CamoTimeField and CamoTimeInput

    @Test
    fun aTimeFieldTakesTypedPartsAndConfirms() = runComposeUiTest {
        var value by mutableStateOf<LocalTime?>(null)
        camo {
            CamoTimeField(value, { value = it }, label = "Pickup", use24Hour = true, presentation = Popup)
        }
        onNodeWithContentDescription("Pickup").performClick()
        onNodeWithContentDescription("Hour").performTextReplacement("14")
        onNodeWithContentDescription("Minute").performTextReplacement("30")
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(LocalTime(14, 30), value)
    }

    @Test
    fun minutesSnapToTheStepWhenLeftAndArrowsStep() = runComposeUiTest {
        var value by mutableStateOf<LocalTime?>(LocalTime(10, 0))
        camo { CamoTimeInput(value, { value = it }, minuteStep = 15, use24Hour = true) }
        onNodeWithContentDescription("Minute").performTextReplacement("08")
        waitForIdle()
        assertEquals(LocalTime(10, 8), value)
        onNodeWithContentDescription("Hour").requestFocus()
        waitForIdle()
        assertEquals(LocalTime(10, 15), value)
        onNodeWithContentDescription("Hour").performKeyInput { pressKey(Key.DirectionUp) }
        waitForIdle()
        assertEquals(LocalTime(11, 15), value)
    }

    // CamoSheetFrame

    @Test
    fun escAndTheDismissActionCloseADismissibleSheetOnly() = runComposeUiTest {
        var dismissed = 0
        var dismissible by mutableStateOf(true)
        camo {
            CamoSheetFrame({ dismissed++ }, dismissible = dismissible) {
                CamoBottomSheet(title = "Filters") { CamoTimeInput(LocalTime(9, 0), {}) }
            }
        }
        waitForIdle()
        onNodeWithContentDescription("Hour").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(1, dismissed)
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)).performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertEquals(2, dismissed)
        dismissible = false
        waitForIdle()
        onNodeWithContentDescription("Hour").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(2, dismissed)
    }
}
