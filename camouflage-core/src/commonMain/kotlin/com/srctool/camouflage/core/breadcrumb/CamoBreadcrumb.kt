package com.srctool.camouflage.core.breadcrumb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a breadcrumb uses: [link] for items you can go to, [current] for the last one. */
@Immutable
public data class BreadcrumbColors(val link: Color, val current: Color, val separator: Color, val hover: Color)

/** Overrides for breadcrumbs. `null` keeps the skin's default. */
@Immutable
public data class BreadcrumbTheme(
    val separator: IconSource? = null,
    val itemPadding: Dp? = null,
    val overflowIcon: IconSource? = null,
    val iconSize: Dp? = null,
    val textStyle: TextStyle? = null,
    val colors: BreadcrumbColors? = null,
)

/** The resolved breadcrumbs style: every field set. */
@Immutable
public data class BreadcrumbStyle(
    val separator: IconSource,
    val itemPadding: Dp,
    val overflowIcon: IconSource,
    val iconSize: Dp,
    val textStyle: TextStyle,
    val colors: BreadcrumbColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: BreadcrumbTheme?): BreadcrumbStyle = if (overrides == null) {
        this
    } else {
        BreadcrumbStyle(
            separator = overrides.separator ?: separator,
            itemPadding = overrides.itemPadding ?: itemPadding,
            overflowIcon = overrides.overflowIcon ?: overflowIcon,
            iconSize = overrides.iconSize ?: iconSize,
            textStyle = overrides.textStyle ?: textStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/** One step of the trail. A null [onClick] makes it plain text. */
@Immutable
public data class CamoBreadcrumbItem(
    val key: Any,
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
)

/** What the container renderer draws: the row around the items. [collapsed] is how many items moved into the menu. */
@Immutable
public data class BreadcrumbState(val count: Int, val collapsed: Int)

/** What an item draws: the [current] (last) one is not a link; [link] items show hover and focus. */
@Immutable
public data class BreadcrumbItemState(
    val current: Boolean,
    val link: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** An item's content. */
@Stable
public class BreadcrumbSlots(public val label: String, public val icon: (@Composable () -> Unit)?)

/** Draws breadcrumbs in parts: the container, each item and the separator between them. */
@Stable
public interface BreadcrumbRenderer {
    /** The complete breadcrumb style for [theme]. */
    public fun defaults(theme: CamoTheme): BreadcrumbStyle

    /** The row around [content]. */
    @Composable
    public fun Container(
        state: BreadcrumbState,
        theme: CamoTheme,
        style: BreadcrumbStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    )

    /** One item. */
    @Composable
    public fun Item(
        state: BreadcrumbItemState,
        slots: BreadcrumbSlots,
        theme: CamoTheme,
        style: BreadcrumbStyle,
        modifier: Modifier,
    )

    /** The separator between two items. */
    @Composable
    public fun Separator(theme: CamoTheme, style: BreadcrumbStyle)
}

/** Which items show: the first [head], then (when some are hidden) the overflow, then the last [tail]. */
internal class CollapsePlan(val head: Int, val tail: Int)

/**
 * Chooses what shows in [available] px. Everything shows when it fits (and within [maxVisible]); otherwise the
 * first item stays, the middle collapses behind an overflow button, and as many of the last items as fit stay.
 */
internal fun planBreadcrumb(
    widths: List<Int>,
    separator: Int,
    overflow: Int,
    available: Int,
    maxVisible: Int?,
): CollapsePlan {
    val n = widths.size
    val all = widths.sum() + separator * (n - 1).coerceAtLeast(0)
    val fitsAll = all <= available && (maxVisible == null || n <= maxVisible)
    if (n <= 2 || fitsAll) return CollapsePlan(n, 0)
    var tail = n - 2
    while (tail > 1 && !fits(widths, separator, overflow, available, maxVisible, tail)) tail--
    return CollapsePlan(1, tail)
}

private fun fits(widths: List<Int>, separator: Int, overflow: Int, available: Int, maxVisible: Int?,
    tail: Int): Boolean {
    val width = widths[0] + overflow + widths.takeLast(tail).sum() + separator * (tail + 1)
    return width <= available && (maxVisible == null || 1 + tail <= maxVisible)
}

private enum class Slot { Measure, Separator, Overflow, Shown }

/**
 * A trail of where you are: the last item is the current location, earlier ones link up the hierarchy. When it
 * doesn't fit (or has more than [maxVisible] items) the middle items collapse into a "…" menu. One Tab stop: the
 * arrow keys move along the trail. [label] names the navigation; the current item announces "current page".
 */
@Composable
public fun CamoBreadcrumb(
    items: List<CamoBreadcrumbItem>,
    modifier: Modifier = Modifier,
    maxVisible: Int? = null,
    label: String = "Breadcrumb",
) {
    if (items.isEmpty()) return
    val skin = requireSkin("CamoBreadcrumb")
    val theme = requireTheme("CamoBreadcrumb")
    val style = remember(skin, theme) { skin.breadcrumb.defaults(theme).merge(theme.components.breadcrumb) }
    val focus = rememberRovingFocus()
    val behavior = modifier.semantics {
        contentDescription = label
        isTraversalGroup = true
    }
    // The widths are measured on plain copies of the parts, so the shown parts' behavior can't change them.
    SubcomposeLayout(behavior) { constraints ->
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val widths = items.mapIndexed { i, item ->
            subcompose(Pair(Slot.Measure, i)) {
                Box(Modifier.clearAndSetSemantics {}) { ItemView(skin.breadcrumb, theme, style, item,
                    i == items.lastIndex, null, null) }
            }
                .maxOf { it.measure(loose).width }
        }
        val separator = subcompose(Slot.Separator) {
            Box(Modifier.clearAndSetSemantics {}) { skin.breadcrumb.Separator(theme, style) }
        }.maxOf { it.measure(loose).width }
        val overflow = subcompose(Slot.Overflow) { Box(Modifier.clearAndSetSemantics {}) { OverflowButton(emptyList(),
            style, null, 0, "More") } }
            .maxOf { it.measure(loose).width }
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val plan = planBreadcrumb(widths, separator, overflow, available, maxVisible)
        val placeables = subcompose(Slot.Shown) {
            Shown(skin.breadcrumb, theme, style, items, plan, focus)
        }.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) { placeables.forEach { it.placeRelative(0, 0) } }
    }
}

@Composable
private fun Shown(
    renderer: BreadcrumbRenderer,
    theme: CamoTheme,
    style: BreadcrumbStyle,
    items: List<CamoBreadcrumbItem>,
    plan: CollapsePlan,
    focus: RovingFocus,
) {
    val hidden = items.subList(plan.head.coerceAtMost(items.size), items.size - plan.tail)
    val collapsed = if (plan.tail == 0) 0 else hidden.size
    renderer.Container(BreadcrumbState(items.size, collapsed), theme, style, Modifier) {
        val head = if (plan.tail == 0) items.size else plan.head
        for (i in 0 until head) {
            if (i > 0) renderer.Separator(theme, style)
            ItemView(renderer, theme, style, items[i], i == items.lastIndex, focus, i)
        }
        if (plan.tail > 0) {
            renderer.Separator(theme, style)
            OverflowButton(hidden, style, focus, head, "More")
            for (i in items.size - plan.tail until items.size) {
                renderer.Separator(theme, style)
                ItemView(renderer, theme, style, items[i], i == items.lastIndex, focus, i)
            }
        }
    }
}

@Composable
private fun ItemView(
    renderer: BreadcrumbRenderer,
    theme: CamoTheme,
    style: BreadcrumbStyle,
    item: CamoBreadcrumbItem,
    current: Boolean,
    focus: RovingFocus?,
    index: Int?,
) {
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val link = item.onClick != null && !current
    var behavior = Modifier.heightIn(min = TARGET)
    if (focus != null && index != null) behavior = behavior.rovingFocusItem(focus, index, link, selected = false,
        onSelect = {})
    behavior = behavior.semantics(mergeDescendants = true) { if (current) stateDescription = "current page" }
    if (link) behavior = behavior.clickable(source, null, true, role = Role.Button, onClick = item.onClick!!)
    renderer.Item(BreadcrumbItemState(current, link, interaction, source), BreadcrumbSlots(item.label, item.icon),
        theme, style, behavior)
}

@Composable
private fun OverflowButton(hidden: List<CamoBreadcrumbItem>, style: BreadcrumbStyle, focus: RovingFocus?, slot: Int,
    label: String) {
    val items = hidden.map { CamoMenuItem(it.label, it.onClick ?: {}, leadingIcon = it.icon,
        enabled = it.onClick != null) }
    CamoMenuAnchor(items = items) { open ->
        var modifier: Modifier = Modifier
        if (focus != null) modifier = modifier.rovingFocusItem(focus, slot, enabled = true, selected = false,
            onSelect = {})
        CamoIconButton(
            icon = { CamoIcon(style.overflowIcon, contentDescription = null) },
            contentDescription = label,
            onClick = open,
            modifier = modifier,
            appearance = Appearance.Ghost,
        )
    }
}

private val TARGET = 48.dp
