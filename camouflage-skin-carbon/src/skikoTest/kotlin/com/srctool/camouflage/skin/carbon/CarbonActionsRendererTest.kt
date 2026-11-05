package com.srctool.camouflage.skin.carbon

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.buttongroup.ButtonGroupTheme
import com.srctool.camouflage.core.buttongroup.CamoButtonGroup
import com.srctool.camouflage.core.buttongroup.CamoGroupButton
import com.srctool.camouflage.core.splitbutton.CamoSplitButton
import com.srctool.camouflage.core.splitbutton.SplitButtonTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.toolbar.CamoToolbar
import com.srctool.camouflage.core.toolbar.CamoToolbarAction
import com.srctool.camouflage.core.toolbar.ToolbarTheme
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import kotlin.math.roundToInt
import kotlin.test.Test

// The M9 button group, split button and toolbar renderers against the contract: theme fields honored.
@OptIn(ExperimentalTestApi::class)
class CarbonActionsRendererTest {
    private val skin = CarbonSkin
    private val theme = carbonLight

    @Test
    fun aButtonGroupHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, ButtonGroupTheme(height = 61.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(buttonGroup = it)) {
                CamoButtonGroup(listOf(CamoGroupButton("a", {}, text = "A"), CamoGroupButton("b", {}, text = "B")))
            }
        }, measure = { it.size.height == px(61) })
    }

    @Test
    fun aSplitButtonHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, SplitButtonTheme(height = 57.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(splitButton = it)) { CamoSplitButton("Save", {}, emptyList()) }
        }, measure = { it.size.height == px(57) })
    }

    @Test
    fun aToolbarHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, ToolbarTheme(height = 71.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(toolbar = it)) {
                CamoToolbar(listOf(CamoToolbarAction("a", "A", { BasicText("A") }, {})), Modifier)
            }
        }, measure = { it.size.height == px(71) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
