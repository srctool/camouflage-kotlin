package com.srctool.camouflage.core.tree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a tree uses. */
@Immutable
public data class TreeColors(val selected: Color, val hover: Color, val line: Color, val text: Color,
    val chevron: Color)

/** Overrides for trees. `null` keeps the skin's default. */
@Immutable
public data class TreeTheme(
    val rowHeight: Dp? = null,
    val indent: Dp? = null,
    val showLines: Boolean? = null,
    val chevron: IconSource? = null,
    val iconSize: Dp? = null,
    val textStyle: TextStyle? = null,
    val colors: TreeColors? = null,
)

/** The resolved trees style: every field set. */
@Immutable
public data class TreeStyle(
    val rowHeight: Dp,
    val indent: Dp,
    val showLines: Boolean,
    val chevron: IconSource,
    val iconSize: Dp,
    val textStyle: TextStyle,
    val colors: TreeColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: TreeTheme?): TreeStyle = if (overrides == null) {
        this
    } else {
        TreeStyle(
            rowHeight = overrides.rowHeight ?: rowHeight,
            indent = overrides.indent ?: indent,
            showLines = overrides.showLines ?: showLines,
            chevron = overrides.chevron ?: chevron,
            iconSize = overrides.iconSize ?: iconSize,
            textStyle = overrides.textStyle ?: textStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * A node of a [CamoTree]. [children] is `null` for a leaf, a list for loaded children; [hasChildren] `true`
 * with null [children] means they are not loaded yet (the tree asks for them when the node expands).
 */
@Immutable
public data class CamoTreeNode<T>(
    val key: Any,
    val value: T,
    val children: List<CamoTreeNode<T>>? = null,
    val hasChildren: Boolean = children != null,
)

/**
 * What a node row draws: its [level] (0 for a root), and whether it is [expanded], [loading] its children or
 * [selected].
 */
@Immutable
public data class TreeNodeState(
    val level: Int,
    val expanded: Boolean,
    val hasChildren: Boolean,
    val loading: Boolean,
    val selected: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** A node's content: its [icon] and [label], and the [toggle] behavior for the chevron's hit area. */
@Stable
public class TreeNodeSlots(
    public val icon: (@Composable () -> Unit)?,
    public val label: String,
    public val toggle: Modifier,
)

/** Draws one node row. Apply `modifier` (the behavior) to the outermost node, and `slots.toggle` to the chevron. */
@Stable
public interface TreeRenderer {
    /** The complete tree style for [theme]. */
    public fun defaults(theme: CamoTheme): TreeStyle

    /** Draws one node, indented by its level. */
    @Composable
    public fun Node(state: TreeNodeState, slots: TreeNodeSlots, theme: CamoTheme, style: TreeStyle, modifier: Modifier)
}

private class Visible<T>(val node: CamoTreeNode<T>, val level: Int, val parent: Int)

// The expanded part of the tree as a flat list, in reading order.
private fun <T> flatten(roots: List<CamoTreeNode<T>>, expanded: Set<Any>): List<Visible<T>> {
    val out = mutableListOf<Visible<T>>()
    fun visit(nodes: List<CamoTreeNode<T>>, level: Int, parent: Int) {
        for (node in nodes) {
            val at = out.size
            out += Visible(node, level, parent)
            if (node.key in expanded) node.children?.let { visit(it, level + 1, at) }
        }
    }
    visit(roots, 0, -1)
    return out
}

/**
 * Hierarchical data you can expand and collapse. The app owns [expanded] and [selected] (sets of node keys).
 * Click selects (with [multiSelect], toggles); the chevron expands. A node whose children aren't loaded asks for
 * them once through [onLoadChildren] when it expands. Up and Down move, Right expands or enters the first child,
 * Left collapses or goes to the parent, Home and End jump, Enter or Space selects. [caption] names the tree.
 */
@Composable
public fun <T> CamoTree(
    roots: List<CamoTreeNode<T>>,
    expanded: Set<Any>,
    onExpandedChange: (Set<Any>) -> Unit,
    selected: Set<Any>,
    onSelectedChange: (Set<Any>) -> Unit,
    label: (T) -> String,
    caption: String,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
    onLoadChildren: ((CamoTreeNode<T>) -> Unit)? = null,
    icon: ((T, Boolean) -> (@Composable () -> Unit))? = null,
) {
    val skin = requireSkin("CamoTree")
    val theme = requireTheme("CamoTree")
    val style = remember(skin, theme) { skin.tree.defaults(theme).merge(theme.components.tree) }
    val focus = rememberRovingFocus()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val visible = remember(roots, expanded) { flatten(roots, expanded) }
    val setExpanded = { key: Any, open: Boolean -> onExpandedChange(if (open) expanded + key else expanded - key) }
    val select = { key: Any ->
        onSelectedChange(if (multiSelect) (if (key in selected) selected - key else selected + key) else setOf(key))
    }
    LazyColumn(modifier.semantics {
        contentDescription = caption
        collectionInfo = CollectionInfo(visible.size, 1)
    }) {
        itemsIndexed(visible, key = { _, v -> v.node.key }) { index, v ->
            val node = v.node
            val open = node.key in expanded
            val unloaded = node.hasChildren && node.children == null
            if (open && unloaded && onLoadChildren != null) LaunchedEffect(node.key) { onLoadChildren(node) }
            NodeRow(
                skin.tree, theme, style, v, index, open, unloaded && open, node.key in selected, focus, rtl,
                visible, setExpanded, select, label, icon,
            )
        }
    }
}

@Composable
private fun <T> NodeRow(
    renderer: TreeRenderer,
    theme: CamoTheme,
    style: TreeStyle,
    v: Visible<T>,
    index: Int,
    open: Boolean,
    loading: Boolean,
    isSelected: Boolean,
    focus: RovingFocus,
    rtl: Boolean,
    visible: List<Visible<T>>,
    setExpanded: (Any, Boolean) -> Unit,
    select: (Any) -> Unit,
    label: (T) -> String,
    icon: ((T, Boolean) -> (@Composable () -> Unit))?,
) {
    val node = v.node
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val expandable = node.hasChildren
    val toggle = if (expandable) {
        Modifier.clickable(role = Role.Button, onClickLabel = if (open) "Collapse" else "Expand") {
            setExpanded(node.key, !open)
        }
    } else {
        Modifier
    }
    // Before the roving focus, so Left and Right expand and collapse instead of moving between rows.
    val behavior = Modifier
        .onPreviewKeyEvent { treeKey(it, index, v, open, rtl, visible.size, focus, setExpanded, select) }
        .rovingFocusItem(focus, index, enabled = true, selected = isSelected, onSelect = {})
        .semantics {
            selected = isSelected
            if (expandable) {
                stateDescription = if (open) "Expanded" else "Collapsed"
                if (open) collapse { setExpanded(node.key, false); true } else expand { setExpanded(node.key,
                    true); true }
            }
            collectionItemInfo = CollectionItemInfo(index, 1, v.level, 1)
        }
        .clickable(source, null, role = Role.Button) { select(node.key) }
    renderer.Node(
        TreeNodeState(v.level, open, expandable, loading, isSelected, interaction, source),
        TreeNodeSlots(icon?.invoke(node.value, open), label(node.value), toggle),
        theme,
        style,
        behavior,
    )
}

// Right and Left follow the reading direction: Right expands, then enters the first child; Left collapses, then
// goes to the parent. Home and End jump; Enter and Space select.
private fun <T> treeKey(
    event: KeyEvent,
    index: Int,
    v: Visible<T>,
    open: Boolean,
    rtl: Boolean,
    count: Int,
    focus: RovingFocus,
    setExpanded: (Any, Boolean) -> Unit,
    select: (Any) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val into = if (rtl) Key.DirectionLeft else Key.DirectionRight
    val out = if (rtl) Key.DirectionRight else Key.DirectionLeft
    val node = v.node
    return when (event.key) {
        into -> true.also { enter(v, index, open, focus, setExpanded) }
        out -> true.also { leave(v, index, open, focus, setExpanded) }
        Key.MoveHome -> focus.move(index, -index)
        Key.MoveEnd -> focus.move(index, count - 1 - index)
        Key.Enter, Key.Spacebar -> true.also { select(node.key) }
        else -> false
    }
}

// Right: expand a closed node, or enter the first child of an open one.
private fun <T> enter(v: Visible<T>, index: Int, open: Boolean, focus: RovingFocus, setExpanded: (Any,
    Boolean) -> Unit) {
    when {
        !v.node.hasChildren -> Unit
        !open -> setExpanded(v.node.key, true)
        else -> focus.move(index, 1)
    }
}

// Left: collapse an open node, or go to the parent.
private fun <T> leave(v: Visible<T>, index: Int, open: Boolean, focus: RovingFocus, setExpanded: (Any,
    Boolean) -> Unit) {
    when {
        v.node.hasChildren && open -> setExpanded(v.node.key, false)
        v.parent >= 0 -> focus.move(index, v.parent - index)
    }
}
