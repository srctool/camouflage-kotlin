package com.srctool.camouflage.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.menu.CamoMenuSubmenu
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals

// Nested menus (base CamoMenu, Submenus M16): hover, click and arrow keys open a submenu beside its row.
@OptIn(ExperimentalTestApi::class)
class SubmenuTest {
    private fun ComposeUiTest.camo(direction: LayoutDirection = LayoutDirection.Ltr, content: @Composable () -> Unit) =
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                CamouflageProvider(skinA, themeT, content)
            }
        }

    private fun reset(log: MutableList<String>): List<CamoMenuEntry> = listOf(
        CamoMenuItem("Copy", { log += "copy" }),
        CamoMenuSubmenu(
            "Reset",
            listOf(
                CamoMenuItem("Soft", { log += "soft" }),
                CamoMenuItem("Mixed", { log += "mixed" }),
                CamoMenuSubmenu("Hard", listOf(CamoMenuItem("Really", { log += "really" }))),
            ),
        ),
        CamoMenuItem("Paste", { log += "paste" }),
    )

    private fun ComposeUiTest.show(log: MutableList<String>, direction: LayoutDirection = LayoutDirection.Ltr) =
        camo(direction) { CamoMenuAnchor(items = reset(log)) { open -> CamoButton("More", open) } }

    // Opened with the pointer: no item is focused.
    private fun ComposeUiTest.open(log: MutableList<String>, direction: LayoutDirection = LayoutDirection.Ltr) {
        show(log, direction)
        onNodeWithText("More").performClick()
        waitForIdle()
    }

    // Opened from the keyboard: the first item takes focus.
    private fun ComposeUiTest.openByKeyboard(
        log: MutableList<String>,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        show(log, direction)
        onNodeWithText("More").requestFocus()
        onNodeWithText("More").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
    }

    @Test
    fun aClickOpensTheSubmenuAndALeafClosesEveryLevel() = runComposeUiTest {
        val log = mutableListOf<String>()
        open(log)
        onNodeWithText("Soft").assertDoesNotExist()
        onNodeWithText("Reset").performClick()
        onNodeWithText("Soft").assertExists()
        onNodeWithText("Mixed").performClick()
        waitForIdle()
        assertEquals(listOf("mixed"), log)
        onNodeWithText("Reset").assertDoesNotExist()
        onNodeWithText("Mixed").assertDoesNotExist()
    }

    @Test
    fun hoveringARowOpensItAfterAMoment() = runComposeUiTest {
        val log = mutableListOf<String>()
        open(log)
        mainClock.autoAdvance = false
        onNodeWithText("Reset").performMouseInput { moveTo(center) }
        mainClock.advanceTimeBy(100)
        onNodeWithText("Soft").assertDoesNotExist()
        mainClock.advanceTimeBy(400)
        onNodeWithText("Soft").assertExists()
    }

    @Test
    fun theArrowOpensItAndFocusesTheFirstItemAndTheOtherArrowClosesIt() = runComposeUiTest {
        val log = mutableListOf<String>()
        openByKeyboard(log)
        onNodeWithText("Copy").assertIsFocused()
        onNodeWithText("Copy").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithText("Reset").assertIsFocused()
        onNodeWithText("Reset").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithText("Soft").assertIsFocused()
        onNodeWithText("Soft").performKeyInput { pressKey(Key.DirectionLeft) }
        onNodeWithText("Soft").assertDoesNotExist()
        onNodeWithText("Reset").assertIsFocused()
    }

    @Test
    fun escClosesOnlyTheSubmenuAndEscAgainClosesTheMenu() = runComposeUiTest {
        val log = mutableListOf<String>()
        open(log)
        onNodeWithText("Reset").performClick()
        onNodeWithText("Soft").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Soft").assertDoesNotExist()
        onNodeWithText("Reset").assertExists()
    }

    @Test
    fun aTapOnAParentRowWhileTheSubmenuIsOpenIsNotAnOutsideTap() = runComposeUiTest {
        val log = mutableListOf<String>()
        open(log)
        onNodeWithText("Reset").performClick()
        onNodeWithText("Paste").performClick()
        waitForIdle()
        assertEquals(listOf("paste"), log)
    }

    @Test
    fun theLeftArrowOpensItInRightToLeft() = runComposeUiTest {
        val log = mutableListOf<String>()
        openByKeyboard(log, LayoutDirection.Rtl)
        onNodeWithText("Copy").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithText("Reset").performKeyInput { pressKey(Key.DirectionLeft) }
        onNodeWithText("Soft").assertIsFocused()
    }

    @Test
    fun theRowSaysItCanExpandAndThenCollapse() = runComposeUiTest {
        val log = mutableListOf<String>()
        open(log)
        onNodeWithText("Reset").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand))
        onNodeWithText("Reset").performClick()
        onNodeWithText("Reset").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
    }

    @Test
    fun aSubmenuMayHoldASubmenu() = runComposeUiTest {
        val log = mutableListOf<String>()
        open(log)
        onNodeWithText("Reset").performClick()
        onNodeWithText("Hard").performClick()
        onNodeWithText("Really").performClick()
        waitForIdle()
        assertEquals(listOf("really"), log)
    }
}
