package com.srctool.camouflage.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import com.srctool.camouflage.core.button.DebugChecks
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Overrides for navigation bars. `null` keeps the skin's default. */
@Immutable
public data class NavigationBarTheme(
    val placement: NavPlacement? = null,
    val floatingMargin: Dp? = null,
    val floatingShape: Shape? = null,
    val floatingElevation: ElevationLevel? = null,
    val indicator: NavIndicator? = null,
    val showLabels: LabelVisibility? = null,
    val height: Dp? = null,
    val indicatorSize: DpSize? = null,
    val indicatorShape: Shape? = null,
    val labelStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val colors: NavColors? = null,
)

/** The resolved navigation bars style: every field set. */
@Immutable
public data class NavigationBarStyle(
    val placement: NavPlacement,
    val floatingMargin: Dp,
    val floatingShape: Shape,
    val floatingElevation: ElevationLevel,
    val indicator: NavIndicator,
    val showLabels: LabelVisibility,
    val height: Dp,
    val indicatorSize: DpSize,
    val indicatorShape: Shape,
    val labelStyle: TextStyle,
    val iconSize: Dp,
    val colors: NavColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: NavigationBarTheme?): NavigationBarStyle = if (overrides == null) {
        this
    } else {
        NavigationBarStyle(
            placement = overrides.placement ?: placement,
            floatingMargin = overrides.floatingMargin ?: floatingMargin,
            floatingShape = overrides.floatingShape ?: floatingShape,
            floatingElevation = overrides.floatingElevation ?: floatingElevation,
            indicator = overrides.indicator ?: indicator,
            showLabels = overrides.showLabels ?: showLabels,
            height = overrides.height ?: height,
            indicatorSize = overrides.indicatorSize ?: indicatorSize,
            indicatorShape = overrides.indicatorShape ?: indicatorShape,
            labelStyle = overrides.labelStyle ?: labelStyle,
            iconSize = overrides.iconSize ?: iconSize,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What the bar renderer draws. [insets] is the bottom system inset (the gesture bar): the renderer pads its
 * content by it, after painting its background under it.
 */
@Immutable
public data class NavBarState(val count: Int, val selectedIndex: Int, val insets: WindowInsets)

/**
 * Draws navigation bars in two parts: the [Container] (the surface, placement and spacing around the items) and
 * each [Item]. Apply `modifier` (the behavior) to the outermost node of each.
 */
@Stable
public interface NavigationBarRenderer {
    /** The complete navigation bar style for [theme]. */
    public fun defaults(theme: CamoTheme): NavigationBarStyle

    /** Draws the bar around [content], which places the items in a row. */
    @Composable
    public fun Container(
        state: NavBarState,
        theme: CamoTheme,
        style: NavigationBarStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    )

    /** Draws one item with [slots]. */
    @Composable
    public fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationBarStyle,
        modifier: Modifier,
    )
}

private const val MIN_BAR_ITEMS = 3
private const val MAX_BAR_ITEMS = 5

/**
 * The bottom bar for a phone top-level destinations (3 to 5). [selected] in, [onSelect] out; routing stays
 * with the app router (ADR-19). Items are announced "Home, tab, 1 of 4, selected" with their badge, the
 * arrow keys move between them, and `visible = false` slides the bar out (pair it with `rememberHideOnScroll`).
 * Reselecting the current item calls [onSelect] again, so apps can scroll to the top.
 */
@Composable
public fun <T> CamoNavigationBar(
    items: List<CamoNavItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    if (DebugChecks.enabled) {
        check(items.size in MIN_BAR_ITEMS..MAX_BAR_ITEMS) { "CamoNavigationBar takes 3 to 5 items." }
    }
    CamoNavBarContainer(items.indexOfFirst { it.value == selected }, items.size, modifier, visible) {
        items.forEachIndexed { index, item ->
            CamoNavItemButton(item, item.value == selected, { onSelect(item.value) }, ItemPosition(index, items.size))
        }
    }
}

/**
 * The building block (ADR-30) for custom bars: the skin bar surface, placement and bottom inset, the
 * navigation-group semantics and the arrow keys across the [CamoNavItemButton]s in [content]. [count] is the
 * number of items and [selectedIndex] the selected one (`-1` for none).
 */
@Composable
public fun CamoNavBarContainer(
    selectedIndex: Int,
    count: Int,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val skin = requireSkin("CamoNavBarContainer")
    val theme = requireTheme("CamoNavBarContainer")
    val style = remember(skin, theme) { skin.navigationBar.defaults(theme).merge(theme.components.navigationBar) }
    val focus = rememberRovingFocus()
    val group = remember(focus, enabled) { NavGroup(NavHost.Bar, focus, enabled) }
    val slide = tween<IntOffset>(theme.motion.durationMedium, easing = theme.motion.easingEmphasized)
    val fade = tween<Float>(theme.motion.durationMedium)
    val state = NavBarState(count, selectedIndex, WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
    val behavior = Modifier
        .selectableGroup()
        .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = count) }
    AnimatedVisibility(
        visible,
        modifier,
        enter = slideInVertically(slide) { it } + fadeIn(fade),
        exit = slideOutVertically(slide) { it } + fadeOut(fade),
    ) {
        CompositionLocalProvider(LocalNavGroup provides group) {
            skin.navigationBar.Container(state, theme, style, behavior, content)
        }
    }
}

