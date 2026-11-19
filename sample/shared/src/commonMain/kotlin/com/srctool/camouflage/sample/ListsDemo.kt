package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoColumnState
import com.srctool.camouflage.core.speedsearch.CamoSpeedSearchState
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.datatable.ColumnAlign
import com.srctool.camouflage.core.datatable.ColumnWidth
import com.srctool.camouflage.core.datatable.TableSort
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.palette.CamoCommandItem
import com.srctool.camouflage.core.palette.CamoCommandPalette
import com.srctool.camouflage.core.palette.CamoCommandPaletteState
import com.srctool.camouflage.core.palette.CamoCommandSource
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.tree.CamoTree
import com.srctool.camouflage.core.tree.CamoTreeNode

// The M10 table and tree together: a table you can sort and select, and a tree whose branch loads when opened.

private val TableHeight = 260.dp
private val TreeHeight = 200.dp
private val AgeWidth = 72.dp
private const val REMOTE_DELAY = 600L

private data class Member(val id: Int, val name: String, val role: String, val age: Int)

private val Members = listOf(
    Member(1, "Ada Lovelace", "Mathematician", 36),
    Member(2, "Grace Hopper", "Admiral", 85),
    Member(3, "Linus Torvalds", "Maintainer", 55),
    Member(4, "Margaret Hamilton", "Engineer", 88),
    Member(5, "Alan Turing", "Cryptanalyst", 41),
)

@Composable
internal fun ListsDemo() {
    var sort by remember { mutableStateOf(TableSort("name")) }
    var selection by remember { mutableStateOf(emptySet<Any>()) }
    var expanded by remember { mutableStateOf(setOf<Any>("docs")) }
    var selected by remember { mutableStateOf(emptySet<Any>()) }
    var remote by remember { mutableStateOf<List<CamoTreeNode<String>>?>(null) }
    val rows = Members.sortedWith { a, b ->
        val order = when (sort.columnKey) {
            "age" -> a.age.compareTo(b.age)
            else -> a.name.compareTo(b.name)
        }
        if (sort.ascending) order else -order
    }
    val columns = listOf(
        CamoColumn<Member>("name", "Name", sortable = true, text = { it.name }, cell = { CamoText(it.name) }),
        CamoColumn<Member>("role", "Role", width = ColumnWidth.Weight(0.8f), cell = { CamoText(it.role) }),
        CamoColumn<Member>("age", "Age", ColumnWidth.Fixed(AgeWidth), ColumnAlign.End, true, { CamoText("${it.age}") }),
    )
    val columnState = remember { CamoColumnState(columns.map { it.key }) }
    val treeSearch = remember { CamoSpeedSearchState() }
    val tree = listOf(
        CamoTreeNode("docs", "Documents", listOf(CamoTreeNode("cv", "CV.pdf"), CamoTreeNode("notes", "Notes.md"))),
        CamoTreeNode("remote", "Shared drive", remote, hasChildren = true),
    )
    val palette = remember { CamoCommandPaletteState() }
    var lastRun by remember { mutableStateOf("Nothing yet") }
    val sources = remember {
        fun item(title: String, shortcut: String? = null) =
            CamoCommandItem(title, title, { lastRun = title }, shortcut = shortcut)
        listOf(
            CamoCommandSource.of(
                "Actions",
                listOf(item("Open Repository", "Ctrl+O"), item("Commit", "Ctrl+Enter"), item("Toggle Theme")),
            ),
            CamoCommandSource.of("Files", Members.map { item("${it.name}.md") }),
            // A slow source: the others answer at once and this group shows a loading row.
            CamoCommandSource("Remote", limit = 3) { query ->
                delay(REMOTE_DELAY)
                listOf(item("origin/main"), item("origin/$query")).filter { query.isNotEmpty() }
            },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CamoButton("Find action", { palette.show() })
        CamoText("Last run: $lastRun")
        CamoCommandPalette(palette, sources)
        CamoDataTable(
            columns, rows, { it.id }, "Members", Modifier.height(TableHeight),
            sort = sort, onSortChange = { sort = it },
            selection = selection, onSelectionChange = { selection = it },
            columnState = columnState,
        )
        CamoText("${selection.size} selected")
        CamoTree(
            tree, expanded, { expanded = it }, selected, { selected = it }, { it }, "Files",
                Modifier.height(TreeHeight), speedSearch = treeSearch,
            onLoadChildren = { remote = listOf(CamoTreeNode("backup", "Backup.zip"), CamoTreeNode("photos",
                "Photos")) },
        )
    }
}
