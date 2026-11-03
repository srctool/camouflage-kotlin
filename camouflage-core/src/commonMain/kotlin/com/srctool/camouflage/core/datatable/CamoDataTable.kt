package com.srctool.camouflage.core.datatable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a data table uses. */
@Immutable
public data class TableColors(
    val container: Color,
    val header: Color,
    val row: Color,
    val stripedRow: Color,
    val selectedRow: Color,
    val divider: Color,
    val text: Color,
    val headerText: Color,
)

/** Overrides for data tables. `null` keeps the skin's default. */
@Immutable
public data class TableTheme(
    val headerHeight: Dp? = null,
    val rowHeight: Dp? = null,
    val cellPadding: Dp? = null,
    val striped: Boolean? = null,
    val dividers: Boolean? = null,
    val headerStyle: TextStyle? = null,
    val cellStyle: TextStyle? = null,
    val sortIcon: IconSource? = null,
    val colors: TableColors? = null,
)

/** The resolved data tables style: every field set. */
@Immutable
public data class TableStyle(
    val headerHeight: Dp,
    val rowHeight: Dp,
    val cellPadding: Dp,
    val striped: Boolean,
    val dividers: Boolean,
    val headerStyle: TextStyle,
    val cellStyle: TextStyle,
    val sortIcon: IconSource,
    val colors: TableColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: TableTheme?): TableStyle = if (overrides == null) {
        this
    } else {
        TableStyle(
            headerHeight = overrides.headerHeight ?: headerHeight,
            rowHeight = overrides.rowHeight ?: rowHeight,
            cellPadding = overrides.cellPadding ?: cellPadding,
            striped = overrides.striped ?: striped,
            dividers = overrides.dividers ?: dividers,
            headerStyle = overrides.headerStyle ?: headerStyle,
            cellStyle = overrides.cellStyle ?: cellStyle,
            sortIcon = overrides.sortIcon ?: sortIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/** How wide a column is: a fixed width, or a share of what the fixed columns leave. */
@Immutable
public sealed interface ColumnWidth {
    /** Exactly [width]. */
    public data class Fixed(val width: Dp) : ColumnWidth

    /** [weight] parts of the remaining width. */
    public data class Weight(val weight: Float = 1f) : ColumnWidth
}

/** How a column's content lines up. */
public enum class ColumnAlign { Start, Center, End }

/** A column: its [header], [width], whether it can be sorted, and how to draw its [cell] for a row. */
@Immutable
public data class CamoColumn<T>(
    val key: Any,
    val header: String,
    val width: ColumnWidth = ColumnWidth.Weight(),
    val align: ColumnAlign = ColumnAlign.Start,
    val sortable: Boolean = false,
    val cell: @Composable (T) -> Unit,
)

/** The sort the table shows: by the column with [columnKey], ascending or not. The app sorts the rows. */
@Immutable
public data class TableSort(val columnKey: Any, val ascending: Boolean = true)

/** What the container renderer draws: the frame around the header and the body. */
@Immutable
public data class TableState(val columnCount: Int, val rowCount: Int, val selectable: Boolean, val loading: Boolean)

/** What a header cell draws: [sorted] is `null` unless the table is sorted by this column. */
@Immutable
public data class HeaderCellState(
    val sortable: Boolean,
    val sorted: Boolean?,
    val align: ColumnAlign,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** What a body row draws: [selected] rows and [striped] alternate rows get their fills; show hover and focus. */
@Immutable
public data class RowState(
    val index: Int,
    val selected: Boolean,
    val striped: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/**
 * Draws data tables in parts. Core sizes the columns (the same widths in the header and every row) and fills
 * each row's [RowScope] with cells; a skin draws the frame, the rows and the headers.
 */
@Stable
public interface DataTableRenderer {
    /** The complete data table style for [theme]. */
    public fun defaults(theme: CamoTheme): TableStyle

    /** The frame around the header and the body. */
    @Composable
    public fun Container(
        state: TableState,
        theme: CamoTheme,
        style: TableStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    )

    /** The header row around the header cells. */
    @Composable
    public fun HeaderRow(theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable RowScope.() -> Unit)

    /** A header cell with [label] and the sort arrow. Apply `modifier` (the behavior) to the outermost node. */
    @Composable
    public fun HeaderCell(
        state: HeaderCellState,
        label: String,
        theme: CamoTheme,
        style: TableStyle,
        modifier: Modifier,
    )

    /** A body row around its cells. Apply `modifier` (the behavior) to the outermost node. */
    @Composable
    public fun Row(
        state: RowState,
        theme: CamoTheme,
        style: TableStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    )

    /** A cell's frame (padding and alignment) around the app's [content]. */
    @Composable
    public fun Cell(
        align: ColumnAlign,
        theme: CamoTheme,
        style: TableStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    )
}

private val SelectColumn = 48.dp

/**
 * Rows of records in columns, with sorting, selection and an optional footer. The app owns the data, the [sort]
 * (and does the sorting) and the [selection] (a set of [rowKey]s; `null` means rows can't be selected). A click
 * on a header of a sortable column calls [onSortChange]; a click or Space on a row toggles its selection (a
 * click calls [onRowClick] instead when it is set); Up and Down move between rows. [caption] names the table.
 * Rows are lazy; the header stays above them.
 */
@Composable
public fun <T> CamoDataTable(
    columns: List<CamoColumn<T>>,
    rows: List<T>,
    rowKey: (T) -> Any,
    caption: String,
    modifier: Modifier = Modifier,
    sort: TableSort? = null,
    onSortChange: ((TableSort) -> Unit)? = null,
    selection: Set<Any>? = null,
    onSelectionChange: ((Set<Any>) -> Unit)? = null,
    onRowClick: ((T) -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    loading: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val skin = requireSkin("CamoDataTable")
    val theme = requireTheme("CamoDataTable")
    val style = remember(skin, theme) { skin.dataTable.defaults(theme).merge(theme.components.dataTable) }
    val focus = rememberRovingFocus()
    val selectable = selection != null
    val allSelected = selectable && rows.isNotEmpty() && rows.all { rowKey(it) in selection.orEmpty() }
    val toggleAll = {
        onSelectionChange?.invoke(if (allSelected) emptySet() else rows.map(rowKey).toSet())
    }
    val toggle: (Any) -> Unit = { key ->
        val current = selection.orEmpty()
        onSelectionChange?.invoke(if (key in current) current - key else current + key)
    }
    val behavior = modifier.semantics {
        contentDescription = caption
        if (loading) stateDescription = "Loading"
    }
    skin.dataTable.Container(TableState(columns.size, rows.size, selectable, loading), theme, style, behavior) {
        Column {
            skin.dataTable.HeaderRow(theme, style, Modifier.fillMaxWidth()) {
                if (selectable) {
                    Box(Modifier.width(SelectColumn), contentAlignment = Alignment.Center) {
                        CamoCheckbox(
                            allSelected,
                            { toggleAll() },
                            Modifier.semantics { contentDescription = "Select all rows" },
                        )
                    }
                }
                columns.forEach { column -> Header(skin.dataTable, theme, style, column, sort, onSortChange) }
            }
            Box(Modifier.weight(1f, fill = false)) {
                when {
                    rows.isEmpty() -> EmptyBody(loading, empty)
                    else -> LazyColumn(
                        Modifier.semantics { collectionInfo = CollectionInfo(rows.size, columns.size) },
                    ) {
                        itemsIndexed(rows, key = { _, row -> rowKey(row) }) { index, row ->
                            BodyRow(
                                skin.dataTable, theme, style, columns, row, index, rowKey(row), focus,
                                selection, toggle, onRowClick,
                            )
                        }
                    }
                }
            }
            footer?.invoke()
        }
    }
}

@Composable
private fun EmptyBody(loading: Boolean, empty: (@Composable () -> Unit)?) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (loading) CamoProgressCircle(label = "Loading") else empty?.invoke()
    }
}

@Composable
private fun <T> RowScope.Header(
    renderer: DataTableRenderer,
    theme: CamoTheme,
    style: TableStyle,
    column: CamoColumn<T>,
    sort: TableSort?,
    onSortChange: ((TableSort) -> Unit)?,
) {
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val sorted = sort?.takeIf { it.columnKey == column.key }?.ascending
    val sortable = column.sortable && onSortChange != null
    var behavior = columnModifier(column.width)
    if (sortable) {
        behavior = behavior
            .semantics {
                if (sorted != null) stateDescription = if (sorted) "sorted ascending" else "sorted descending"
            }
            .clickable(source, null, role = Role.Button) {
                onSortChange?.invoke(TableSort(column.key, ascending = sorted != true))
            }
    }
    renderer.HeaderCell(HeaderCellState(sortable, sorted, column.align, interaction, source), column.header, theme,
        style, behavior)
}

@Composable
private fun RowScope.columnModifier(width: ColumnWidth): Modifier = when (width) {
    is ColumnWidth.Fixed -> Modifier.width(width.width)
    is ColumnWidth.Weight -> Modifier.weight(width.weight)
}

@Composable
private fun <T> BodyRow(
    renderer: DataTableRenderer,
    theme: CamoTheme,
    style: TableStyle,
    columns: List<CamoColumn<T>>,
    row: T,
    index: Int,
    key: Any,
    focus: RovingFocus,
    selection: Set<Any>?,
    toggle: (Any) -> Unit,
    onRowClick: ((T) -> Unit)?,
) {
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val selected = selection != null && key in selection
    val act = { if (onRowClick != null) onRowClick(row) else if (selection != null) toggle(key) }
    val behavior = Modifier
        .fillMaxWidth()
        .rovingFocusItem(focus, index, enabled = true, selected = selected, onSelect = {})
        .onKeyEvent {
            val space = it.type == KeyEventType.KeyDown && it.key == Key.Spacebar && selection != null
            if (space) toggle(key)
            space
        }
        .semantics {
            this.selected = selected
            collectionItemInfo = CollectionItemInfo(index, 1, 0, columns.size)
        }
        .clickable(source, null, role = Role.Button, onClick = act)
    renderer.Row(RowState(index, selected, index % 2 == 1, interaction, source), theme, style, behavior) {
        if (selection != null) {
            Box(Modifier.width(SelectColumn), contentAlignment = Alignment.Center) {
                CamoCheckbox(
                    selected,
                    { toggle(key) },
                    Modifier.semantics { contentDescription = "Select row" },
                )
            }
        }
        columns.forEach { column ->
            renderer.Cell(column.align, theme, style, columnModifier(column.width)) { column.cell(row) }
        }
    }
}
