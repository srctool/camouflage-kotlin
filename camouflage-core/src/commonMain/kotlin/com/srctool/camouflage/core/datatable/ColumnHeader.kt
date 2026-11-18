package com.srctool.camouflage.core.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.menu.CamoContextMenuArea
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.menu.CamoMenuSubmenu
import com.srctool.camouflage.core.splitpane.resizeCursor
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState

private val KeyStep = 16.dp
private val GrabWidth = 8.dp
private val LineWidth = 2.dp
private const val DRAGGED_ALPHA = 0.6f
private const val FIT_ROWS = 200

/** What a header needs to know about its table: every column, the visible ones, the rows and the state. */
internal class ColumnContext<T>(
    val all: List<CamoColumn<T>>,
    val shown: List<CamoColumn<T>>,
    val rows: List<T>,
    val state: CamoColumnState,
)

/**
 * A header cell of a table with a `columnState`: the cell's sort click, a resize edge (drag; a double-click fits the
 * content), a drag and Alt+arrows to reorder, Ctrl+arrows to resize, a context menu to hide columns, and the same
 * as accessibility actions.
 */
@Composable
internal fun <T> RowScope.ColumnHeader(
    renderer: DataTableRenderer,
    theme: CamoTheme,
    style: TableStyle,
    column: CamoColumn<T>,
    index: Int,
    table: ColumnContext<T>,
    sort: TableSort?,
    onSortChange: ((TableSort) -> Unit)?,
) {
    val state = table.state
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val sorted = sort?.takeIf { it.columnKey == column.key }?.ascending
    val sortable = column.sortable && onSortChange != null
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragging by remember { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()

    fun widthNow(): Dp = with(density) { (state.measured[column.key] ?: 0f).toDp() }
    fun widen(by: Dp) = state.resize(column.key, widthNow() + by, column.minWidth)
    val move = { step: Int -> state.moveBy(column.key, if (rtl) -step else step) }
    var behavior = Modifier
        .fillMaxSize()
        .semantics(mergeDescendants = true) {
            customActions = columnActions(column, state, ::widen)
            if (sorted != null) stateDescription = if (sorted) "sorted ascending" else "sorted descending"
        }
        .onKeyEvent { headerKey(it, column, rtl, ::widen, move) }
        .focusable()
    if (sortable) {
        behavior = behavior.clickable(source, null, role = Role.Button) {
            onSortChange?.invoke(TableSort(column.key, ascending = sorted != true))
        }
    }
    behavior = behavior.reorderDrag(column, index, table, rtl) { dragging = it }
    Box(
        columnModifier(column.width)
            .onSizeChanged { state.measured[column.key] = it.width.toFloat() }
            .alpha(if (dragging) DRAGGED_ALPHA else 1f),
    ) {
        CamoContextMenuArea(headerMenu(column, table), Modifier.fillMaxSize()) {
            renderer.HeaderCell(
                HeaderCellState(sortable, sorted, column.align, interaction, source),
                column.header,
                theme,
                style,
                behavior,
            )
        }
        if (column.resizable) {
            ResizeEdge(
                theme,
                rtl,
                onDrag = { widen(with(density) { it.toDp() }) },
                onFit = { fitColumn(column, table, style, measurer, density) },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        DropLine(state.dropTarget == index, theme, Modifier.align(Alignment.CenterStart))
    }
}

private fun <T> columnActions(
    column: CamoColumn<T>,
    state: CamoColumnState,
    widen: (Dp) -> Unit,
): List<CustomAccessibilityAction> = buildList {
    if (column.resizable) {
        add(CustomAccessibilityAction("Widen column") { widen(KeyStep); true })
        add(CustomAccessibilityAction("Narrow column") { widen(-KeyStep); true })
    }
    if (column.reorderable) {
        add(CustomAccessibilityAction("Move column earlier") { state.moveBy(column.key, -1); true })
        add(CustomAccessibilityAction("Move column later") { state.moveBy(column.key, 1); true })
    }
    if (column.hideable) add(CustomAccessibilityAction("Hide column") { state.hide(column.key); true })
}

// A double-click on the edge: the width of the widest of the first rows' text, or the column's own width.
private fun <T> fitColumn(
    column: CamoColumn<T>,
    table: ColumnContext<T>,
    style: TableStyle,
    measurer: TextMeasurer,
    density: Density,
) {
    val texts = column.text
    if (texts == null) {
        table.state.resizeToOwn(column.key)
        return
    }
    val cells = table.rows.take(FIT_ROWS).map { measurer.measure(texts(it), style.cellStyle).size.width }
    val label = measurer.measure(column.header, style.headerStyle).size.width
    val widest = with(density) { maxOf(label + SORT_ROOM.roundToPx(), cells.maxOrNull() ?: 0).toDp() }
    table.state.resize(column.key, widest + style.cellPadding * 2, column.minWidth)
}

// Dragging the header reorders: a line shows the drop place and the header fades while it is dragged.
private fun <T> Modifier.reorderDrag(
    column: CamoColumn<T>,
    index: Int,
    table: ColumnContext<T>,
    rtl: Boolean,
    onDragging: (Boolean) -> Unit,
): Modifier = if (!column.reorderable) this else pointerInput(column.key, index) {
    val state = table.state
    var offset = 0f
    val finish = {
        state.dropTarget = CamoColumnState.NONE
        onDragging(false)
    }
    detectDragGestures(
        onDragStart = {
            offset = 0f
            onDragging(true)
        },
        onDrag = { change, amount ->
            change.consume()
            offset += amount.x * (if (rtl) -1 else 1)
            state.dropTarget = dropIndex(table.shown.map { state.measured[it.key] ?: 0f }, index, offset)
        },
        onDragEnd = {
            val target = state.dropTarget
            if (target >= 0 && target != index) {
                table.shown.getOrNull(target)?.let { state.move(column.key, state.order.indexOf(it.key)) }
            }
            finish()
        },
        onDragCancel = finish,
    )
}

// The primary line at the start of the header the dragged column would drop before.
@Composable
private fun DropLine(visible: Boolean, theme: CamoTheme, modifier: Modifier) {
    if (visible) Box(modifier.width(LineWidth).fillMaxHeight().background(theme.colors.primary))
}

private val SORT_ROOM = 24.dp

// The edge of a header that is dragged to resize; a double-click fits the content.
@Composable
private fun ResizeEdge(
    theme: CamoTheme,
    rtl: Boolean,
    onDrag: (Float) -> Unit,
    onFit: () -> Unit,
    modifier: Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val sign = if (rtl) -1f else 1f
    Box(
        modifier
            .width(GrabWidth)
            .fillMaxHeight()
            .resizeCursor(horizontal = true)
            .hoverable(source)
            .draggable(
                rememberDraggableState { onDrag(it * sign) },
                Orientation.Horizontal,
                onDragStarted = { dragging = true },
                onDragStopped = { dragging = false },
            )
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onFit() }) },
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (hovered || dragging) {
            Box(
                Modifier.width(LineWidth).fillMaxHeight()
                    .background(if (dragging) theme.colors.primary else theme.colors.outline),
            )
        }
    }
}

