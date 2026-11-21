package com.srctool.camouflage.core.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.srctool.camouflage.core.dock.CamoDockLayout
import com.srctool.camouflage.core.dock.CamoDockState
import com.srctool.camouflage.core.dock.CamoToolWindow
import com.srctool.camouflage.core.dock.DockEdge
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertTrue

// The dock layout: stripes, tool windows, maximize, moving, focus and right to left (base CamoDockLayout, M16).
@OptIn(ExperimentalTestApi::class)
class DockLayoutTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private val ids = listOf("files", "search", "details", "log", "terminal")
    private val edges = listOf(DockEdge.Left, DockEdge.Left, DockEdge.Right, DockEdge.Bottom, DockEdge.Bottom)

    private fun state(open: Set<Any> = emptySet()) =
        CamoDockState(ids.zip(edges).toMap(), open)

    private val windows = ids.zip(edges).map { (id, edge) ->
        CamoToolWindow(
            id, id.replaceFirstChar { it.uppercase() }, { Box(Modifier.size(12.dp)) }, edge,
            { Box(Modifier.fillMaxSize().testTag("content-$id").focusable()) },
        )
    }

    @Composable
    private fun Dock(state: CamoDockState, width: Int = 1200, height: Int = 600, centerFocusable: Boolean = true) {
        Box(Modifier.size(width.dp, height.dp)) {
            CamoDockLayout(state, windows) {
                val center = Modifier.fillMaxSize().testTag("center")
                Box(if (centerFocusable) center.focusable() else center)
            }
        }
    }

    @Test
    fun stripeButtonsToggleTheirToolWindows() = runComposeUiTest {
        val state = state()
        camo { Dock(state) }
        onNodeWithTag("content-files").assertDoesNotExist()
        onNodeWithContentDescription("Files").performClick()
        onNodeWithTag("content-files").assertExists()
        onNodeWithContentDescription("Log").performClick()
        onNodeWithTag("content-log").assertExists()
        onNodeWithContentDescription("Files").performClick()
        onNodeWithTag("content-files").assertDoesNotExist()
        assertTrue(state.isOpen("log"))
    }

    @Test
    fun threeDocksWithFiveWindowsStackAndRestoreFromState() = runComposeUiTest {
        val state = state(setOf("files", "search", "details", "log", "terminal"))
        camo { Dock(state) }
        ids.forEach { onNodeWithTag("content-$it").assertExists() }
        state.move("search", DockEdge.Right)
        waitForIdle()
        onNodeWithTag("content-search").assertExists()
        val saved = state.snapshot()
        state.close("files")
        state.move("log", DockEdge.Left)
        state.apply(saved)
        waitForIdle()
        onNodeWithTag("content-files").assertExists()
        assertTrue(state.edgeOf("log") == DockEdge.Bottom)
    }

    @Test
    fun maximizeHidesTheCenterAndRestoreBringsItBack() = runComposeUiTest {
        val state = state(setOf("files"))
        camo { Dock(state) }
        onNodeWithTag("center").assertExists()
        onNodeWithContentDescription("Maximize").performClick()
        onNodeWithTag("center").assertDoesNotExist()
        onNodeWithTag("content-files").assertExists()
        onNodeWithContentDescription("Restore").performClick()
        onNodeWithTag("center").assertExists()
    }

    @Test
    fun aNarrowDockFoldsMaximizeAndHideIntoItsMenu() = runComposeUiTest {
        val state = state(setOf("files"))
        camo { Box(Modifier.size(300.dp, 400.dp)) { Dock(state, 300, 400) } }
        onNodeWithContentDescription("Maximize").assertDoesNotExist()
        onNodeWithContentDescription("Tool window options").performClick()
        onNodeWithText("Maximize").performClick()
        waitForIdle()
        assertTrue(state.maximized == "files")
    }

    @Test
    fun hideClosesTheWindowButKeepsItsStripeButton() = runComposeUiTest {
        val state = state(setOf("files"))
        camo { Dock(state) }
        onNodeWithContentDescription("Hide").performClick()
        onNodeWithTag("content-files").assertDoesNotExist()
        onNodeWithContentDescription("Files").assertExists()
    }

    @Test
    fun theHeaderMenuMovesTheWindowToAnotherEdge() = runComposeUiTest {
        val state = state(setOf("files"))
        camo { Dock(state) }
        onNodeWithContentDescription("Tool window options").performClick()
        onNodeWithText("Move to").performClick()
        onNodeWithText("Bottom").performClick()
        waitForIdle()
        assertTrue(state.edgeOf("files") == DockEdge.Bottom)
    }

    @Test
    fun focusMovesIntoAWindowAndEscReturnsToTheCenter() = runComposeUiTest {
        val state = state()
        camo { Dock(state) }
        state.focus("details")
        waitForIdle()
        onNodeWithTag("content-details").assertIsFocused()
        onNodeWithTag("content-details").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("center").assertIsFocused()
    }

    @Test
    fun escLeavesTheWindowWhenTheCenterHasNothingToFocus() = runComposeUiTest {
        val state = state()
        camo { Dock(state, centerFocusable = false) }
        state.focus("details")
        waitForIdle()
        onNodeWithTag("content-details").assertIsFocused()
        onNodeWithTag("content-details").performKeyInput { pressKey(Key.Escape) }
        onNodeWithTag("content-details").assertIsNotFocused()
    }

    @Test
    fun theLeftDockSitsAtTheEndInRightToLeft() = runComposeUiTest {
        val state = state(setOf("files"))
        camo {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Dock(state) }
        }
        val files = onNodeWithTag("content-files").getUnclippedBoundsInRoot()
        val center = onNodeWithTag("center").getUnclippedBoundsInRoot()
        assertTrue(files.left > center.left, "the left dock should be right of the center in RTL")
    }
}
