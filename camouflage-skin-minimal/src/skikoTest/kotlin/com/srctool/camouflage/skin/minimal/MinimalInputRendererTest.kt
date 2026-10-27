package com.srctool.camouflage.skin.minimal

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.checkbox.CheckboxTheme
import com.srctool.camouflage.core.radio.CamoRadio
import com.srctool.camouflage.core.radio.RadioTheme
import com.srctool.camouflage.core.segmented.CamoSegmentedControl
import com.srctool.camouflage.core.segmented.Segment
import com.srctool.camouflage.core.segmented.SegmentedControlTheme
import com.srctool.camouflage.core.slider.CamoSlider
import com.srctool.camouflage.core.slider.SliderTheme
import com.srctool.camouflage.core.stepper.CamoStepper
import com.srctool.camouflage.core.stepper.StepperLayout
import com.srctool.camouflage.core.stepper.StepperTheme
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.switch.SwitchTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.assertRendererAppliesBehavior
import com.srctool.camouflage.skin.testing.setCamoContent
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

// The M5 selection controls, slider and stepper against the contract: behavior outermost, theme fields honored.
@OptIn(ExperimentalTestApi::class)
class MinimalInputRendererTest {
    private val skin = MinimalSkin
    private val theme = minimalLight

    @Test
    fun checkboxAppliesBehavior() = runComposeUiTest {
        assertRendererAppliesBehavior(skin, theme) { onClick -> CamoCheckbox(false, { onClick() }, label = "Remember") }
    }

    @Test
    fun switchAppliesBehavior() = runComposeUiTest {
        assertRendererAppliesBehavior(skin, theme) { onClick -> CamoSwitch(false, { onClick() }, label = "Wi-Fi") }
    }

    @Test
    fun radioAppliesBehavior() = runComposeUiTest {
        assertRendererAppliesBehavior(skin, theme) { onClick -> CamoRadio(false, onClick, label = "Card") }
    }

    // A segment sits inside the container's track, so the check is on the segment's own box: its corner clicks.
    @Test
    fun segmentAppliesBehavior() = runComposeUiTest {
        var clicks = 0
        setCamoContent(skin, theme) {
            CamoSegmentedControl(listOf(Segment(1, "One"), Segment(2, "Two")), 1, { clicks++ })
        }
        onNodeWithText("Two").performTouchInput { click(Offset(1f, 1f)) }
        onNodeWithText("Two").performTouchInput { click(Offset(width - 2f, height - 2f)) }
        waitForIdle()
        assertEquals(2, clicks)
    }

    @Test
    fun checkboxHonorsBoxSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, CheckboxTheme(boxSize = 77.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(checkbox = it)) { CamoCheckbox(true, {}) }
        }, measure = { it.size.width == px(77) })
    }

    @Test
    fun switchHonorsTrackSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, SwitchTheme(trackWidth = 90.dp, trackHeight = 60.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(switch = it)) { CamoSwitch(true, {}) }
        }, measure = { it.size.width == px(90) && it.size.height == px(60) })
    }

    @Test
    fun radioHonorsSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, RadioTheme(size = 70.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(radio = it)) { CamoRadio(true, {}) }
        }, measure = { it.size.width == px(70) })
    }

    @Test
    fun segmentedControlHonorsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, SegmentedControlTheme(height = 70.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(segmentedControl = it)) {
                CamoSegmentedControl(listOf(Segment(1, "One"), Segment(2, "Two")), 1, {})
            }
        }, measure = { it.size.height == px(70) })
    }

    @Test
    fun sliderHonorsThumbSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, SliderTheme(thumbSize = DpSize(60.dp, 60.dp)), render = {
            ProvideComponentThemes(CamoComponentThemes(slider = it)) { CamoSlider(0.5f, {}, Modifier.width(200.dp)) }
        }, measure = { it.size.height == px(60) })
    }

    @Test
    fun stepperHonorsHeightInBothLayouts() {
        for (layout in StepperLayout.entries) runComposeUiTest {
            assertNoLiterals(skin, theme, StepperTheme(layout = layout, height = 70.dp), render = {
                ProvideComponentThemes(CamoComponentThemes(stepper = it)) { CamoStepper(2, {}, label = "Qty") }
            }, measure = { it.size.height == px(70) })
        }
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