// Alt+Left and Alt+Right move the column, Ctrl+Left and Ctrl+Right resize it; arrows keep their visual meaning.
private fun <T> headerKey(
    event: KeyEvent,
    column: CamoColumn<T>,
    rtl: Boolean,
    widen: (Dp) -> Unit,
    move: (Int) -> Unit,
): Boolean {
    val right = event.key == Key.DirectionRight
    val arrow = event.type == KeyEventType.KeyDown && (right || event.key == Key.DirectionLeft)
    val reorder = arrow && event.isAltPressed && column.reorderable
    val resize = arrow && !reorder && event.isCtrlPressed && column.resizable
    if (reorder) move(if (right) 1 else -1)
    // The trailing edge is on the right, or on the left in right-to-left.
    if (resize) widen(if (right != rtl) KeyStep else -KeyStep)
    return reorder || resize
}

// The header's context menu: hide it, choose the visible columns, reset the widths.
private fun <T> headerMenu(column: CamoColumn<T>, table: ColumnContext<T>): List<CamoMenuEntry> {
    val state = table.state
    return buildList {
        if (column.hideable && table.shown.size > 1) add(CamoMenuItem("Hide column", { state.hide(column.key) }))
        add(
            CamoMenuSubmenu(
                "Columns",
                table.all.map { other ->
                    CamoMenuItem(
                        other.header,
                        { if (other.key in state.hidden) state.show(other.key) else state.hide(other.key) },
                        enabled = other.hideable || other.key in state.hidden,
                        checked = other.key !in state.hidden,
                    )
                },
            ),
        )
        add(CamoMenuItem("Reset widths", { state.resetWidths() }, enabled = state.widths.isNotEmpty()))
    }
}
