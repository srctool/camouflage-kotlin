package com.srctool.camouflage.core

import com.srctool.camouflage.core.dock.CamoDockState
import com.srctool.camouflage.core.dock.DockEdge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The placement, open and maximize rules of the dock layout (base CamoDockLayout, M16), without a window.
class DockStateTest {
    private fun state() = CamoDockState(
        linkedMapOf<Any, DockEdge>(
            "files" to DockEdge.Left,
            "search" to DockEdge.Left,
            "details" to DockEdge.Right,
            "log" to DockEdge.Bottom,
            "terminal" to DockEdge.Bottom,
        ),
        open = setOf("files", "log"),
    )

    @Test
    fun windowsKeepTheirStripeOrderPerEdge() {
        val s = state()
        assertEquals(listOf<Any>("files", "search"), s.windowsOn(DockEdge.Left))
        assertEquals(listOf<Any>("files"), s.openOn(DockEdge.Left))
        assertEquals(listOf<Any>("log"), s.openOn(DockEdge.Bottom))
    }

    @Test
    fun toggleOpensAndHidesAndSeveralWindowsShareAnEdge() {
        val s = state()
        s.toggle("search")
        assertEquals(listOf<Any>("files", "search"), s.openOn(DockEdge.Left))
        s.toggle("files")
        assertEquals(listOf<Any>("search"), s.openOn(DockEdge.Left))
        assertEquals(listOf<Any>("files", "search"), s.windowsOn(DockEdge.Left))
    }

    @Test
    fun movingAWindowJoinsTheOtherEdgeAtTheEndAndKeepsItOpen() {
        val s = state()
        s.move("files", DockEdge.Bottom)
        assertEquals(listOf<Any>("search"), s.windowsOn(DockEdge.Left))
        assertEquals(listOf<Any>("log", "terminal", "files"), s.windowsOn(DockEdge.Bottom))
        assertTrue(s.isOpen("files"))
        assertEquals(DockEdge.Bottom, s.edgeOf("files"))
    }

    @Test
    fun maximizeOpensTheWindowAndEndsWhenAnotherOpensOrItHides() {
        val s = state()
        s.maximize("details")
        assertEquals("details", s.maximized)
        assertTrue(s.isOpen("details"))
        s.open("search")
        assertNull(s.maximized)
        s.maximize("log")
        s.close("log")
        assertNull(s.maximized)
        s.maximize("log")
        s.restore()
        assertNull(s.maximized)
        assertTrue(s.isOpen("log"))
    }

    @Test
    fun anUnknownIdChangesNothing() {
        val s = state()
        s.open("nope")
        s.move("nope", DockEdge.Right)
        s.maximize("nope")
        assertFalse(s.isOpen("nope"))
        assertNull(s.maximized)
        assertEquals(5, s.ids.size)
    }

    @Test
    fun aLayoutRestoresFromItsSnapshot() {
        val s = state()
        s.move("log", DockEdge.Right)
        s.toggle("details")
        s.setSize(DockEdge.Left, 0.4f)
        s.maximize("files")
        val saved = s.snapshot()
        val restored = state()
        restored.apply(saved)
        assertEquals(saved, restored.snapshot())
        assertEquals(DockEdge.Right, restored.edgeOf("log"))
        assertEquals(0.4f, restored.sizeOf(DockEdge.Left))
        assertEquals("files", restored.maximized)
    }

    @Test
    fun aSnapshotWithUnknownOrMissingWindowsIsFitted() {
        val s = state()
        val saved = state().also { it.move("log", DockEdge.Left) }.snapshot()
        s.apply(
            saved.copy(
                order = listOf("gone", "log"),
                edges = saved.edges + ("gone" to DockEdge.Right),
                open = setOf("gone", "log"),
                maximized = "gone",
            ),
        )
        assertEquals(setOf<Any>("files", "search", "details", "log", "terminal"), s.ids.toSet())
        assertEquals("log", s.ids.first())
        assertEquals(listOf<Any>("log"), s.openOn(DockEdge.Left))
        assertNull(s.maximized)
    }

    @Test
    fun focusOpensTheWindow() {
        val s = state()
        s.focus("details")
        assertTrue(s.isOpen("details"))
    }
}
