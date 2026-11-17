package com.srctool.camouflage.core

import com.srctool.camouflage.core.splitpane.CamoSplitState
import com.srctool.camouflage.core.splitpane.PaneLimits
import com.srctool.camouflage.core.splitpane.layoutSizes
import com.srctool.camouflage.core.splitpane.resize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// The split pane's layout rule and state, with no window.
class SplitLayoutTest {
    private val none = emptySet<Int>()
    private val limits = List(3) { PaneLimits(min = 50f) }

    private fun near(expected: Float, actual: Float) =
        assertTrue(kotlin.math.abs(expected - actual) < 0.5f, "$expected vs $actual")

    @Test
    fun sizesFollowTheFractions() {
        val sizes = layoutSizes(listOf(0.25f, 0.25f, 0.5f), none, 800f, limits)
        near(200f, sizes[0])
        near(200f, sizes[1])
        near(400f, sizes[2])
    }

    @Test
    fun aPaneNeverGetsSmallerThanItsMinimumOrLargerThanItsMaximum() {
        val sizes = layoutSizes(
            listOf(0.05f, 0.45f, 0.5f),
            none,
            1000f,
            listOf(PaneLimits(100f), PaneLimits(100f, max = 300f), PaneLimits(100f)),
        )
        near(100f, sizes[0])
        near(300f, sizes[1])
        near(1000f, sizes.sum())
    }

    @Test
    fun aContainerSmallerThanTheMinimumsShrinksThemInProportion() {
        val sizes = layoutSizes(listOf(0.5f, 0.5f), none, 100f, listOf(PaneLimits(100f), PaneLimits(300f)))
        near(25f, sizes[0])
        near(75f, sizes[1])
    }

    @Test
    fun aCollapsedPaneHasNoSizeAndItsSpaceGoesToTheOthers() {
        val sizes = layoutSizes(listOf(0.25f, 0.25f, 0.5f), setOf(2), 600f, limits)
        near(300f, sizes[0])
        near(300f, sizes[1])
        assertEquals(0f, sizes[2])
    }

    @Test
    fun aDividerMovesSpaceBetweenItsTwoNeighboursOnly() {
        val result = resize(listOf(0.25f, 0.25f, 0.5f), none, 0, 100f, 800f, limits, none)
        near(300f, result.fractions[0] * 800f)
        near(100f, result.fractions[1] * 800f)
        near(400f, result.fractions[2] * 800f)
    }

    @Test
    fun aDividerStopsAtTheNeighboursMinimum() {
        val result = resize(listOf(0.25f, 0.25f, 0.5f), none, 0, 10_000f, 800f, limits, none)
        near(350f, result.fractions[0] * 800f)
        near(50f, result.fractions[1] * 800f)
    }

    @Test
    fun aCollapsiblePaneDraggedBelowHalfItsMinimumCollapses() {
        val collapsible = setOf(1)
        assertEquals(1, resize(listOf(0.25f, 0.25f, 0.5f), none, 0, 190f, 800f, limits, collapsible).collapsed)
        assertEquals(null, resize(listOf(0.25f, 0.25f, 0.5f), none, 0, 140f, 800f, limits, collapsible).collapsed)
    }

    @Test
    fun nothingMovesNextToACollapsedPane() {
        val fractions = listOf(0.25f, 0.25f, 0.5f)
        assertEquals(fractions, resize(fractions, setOf(1), 0, 50f, 800f, limits, none).fractions)
    }

    @Test
    fun theStateScalesItsFractionsAndRestoresACollapsedPane() {
        val state = CamoSplitState(listOf(1f, 1f, 2f))
        assertEquals(listOf(0.25f, 0.25f, 0.5f), state.fractions)
        state.collapse(2)
        assertEquals(setOf(2), state.collapsed)
        state.restore(2)
        assertEquals(emptySet(), state.collapsed)
        assertEquals(listOf(0.25f, 0.25f, 0.5f), state.fractions) // back at its previous fraction
    }

    @Test
    fun theLastVisiblePaneCannotBeCollapsed() {
        val state = CamoSplitState(listOf(1f, 1f))
        state.collapse(0)
        state.collapse(1)
        assertEquals(setOf(0), state.collapsed)
    }

    @Test
    fun aSnapshotRestoresTheLayoutAndAWrongSizedOneIsIgnored() {
        val state = CamoSplitState(listOf(1f, 1f, 1f))
        state.apply(com.srctool.camouflage.core.splitpane.CamoSplitSnapshot(listOf(1f, 2f, 1f), setOf(1)))
        assertEquals(listOf(0.25f, 0.5f, 0.25f), state.fractions)
        assertEquals(setOf(1), state.collapsed)
        assertEquals(state.snapshot().fractions, listOf(0.25f, 0.5f, 0.25f))
        state.apply(com.srctool.camouflage.core.splitpane.CamoSplitSnapshot(listOf(1f, 1f), emptySet()))
        assertEquals(listOf(0.25f, 0.5f, 0.25f), state.fractions)
    }

    @Test
    fun aStateNeedsTwoPositivePanes() {
        assertFailsWith<IllegalArgumentException> { CamoSplitState(listOf(1f)) }
        assertFailsWith<IllegalArgumentException> { CamoSplitState(listOf(1f, 0f)) }
    }
}
