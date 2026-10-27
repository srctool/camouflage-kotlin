package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.DebugChecks
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.radio.CamoRadio
import com.srctool.camouflage.core.radio.CamoRadioGroup
import com.srctool.camouflage.core.radio.RadioOption
import com.srctool.camouflage.core.segmented.CamoSegmentedControl
import com.srctool.camouflage.core.segmented.Segment
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.slider.CamoSlider
import com.srctool.camouflage.core.stepper.CamoDecimal
import com.srctool.camouflage.core.stepper.CamoStepper
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

// The batch-1 inputs' tests from their Kotlin notes, under the plain test skin.
@OptIn(ExperimentalTestApi::class)
class InputTest {
    private fun ComposeUiTest.camo(rtl: Boolean = false, content: @Composable () -> Unit) = setContent {
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            CamouflageProvider(skinA, themeT, content)
        }
    }

    private val isError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)

    // CamoCheckbox

    @Test
    fun aLabelledCheckboxIsOneToggleThatTheLabelFlips() = runComposeUiTest {
        var checked by mutableStateOf(false)
        camo { CamoCheckbox(checked, { checked = it }, label = "Remember me") }
        onNodeWithText("Remember me")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .performClick()
            .assertIsOn()
        assertEquals(1, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun aDisplayOnlyOrDisabledCheckboxDoesNotToggle() = runComposeUiTest {
        var changes = 0
        camo {
            Column {
                CamoCheckbox(true, null, label = "Shown")
                CamoCheckbox(false, { changes++ }, label = "Off", enabled = false)
            }
        }
        onNodeWithText("Off").assertIsNotEnabled().performClick()
        waitForIdle()
        assertEquals(0, changes)
        assertTrue(SemanticsActions.OnClick !in onNodeWithText("Shown").fetchSemanticsNode().config)
    }

    @Test
    fun aLinkInTheLabelFiresAndDoesNotToggle() = runComposeUiTest {
        var checked by mutableStateOf(false)
        var opened = 0
        val spans = listOf(CamoSpan("I agree to the "), CamoSpan("Terms", link = { opened++ }))
        camo { CamoCheckbox(checked, { checked = it }, labelSpans = spans) }
        waitForIdle()
        val text = onNodeWithText("I agree to the Terms", useUnmergedTree = true)
        val layout = mutableListOf<TextLayoutResult>()
        text.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layout)
        val terms = layout.single().getBoundingBox("I agree to the T".length)
        text.performTouchInput { click(terms.center) }
        waitForIdle()
        assertEquals(1, opened)
        assertEquals(false, checked)
    }

    @Test
    fun aCheckboxErrorMarksItInvalid() = runComposeUiTest {
        camo { CamoCheckbox(false, {}, label = "Consent", error = "Please accept the terms") }
        onNodeWithText("Consent", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Please accept the terms"))
    }

    // CamoSwitch

    @Test
    fun aSwitchIsAnnouncedAsASwitch() = runComposeUiTest {
        var on by mutableStateOf(false)
        camo { CamoSwitch(on, { on = it }, label = "Wi-Fi") }
        onNodeWithText("Wi-Fi")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()
            .assertIsOn()
    }

    @Test
    fun draggingTheThumbPastTheMiddleTogglesAndAShortDragSpringsBack() = runComposeUiTest {
        var on by mutableStateOf(false)
        camo { CamoSwitch(on, { on = it }, Modifier.testTag("s")) }
        onNodeWithTag("s").performTouchInput { swipeRight(startX = centerX - 12f, endX = centerX + 14f) }
        waitForIdle()
        assertEquals(false, on)
        onNodeWithTag("s").performTouchInput { swipeRight(startX = left + 1f, endX = right - 1f) }
        waitForIdle()
        assertEquals(true, on)
    }

    @Test
    fun inRtlDraggingLeftTurnsASwitchOn() = runComposeUiTest {
        var on by mutableStateOf(false)
        camo(rtl = true) { CamoSwitch(on, { on = it }, Modifier.testTag("s")) }
        onNodeWithTag("s").performTouchInput { swipeLeft(startX = right - 1f, endX = left + 1f) }
        waitForIdle()
        assertEquals(true, on)
    }

    // CamoRadio

    private val speeds = listOf(
        RadioOption("standard", "Standard"),
        RadioOption("express", "Express", enabled = false),
        RadioOption("overnight", "Overnight"),
    )

    @Test
    fun tappingAnOptionSelectsItsValue() = runComposeUiTest {
        var value by mutableStateOf<String?>(null)
        camo { CamoRadioGroup(value, { value = it }, speeds, label = "Shipping") }
        onNodeWithText("Overnight")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performClick()
            .assertIsSelected()
        assertEquals("overnight", value)
    }

    @Test
    fun arrowsMoveSelectAndSkipDisabledOptions() = runComposeUiTest {
        var value by mutableStateOf<String?>("standard")
        camo { CamoRadioGroup(value, { value = it }, speeds) }
        onNodeWithText("Standard").requestFocus()
        onNodeWithText("Standard").performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()
        assertEquals("overnight", value)
        onNodeWithText("Overnight").assertIsFocused()
        onNodeWithText("Overnight").performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()
        assertEquals("standard", value)
    }

    @Test
    fun aGroupErrorMarksTheGroupInvalid() = runComposeUiTest {
        camo { CamoRadioGroup<String>(null, {}, speeds, label = "Shipping", error = "Pick a speed") }
        onNode(isError).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Pick a speed"))
    }

    @Test
    fun aDisplayOnlyRadioHasNoClick() = runComposeUiTest {
        camo { CamoRadio(true, null, label = "Card") }
        waitForIdle()
        assertTrue(onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty())
    }

    // CamoSegmentedControl

    private val periods = listOf(Segment("d", "Day"), Segment("w", "Week"), Segment("m", "A whole month"))

    @Test
    fun segmentsShareTheWidthAndATapSelects() = runComposeUiTest {
        var value by mutableStateOf("d")
        camo { CamoSegmentedControl(periods, value, { value = it }) }
        val day = onNodeWithText("Day").fetchSemanticsNode().size.width
        val month = onNodeWithText("A whole month").fetchSemanticsNode().size.width
        assertEquals(day, month)
        onNodeWithText("Week").performClick().assertIsSelected()
        assertEquals("w", value)
    }

    @Test
    fun arrowsSelectTheNextSegment() = runComposeUiTest {
        var value by mutableStateOf("d")
        camo { CamoSegmentedControl(periods, value, { value = it }) }
        onNodeWithText("Day").requestFocus()
        onNodeWithText("Day").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals("w", value)
    }

    @Test
    fun anIconOnlySegmentWithoutADescriptionFails() {
        assertTrue(DebugChecks.enabled)
        assertFails {
            runComposeUiTest {
                camo { CamoSegmentedControl(listOf(Segment("g", icon = {}), Segment("l", "List")), "g", {}) }
                waitForIdle()
            }
        }
    }

    // CamoSlider

    @Test
    fun aTapJumpsAndSnapsAndFinishesOnce() = runComposeUiTest {
        var value by mutableStateOf(0f)
        var finished = 0
        camo {
            CamoSlider(value, { value = it }, Modifier.width(300.dp).testTag("s"), steps = 3,
                onValueChangeFinished = { finished++ }, label = "Rating")
        }
        onNodeWithTag("s").performTouchInput { click(percentOffset(0.55f, 0.5f)) }
        waitForIdle()
        assertEquals(0.5f, value)
        assertEquals(1, finished)
    }

    @Test
    fun keysAndSetProgressMoveTheValue() = runComposeUiTest {
        var value by mutableStateOf(0.5f)
        camo { CamoSlider(value, { value = it }, Modifier.width(200.dp), steps = 9, label = "Volume") }
        val slider = onNodeWithContentDescription("Volume")
        slider.requestFocus()
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals(0.6f, value, 0.001f)
        slider.performKeyInput { pressKey(Key.MoveHome) }
        waitForIdle()
        assertEquals(0f, value)
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(0.31f) }
        waitForIdle()
        assertEquals(0.3f, value, 0.001f)
    }

    @Test
    fun inRtlTheMinimumIsOnTheRight() = runComposeUiTest {
        var value by mutableStateOf(0.5f)
        camo(rtl = true) { CamoSlider(value, { value = it }, Modifier.width(200.dp).testTag("s")) }
        onNodeWithTag("s").performTouchInput { click(percentOffset(0.95f, 0.5f)) }
        waitForIdle()
        assertTrue(value < 0.1f)
    }

    // CamoStepper

    @Test
    fun theButtonsStepAndDisableAtTheLimits() = runComposeUiTest {
        var count by mutableIntStateOf(1)
        camo { CamoStepper(count, { count = it }, range = 0..2, label = "quantity") }
        onNodeWithContentDescription("Increase quantity").performClick()
        waitForIdle()
        assertEquals(2, count)
        onNodeWithContentDescription("Increase quantity").assertIsNotEnabled()
        onNodeWithContentDescription("Decrease quantity").performClick().performClick()
        waitForIdle()
        assertEquals(0, count)
        onNodeWithContentDescription("Decrease quantity").assertIsNotEnabled()
    }

    @Test
    fun aTypedValueIsClampedAndSnappedWhenFocusLeaves() = runComposeUiTest {
        var count by mutableIntStateOf(2)
        camo {
            Column {
                CamoStepper(count, { count = it }, range = 0..10, step = 2, label = "Guests")
                CamoCheckbox(false, {}, label = "Elsewhere")
            }
        }
        val field = onNodeWithContentDescription("Guests")
        field.requestFocus()
        field.performTextReplacement("7")
        onNodeWithText("Elsewhere").requestFocus()
        waitForIdle()
        assertEquals(8, count)
        field.requestFocus()
        field.performTextReplacement("99")
        onNodeWithText("Elsewhere").requestFocus()
        waitForIdle()
        assertEquals(10, count)
    }

    @Test
    fun decimalStepsNeverDrift() = runComposeUiTest {
        var dose by mutableStateOf(CamoDecimal("0.0"))
        camo {
            CamoStepper(dose, { dose = it }, CamoDecimal("0")..CamoDecimal("1"), CamoDecimal("0.1"), decimals = 1,
                label = "dose")
        }
        repeat(3) { onNodeWithContentDescription("Increase dose").performClick() }
        waitForIdle()
        assertEquals(0, dose.compareTo(CamoDecimal("0.3")))
    }

    @Test
    fun theValueIsItsOwnStopBetweenTheButtons() = runComposeUiTest {
        camo { CamoStepper(3, {}, label = "Quantity", allowTyping = false) }
        onNodeWithContentDescription("Quantity")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        onNodeWithContentDescription("Decrease Quantity")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
}
