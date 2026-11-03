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
import com.srctool.camouflage.core.datatable.CamoColumn
import com.srctool.camouflage.core.datatable.CamoDataTable
import com.srctool.camouflage.core.datatable.ColumnAlign
import com.srctool.camouflage.core.datatable.ColumnWidth
import com.srctool.camouflage.core.datatable.TableSort
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.tree.CamoTree
import com.srctool.camouflage.core.tree.CamoTreeNode

// The M10 table and tree together: a table you can sort and select, and a tree whose branch loads when opened.

private val TableHeight = 260.dp
private val TreeHeight = 200.dp
private val AgeWidth = 72.dp

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
        CamoColumn<Member>("name", "Name", sortable = true, cell = { CamoText(it.name) }),
        CamoColumn<Member>("role", "Role", width = ColumnWidth.Weight(0.8f), cell = { CamoText(it.role) }),
        CamoColumn<Member>("age", "Age", ColumnWidth.Fixed(AgeWidth), ColumnAlign.End, true, { CamoText("${it.age}") }),
    )
    val tree = listOf(
        CamoTreeNode("docs", "Documents", listOf(CamoTreeNode("cv", "CV.pdf"), CamoTreeNode("notes", "Notes.md"))),
        CamoTreeNode("remote", "Shared drive", remote, hasChildren = true),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CamoDataTable(
            columns, rows, { it.id }, "Members", Modifier.height(TableHeight),
            sort = sort, onSortChange = { sort = it },
            selection = selection, onSelectionChange = { selection = it },
        )
        CamoText("${selection.size} selected")
        CamoTree(
            tree, expanded, { expanded = it }, selected, { selected = it }, { it }, "Files",
                Modifier.height(TreeHeight),
            onLoadChildren = { remote = listOf(CamoTreeNode("backup", "Backup.zip"), CamoTreeNode("photos",
                "Photos")) },
        )
    }
}