internal val LocalNavGroup = compositionLocalOf<NavGroup?> { null }

/**
 * One skin-styled destination inside a [CamoNavBarContainer], [CamoNavigationRail] or [CamoNavigationDrawer],
 * drawn by the renderer of the chrome it is in. Tab semantics ("Inbox, tab, 2 of 4, selected") and arrow keys
 * are built in; the badge description joins the item.
 */
@Composable
public fun <T> CamoNavItemButton(
    item: CamoNavItem<T>,
    selected: Boolean,
    onClick: () -> Unit,
    position: ItemPosition,
    modifier: Modifier = Modifier,
) {
    val skin = requireSkin("CamoNavItemButton")
    val theme = requireTheme("CamoNavItemButton")
    val group = checkNotNull(LocalNavGroup.current) {
        "CamoNavItemButton must be inside a navigation bar, rail or drawer container."
    }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val enabled = group.enabled && item.enabled
    val slots = NavItemSlots(item.label, if (selected) item.selectedIcon ?: item.icon else item.icon, item.badge)
    val behavior = modifier
        .rovingFocusItem(group.focus, position.index, enabled, selected, onClick)
        .semantics(mergeDescendants = true) { collectionItemInfo = CollectionItemInfo(0, 1, position.index, 1) }
        .selectable(selected, source, indication = null, enabled = enabled, role = Role.Tab, onClick = onClick)
    when (group.host) {
        NavHost.Bar -> {
            val style = remember(skin, theme) {
                skin.navigationBar.defaults(theme).merge(theme.components.navigationBar)
            }
            val label = showsLabel(style.showLabels, selected)
            val state = NavItemState(selected, enabled, position, label, style.indicator, interaction, source)
            skin.navigationBar.Item(state, slots, theme, style, behavior.named(item.label, label))
        }
        NavHost.Rail -> {
            val style = remember(skin, theme) {
                skin.navigationRail.defaults(theme).merge(theme.components.navigationRail)
            }
            val label = showsLabel(style.showLabels, selected)
            val state = NavItemState(selected, enabled, position, label, style.indicator, interaction, source)
            skin.navigationRail.Item(state, slots, theme, style, behavior.named(item.label, label))
        }
        NavHost.Drawer -> {
            val style = remember(skin, theme) {
                skin.navigationDrawer.defaults(theme).merge(theme.components.navigationDrawer)
            }
            val state = NavItemState(selected, enabled, position, true, style.indicator, interaction, source)
            skin.navigationDrawer.Item(state, slots, theme, style, behavior)
        }
    }
}

// An item whose label isn't drawn is still named for screen readers (a drawn label names it by itself).
private fun Modifier.named(label: String, drawn: Boolean): Modifier =
    if (drawn) this else semantics(mergeDescendants = true) { contentDescription = label }

private fun showsLabel(visibility: LabelVisibility, selected: Boolean) = when (visibility) {
    LabelVisibility.Always -> true
    LabelVisibility.Selected -> selected
    LabelVisibility.Never -> false
}
