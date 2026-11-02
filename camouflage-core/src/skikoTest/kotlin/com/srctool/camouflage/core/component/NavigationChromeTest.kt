package com.srctool.camouflage.core.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.navigation.CamoAdaptiveNavigation
import com.srctool.camouflage.core.navigation.CamoModalDrawer
import com.srctool.camouflage.core.navigation.CamoNavItem
import com.srctool.camouflage.core.navigation.CamoNavSection
import com.srctool.camouflage.core.navigation.CamoNavigationBar
import com.srctool.camouflage.core.navigation.CamoNavigationDrawer
import com.srctool.camouflage.core.navigation.CamoNavigationRail
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// The M8 navigation chrome tests from their Kotlin notes: bar, rail, drawer, modal drawer, adaptive layout.
@OptIn(ExperimentalTestApi::class)
class NavigationChromeTest {
    private val caps = CamoCapabilities(true, true, false, false)

    private fun env(size: WindowSizeClass) = CamoEnvironment(size, InputMode.Touch, CamoPlatform.Android, caps)

    private fun ComposeUiTest.camo(size: WindowSizeClass = WindowSizeClass.Compact, content: @Composable () -> Unit) =
        setContent { CamouflageProvider(skinA, themeT, env(size), content) }

    private val items = listOf("Home", "Inbox", "Search", "Profile").map {
        CamoNavItem(it, it, icon = { BasicText("i") })
    }

    @Test
    fun aTapSelectsAndItemsAreTabs() = runComposeUiTest {
        var selected by mutableStateOf("Home")
        camo { CamoNavigationBar(items, selected, { selected = it }) }
        onNodeWithText("Home").assertIsSelected()
        onNodeWithText("Inbox").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        onNodeWithText("Inbox").performClick()
        assertEquals("Inbox", selected)
        onNodeWithText("Inbox").assertIsSelected()
    }

    @Test
    fun reselectingTheCurrentItemCallsBack() = runComposeUiTest {
        var calls = 0
        camo { CamoNavigationBar(items, "Home", { calls++ }) }
        onNodeWithText("Home").performClick()
        assertEquals(1, calls)
    }

    @Test
    fun aDisabledItemDoesNotSelect() = runComposeUiTest {
        var selected = "Home"
        val withOff = items.map { if (it.value == "Search") it.copy(enabled = false) else it }
        camo { CamoNavigationBar(withOff, selected, { selected = it }) }
        onNodeWithText("Search").performClick()
        assertEquals("Home", selected)
    }

    @Test
    fun theArrowKeysMoveAndSelect() = runComposeUiTest {
        var selected by mutableStateOf("Home")
        camo { CamoNavigationBar(items, selected, { selected = it }) }
        onNodeWithText("Home").requestFocus()
        onNodeWithText("Home").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals("Inbox", selected)
    }

    @Test
    fun aBarTakesThreeToFiveItems() = runComposeUiTest {
        assertFailsWith<IllegalStateException> {
            camo { CamoNavigationBar(items.take(1), "Home", {}) }
            waitForIdle()
        }
    }

    @Test
    fun aHiddenBarLeavesTheTree() = runComposeUiTest {
        var visible by mutableStateOf(true)
        camo { CamoNavigationBar(items, "Home", {}, visible = visible) }
        onNodeWithText("Home").assertExists()
        visible = false
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("Home").assertDoesNotExist()
    }

    @Test
    fun theRailShowsItsHeaderAndItems() = runComposeUiTest {
        var selected by mutableStateOf("Home")
        camo(WindowSizeClass.Medium) {
            CamoNavigationRail(items, selected, { selected = it }, header = { BasicText("FAB") })
        }
        onNodeWithText("FAB").assertExists()
        onNodeWithText("Profile").performClick()
        assertEquals("Profile", selected)
    }

    @Test
    fun theDrawerListsSectionsAndSelects() = runComposeUiTest {
        var selected by mutableStateOf("Home")
        val sections = listOf(
            CamoNavSection("Main", items.take(2)),
            CamoNavSection("More", items.drop(2)),
        )
        camo(WindowSizeClass.Large) { CamoNavigationDrawer(sections, selected, { selected = it }) }
        onNodeWithText("Main").assertExists()
        onNodeWithText("More").assertExists()
        onNodeWithText("Search").performClick()
        assertEquals("Search", selected)
    }

    @Test
    fun aModalDrawerOpensClosesAndSelectingClosesIt() = runComposeUiTest {
        var open by mutableStateOf(false)
        var selected by mutableStateOf("Home")
        camo {
            CamoModalDrawer(open, { open = false }) {
                CamoNavigationDrawer(listOf(CamoNavSection(items = items)), selected, { selected = it })
            }
        }
        onNodeWithText("Inbox").assertDoesNotExist()
        open = true
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("Inbox").assertExists()
        onNodeWithText("Inbox").performClick()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        assertEquals("Inbox", selected)
        assertEquals(false, open)
    }

    @Test
    fun theAdaptiveLayoutSwitchesAtTheBreakpointsAndKeepsTheContentState() = runComposeUiTest {
        var size by mutableStateOf(WindowSizeClass.Compact)
        var selected by mutableStateOf("Home")
        setContent {
            CamouflageProvider(skinA, themeT, env(size)) {
                CamoAdaptiveNavigation(items, selected, { selected = it }, header = { BasicText("HEADER") }) {
                    var n by remember { mutableStateOf(0) }
                    BasicText("count $n")
                    onClickCounter = { n++ }
                }
            }
        }
        onNodeWithText("HEADER").assertDoesNotExist()
        onClickCounter()
        waitForIdle()
        onNodeWithText("count 1").assertExists()
        size = WindowSizeClass.Medium
        waitForIdle()
        onNodeWithText("HEADER").assertExists()
        onNodeWithText("count 1").assertExists()
        size = WindowSizeClass.Large
        waitForIdle()
        onNodeWithText("HEADER").assertExists()
        onNodeWithText("count 1").assertExists()
        onNodeWithText("Home").assertIsSelected()
    }

    private var onClickCounter: () -> Unit = {}
}
