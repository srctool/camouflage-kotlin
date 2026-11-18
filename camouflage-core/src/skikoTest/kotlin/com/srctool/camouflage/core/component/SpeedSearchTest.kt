package com.srctool.camouflage.core.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.speedsearch.CamoSpeedSearchState
import com.srctool.camouflage.core.speedsearch.SpeedSearchMode
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.tree.CamoTree
import com.srctool.camouflage.core.tree.CamoTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals

// Speed search on a tree (base CamoSpeedSearch, M16).
@OptIn(ExperimentalTestApi::class)
class SpeedSearchTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private val tree = listOf(
        CamoTreeNode(
            "docs", "Documents",
            listOf(CamoTreeNode("cv", "CV"), CamoTreeNode("notes", "Notes", listOf(CamoTreeNode("n1", "Note 1")))),
        ),
        CamoTreeNode("pics", "Pictures", listOf(CamoTreeNode("p1", "Photo"))),
        CamoTreeNode("readme", "Readme"),
    )

    @Composable
    private fun Tree(state: CamoSpeedSearchState) {
        CamoTree(tree, emptySet(), {}, emptySet(), {}, { it }, "Files", speedSearch = state)
    }

    @Test
    fun typingFiltersAndKeepsTheAncestorsOfMatchesOpen() = runComposeUiTest {
        val state = CamoSpeedSearchState()
        camo { Tree(state) }
        onNodeWithText("Documents").requestFocus()
        state.type("note")
        waitForIdle()
        onNodeWithText("Documents").assertExists()
        onNodeWithText("Notes").assertExists()
        onNodeWithText("Note 1").assertExists()
        onNodeWithText("Pictures").assertDoesNotExist()
        onNodeWithText("Readme").assertDoesNotExist()
        onNodeWithText("CV").assertDoesNotExist()
        assertEquals(2, state.matchCount)
    }

    @Test
    fun clearingBringsTheWholeTreeBack() = runComposeUiTest {
        val state = CamoSpeedSearchState()
        camo { Tree(state) }
        state.type("zzz")
        waitForIdle()
        assertEquals(0, state.matchCount)
        onNodeWithText("Readme").assertDoesNotExist()
        state.clear()
        waitForIdle()
        onNodeWithText("Readme").assertExists()
        onNodeWithText("Pictures").assertExists()
    }

    @Test
    fun jumpModeKeepsEveryNodeAndCountsTheMatches() = runComposeUiTest {
        val state = CamoSpeedSearchState(SpeedSearchMode.Jump)
        camo { Tree(state) }
        state.type("re")
        waitForIdle()
        onNodeWithText("Pictures").assertExists()
        onNodeWithText("Readme").assertExists()
        assertEquals(2, state.matchCount) // Readme, and Pictures ("pictuREs")
    }

    @Test
    fun focusMovesToTheFirstMatchAfterTyping() = runComposeUiTest {
        val state = CamoSpeedSearchState()
        camo { Tree(state) }
        onNodeWithText("Documents").requestFocus()
        state.type("read")
        waitForIdle()
        onNodeWithText("Readme").assertIsFocused()
    }

    @Test
    fun theKeyboardTypesBacksUpAndEscClears() = runComposeUiTest {
        val state = CamoSpeedSearchState()
        camo { Tree(state) }
        onNodeWithText("Documents").requestFocus()
        onNodeWithText("Documents").performKeyInput { pressKey(Key.R); pressKey(Key.E); pressKey(Key.A) }
        waitForIdle()
        assertEquals("rea", state.query)
        onNodeWithText("Readme").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()
        assertEquals("re", state.query)
        onNodeWithText("Readme").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals("", state.query)
        onNodeWithText("Documents").assertExists()
    }

    @Test
    fun aMenuWithSpeedSearchFiltersWhatIsTypedAndEscClearsBeforeClosing() = runComposeUiTest {
        val items = listOf(CamoMenuItem("Copy", {}), CamoMenuItem("Cut", {}), CamoMenuItem("Paste", {}))
        camo { CamoMenuAnchor(items = items, speedSearch = true) { open -> CamoButton("Edit", open) } }
        onNodeWithText("Edit").requestFocus()
        onNodeWithText("Edit").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithText("Copy").assertIsFocused()
        onNodeWithText("Copy").performKeyInput { pressKey(Key.P); pressKey(Key.A) }
        waitForIdle()
        onNodeWithText("Paste").assertIsFocused()
        onNodeWithText("Copy").assertDoesNotExist()
        onNodeWithText("Cut").assertDoesNotExist()
        onNodeWithText("Paste").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithText("Copy").assertExists() // Esc cleared the search; the menu stays open
        onNodeWithText("Cut").assertExists()
    }

    @Test
    fun aTableFiltersItsRowsByTheColumnsThatGiveTheirText() = runComposeUiTest {
        val state = CamoSpeedSearchState()
        val rows = listOf("Ada", "Grace", "Linus")
        camo {
            CamoDataTable(
                listOf(CamoColumn<String>("name", "Name", text = { it }, cell = { BasicText(it) })),
                rows, { it }, "People", speedSearch = state,
            )
        }
        onNodeWithText("Linus").assertExists()
        state.type("gr")
        waitForIdle()
        onNodeWithText("Grace").assertExists()
        onNodeWithText("Ada").assertDoesNotExist()
        onNodeWithText("Linus").assertDoesNotExist()
        assertEquals(1, state.matchCount)
        state.clear()
        waitForIdle()
        onNodeWithText("Ada").assertExists()
    }
}
