package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datatable.ColumnAlign
import com.srctool.camouflage.core.datatable.DataTableRenderer
import com.srctool.camouflage.core.datatable.HeaderCellState
import com.srctool.camouflage.core.datatable.RowState
import com.srctool.camouflage.core.datatable.TableColors
import com.srctool.camouflage.core.datatable.TableState
import com.srctool.camouflage.core.datatable.TableStyle
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.tree.TreeColors
import com.srctool.camouflage.core.tree.TreeNodeSlots
import com.srctool.camouflage.core.tree.TreeNodeState
import com.srctool.camouflage.core.tree.TreeRenderer
import com.srctool.camouflage.core.tree.TreeStyle

// The M10 data table and tree in the plain test skin: boxes, rows and text.

private val PlainListIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())

/** The state the plain table last drew, for tests. */
internal var lastTable: TableState? = null

/** The state each plain header cell, body row (by index) and tree node (by label) last drew, for tests. */
internal val headerCells = mutableListOf<HeaderCellState>()
internal val tableRows = mutableMapOf<Int, RowState>()
internal val treeNodes = mutableMapOf<String, TreeNodeState>()

internal object PlainDataTable : DataTableRenderer {
    override fun defaults(theme: CamoTheme) = TableStyle(
        40.dp, 44.dp, 8.dp, true, true, TextStyle.Default, TextStyle.Default, PlainListIcon,
        TableColors(Color.White, Color.LightGray, Color.Transparent, Color(0xFFEEEEEE), Color.Cyan, Color.Gray,
            Color.Black, Color.DarkGray),
    )

    @Composable
    override fun Container(state: TableState, theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        lastTable = state
        headerCells.clear()
        Box(modifier.background(style.colors.container)) { content() }
    }

    @Composable
    override fun HeaderRow(theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable RowScope.() -> Unit) {
        Row(modifier.background(style.colors.header), verticalAlignment = Alignment.CenterVertically, content = content)
    }

    @Composable
    override fun HeaderCell(state: HeaderCellState, label: String, theme: CamoTheme, style: TableStyle,
        modifier: Modifier) {
        headerCells += state
        Row(modifier.padding(8.dp)) {
            BasicText(label)
            if (state.sorted != null) BasicText(if (state.sorted == true) "^" else "v")
        }
    }

    @Composable
    override fun Row(state: RowState, theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable RowScope.() -> Unit) {
        tableRows[state.index] = state
        Row(
            modifier.background(if (state.selected) style.colors.selectedRow else style.colors.row),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }

    @Composable
    override fun Cell(align: ColumnAlign, theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        Box(modifier.padding(8.dp)) { content() }
    }
}

internal object PlainTree : TreeRenderer {
    override fun defaults(theme: CamoTheme) = TreeStyle(
        32.dp, 16.dp, false, PlainListIcon, 20.dp, TextStyle.Default,
        TreeColors(Color.Cyan, Color.LightGray, Color.Gray, Color.Black, Color.DarkGray),
    )

    @Composable
    override fun Node(state: TreeNodeState, slots: TreeNodeSlots, theme: CamoTheme, style: TreeStyle,
        modifier: Modifier) {
        treeNodes[slots.label] = state
        Row(modifier.padding(start = 16.dp * state.level)) {
            Box(slots.toggle.size(24.dp)) { if (state.hasChildren) BasicText(if (state.expanded) "-" else "+") }
            slots.icon?.invoke()
            BasicText(slots.label)
        }
    }
}
