package com.srctool.camouflage.core

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoColumnSnapshot
import com.srctool.camouflage.core.datatable.CamoColumnState
import com.srctool.camouflage.core.datatable.ColumnWidth
import com.srctool.camouflage.core.datatable.dropIndex
import com.srctool.camouflage.core.datatable.shown
import kotlin.test.Test
import kotlin.test.assertEquals

// The data table's column state, with no window.
class ColumnStateTest {
    private fun columns() = listOf("a", "b", "c", "d").map { key ->
        CamoColumn<Int>(key, key.uppercase(), cell = { BasicText("$it") })
    }

    @Test
    fun moveAndMoveByChangeTheOrderAndSkipHiddenColumns() {
        val state = CamoColumnState(listOf("a", "b", "c", "d"))
        state.move("d", 0)
        assertEquals(listOf("d", "a", "b", "c"), state.order)
        state.hide("a")
        state.moveBy("d", 1) // past the hidden "a" to "b"
        assertEquals(listOf("a", "b", "d", "c"), state.order)
        state.moveBy("c", 1) // already last: nothing
        assertEquals(listOf("a", "b", "d", "c"), state.order)
    }

    @Test
    fun theLastVisibleColumnStaysAndShowBringsOneBack() {
        val state = CamoColumnState(listOf("a", "b"))
        state.hide("a")
        state.hide("b")
        assertEquals(setOf<Any>("a"), state.hidden)
        state.show("a")
        assertEquals(emptySet(), state.hidden)
    }

    @Test
    fun aWidthIsAtLeastTheMinimumAndResetsGoBack() {
        val state = CamoColumnState(listOf("a", "b"))
        state.resize("a", 10.dp, min = 48.dp)
        assertEquals(48.dp, state.widths["a"])
        state.resize("a", 120.dp)
        state.resize("b", 90.dp)
        state.resizeToOwn("a")
        assertEquals(mapOf<Any, Any>("b" to 90.dp), state.widths)
        state.resetWidths()
        assertEquals(emptyMap(), state.widths)
    }

    @Test
    fun theTablesColumnsFollowTheStateOrderHiddenAndWidths() {
        val state = CamoColumnState(listOf("a", "b", "c", "d"))
        state.move("c", 0)
        state.hide("b")
        state.resize("d", 100.dp)
        val shown = state.shown(columns())
        assertEquals(listOf<Any>("c", "a", "d"), shown.map { it.key })
        assertEquals(ColumnWidth.Fixed(100.dp), shown.last().width)
    }

    @Test
    fun aColumnTheStateDoesNotKnowIsAppendedAndAnUnknownKeyIsIgnored() {
        val state = CamoColumnState(listOf("a", "b"))
        assertEquals(listOf<Any>("a", "b", "c", "d"), state.shown(columns()).map { it.key })
    }

    @Test
    fun aSnapshotRestoresTheLayoutAndIgnoresWhatTheTableDoesNotHave() {
        val state = CamoColumnState(listOf("a", "b", "c"))
        state.apply(CamoColumnSnapshot(listOf("c", "x", "a"), mapOf("a" to 80.dp, "x" to 10.dp), setOf("b", "x")))
        assertEquals(listOf<Any>("c", "a", "b"), state.order)
        assertEquals(mapOf<Any, Any>("a" to 80.dp), state.widths)
        assertEquals(setOf<Any>("b"), state.hidden)
        assertEquals(state.order, state.snapshot().order)
        // A snapshot that would hide everything shows everything.
        state.apply(CamoColumnSnapshot(listOf("a", "b", "c"), emptyMap(), setOf("a", "b", "c")))
        assertEquals(emptySet(), state.hidden)
        state.reset()
        assertEquals(listOf<Any>("a", "b", "c"), state.order)
    }

    @Test
    fun aDroppedHeaderLandsWhereItsCenterIs() {
        val widths = listOf(100f, 100f, 100f)
        assertEquals(0, dropIndex(widths, from = 1, offset = -100f))
        assertEquals(1, dropIndex(widths, from = 1, offset = 20f))
        assertEquals(2, dropIndex(widths, from = 1, offset = 100f))
        assertEquals(2, dropIndex(widths, from = 0, offset = 5000f))
    }
}
