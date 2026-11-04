package com.srctool.camouflage.skin.material.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.datatable.ColumnAlign
import com.srctool.camouflage.core.datatable.DataTableRenderer
import com.srctool.camouflage.core.datatable.HeaderCellState
import com.srctool.camouflage.core.datatable.RowState
import com.srctool.camouflage.core.datatable.TableColors
import com.srctool.camouflage.core.datatable.TableState
import com.srctool.camouflage.core.datatable.TableStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.tree.TreeColors
import com.srctool.camouflage.core.tree.TreeNodeSlots
import com.srctool.camouflage.core.tree.TreeNodeState
import com.srctool.camouflage.core.tree.TreeRenderer
import com.srctool.camouflage.core.tree.TreeStyle
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.rememberSkinIndication

// Material's data table and tree: a list-like table with tonal headers and selection.

private val Hairline = 1.dp
private val SortIconSize = 14.dp
private val NodeIconSize = 20.dp
private val ToggleSize = 24.dp
private const val HALF_TURN = 180f
private const val QUARTER_TURN = 90f

internal object MaterialDataTableRenderer : DataTableRenderer {
    override fun defaults(theme: CamoTheme): TableStyle {
        val c = theme.colors
        return TableStyle(
            headerHeight = 56.dp,
            rowHeight = 52.dp,
            cellPadding = theme.spacing.l,
            striped = false,
            dividers = true,
            headerStyle = theme.typography.labelLarge,
            cellStyle = theme.typography.bodyMedium,
            sortIcon = IconSource.Vector(ArrowUpGlyph),
            colors = TableColors(c.surface, c.surfaceContainerHigh, Color.Transparent, c.surfaceContainerLow,
                c.secondaryContainer, c.outlineVariant, c.onSurface, c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Container(state: TableState, theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        val shape = theme.shapes.medium
        Box(modifier.clip(shape).background(style.colors.container).border(Hairline, style.colors.divider,
            shape)) { content() }
    }

    @Composable
    override fun HeaderRow(theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable RowScope.() -> Unit) {
        val line = style.colors.divider
        Row(
            modifier.height(style.headerHeight).background(style.colors.header).drawBehind {
                drawLine(line, Offset(0f, size.height - Hairline.toPx() / 2), Offset(size.width,
                    size.height - Hairline.toPx() / 2), Hairline.toPx())
            },
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }

    @Composable
    override fun HeaderCell(state: HeaderCellState, label: String, theme: CamoTheme, style: TableStyle,
        modifier: Modifier) {
        val c = style.colors
        val hover = if (state.interaction.hovered && state.sortable) c.selectedRow else Color.Transparent
        Row(
            modifier
                .fillMaxHeight()
                .background(hover)
                .indication(state.interactionSource, rememberSkinIndication(c.headerText, RectangleShape))
                .padding(horizontal = style.cellPadding),
            horizontalArrangement = arrangement(state.align),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(label, style = style.headerStyle.copy(color = c.headerText, fontWeight = FontWeight.SemiBold),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (state.sorted != null) {
                Spacer(Modifier.size(4.dp))
                Box(Modifier.rotate(if (state.sorted == true) 0f else HALF_TURN)) {
                    CamoIcon(style.sortIcon, contentDescription = null, size = SortIconSize, tint = c.headerText)
                }
            }
        }
    }

    @Composable
    override fun Row(state: RowState, theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable RowScope.() -> Unit) {
        val c = style.colors
        val fill = when {
            state.selected -> c.selectedRow
            state.striped && style.striped -> c.stripedRow
            else -> c.row
        }
        val line = if (style.dividers) c.divider else Color.Transparent
        Row(
            modifier
                .heightIn(min = style.rowHeight)
                .background(fill)
                .indication(state.interactionSource, rememberSkinIndication(c.text, RectangleShape))
                .drawBehind { drawLine(line, Offset(0f, size.height - Hairline.toPx() / 2), Offset(size.width,
                    size.height - Hairline.toPx() / 2), Hairline.toPx()) },
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }

    @Composable
    override fun Cell(align: ColumnAlign, theme: CamoTheme, style: TableStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        Box(modifier.padding(horizontal = style.cellPadding, vertical = 4.dp), contentAlignment = alignment(align)) {
            ProvideContentStyle(style.colors.text, IconStyle(NodeIconSize, style.colors.text)) { content() }
        }
    }
}

private fun arrangement(align: ColumnAlign) = when (align) {
    ColumnAlign.Start -> Arrangement.Start
    ColumnAlign.Center -> Arrangement.Center
    ColumnAlign.End -> Arrangement.End
}

private fun alignment(align: ColumnAlign) = when (align) {
    ColumnAlign.Start -> Alignment.CenterStart
    ColumnAlign.Center -> Alignment.Center
    ColumnAlign.End -> Alignment.CenterEnd
}

internal object MaterialTreeRenderer : TreeRenderer {
    override fun defaults(theme: CamoTheme): TreeStyle {
        val c = theme.colors
        return TreeStyle(
            rowHeight = 48.dp,
            indent = 24.dp,
            showLines = false,
            chevron = IconSource.Vector(ChevronRightGlyph, autoMirror = true),
            iconSize = NodeIconSize,
            textStyle = theme.typography.bodyMedium,
            colors = TreeColors(c.secondaryContainer, c.surfaceContainerLow, c.outlineVariant, c.onSurface,
                c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Node(state: TreeNodeState, slots: TreeNodeSlots, theme: CamoTheme, style: TreeStyle,
        modifier: Modifier) {
        val c = style.colors
        val fill = when {
            state.selected -> c.selected
            state.interaction.hovered -> c.hover
            else -> Color.Transparent
        }
        val guides = if (style.showLines) state.level else 0
        val indent = style.indent
        Row(
            modifier
                .heightIn(min = style.rowHeight)
                .background(fill)
                .indication(state.interactionSource, rememberSkinIndication(c.text, RectangleShape))
                .drawBehind {
                    for (level in 0 until guides) {
                        val x = (indent * level + ToggleSize / 2).toPx()
                        drawLine(c.line, Offset(x, 0f), Offset(x, size.height), Hairline.toPx(), StrokeCap.Butt)
                    }
                }
                .padding(start = indent * state.level, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(slots.toggle.size(ToggleSize), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CamoProgressCircle(size = ProgressSize.Small)
                    state.hasChildren -> Box(Modifier.rotate(if (state.expanded) QUARTER_TURN else 0f)) {
                        CamoIcon(style.chevron, contentDescription = null, size = style.iconSize * CHEVRON_SHARE,
                            tint = c.chevron)
                    }
                }
            }
            ProvideContentStyle(c.text, IconStyle(style.iconSize, c.text)) {
                slots.icon?.invoke()
                BasicText(slots.label, style = style.textStyle.copy(color = c.text), maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private const val CHEVRON_SHARE = 0.8f

internal val ArrowUpGlyph: ImageVector = ImageVector.Builder("arrow-up", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 19f)
        lineTo(12f, 5f)
        moveTo(6f, 11f)
        lineTo(12f, 5f)
        lineTo(18f, 11f)
    }
    .build()
