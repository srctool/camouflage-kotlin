package com.srctool.camouflage.skin.minimal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datepicker.CalendarSelection
import com.srctool.camouflage.core.datepicker.CamoCalendar
import com.srctool.camouflage.core.datepicker.DatePickerTheme
import com.srctool.camouflage.core.menu.CamoMenuPanel
import com.srctool.camouflage.core.menu.MenuTheme
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.timepicker.CamoTimeInput
import com.srctool.camouflage.core.timepicker.TimePickerTheme
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.setCamoContent
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth

// The M5 pickers and overlay panel against the contract: a day's behavior on its whole cell, theme fields honored.
@OptIn(ExperimentalTestApi::class)
class MinimalPickerRendererTest {
    private val skin = MinimalSkin
    private val theme = minimalLight

    @Test
    fun aDayCellAppliesItsBehaviorToTheWholeCell() = runComposeUiTest {
        var picks = 0
        setCamoContent(skin, theme) {
            CamoCalendar(CalendarSelection.Single(null), { picks++ }, YearMonth(2024, 3), {})
        }
        val day = onNode(hasClickAction() and hasContentDescription("14", substring = true))
        day.performTouchInput { click(Offset(1f, 1f)) }
        day.performTouchInput { click(center) }
        waitForIdle()
        assertEquals(2, picks)
    }

    @Test
    fun theCalendarHonorsDaySize() = runComposeUiTest {
        assertNoLiterals(skin, theme, DatePickerTheme(daySize = 50.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(datePicker = it)) {
                CamoCalendar(CalendarSelection.Single(null), {}, YearMonth(2024, 3), {})
            }
        }, measure = { it.size.width == px(350) })
    }

    @Test
    fun theTimeBoxesHonorFieldSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, TimePickerTheme(fieldSize = 77.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(timePicker = it)) {
                CamoTimeInput(LocalTime(9, 0), {}, use24Hour = true)
            }
        }, measure = { it.size.height == px(77) })
    }

    @Test
    fun thePopupPanelHonorsMinWidth() = runComposeUiTest {
        assertNoLiterals(skin, theme, MenuTheme(minWidth = 333.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(menu = it)) { CamoMenuPanel { CamoText("Hi") } }
        }, measure = { it.size.width == px(333) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
