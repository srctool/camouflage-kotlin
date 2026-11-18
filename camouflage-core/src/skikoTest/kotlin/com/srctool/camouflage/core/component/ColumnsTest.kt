package com.srctool.camouflage.core.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoColumnState
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.datatable.ColumnWidth
import com.srctool.camouflage.core.lastTable
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// The data table's resizable, hideable and reorderable columns (base CamoDataTable, M16).
@OptIn(ExperimentalTestApi::class)
class ColumnsTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private data class Person(val id: Int, val name: String, val age: Int)

    private val people = listOf(Person(1, "Ada", 36), Person(2, "Grace", 45))

    private val columns = listOf(
        CamoColumn<Person>(
            "name", "Name", width = ColumnWidth.Fixed(120.dp), text = { it.name }, cell = { BasicText(it.name) },
        ),
        CamoColumn<Person>("age", "Age", width = ColumnWidth.Fixed(80.dp), cell = { BasicText("${it.age}") }),
        CamoColumn<Person>("id", "Id", width = ColumnWidth.Fixed(60.dp), cell = { BasicText("${it.id}") }),
    )

    private fun ComposeUiTest.table(state: CamoColumnState) =
        camo { CamoDataTable(columns, people, { it.id }, "People", columnState = state) }

    private fun ComposeUiTest.left(label: String) = onNodeWithText(label).fetchSemanticsNode().boundsInRoot.left

    private fun ComposeUiTest.width(label: String) = onNodeWithText(label).fetchSemanticsNode().boundsInRoot.width

    private fun ComposeUiTest.order() = listOf("Name", "Age", "Id")
        .filter { onAllNodesWithTextCount(it) > 0 }
        .sortedBy { left(it) }

    private fun ComposeUiTest.onAllNodesWithTextCount(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size

    private fun SemanticsNodeInteraction.action(label: String) =
        fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }.action()

    @Test
    fun theStateReordersAndHidesTheColumns() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        table(state)
        assertEquals(listOf("Name", "Age", "Id"), order())
        state.move("id", 0)
        waitForIdle()
        assertEquals(listOf("Id", "Name", "Age"), order())
        state.hide("name")
        waitForIdle()
        assertEquals(listOf("Id", "Age"), order())
        assertEquals(2, lastTable?.columnCount)
    }

    @Test
    fun aSavedWidthIsUsedAndAColumnNotInTheStateKeepsItsOwn() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        state.resize("name", 200.dp)
        table(state)
        assertEquals(200f * density.density, width("Name"), 2f)
        assertEquals(80f * density.density, width("Age"), 2f)
    }

    @Test
    fun controlArrowsResizeAHeaderByAKeyStep() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        table(state)
        onNodeWithText("Name").requestFocus()
        onNodeWithText("Name").performKeyInput {
            keyDown(Key.CtrlLeft)
            pressKey(Key.DirectionRight)
            keyUp(Key.CtrlLeft)
        }
        waitForIdle()
        assertEquals(136f, state.widths["name"]!!.value, 1f)
        onNodeWithText("Name").performKeyInput {
            keyDown(Key.CtrlLeft)
            repeat(20) { pressKey(Key.DirectionLeft) }
            keyUp(Key.CtrlLeft)
        }
        waitForIdle()
        assertEquals(48f, state.widths["name"]!!.value, 1f) // the column's minimum
    }

    @Test
    fun altArrowsMoveAHeader() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        table(state)
        onNodeWithText("Name").requestFocus()
        onNodeWithText("Name").performKeyInput {
            keyDown(Key.AltLeft)
            pressKey(Key.DirectionRight)
            keyUp(Key.AltLeft)
        }
        waitForIdle()
        assertEquals(listOf("Age", "Name", "Id"), order())
    }

    @Test
    fun aScreenReaderCanWidenMoveAndHideAColumn() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        table(state)
        assertTrue(onNodeWithText("Age").action("Widen column"))
        waitForIdle()
        assertEquals(96f, state.widths["age"]!!.value, 1f)
        assertTrue(onNodeWithText("Age").action("Move column earlier"))
        waitForIdle()
        assertEquals(listOf("Age", "Name", "Id"), order())
        assertTrue(onNodeWithText("Id").action("Hide column"))
        waitForIdle()
        assertEquals(setOf<Any>("id"), state.hidden)
    }

    @Test
    fun draggingTheResizeEdgeWidensTheColumn() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        table(state)
        val header = onNodeWithText("Name").fetchSemanticsNode().boundsInRoot
        onRoot().performMouseInput {
            moveTo(Offset(header.right - 3f, header.center.y))
            press()
            moveBy(Offset(10f, 0f))
            moveBy(Offset(30f, 0f))
            release()
        }
        waitForIdle()
        assertNotNull(state.widths["name"])
        assertTrue(state.widths["name"]!!.value > 120f, "widened to ${state.widths["name"]}")
    }

    @Test
    fun aRightClickOnAHeaderOpensTheColumnsMenu() = runComposeUiTest {
        val state = CamoColumnState(listOf("name", "age", "id"))
        table(state)
        onNodeWithText("Age").performMouseInput { rightClick() }
        waitForIdle()
        onNodeWithText("Hide column").assertExists()
        onNodeWithText("Columns").assertExists()
        onNodeWithText("Reset widths").assertExists()
    }
}
