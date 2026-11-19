package com.srctool.camouflage.core.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.core.palette.CamoCommandItem
import com.srctool.camouflage.core.palette.CamoCommandPalette
import com.srctool.camouflage.core.palette.CamoCommandPaletteState
import com.srctool.camouflage.core.palette.CamoCommandSource
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.delay

// The command palette (base CamoCommandPalette, M16).
@OptIn(ExperimentalTestApi::class)
class CommandPaletteTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private val ran = mutableListOf<String>()

    private fun item(title: String, shortcut: String? = null) =
        CamoCommandItem(title, title, { ran += title }, shortcut = shortcut)

    private val actions = CamoCommandSource.of(
        "Actions",
        listOf(item("Open File", "Ctrl+O"), item("Close File"), item("Toggle Theme")),
    )
    private val files = CamoCommandSource.of("Files", listOf(item("README.md"), item("build.gradle.kts")))

    @Test
    fun hiddenUntilOpenedThenGroupsEverySourceWithShortcutHints() = runComposeUiTest {
        val state = CamoCommandPaletteState()
        camo { CamoCommandPalette(state, listOf(actions, files)) }
        onNodeWithText("Open File").assertDoesNotExist()
        state.show()
        waitForIdle()
        onNodeWithText("Actions").assertExists()
        onNodeWithText("Files").assertExists()
        onNodeWithText("Open File").assertExists()
        onNodeWithText("Ctrl+O").assertExists()
        onNodeWithText("README.md").assertExists()
    }

    @Test
    fun typingFiltersAndEnterRunsTheActiveRowAfterClosing() = runComposeUiTest {
        val state = CamoCommandPaletteState()
        camo { CamoCommandPalette(state, listOf(actions, files)) }
        state.show()
        waitForIdle()
        onNode(hasSetTextAction()).performTextInput("clo")
        waitForIdle()
        onNodeWithText("Close File").assertExists()
        onNodeWithText("Toggle Theme").assertDoesNotExist()
        onNodeWithText("README.md").assertDoesNotExist()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf("Close File"), ran)
        assertEquals(false, state.open)
        assertEquals(listOf<Any>("Close File"), state.recent)
    }

    @Test
    fun arrowsMoveTheActiveRowAndTabJumpsToTheNextGroup() = runComposeUiTest {
        val state = CamoCommandPaletteState()
        camo { CamoCommandPalette(state, listOf(actions, files)) }
        state.show()
        waitForIdle()
        val field = onNode(hasSetTextAction())
        field.performKeyInput { pressKey(Key.DirectionDown) }
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf("Close File"), ran)
        state.show()
        waitForIdle()
        // Close File is a recent now: it leads its group.
        field.performKeyInput { pressKey(Key.Tab) }
        field.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf("Close File", "README.md"), ran)
    }

    @Test
    fun escapeClosesAndASlowSourceShowsALoadingRowWhileTheOthersAnswer() = runComposeUiTest {
        val state = CamoCommandPaletteState()
        val slow = CamoCommandSource("Symbols") {
            delay(60_000)
            listOf(item("Late"))
        }
        camo { CamoCommandPalette(state, listOf(actions, slow)) }
        state.show()
        waitForIdle()
        onNodeWithText("Open File").assertExists()
        onNodeWithText("Symbols").assertExists()
        onNodeWithText("Loading…").assertExists()
        onNodeWithText("Late").assertDoesNotExist()
        onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(false, state.open)
        onNodeWithText("Open File").assertDoesNotExist()
    }

    @Test
    fun noMatchesShowsOneRow() = runComposeUiTest {
        val state = CamoCommandPaletteState()
        camo { CamoCommandPalette(state, listOf(actions, files)) }
        state.show()
        waitForIdle()
        onNode(hasSetTextAction()).performTextInput("zzzz")
        waitForIdle()
        onNodeWithText("No results").assertExists()
    }
}
