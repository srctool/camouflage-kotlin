package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.accordion.AccordionTheme
import com.srctool.camouflage.core.accordion.CamoAccordion
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.card.CamoCard
import com.srctool.camouflage.core.card.CardTheme
import com.srctool.camouflage.core.dialog.CamoDialog
import com.srctool.camouflage.core.dialog.CamoDialogFrame
import com.srctool.camouflage.core.dialog.DialogTheme
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.list.CamoListSection
import com.srctool.camouflage.core.list.ListItemTheme
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.topbar.CamoTopBar
import com.srctool.camouflage.core.topbar.TopBarTheme
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.setCamoContent
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

// The M6 structure renderers against the contract: behavior on the whole row, headings drawn, theme fields honored.
@OptIn(ExperimentalTestApi::class)
class FluentStructureRendererTest {
    private val skin = FluentSkin
    private val theme = fluentTheme()

    @Test
    fun aRowAppliesItsBehaviorToTheWholeRow() = runComposeUiTest {
        var taps = 0
        setCamoContent(skin, theme) { CamoListItem("Wi-Fi", Modifier.width(300.dp), onClick = { taps++ }) }
        val row = onNodeWithText("Wi-Fi")
        row.performTouchInput { click(Offset(1f, 1f)) }
        row.performTouchInput { click(Offset(width - 1f, height - 1f)) }
        waitForIdle()
        assertEquals(2, taps)
    }

    @Test
    fun theSectionAndBarTitlesAreHeadings() = runComposeUiTest {
        setCamoContent(skin, theme) {
            androidx.compose.foundation.layout.Column {
                CamoTopBar("Settings")
                CamoListSection(title = "Network") { CamoListItem("Wi-Fi") }
            }
        }
        onNodeWithText("Settings").assert(isHeading())
        onNodeWithText("Network").assert(isHeading())
    }

    @Test
    fun aRowHonorsItsMinHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, ListItemTheme(minHeightOneLine = 61.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(listItem = it)) { CamoListItem("Row") }
        }, measure = { it.size.height == px(61) })
    }

    @Test
    fun theBarHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, TopBarTheme(height = 77.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(topBar = it)) { CamoTopBar("Title") }
        }, measure = { it.size.height == px(77) })
    }

    @Test
    fun aClosedAccordionIsItsHeaderHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, AccordionTheme(headerMinHeight = 67.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(accordion = it)) {
                CamoAccordion("Shipping", false, {}) { CamoText("Hidden") }
            }
        }, measure = { it.size.height == px(67) })
    }

    @Test
    fun aCardHonorsItsPadding() = runComposeUiTest {
        assertNoLiterals(skin, theme, CardTheme(contentPadding = PaddingValues(13.dp)), render = {
            ProvideComponentThemes(CamoComponentThemes(card = it)) { CamoCard { Box(Modifier.size(10.dp)) } }
        }, measure = { it.size.width == px(36) })
    }

    @Test
    fun theCloseButtonShowsOnlyWhenDismissibleAndAsked() = runComposeUiTest {
        var dismissed = 0
        var dismissible by mutableStateOf(true)
        setCamoContent(skin, theme) {
            ProvideComponentThemes(CamoComponentThemes(dialog = DialogTheme(showCloseButton = true))) {
                CamoDialogFrame({ dismissed++ }, dismissible = dismissible) {
                    CamoDialog("Saved.", confirm = { CamoButton("OK", {}) }, title = "Done")
                }
            }
        }
        onNodeWithContentDescription("Close").performClick()
        waitForIdle()
        assertEquals(1, dismissed)
        dismissible = false
        waitForIdle()
        onNodeWithContentDescription("Close").assertDoesNotExist()
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
