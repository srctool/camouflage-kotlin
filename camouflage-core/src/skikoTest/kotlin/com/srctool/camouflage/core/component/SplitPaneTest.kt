package com.srctool.camouflage.core.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.splitpane.CamoPane
import com.srctool.camouflage.core.splitpane.CamoSplitPane
import com.srctool.camouflage.core.splitpane.CamoSplitSnapshot
import com.srctool.camouflage.core.splitpane.CamoSplitState
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// CamoSplitPane (base Split Pane, M16): sizes from fractions, drag, keys, collapse, screen reader, RTL, snapshots.
@OptIn(ExperimentalTestApi::class)
class SplitPaneTest {
    private fun ComposeUiTest.camo(direction: LayoutDirection = LayoutDirection.Ltr, content: @Composable () -> Unit) =
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                CamouflageProvider(skinA, themeT, content)
            }
        }

    private fun pane(tag: String, min: Int = 50, collapsible: Boolean = false, max: Int? = null) = CamoPane(
        minSize = min.dp,
        maxSize = max?.dp,
        collapsible = collapsible,
    ) { Box(Modifier.fillMaxSize().testTag(tag)) }

    // 800 dp wide, 202 dp for the two 2 dp lines: 798 for the panes.
    private fun ComposeUiTest.three(
        state: CamoSplitState,
        width: Int = 800,
        axis: Orientation = Orientation.Horizontal,
    ) =
        camo {
            val size = when (axis) {
                Orientation.Horizontal -> Modifier.size(width.dp, 200.dp)
                Orientation.Vertical -> Modifier.size(200.dp, width.dp)
            }
            Box(size) {
                CamoSplitPane(axis, listOf(pane("a"), pane("b"), pane("c", collapsible = true)), state)
            }
        }

    @Test
    fun panesGetTheirFractionsOfTheContainerLessTheDividers() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        onNodeWithTag("a").assertWidthIsEqualTo(199.dp)
        onNodeWithTag("b").assertWidthIsEqualTo(199.dp)
        onNodeWithTag("c").assertWidthIsEqualTo(398.dp)
    }

    @Test
    fun aVerticalSplitStacksThePanes() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state, width = 400, axis = Orientation.Vertical)
        onNodeWithTag("a").assertHeightIsEqualTo(99.dp)
        onNodeWithTag("c").assertHeightIsEqualTo(198.dp)
    }

    @Test
    fun draggingADividerMovesSpaceBetweenItsNeighboursOnly() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        onAllNodesWithDivider()[0].performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(60f, 0f))
            up()
        }
        waitForIdle()
        // Space moved from the second pane to the first (less the touch slop); the third kept its share.
        assertTrue(state.fractions[0] > 0.3f)
        assertEquals(0.5f, state.fractions[0] + state.fractions[1], 0.001f)
        assertEquals(0.5f, state.fractions[2], 0.001f)
        onNodeWithTag("c").assertWidthIsEqualTo(398.dp)
    }

    private fun ComposeUiTest.onAllNodesWithDivider() = onAllNodes(
        androidx.compose.ui.test.hasContentDescription("Resize panel"),
    ).fetchSemanticsNodes().indices.map { i ->
        onAllNodes(androidx.compose.ui.test.hasContentDescription("Resize panel"))[i]
    }

    @Test
    fun arrowKeysResizeByTheKeyStepAndShiftByTheLargeStep() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        val divider = onAllNodesWithDivider()[0]
        divider.requestFocus()
        divider.performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("a").assertWidthIsEqualTo(215.dp)
        divider.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.ShiftLeft)
        }
        onNodeWithTag("a").assertWidthIsEqualTo(151.dp)
    }

    @Test
    fun homeAndEndGoToTheLimitsAndNeverCollapse() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        val divider = onAllNodesWithDivider()[1]
        divider.requestFocus()
        divider.performKeyInput { pressKey(Key.MoveEnd) }
        onNodeWithTag("c").assertWidthIsEqualTo(50.dp)
        assertTrue(state.collapsed.isEmpty())
    }

    @Test
    fun enterCollapsesACollapsiblePaneAndEnterAgainRestoresIt() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        val divider = onAllNodesWithDivider()[1]
        divider.requestFocus()
        divider.performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(setOf(2), state.collapsed)
        onNodeWithTag("c").assertDoesNotExist()
        onAllNodesWithDivider()[1].performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithTag("c").assertExists()
        assertTrue(state.collapsed.isEmpty())
    }

    @Test
    fun aScreenReaderAdjustsTheDividerLikeASlider() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        val node = onAllNodesWithDivider()[0]
        val before = node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        assertEquals("25 percent", before)
        node.performSemanticsAction(SemanticsActions.SetProgress) { it(0.4f) }
        waitForIdle()
        val config = onAllNodesWithDivider()[0].fetchSemanticsNode().config
        assertEquals("40 percent", config.getOrNull(SemanticsProperties.StateDescription))
    }

    @Test
    fun aDoubleClickResetsTheTwoNeighbours() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        three(state)
        state.resize(0, 100f)
        waitForIdle()
        onAllNodesWithDivider()[0].performMouseInput { doubleClick() }
        waitForIdle()
        onNodeWithTag("a").assertWidthIsEqualTo(199.dp)
        onNodeWithTag("b").assertWidthIsEqualTo(199.dp)
    }

    @Test
    fun aRestoredSnapshotKeepsItsFractionsOnADifferentWidth() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 1f))
        state.apply(CamoSplitSnapshot(listOf(1f, 2f, 1f), emptySet()))
        three(state, width = 400)
        onNodeWithTag("a").assertWidthIsEqualTo(99.dp)
        onNodeWithTag("b").assertWidthIsEqualTo(198.dp)
    }

    @Test
    fun rightToLeftPutsTheFirstPaneOnTheRightAndKeepsTheArrowsVisual() = runComposeUiTest {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        camo(LayoutDirection.Rtl) {
            Box(Modifier.size(800.dp, 200.dp)) {
                CamoSplitPane(Orientation.Horizontal, listOf(pane("a"), pane("b"), pane("c")), state)
            }
        }
        val a = onNodeWithTag("a").fetchSemanticsNode().boundsInRoot
        val c = onNodeWithTag("c").fetchSemanticsNode().boundsInRoot
        assertTrue(a.left > c.left)
        val divider = onAllNodesWithDivider()[0]
        divider.requestFocus()
        // Right moves the divider right, which shrinks the first pane (it is on the right).
        divider.performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithTag("a").assertWidthIsEqualTo(183.dp)
    }

    @Test
    fun aSplitMayBeNested() = runComposeUiTest {
        val outer = CamoSplitState(listOf(1f, 1f))
        val inner = CamoSplitState(listOf(1f, 1f))
        camo {
            Box(Modifier.size(600.dp, 400.dp)) {
                CamoSplitPane(
                    Orientation.Horizontal,
                    listOf(
                        pane("left"),
                        CamoPane { CamoSplitPane(Orientation.Vertical, listOf(pane("top"), pane("bottom")), inner) },
                    ),
                    outer,
                )
            }
        }
        onNodeWithTag("left").assertWidthIsEqualTo(299.dp)
        onNodeWithTag("top").assertHeightIsEqualTo(199.dp)
    }
}
