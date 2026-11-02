package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.buttongroup.CamoButtonGroup
import com.srctool.camouflage.core.buttongroup.CamoGroupButton
import com.srctool.camouflage.core.groupButtons
import com.srctool.camouflage.core.lastSplit
import com.srctool.camouflage.core.lastToolbar
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.splitbutton.CamoSplitButton
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.toolbar.CamoToolbar
import com.srctool.camouflage.core.toolbar.CamoToolbarAction
import com.srctool.camouflage.core.toolbar.CamoToolbarContainer
import com.srctool.camouflage.core.toolbar.ToolbarPlacement
import com.srctool.camouflage.core.toolbar.fitActions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The M9 button group, split button and toolbar tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class ToolbarGroupsTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private fun action(label: String, log: MutableList<String>, selected: Boolean? = null, enabled: Boolean = true) =
        CamoToolbarAction(label, label, { BasicText(label.take(1)) }, { log += label }, enabled, selected)

    // --- button group ---

    @Test
    fun theButtonsOfAGroupRunTheirCommands() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo {
            CamoButtonGroup(listOf("Cut", "Copy", "Paste").map { CamoGroupButton(it, { log += it }, text = it) })
        }
        onNodeWithText("Copy").performClick()
        onNodeWithText("Paste").performClick()
        assertEquals(listOf("Copy", "Paste"), log)
    }

    @Test
    fun aToggleKeepsTheAppsSelectedState() = runComposeUiTest {
        var bold by mutableStateOf(false)
        camo {
            CamoButtonGroup(
                listOf(
                    CamoGroupButton("b", { bold = !bold }, text = "Bold", selected = bold),
                    CamoGroupButton("i", {}, text = "Italic", selected = false),
                ),
            )
        }
        onNodeWithText("Bold").assertIsOff()
        onNodeWithText("Bold").performClick()
        onNodeWithText("Bold").assertIsOn()
        assertEquals(true, groupButtons[0]?.selected)
    }

    @Test
    fun eachButtonKnowsItsPositionInTheGroup() = runComposeUiTest {
        camo {
            CamoButtonGroup(listOf("A", "B", "C").map { CamoGroupButton(it, {}, text = it) })
        }
        waitForIdle()
        assertEquals(listOf(true, false, false), (0..2).map { groupButtons.getValue(it).position.isFirst })
        assertEquals(listOf(false, false, true), (0..2).map { groupButtons.getValue(it).position.isLast })
        assertEquals(3, groupButtons.getValue(0).position.count)
    }

    @Test
    fun arrowsMoveBetweenTheButtonsWithoutPressingThem() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo {
            CamoButtonGroup(
                listOf(
                    CamoGroupButton("a", { log += "a" }, text = "A"),
                    CamoGroupButton("b", { log += "b" }, text = "B", enabled = false),
                    CamoGroupButton("c", { log += "c" }, text = "C"),
                ),
            )
        }
        onNodeWithText("A").requestFocus()
        onNodeWithText("A").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        onNodeWithText("C").assertIsFocused()
        onNodeWithText("C").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        onNodeWithText("A").assertIsFocused()
        assertTrue(log.isEmpty())
    }

    @Test
    fun aDisabledGroupDisablesEveryButton() = runComposeUiTest {
        camo { CamoButtonGroup(listOf(CamoGroupButton("a", {}, text = "A")), enabled = false) }
        onNodeWithText("A").assertIsNotEnabled()
    }

    @Test
    fun anIconOnlyButtonNeedsADescription() = runComposeUiTest {
        assertFailsWith<IllegalStateException> {
            camo { CamoButtonGroup(listOf(CamoGroupButton("a", {}, icon = { BasicText("*") }))) }
            waitForIdle()
        }
    }

    // --- split button ---

    private val saveAs = listOf(CamoMenuItem("Save as", {}), CamoMenuItem("Save a copy", {}))

    @Test
    fun theMainPartRunsTheActionAndTheArrowOpensTheMenu() = runComposeUiTest {
        var saved = 0
        val log = mutableListOf<String>()
        camo {
            CamoSplitButton("Save", { saved++ }, listOf(CamoMenuItem("Save as", { log += "as" })))
        }
        onNodeWithText("Save as").assertDoesNotExist()
        onNodeWithText("Save").performClick()
        assertEquals(1, saved)
        onNodeWithContentDescription("More options").performClick()
        onNodeWithText("Save as").performClick()
        waitForIdle()
        assertEquals(listOf("as"), log)
        assertEquals(1, saved)
    }

    @Test
    fun aMenuButtonOpensItsMenuFromTheWholeButton() = runComposeUiTest {
        camo { CamoSplitButton("Sort", null, listOf(CamoMenuItem("Newest", {}))) }
        assertEquals(false, lastSplit?.hasAction)
        onNodeWithText("Sort").performClick()
        onNodeWithText("Newest").assertExists()
    }

    @Test
    fun theArrowAnnouncesWhetherTheMenuIsOpen() = runComposeUiTest {
        camo { CamoSplitButton("Save", {}, saveAs) }
        val arrow = onNodeWithContentDescription("More options")
        arrow.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        arrow.performClick()
        waitForIdle()
        onNodeWithContentDescription("More options")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        assertEquals(true, lastSplit?.expanded)
    }

    @Test
    fun downOnEitherPartOpensTheMenu() = runComposeUiTest {
        camo { CamoSplitButton("Save", {}, saveAs) }
        onNodeWithText("Save").requestFocus()
        onNodeWithText("Save").performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()
        onNodeWithText("Save a copy").assertExists()
    }

    @Test
    fun aDisabledSplitButtonDisablesBothParts() = runComposeUiTest {
        camo { CamoSplitButton("Save", {}, saveAs, enabled = false) }
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithContentDescription("More options").assertIsNotEnabled()
    }

    // --- toolbar ---

    @Test
    fun fittingKeepsAllActionsWhenTheyFitAndLeavesRoomForTheOverflowWhenNot() {
        assertEquals(5, fitActions(5, 5 * 48 + 4 * 4, 48, 4))
        // One short of all five: four actions would leave no room for the overflow button, so three show.
        assertEquals(3, fitActions(5, 5 * 48 + 4 * 4 - 1, 48, 4))
        assertEquals(0, fitActions(3, 10, 48, 4))
        assertEquals(0, fitActions(0, 100, 48, 4))
    }

    @Test
    fun actionsThatDoNotFitMoveIntoTheOverflowMenuAndComeBack() = runComposeUiTest {
        val log = mutableListOf<String>()
        var width by mutableStateOf(400.dp)
        camo {
            CamoToolbar(
                listOf("Bold", "Italic", "Link", "Quote").map { action(it, log) },
                Modifier.width(width),
            )
        }
        onNodeWithContentDescription("Bold").assertExists()
        onNodeWithContentDescription("Quote").assertExists()
        onNodeWithContentDescription("More").assertDoesNotExist()
        width = 130.dp
        waitForIdle()
        onNodeWithContentDescription("Bold").assertExists()
        onNodeWithContentDescription("Quote").assertDoesNotExist()
        onNodeWithContentDescription("More").performClick()
        onNodeWithText("Quote").performClick()
        waitForIdle()
        assertEquals(listOf("Quote"), log)
        width = 400.dp
        waitForIdle()
        onNodeWithContentDescription("Quote").assertExists()
        onNodeWithContentDescription("More").assertDoesNotExist()
    }

    @Test
    fun aToggleKeepsItsPressedStateInTheToolbarAndInTheOverflow() = runComposeUiTest {
        val log = mutableListOf<String>()
        var width by mutableStateOf(400.dp)
        camo {
            CamoToolbar(
                listOf(action("Bold", log, selected = true), action("Italic", log, selected = false)),
                Modifier.width(width),
            )
        }
        onNodeWithContentDescription("Bold").assertIsOn()
        onNodeWithContentDescription("Italic").assertIsOff()
        width = 60.dp
        waitForIdle()
        onNodeWithContentDescription("More").performClick()
        onNodeWithText("Bold").assertExists()
        onNodeWithText("Italic").assertExists()
    }

    @Test
    fun arrowsMoveBetweenTheActionsAndSkipADisabledOne() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo {
            CamoToolbar(
                listOf(action("Bold", log), action("Italic", log, enabled = false), action("Link", log)),
            )
        }
        onNodeWithContentDescription("Bold").requestFocus()
        onNodeWithContentDescription("Bold").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        onNodeWithContentDescription("Link").assertIsFocused()
        assertTrue(log.isEmpty())
    }

    @Test
    fun leadingAndTrailingContentAlwaysShow() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo {
            CamoToolbar(
                listOf("Bold", "Italic", "Link").map { action(it, log) },
                Modifier.width(150.dp),
                leading = { BasicText("Menu") },
                trailing = { BasicText("Send") },
            )
        }
        onNodeWithText("Menu").assertExists()
        onNodeWithText("Send").assertExists()
        onNodeWithContentDescription("More").assertExists()
    }

    @Test
    fun aFloatingToolbarAsksTheSkinForTheFloatingPlacement() = runComposeUiTest {
        camo { CamoToolbar(listOf(action("Bold", mutableListOf())), placement = ToolbarPlacement.Floating) }
        assertEquals(ToolbarPlacement.Floating, lastToolbar?.placement)
        assertEquals(1, lastToolbar?.count)
    }

    @Test
    fun theToolbarContainerHoldsCustomContent() = runComposeUiTest {
        camo { CamoToolbarContainer(contentDescription = "Formatting") { Box { BasicText("custom") } } }
        onNodeWithText("custom").assertExists()
        onNodeWithContentDescription("Formatting").assertExists()
    }
}
