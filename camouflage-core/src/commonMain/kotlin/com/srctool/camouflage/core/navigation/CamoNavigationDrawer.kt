package com.srctool.camouflage.core.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.overlay.SideEdge
import com.srctool.camouflage.core.overlay.SideFrame
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.rememberRovingFocus
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Overrides for navigation drawers. `null` keeps the skin's default. */
@Immutable
public data class NavigationDrawerTheme(
    val indicator: NavIndicator? = null,
    val width: Dp? = null,
    val itemHeight: Dp? = null,
    val iconSize: Dp? = null,
    val itemShape: Shape? = null,
    val modalShape: Shape? = null,
    val labelStyle: TextStyle? = null,
    val sectionTitleStyle: TextStyle? = null,
    val scrimAlpha: Float? = null,
    val colors: DrawerColors? = null,
)

/** The resolved navigation drawers style: every field set. */
@Immutable
public data class NavigationDrawerStyle(
    val indicator: NavIndicator,
    val width: Dp,
    val itemHeight: Dp,
    val iconSize: Dp,
    val itemShape: Shape,
    val modalShape: Shape,
    val labelStyle: TextStyle,
    val sectionTitleStyle: TextStyle,
    val scrimAlpha: Float,
    val colors: DrawerColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: NavigationDrawerTheme?): NavigationDrawerStyle = if (overrides == null) {
        this
    } else {
        NavigationDrawerStyle(
            indicator = overrides.indicator ?: indicator,
            width = overrides.width ?: width,
            itemHeight = overrides.itemHeight ?: itemHeight,
            iconSize = overrides.iconSize ?: iconSize,
            itemShape = overrides.itemShape ?: itemShape,
            modalShape = overrides.modalShape ?: modalShape,
            labelStyle = overrides.labelStyle ?: labelStyle,
            sectionTitleStyle = overrides.sectionTitleStyle ?: sectionTitleStyle,
            scrimAlpha = overrides.scrimAlpha ?: scrimAlpha,
            colors = overrides.colors ?: colors,
        )
    }
}

/** What the drawer renderer draws: [modal] while it slides over the content, permanent beside it. */
@Immutable
public data class DrawerState(val modal: Boolean)

/**
 * Draws navigation drawers in three parts: the [Container] (the surface, the optional header and the scrolling
 * column), the [SectionTitle]s and each [Item]. Apply `modifier` (the behavior) to the outermost node of each.
 */
@Stable
public interface NavigationDrawerRenderer {
    /** The complete navigation drawer style for [theme]. */
    public fun defaults(theme: CamoTheme): NavigationDrawerStyle

    /** Draws the drawer: [header] on top, fixed, and [content], the sections in a scrolling column. */
    @Composable
    public fun Container(
        state: DrawerState,
        theme: CamoTheme,
        style: NavigationDrawerStyle,
        modifier: Modifier,
        header: (@Composable () -> Unit)?,
        content: @Composable ColumnScope.() -> Unit,
    )

    /** Draws a section [title]. */
    @Composable
    public fun SectionTitle(title: String, theme: CamoTheme, style: NavigationDrawerStyle, modifier: Modifier)

    /** Draws one item with [slots]. */
    @Composable
    public fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationDrawerStyle,
        modifier: Modifier,
    )
}

private class DrawerScope(val modal: Boolean, val onNavigate: () -> Unit)

private val LocalDrawerScope = compositionLocalOf<DrawerScope?> { null }

/**
 * A drawer with full labels and section headers: permanent beside the content on wide windows, or inside a
 * [CamoModalDrawer]. Selecting an item inside a modal drawer closes it. Section titles are headings; the
 * column scrolls and the [header] (a logo, an account switcher) stays.
 */
@Composable
public fun <T> CamoNavigationDrawer(
    sections: List<CamoNavSection<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val skin = requireSkin("CamoNavigationDrawer")
    val theme = requireTheme("CamoNavigationDrawer")
    val style = remember(skin, theme) { skin.navigationDrawer.defaults(theme).merge(theme.components.navigationDrawer) }
    val focus = rememberRovingFocus()
    val group = remember(focus) { NavGroup(NavHost.Drawer, focus, enabled = true) }
    val scope = LocalDrawerScope.current
    val count = sections.sumOf { it.items.size }
    var index = 0
    CompositionLocalProvider(LocalNavGroup provides group) {
        skin.navigationDrawer.Container(
            DrawerState(scope?.modal == true),
            theme,
            style,
            modifier.selectableGroup().semantics { paneTitle = "Navigation" },
            header,
        ) {
            sections.forEach { section ->
                section.title?.let { skin.navigationDrawer.SectionTitle(it, theme, style, Modifier) }
                section.items.forEach { item ->
                    CamoNavItemButton(
                        item,
                        item.value == selected,
                        {
                            onSelect(item.value)
                            scope?.onNavigate?.invoke()
                        },
                        ItemPosition(index++, count),
                    )
                }
            }
        }
    }
}

/**
 * The modal mode (a phone with many destinations): slides in from the start edge over the content, with a
 * scrim. Its visibility is state ([open]), not a route: a menu button opens it and a selection closes it. A
 * scrim tap, back, Esc and a drag toward the start edge all call [onDismissRequest]; focus is trapped inside.
 * [content] is a [CamoNavigationDrawer].
 */
@Composable
public fun CamoModalDrawer(
    open: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoModalDrawer")
    val theme = requireTheme("CamoModalDrawer")
    val style = remember(skin, theme) { skin.navigationDrawer.defaults(theme).merge(theme.components.navigationDrawer) }
    SideFrame(SideEdge.Start, open, onDismissRequest, style.width, style.scrimAlpha, modifier) {
        val modal = DrawerScope(modal = true, onNavigate = onDismissRequest)
        CompositionLocalProvider(LocalDrawerScope provides modal) { content() }
    }
}
