package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.tabs.CamoDocument
import com.srctool.camouflage.core.tabs.CamoDocumentTabs
import com.srctool.camouflage.core.tabs.CamoDocumentTabsState
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Document tabs: close, reorder, pin, overflow and the screen reader actions (base CamoDocumentTabs, M16).
@OptIn(ExperimentalTestApi::class)
class DocumentTabsTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private val closed = mutableListOf<Any>()

    private fun docs(n: Int, modified: Set<Int> = emptySet()) =
        List(n) { CamoDocument(it + 1, "Doc ${it + 1}", modified = (it + 1) in modified) }

    @Composable
    private fun Tabs(documents: List<CamoDocument>, state: CamoDocumentTabsState, width: Int = 2000) {
        var selected by remember { mutableStateOf<Any>(1) }
        CamoDocumentTabs(
            documents, selected, { selected = it }, { closed += it }, state, Modifier.width(width.dp),
        )
    }

    // Runs the tab's custom accessibility action [label], as a screen reader's actions menu would.
    private fun SemanticsNodeInteraction.act(label: String) {
        val actions = fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        val action = actions.firstOrNull { it.label == label }
            ?: error("no action $label in ${actions.map { it.label }}")
        action.action()
    }

    @Test
    fun everyDocumentGetsATabAndTheCloseActionAsksTheApp() = runComposeUiTest {
        val state = CamoDocumentTabsState()
        camo { Tabs(docs(3), state) }
        onNodeWithContentDescription("Doc 2").assertExists()
        onNodeWithContentDescription("Doc 2").act("Close")
        waitForIdle()
        assertEquals(listOf<Any>(2), closed)
    }

    @Test
    fun aModifiedDocumentIsAnnouncedAsModified() = runComposeUiTest {
        camo { Tabs(docs(3, modified = setOf(2)), CamoDocumentTabsState()) }
        val description = onNodeWithContentDescription("Doc 2").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals("modified", description)
    }

    @Test
    fun altArrowsMoveTheFocusedTabAndAltLeftOnTheFirstTabStaysPut() = runComposeUiTest {
        val state = CamoDocumentTabsState()
        camo { Tabs(docs(3), state) }
        waitForIdle()
        onNodeWithContentDescription("Doc 1").requestFocus()
        onNodeWithContentDescription("Doc 1").performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionRight)
            keyUp(Key.AltLeft)
        }
        waitForIdle()
        assertEquals(listOf<Any>(2, 1, 3), state.order)
        onNodeWithContentDescription("Doc 1").assertIsFocused()
        onNodeWithContentDescription("Doc 1").performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionRight)
            keyUp(Key.AltLeft)
        }
        waitForIdle()
        assertEquals(listOf<Any>(2, 3, 1), state.order)
    }

    @Test
    fun moveAndPinActionsReorderAndPinnedTabsStayFirst() = runComposeUiTest {
        val state = CamoDocumentTabsState()
        camo { Tabs(docs(4), state) }
        waitForIdle()
        onNodeWithContentDescription("Doc 3").act("Move earlier")
        waitForIdle()
        assertEquals(listOf<Any>(1, 3, 2, 4), state.order)
        onNodeWithContentDescription("Doc 4").act("Pin")
        waitForIdle()
        assertEquals(listOf<Any>(4, 1, 3, 2), state.order)
        assertEquals("pinned", onNodeWithContentDescription("Doc 4").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.StateDescription))
        onNodeWithContentDescription("Doc 4").act("Unpin")
        waitForIdle()
        assertEquals(listOf<Any>(4, 1, 3, 2), state.order)
        assertTrue(state.pinned.isEmpty())
    }

    @Test
    fun draggingATabPastItsNeighborSwapsThem() = runComposeUiTest {
        val state = CamoDocumentTabsState()
        camo { Tabs(docs(3), state) }
        waitForIdle()
        val width = onNodeWithContentDescription("Doc 1").fetchSemanticsNode().size.width.toFloat()
        onNodeWithContentDescription("Doc 1").performTouchInput {
            down(center)
            moveBy(Offset(width * 0.4f, 0f))
            moveBy(Offset(width * 0.4f, 0f))
            moveBy(Offset(width * 0.4f, 0f))
            up()
        }
        waitForIdle()
        assertEquals(listOf<Any>(2, 1, 3), state.order)
    }

    @Test
    fun twentyTabsInANarrowBarOverflowIntoAShowAllMenu() = runComposeUiTest {
        val state = CamoDocumentTabsState()
        camo { Tabs(docs(20), state, width = 300) }
        waitForIdle()
        onNodeWithContentDescription("Show all tabs").assertExists()
        onNodeWithContentDescription("Show all tabs").performClick()
        waitForIdle()
        // The tab's own text and the menu's: the menu is the later one.
        onAllNodesWithText("Doc 20").onLast().performClick()
        waitForIdle()
        onNodeWithContentDescription("Doc 20").assertExists()
    }

    @Test
    fun inRightToLeftAltLeftMovesTheTabVisuallyLeftWhichIsLater() = runComposeUiTest {
        val state = CamoDocumentTabsState()
        camo {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { Tabs(docs(3), state) }
        }
        waitForIdle()
        onNodeWithContentDescription("Doc 1").requestFocus()
        onNodeWithContentDescription("Doc 1").performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.AltLeft)
        }
        waitForIdle()
        assertEquals(listOf<Any>(2, 1, 3), state.order)
    }

    @Test
    fun aBarThatFitsHasNoOverflowMenu() = runComposeUiTest {
        camo { Tabs(docs(3), CamoDocumentTabsState()) }
        waitForIdle()
        onNodeWithContentDescription("Show all tabs").assertDoesNotExist()
    }

    @Test
    fun aDocumentThatIsNotClosableHasNoCloseAction() = runComposeUiTest {
        val documents = listOf(CamoDocument(1, "Pinned page", closable = false), CamoDocument(2, "Other"))
        camo { Tabs(documents, CamoDocumentTabsState()) }
        val actions = onNodeWithContentDescription("Pinned page").fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }
        assertTrue("Close" !in actions, "$actions")
    }
}
