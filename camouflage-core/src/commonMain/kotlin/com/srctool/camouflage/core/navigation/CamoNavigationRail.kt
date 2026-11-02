package com.srctool.camouflage.core.navigation

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.rememberRovingFocus

/** Overrides for navigation rails. `null` keeps the skin's default. */
@Immutable
public data class NavigationRailTheme(
    val indicator: NavIndicator? = null,
    val alignment: RailAlignment? = null,
    val showLabels: LabelVisibility? = null,
    val width: Dp? = null,
    val itemSpacing: Dp? = null,
    val indicatorSize: DpSize? = null,
    val indicatorShape: Shape? = null,
    val labelStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val colors: NavColors? = null,
)

/** The resolved navigation rails style: every field set. */
@Immutable
public data class NavigationRailStyle(
    val indicator: NavIndicator,
    val alignment: RailAlignment,
    val showLabels: LabelVisibility,
    val width: Dp,
    val itemSpacing: Dp,
    val indicatorSize: DpSize,
    val indicatorShape: Shape,
    val labelStyle: TextStyle,
    val iconSize: Dp,
    val colors: NavColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: NavigationRailTheme?): NavigationRailStyle = if (overrides == null) {
        this
    } else {
        NavigationRailStyle(
            indicator = overrides.indicator ?: indicator,
            alignment = overrides.alignment ?: alignment,
            showLabels = overrides.showLabels ?: showLabels,
            width = overrides.width ?: width,
            itemSpacing = overrides.itemSpacing ?: itemSpacing,
            indicatorSize = overrides.indicatorSize ?: indicatorSize,
            indicatorShape = overrides.indicatorShape ?: indicatorShape,
            labelStyle = overrides.labelStyle ?: labelStyle,
            iconSize = overrides.iconSize ?: iconSize,
            colors = overrides.colors ?: colors,
        )
    }
}

/** What the rail renderer draws. [insets] is the start-side system inset (a landscape notch). */
@Immutable
public data class NavRailState(val count: Int, val selectedIndex: Int, val insets: WindowInsets)

/**
 * Draws navigation rails in two parts: the [Container] (the surface, the optional header and the item column)
 * and each [Item]. Apply `modifier` (the behavior) to the outermost node of each.
 */
@Stable
public interface NavigationRailRenderer {
    /** The complete navigation rail style for [theme]. */
    public fun defaults(theme: CamoTheme): NavigationRailStyle

    /** Draws the rail: [header] on top, fixed, and [content], which places the items in a column. */
    @Composable
    public fun Container(
        state: NavRailState,
        theme: CamoTheme,
        style: NavigationRailStyle,
        modifier: Modifier,
        header: (@Composable () -> Unit)?,
        content: @Composable ColumnScope.() -> Unit,
    )

    /** Draws one item with [slots]. */
    @Composable
    public fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationRailStyle,
        modifier: Modifier,
    )
}

/**
 * The side rail for medium windows: the bar destinations in a column on the start edge, with an optional
 * [header] (a `CamoFab` or a menu button). It scrolls when the items do not fit, and the header stays. The same
 * `CamoNavItem`s, announcements and keys as the bar, with up and down moving between items.
 */
@Composable
public fun <T> CamoNavigationRail(
    items: List<CamoNavItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val skin = requireSkin("CamoNavigationRail")
    val theme = requireTheme("CamoNavigationRail")
    val focus = rememberRovingFocus()
    val group = remember(focus) { NavGroup(NavHost.Rail, focus, enabled = true) }
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
    val style = remember(skin, theme) { skin.navigationRail.defaults(theme).merge(theme.components.navigationRail) }
    val behavior = modifier
        .selectableGroup()
        .semantics { collectionInfo = CollectionInfo(rowCount = items.size, columnCount = 1) }
    CompositionLocalProvider(LocalNavGroup provides group) {
        skin.navigationRail.Container(
            NavRailState(items.size, items.indexOfFirst { it.value == selected }, insets),
            theme,
            style,
            behavior,
            header,
        ) {
            items.forEachIndexed { index, item ->
                val position = ItemPosition(index, items.size)
                CamoNavItemButton(item, item.value == selected, { onSelect(item.value) }, position)
            }
        }
    }
}
