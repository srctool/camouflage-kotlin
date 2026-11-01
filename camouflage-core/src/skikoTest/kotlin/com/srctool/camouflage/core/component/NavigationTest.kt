package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.fab.CamoFab
import com.srctool.camouflage.core.fab.FabSize
import com.srctool.camouflage.core.fab.FabTheme
import com.srctool.camouflage.core.lastIndicator
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.tabs.CamoTab
import com.srctool.camouflage.core.tabs.CamoTabs
import com.srctool.camouflage.core.tabs.rememberCamoTabsPagerSync
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// The M8 tabs and FAB tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class NavigationTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private val tabs = listOf(CamoTab("a", "Overview"), CamoTab("b", "Reviews"), CamoTab("c", "Specs"))

    @Test
    fun aTapSelectsAndTheIndicatorMoves() = runComposeUiTest {
        var selected by mutableStateOf("a")
        camo { CamoTabs(selected, { selected = it }, tabs) }
        onNodeWithText("Overview").assertIsSelected()
        val first = lastIndicator
        assertNotNull(first)
        onNodeWithText("Reviews").performClick()
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        assertEquals("b", selected)
        onNodeWithText("Reviews").assertIsSelected()
        onNodeWithText("Overview").assertIsNotSelected()
        assertTrue(checkNotNull(lastIndicator).start > first.start)
    }

    @Test
    fun tabsHaveTabSemantics() = runComposeUiTest {
        camo { CamoTabs("b", {}, tabs) }
        onNodeWithText("Reviews").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    @Test
    fun aDisabledTabDoesNotSelect() = runComposeUiTest {
        var selected by mutableStateOf("a")
        val two = listOf(CamoTab("a", "One"), CamoTab("b", "Two", enabled = false))
        camo { CamoTabs(selected, { selected = it }, two) }
        onNodeWithText("Two").performClick()
        assertEquals("a", selected)
    }

    @Test
    fun aSelectionNotInTheTabsDrawsNoIndicator() = runComposeUiTest {
        camo { CamoTabs("zzz", {}, tabs) }
        waitForIdle()
        assertEquals(null, lastIndicator)
    }

    @Test
    fun scrollableTabsKeepTheirNaturalWidth() = runComposeUiTest {
        camo { Column(Modifier.size(120.dp)) { CamoTabs("a", {}, tabs, scrollable = true) } }
        onNodeWithText("Specs").assertWidthIsAtLeast(1.dp)
        onNodeWithText("Overview").assertIsSelected()
    }

    @Test
    fun theTabsFollowAPager() = runComposeUiTest {
        camo {
            val pager = rememberPagerState { tabs.size }
            val sync = rememberCamoTabsPagerSync(pager, tabs)
            Column {
                CamoTabs(sync.selected, sync.onSelect, tabs, indicatorPosition = sync.position)
                HorizontalPager(pager, Modifier.size(200.dp)) { BasicText("page ${tabs[it].label}") }
            }
        }
        onNodeWithText("Overview").assertIsSelected()
        onNodeWithText("Specs").performClick()
        mainClock.advanceTimeBy(2000)
        waitForIdle()
        onNodeWithText("Specs").assertIsSelected()
    }

    @Test
    fun theFabNamesItselfFromItsTextOrItsDescription() = runComposeUiTest {
        var expanded by mutableStateOf(true)
        camo {
            CamoFab({ BasicText("+") }, "Compose message", {}, text = "Compose", expanded = expanded)
        }
        onNodeWithContentDescription("Compose").assert(hasClickAction())
        expanded = false
        waitForIdle()
        onNodeWithContentDescription("Compose").assert(hasClickAction())
    }

    @Test
    fun aHiddenFabLeavesTheTree() = runComposeUiTest {
        var visible by mutableStateOf(true)
        var clicks = 0
        camo { CamoFab({ BasicText("+") }, "Add", { clicks++ }, visible = visible) }
        onNodeWithContentDescription("Add").performClick()
        assertEquals(1, clicks)
        visible = false
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithContentDescription("Add").assertDoesNotExist()
    }

    @Test
    fun theSmallFabKeepsA48Target() = runComposeUiTest {
        val theme = themeT.copy(components = CamoComponentThemes(fab = FabTheme(size = FabSize.Small)))
        setContent { CamouflageProvider(skinA, theme) { CamoFab({ BasicText("+") }, "Add", {}) } }
        onNodeWithContentDescription("Add").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun aToneChangesTheFabWithoutChangingItsBehavior() = runComposeUiTest {
        var clicks = 0
        camo { CamoFab({ BasicText("!") }, "Alert", { clicks++ }, tone = Tone.Error) }
        onNodeWithContentDescription("Alert").performClick()
        assertEquals(1, clicks)
    }
}
