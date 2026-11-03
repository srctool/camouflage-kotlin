package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.datatable.ColumnWidth
import com.srctool.camouflage.core.datatable.TableSort
import com.srctool.camouflage.core.headerCells
import com.srctool.camouflage.core.lastTable
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.tableRows
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.tree.CamoTree
import com.srctool.camouflage.core.tree.CamoTreeNode
import com.srctool.camouflage.core.treeNodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The M10 data table and tree tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class ListsTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private data class Person(val id: Int, val name: String, val age: Int)

    private val people = listOf(Person(1, "Ada", 36), Person(2, "Grace", 45), Person(3, "Linus", 28))

    private val columns = listOf(
        CamoColumn<Person>("name", "Name", sortable = true, cell = { BasicText(it.name) }),
        CamoColumn<Person>("age", "Age", width = ColumnWidth.Fixed(80.dp), cell = { BasicText("${it.age}") }),
    )

    // --- data table ---

    @Test
    fun theHeaderShowsTheColumnsAndASortableOneReportsTheNewSort() = runComposeUiTest {
        var sort by mutableStateOf<TableSort?>(null)
        camo {
            CamoDataTable(columns, people, { it.id }, "People", sort = sort, onSortChange = { sort = it })
        }
        assertEquals(listOf(true, false), headerCells.map { it.sortable })
        onNodeWithText("Name").performClick()
        assertEquals(TableSort("name", true), sort)
        waitForIdle()
        assertEquals(true, headerCells.last { it.sortable }.sorted)
        onNodeWithText("Name").performClick()
        assertEquals(TableSort("name", false), sort)
        onNodeWithText("Age").performClick()
        assertEquals(TableSort("name", false), sort)
    }

    @Test
    fun theTableIsNamedAndReportsItsSize() = runComposeUiTest {
        camo { CamoDataTable(columns, people, { it.id }, "People") }
        onNodeWithContentDescription("People").assertExists()
        assertEquals(3, lastTable?.rowCount)
        assertEquals(2, lastTable?.columnCount)
        assertFalse(lastTable!!.selectable)
    }

    @Test
    fun aRowClickTogglesItsSelectionAndSelectAllSelectsEveryRow() = runComposeUiTest {
        var selection by mutableStateOf(emptySet<Any>())
        camo {
            CamoDataTable(columns, people, { it.id }, "People", selection = selection,
                onSelectionChange = { selection = it })
        }
        onNodeWithText("Grace").performClick()
        assertEquals(setOf<Any>(2), selection)
        waitForIdle()
        assertEquals(true, tableRows[1]?.selected)
        onNodeWithText("Grace").performClick()
        assertTrue(selection.isEmpty())
        onNodeWithContentDescription("Select all rows").performClick()
        assertEquals(setOf<Any>(1, 2, 3), selection)
        onNodeWithContentDescription("Select all rows").performClick()
        assertTrue(selection.isEmpty())
    }

    @Test
    fun aRowClickCallsTheCallbackInsteadWhenItIsSet() = runComposeUiTest {
        var clicked: Person? = null
        var selection by mutableStateOf(emptySet<Any>())
        camo {
            CamoDataTable(
                columns, people, { it.id }, "People",
                selection = selection, onSelectionChange = { selection = it }, onRowClick = { clicked = it },
            )
        }
        onNodeWithText("Ada").performClick()
        assertEquals(people[0], clicked)
        assertTrue(selection.isEmpty())
    }

    @Test
    fun arrowsMoveBetweenRowsAndSpaceTogglesTheFocusedOne() = runComposeUiTest {
        var selection by mutableStateOf(emptySet<Any>())
        camo {
            CamoDataTable(columns, people, { it.id }, "People", selection = selection,
                onSelectionChange = { selection = it })
        }
        onNodeWithText("Ada").requestFocus()
        onNodeWithText("Ada").performKeyInput { pressKey(Key.DirectionDown) }
        waitForIdle()
        onNodeWithText("Grace").assertIsFocused()
        onNodeWithText("Grace").performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(setOf<Any>(2), selection)
    }

    @Test
    fun anEmptyTableShowsItsSlotAndALoadingOneSaysSo() = runComposeUiTest {
        camo { CamoDataTable(columns, emptyList(), { it.id }, "People", empty = { BasicText("No people") }) }
        onNodeWithText("No people").assertExists()
        camo { CamoDataTable(columns, emptyList(), { it.id }, "People", loading = true) }
        onNodeWithContentDescription("People")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Loading"))
    }

    @Test
    fun manyRowsStayLazy() = runComposeUiTest {
        val many = List(1000) { Person(it, "person-$it", it) }
        camo { CamoDataTable(columns, many, { it.id }, "People", Modifier.height(300.dp)) }
        onNodeWithText("person-0").assertExists()
        onNodeWithText("person-999").assertDoesNotExist()
    }

    // --- tree ---

    private val tree = listOf(
        CamoTreeNode(
            "docs", "Documents",
            listOf(CamoTreeNode("cv", "CV"), CamoTreeNode("notes", "Notes", listOf(CamoTreeNode("n1", "Note 1")))),
        ),
        CamoTreeNode("pics", "Pictures", listOf(CamoTreeNode("p1", "Photo"))),
        CamoTreeNode("readme", "Readme"),
    )

    @Composable
    private fun Tree(
        expanded: Set<Any>,
        setExpanded: (Set<Any>) -> Unit,
        selected: Set<Any>,
        setSelected: (Set<Any>) -> Unit,
        multi: Boolean = false,
        roots: List<CamoTreeNode<String>> = tree,
        load: ((CamoTreeNode<String>) -> Unit)? = null,
    ) = CamoTree(roots, expanded, setExpanded, selected, setSelected, { it }, "Files", multiSelect = multi,
        onLoadChildren = load)

    @Test
    fun onlyTheExpandedNodesShowWithTheirLevels() = runComposeUiTest {
        camo { Tree(setOf("docs", "notes"), {}, emptySet(), {}) }
        listOf("Documents", "CV", "Notes", "Note 1", "Pictures", "Readme").forEach { onNodeWithText(it).assertExists() }
        onNodeWithText("Photo").assertDoesNotExist()
        assertEquals(0, treeNodes["Documents"]?.level)
        assertEquals(1, treeNodes["CV"]?.level)
        assertEquals(2, treeNodes["Note 1"]?.level)
        assertEquals(true, treeNodes["Documents"]?.expanded)
        assertEquals(false, treeNodes["Pictures"]?.expanded)
        assertEquals(false, treeNodes["Readme"]?.hasChildren)
    }

    @Test
    fun rightExpandsThenEntersAndLeftCollapsesThenGoesToTheParent() = runComposeUiTest {
        var expanded by mutableStateOf(emptySet<Any>())
        camo { Tree(expanded, { expanded = it }, emptySet(), {}) }
        onNodeWithText("Documents").requestFocus()
        onNodeWithText("Documents").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(setOf<Any>("docs"), expanded)
        waitForIdle()
        onNodeWithText("Documents").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        onNodeWithText("CV").assertIsFocused()
        onNodeWithText("CV").performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        onNodeWithText("Documents").assertIsFocused()
        onNodeWithText("Documents").performKeyInput { pressKey(Key.DirectionLeft) }
        assertTrue(expanded.isEmpty())
    }

    @Test
    fun homeAndEndJumpToTheEnds() = runComposeUiTest {
        camo { Tree(emptySet(), {}, emptySet(), {}) }
        onNodeWithText("Pictures").requestFocus()
        onNodeWithText("Pictures").performKeyInput { pressKey(Key.MoveEnd) }
        waitForIdle()
        onNodeWithText("Readme").assertIsFocused()
        onNodeWithText("Readme").performKeyInput { pressKey(Key.MoveHome) }
        waitForIdle()
        onNodeWithText("Documents").assertIsFocused()
    }

    @Test
    fun aClickSelectsAndMultiSelectToggles() = runComposeUiTest {
        var selected by mutableStateOf(emptySet<Any>())
        camo { Tree(emptySet(), {}, selected, { selected = it }) }
        onNodeWithText("Pictures").performClick()
        onNodeWithText("Readme").performClick()
        assertEquals(setOf<Any>("readme"), selected)
        camo { Tree(emptySet(), {}, selected, { selected = it }, multi = true) }
        onNodeWithText("Pictures").performClick()
        assertEquals(setOf<Any>("readme", "pics"), selected)
        onNodeWithText("Readme").performClick()
        assertEquals(setOf<Any>("pics"), selected)
    }

    @Test
    fun aNodeAnnouncesItsStateAndTheAccessibilityActionExpandsIt() = runComposeUiTest {
        var expanded by mutableStateOf(emptySet<Any>())
        camo { Tree(expanded, { expanded = it }, emptySet(), {}) }
        onNodeWithText("Documents")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        onNodeWithText("Documents").performSemanticsAction(SemanticsActions.Expand)
        assertEquals(setOf<Any>("docs"), expanded)
        waitForIdle()
        onNodeWithText("Documents")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
    }

    @Test
    fun aNodeWithUnloadedChildrenAsksForThemOnceWhenItExpands() = runComposeUiTest {
        var asked = 0
        var expanded by mutableStateOf(emptySet<Any>())
        val lazy = listOf(CamoTreeNode<String>("remote", "Remote", children = null, hasChildren = true))
        camo { Tree(expanded, { expanded = it }, emptySet(), {}, roots = lazy, load = { asked++ }) }
        assertEquals(0, asked)
        expanded = setOf("remote")
        waitForIdle()
        assertEquals(1, asked)
        assertEquals(true, treeNodes["Remote"]?.loading)
        assertNull(treeNodes["Missing"])
    }
}
