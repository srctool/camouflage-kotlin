package com.srctool.camouflage.core

import com.srctool.camouflage.core.tabs.CamoDocumentTabsState
import kotlin.test.Test
import kotlin.test.assertEquals

// The ordering and pinning rules of the document tabs (base CamoDocumentTabs, M16), without a window.
class DocumentTabsStateTest {
    private fun state() = CamoDocumentTabsState(listOf("a", "b", "c", "d"), setOf("b"))

    @Test
    fun pinnedTabsSitAtTheStart() {
        assertEquals(listOf<Any>("b", "a", "c", "d"), state().order)
        assertEquals(setOf<Any>("b"), state().pinned)
    }

    @Test
    fun aTabMovesWithinItsOwnGroupOnly() {
        val s = state()
        s.move("d", 0)
        assertEquals(listOf<Any>("b", "d", "a", "c"), s.order)
        s.move("b", 3)
        assertEquals(listOf<Any>("b", "d", "a", "c"), s.order)
        s.moveBy("a", 1)
        assertEquals(listOf<Any>("b", "d", "c", "a"), s.order)
    }

    @Test
    fun pinningMovesToTheEndOfThePinnedAndUnpinningToTheStartOfTheRest() {
        val s = state()
        s.pin("d")
        assertEquals(listOf<Any>("b", "d", "a", "c"), s.order)
        s.unpin("b")
        assertEquals(listOf<Any>("d", "b", "a", "c"), s.order)
        assertEquals(setOf<Any>("d"), s.pinned)
    }

    @Test
    fun syncAddsNewIdsAtTheEndAndDropsClosedOnes() {
        val s = state()
        s.sync(listOf("a", "c", "e", "b"))
        assertEquals(listOf<Any>("b", "a", "c", "e"), s.order)
        s.sync(listOf("a", "e"))
        assertEquals(listOf<Any>("a", "e"), s.order)
        assertEquals(emptySet(), s.pinned)
    }

    @Test
    fun sortedShowsADocumentListInTheStatesOrderWithoutChangingIt() {
        val s = state()
        assertEquals(listOf<Any>("b", "c", "e"), s.sorted(listOf("e", "c", "b")))
        assertEquals(listOf<Any>("b", "a", "c", "d"), s.order)
    }

    @Test
    fun aSnapshotRoundTrips() {
        val s = state()
        s.move("d", 1)
        val copy = CamoDocumentTabsState()
        copy.apply(s.snapshot())
        assertEquals(s.order, copy.order)
        assertEquals(s.pinned, copy.pinned)
    }

    @Test
    fun movingAnUnknownIdOrOutOfRangeIsSafe() {
        val s = state()
        s.move("zzz", 0)
        s.move("a", 99)
        s.move("a", -99)
        assertEquals(listOf<Any>("b", "a", "c", "d").size, s.order.size)
        assertEquals("a", s.order[1])
    }
}
