package com.srctool.camouflage.core.toolbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuController
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem
import kotlin.math.max

/** Where a toolbar sits: edge to edge, or detached from the edges. */
public enum class ToolbarPlacement { Docked, Floating }

/** Every color a toolbar uses. */
@Immutable
public data class ToolbarColors(val container: Color, val divider: Color)

/** Overrides for toolbars. `null` keeps the skin's default. */
@Immutable
public data class ToolbarTheme(
    val placement: ToolbarPlacement? = null,
    val height: Dp? = null,
    val actionSize: Dp? = null,
    val floatingMargin: Dp? = null,
    val floatingShape: Shape? = null,
    val floatingElevation: ElevationLevel? = null,
    val actionSpacing: Dp? = null,
    val padding: Dp? = null,
    val showDivider: Boolean? = null,
    val overflowIcon: IconSource? = null,
    val colors: ToolbarColors? = null,
)

/** The resolved toolbars style: every field set. */
@Immutable
public data class ToolbarStyle(
    val placement: ToolbarPlacement,
    val height: Dp,
    val actionSize: Dp,
    val floatingMargin: Dp,
    val floatingShape: Shape,
    val floatingElevation: ElevationLevel,
    val actionSpacing: Dp,
    val padding: Dp,
    val showDivider: Boolean,
    val overflowIcon: IconSource,
    val colors: ToolbarColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: ToolbarTheme?): ToolbarStyle = if (overrides == null) {
        this
    } else {
        ToolbarStyle(
            placement = overrides.placement ?: placement,
            height = overrides.height ?: height,
            actionSize = overrides.actionSize ?: actionSize,
            floatingMargin = overrides.floatingMargin ?: floatingMargin,
            floatingShape = overrides.floatingShape ?: floatingShape,
            floatingElevation = overrides.floatingElevation ?: floatingElevation,
            actionSpacing = overrides.actionSpacing ?: actionSpacing,
            padding = overrides.padding ?: padding,
            showDivider = overrides.showDivider ?: showDivider,
            overflowIcon = overrides.overflowIcon ?: overflowIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * One action of a [CamoToolbar]. [label] names it for screen readers and in the overflow menu. A non-null
 * [selected] makes it a toggle (Bold, Italic): the app owns the state and flips it in [onClick].
 */
@Immutable
public data class CamoToolbarAction(
    val key: Any,
    val label: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val selected: Boolean? = null,
    val tone: Tone = Tone.Default,
)

/** What the toolbar renderer draws: the [placement] and how many actions the toolbar was given. */
@Immutable
public data class ToolbarState(val placement: ToolbarPlacement, val count: Int)

/**
 * Draws toolbars: the container (surface, shape, depth, padding) around [content], which fills the width with
 * the leading content, the actions and the trailing content. Apply `modifier` (the semantics) to the outermost
 * node. The actions are `skin.iconButton`s.
 */
@Stable
public interface ToolbarRenderer {
    /** The complete toolbar style for [theme]. */
    public fun defaults(theme: CamoTheme): ToolbarStyle

    /** Draws the container around [content]. */
    @Composable
    public fun Container(
        state: ToolbarState,
        theme: CamoTheme,
        style: ToolbarStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    )
}

/**
 * A row of actions for the current context: formatting buttons in an editor, the bar under a message list.
 * Actions that don't fit collapse, in order, into a trailing "more" menu (a toggle there becomes a checkable
 * item). [leading] and [trailing] (a menu button, a title, a primary action) always show; custom content such as
 * button groups goes there or in a [CamoToolbarContainer]. One Tab stop: the arrow keys move between the
 * actions. [contentDescription] names the toolbar. A toolbar is neither navigation nor a title bar.
 */
@Composable
public fun CamoToolbar(
    actions: List<CamoToolbarAction>,
    modifier: Modifier = Modifier,
    placement: ToolbarPlacement? = null,
    contentDescription: String? = null,
    overflowLabel: String = "More",
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val skin = requireSkin("CamoToolbar")
    val theme = requireTheme("CamoToolbar")
    val style = remember(skin, theme) { skin.toolbar.defaults(theme).merge(theme.components.toolbar) }
    val focus = rememberRovingFocus()
    ToolbarSurface(placement ?: style.placement, actions.size, modifier, contentDescription) {
        ToolbarLayout(actions.size, style.actionSize, style.actionSpacing, leading, trailing) { fit ->
            actions.take(fit).forEachIndexed { index, action ->
                key(action.key) { ToolbarButton(action, index, focus) }
            }
            if (fit < actions.size) Overflow(actions.drop(fit), overflowLabel, fit, focus, style.overflowIcon)
        }
    }
}

/**
 * The building block (ADR-30) for custom toolbar content: the skin's toolbar surface in [placement], named
 * [contentDescription], with [content] in a row. Nothing in it overflows.
 */
@Composable
public fun CamoToolbarContainer(
    modifier: Modifier = Modifier,
    placement: ToolbarPlacement? = null,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val skin = requireSkin("CamoToolbarContainer")
    val theme = requireTheme("CamoToolbarContainer")
    val style = remember(skin, theme) { skin.toolbar.defaults(theme).merge(theme.components.toolbar) }
    ToolbarSurface(placement ?: style.placement, 0, modifier, contentDescription) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(style.actionSpacing),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun ToolbarSurface(
    placement: ToolbarPlacement,
    count: Int,
    modifier: Modifier,
    contentDescription: String?,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoToolbar")
    val theme = requireTheme("CamoToolbar")
    val style = remember(skin, theme) { skin.toolbar.defaults(theme).merge(theme.components.toolbar) }
    skin.toolbar.Container(
        ToolbarState(placement, count),
        theme,
        style,
        modifier.semantics {
            contentDescription?.let { this.contentDescription = it }
            collectionInfo = CollectionInfo(rowCount = 1, columnCount = count)
        },
        content,
    )
}

@Composable
private fun ToolbarButton(action: CamoToolbarAction, index: Int, focus: RovingFocus) {
    CamoIconButton(
        icon = action.icon,
        contentDescription = action.label,
        onClick = action.onClick,
        modifier = Modifier.rovingFocusItem(focus, index, action.enabled, selected = false, onSelect = {}),
        appearance = Appearance.Ghost,
        tone = action.tone,
        enabled = action.enabled,
        checked = action.selected,
    )
}

@Composable
private fun Overflow(
    actions: List<CamoToolbarAction>,
    label: String,
    index: Int,
    focus: RovingFocus,
    icon: IconSource,
) {
    val controller = remember { CamoMenuController() }
    val items = actions.map {
        CamoMenuItem(
            text = it.label,
            onClick = it.onClick,
            leadingIcon = it.icon,
            tone = it.tone,
            enabled = it.enabled,
            checked = it.selected,
        )
    }
    CamoMenuAnchor(items = items, controller = controller) { open ->
        CamoIconButton(
            icon = { CamoIcon(icon, contentDescription = null) },
            contentDescription = label,
            onClick = open,
            modifier = Modifier
                .rovingFocusItem(focus, index, enabled = true, selected = false, onSelect = {})
                .semantics { stateDescription = if (controller.isOpen) "Expanded" else "Collapsed" },
            appearance = Appearance.Ghost,
        )
    }
}

private enum class Slot { Leading, Actions, Trailing }

// Leading and trailing content take what they need; the actions share the rest, and those that don't fit move
// into the overflow. Needs the measured widths first, so the actions are composed last.
@Composable
private fun ToolbarLayout(
    count: Int,
    actionWidth: Dp,
    spacing: Dp,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    actions: @Composable (fit: Int) -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val start = leading?.let { slot -> subcompose(Slot.Leading, slot).map { it.measure(loose) } }.orEmpty()
        val end = trailing?.let { slot -> subcompose(Slot.Trailing, slot).map { it.measure(loose) } }.orEmpty()
        val used = start.sumOf { it.width } + end.sumOf { it.width }
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth - used else Int.MAX_VALUE
        val fit = fitActions(count, available, actionWidth.roundToPx(), spacing.roundToPx())
        val row = subcompose(Slot.Actions) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
                actions(fit)
            }
        }.map { it.measure(loose) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else used + row.sumOf { it.width }
        val height = max(constraints.minHeight, (start + row + end).maxOfOrNull { it.height } ?: 0)
        layout(width, height) {
            var x = 0
            fun place(items: List<Placeable>, at: Int) {
                var cursor = at
                items.forEach {
                    it.placeRelative(cursor, (height - it.height) / 2)
                    cursor += it.width
                }
            }
            place(start, x)
            x += start.sumOf { it.width }
            place(row, x)
            place(end, width - end.sumOf { it.width })
        }
    }
}

/**
 * How many of [count] actions of [action] px with [spacing] px between them show in [available] px: all, or
 * those that fit beside the overflow button (which is the width of one more action).
 */
internal fun fitActions(count: Int, available: Int, action: Int, spacing: Int): Int {
    fun width(n: Int) = n * action + (n - 1).coerceAtLeast(0) * spacing
    if (width(count) <= available) return count
    var shown = count - 1
    while (shown > 0 && width(shown + 1) > available) shown--
    return shown
}
