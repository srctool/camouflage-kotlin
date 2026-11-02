package com.srctool.camouflage.core.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.lastPopover
import com.srctool.camouflage.core.navigation.CamoModalDrawer
import com.srctool.camouflage.core.navigation.CamoNavSection
import com.srctool.camouflage.core.navigation.CamoNavItem
import com.srctool.camouflage.core.navigation.CamoNavigationDrawer
import com.srctool.camouflage.core.popover.CamoPopover
import com.srctool.camouflage.core.popover.PopoverPlacement
import com.srctool.camouflage.core.popover.placePopover
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.sidesheet.CamoModalSideSheet
import com.srctool.camouflage.core.sidesheet.CamoSideSheet
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// The M9 popover and side sheet tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class PanelsTest {
    private companion object {
        // Far enough and fast enough to pass the dismissal thresholds, whatever the panel's width.
        const val DRAG = 400f
        const val DRAG_MILLIS = 100L
    }

    private val window = IntSize(800, 600)
    private val panel = IntSize(200, 100)

    private fun anchorAt(x: Int, y: Int) = IntRect(IntOffset(x, y), IntSize(40, 20))

    @Test
    fun aPopoverOpensBelowItsAnchorCenteredOnIt() {
        val layout = placePopover(anchorAt(300, 100), panel, window, PopoverPlacement.Auto, 8, ltr = true)
        assertEquals(PopoverPlacement.Bottom, layout.placement)
        assertEquals(IntOffset(300 + 20 - 100, 128), layout.offset)
        assertEquals(100, layout.arrow)
    }

    @Test
    fun aPopoverFlipsAboveWhenThereIsNoRoomBelow() {
        val layout = placePopover(anchorAt(300, 560), panel, window, PopoverPlacement.Auto, 8, ltr = true)
        assertEquals(PopoverPlacement.Top, layout.placement)
        assertEquals(560 - 8 - 100, layout.offset.y)
    }

    @Test
    fun aPopoverStaysInsideTheWindowAndTheArrowStillPointsAtTheAnchor() {
        val layout = placePopover(anchorAt(0, 100), panel, window, PopoverPlacement.Auto, 8, ltr = true)
        assertEquals(0, layout.offset.x)
        assertEquals(20, layout.arrow)
    }

    @Test
    fun theSidePlacementsFollowTheReadingDirection() {
        val anchor = anchorAt(400, 200)
        val end = placePopover(anchor, panel, window, PopoverPlacement.End, 8, ltr = true)
        assertEquals(PopoverPlacement.End, end.placement)
        assertEquals(448, end.offset.x)
        val mirrored = placePopover(anchor, panel, window, PopoverPlacement.End, 8, ltr = false)
        assertEquals(PopoverPlacement.End, mirrored.placement)
        assertEquals(400 - 8 - 200, mirrored.offset.x)
    }

    @Test
    fun aSidePlacementFlipsToTheOtherSideWhenItDoesNotFit() {
        val layout = placePopover(anchorAt(700, 200), panel, window, PopoverPlacement.End, 8, ltr = true)
        assertEquals(PopoverPlacement.Start, layout.placement)
    }

    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    @Test
    fun aPopoverShowsWhileOpenAndEscAsksToClose() = runComposeUiTest {
        var open by mutableStateOf(false)
        var requests = 0
        camo {
            CamoPopover(open, { requests++ }, anchor = { BasicText("anchor") }, title = "Details") {
                BasicText("body")
            }
        }
        onNodeWithText("body").assertDoesNotExist()
        open = true
        waitForIdle()
        onNodeWithText("body").assertExists()
        onNodeWithText("Details").assertExists()
        assertNotNull(lastPopover)
        onNodeWithText("body").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertTrue(requests > 0)
    }

    @Test
    fun theAnchorAnnouncesWhetherThePopoverIsOpen() = runComposeUiTest {
        var open by mutableStateOf(false)
        camo { CamoPopover(open, {}, anchor = { BasicText("anchor") }) { BasicText("body") } }
        onNodeWithText("anchor").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        open = true
        waitForIdle()
        onNodeWithText("anchor").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
    }

    @Test
    fun aStandardSideSheetSitsInTheLayoutAndItsCloseButtonCloses() = runComposeUiTest {
        var closed = false
        camo {
            CamoSideSheet(title = "Properties", onClose = { closed = true }, actions = { BasicText("Apply") }) {
                BasicText("size")
            }
        }
        onNodeWithText("Properties").assertExists()
        onNodeWithText("size").assertExists()
        onNodeWithText("Apply").assertExists()
        onNodeWithText("close").performClick()
        assertTrue(closed)
    }

    @Test
    fun aModalSideSheetOpensAndADragTowardTheEndEdgeAsksToClose() = runComposeUiTest {
        var open by mutableStateOf(true)
        var requests = 0
        camo {
            CamoModalSideSheet(open, { requests++ }) { CamoSideSheet(title = "Properties") { BasicText("size") } }
        }
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("size").assertExists()
        onNodeWithText("size").performTouchInput { swipe(center, center + Offset(DRAG, 0f), DRAG_MILLIS) }
        waitForIdle()
        assertTrue(requests > 0)
        open = false
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("size").assertDoesNotExist()
    }

    @Test
    fun aModalDrawerAsksToCloseOnADragTowardTheStartEdgeAndNotTheOtherWay() = runComposeUiTest {
        var requests = 0
        val items = listOf("Home", "Inbox", "Search").map { CamoNavItem(it, it, icon = { BasicText("i") }) }
        camo {
            CamoModalDrawer(true, { requests++ }) {
                CamoNavigationDrawer(listOf(CamoNavSection(items = items)), "Home", {})
            }
        }
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("Inbox").performTouchInput { swipe(center, center + Offset(DRAG, 0f), DRAG_MILLIS) }
        waitForIdle()
        assertEquals(0, requests)
        onNodeWithText("Inbox").performTouchInput { swipe(center, center + Offset(-DRAG, 0f), DRAG_MILLIS) }
        waitForIdle()
        assertTrue(requests > 0)
    }

    @Test
    fun theSideSheetSlidesTheOtherWayInRtl() = runComposeUiTest {
        var requests = 0
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CamouflageProvider(skinA, themeT) {
                    CamoModalSideSheet(true, { requests++ }) { CamoSideSheet { BasicText("size") } }
                }
            }
        }
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        onNodeWithText("size").performTouchInput { swipe(center, center + Offset(DRAG, 0f), DRAG_MILLIS) }
        waitForIdle()
        assertEquals(0, requests)
        onNodeWithText("size").performTouchInput { swipe(center, center + Offset(-DRAG, 0f), DRAG_MILLIS) }
        waitForIdle()
        assertTrue(requests > 0)
    }
}
