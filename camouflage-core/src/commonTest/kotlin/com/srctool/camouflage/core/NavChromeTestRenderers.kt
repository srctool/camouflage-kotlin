package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.navigation.DrawerColors
import com.srctool.camouflage.core.navigation.DrawerState
import com.srctool.camouflage.core.navigation.LabelVisibility
import com.srctool.camouflage.core.navigation.NavBarState
import com.srctool.camouflage.core.navigation.NavColors
import com.srctool.camouflage.core.navigation.NavIndicator
import com.srctool.camouflage.core.navigation.NavItemSlots
import com.srctool.camouflage.core.navigation.NavItemState
import com.srctool.camouflage.core.navigation.NavPlacement
import com.srctool.camouflage.core.navigation.NavRailState
import com.srctool.camouflage.core.navigation.NavigationBarRenderer
import com.srctool.camouflage.core.navigation.NavigationBarStyle
import com.srctool.camouflage.core.navigation.NavigationDrawerRenderer
import com.srctool.camouflage.core.navigation.NavigationDrawerStyle
import com.srctool.camouflage.core.navigation.NavigationRailRenderer
import com.srctool.camouflage.core.navigation.NavigationRailStyle
import com.srctool.camouflage.core.navigation.RailAlignment
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.util.ProvideContentStyle

// The M8 navigation chrome in the plain test skin: boxes, rows and text, no decoration.

private const val SCRIM = 0.32f

private fun colors(theme: CamoTheme) = theme.colors.let {
    NavColors(it.surface, it.secondaryContainer, it.primary, it.primary, it.onSurfaceVariant, it.onSurfaceVariant)
}

@Composable
private fun PlainItem(state: NavItemState, slots: NavItemSlots, modifier: Modifier) {
    val tint = if (state.selected) androidx.compose.ui.graphics.Color.Blue else androidx.compose.ui.graphics.Color.Gray
    Row(modifier.padding(8.dp)) {
        ProvideContentStyle(tint, IconStyle(24.dp, tint)) {
            slots.icon()
            if (state.showLabel) BasicText(slots.label)
            slots.badge?.invoke()
        }
    }
}

internal object PlainNavigationBar : NavigationBarRenderer {
    override fun defaults(theme: CamoTheme) = NavigationBarStyle(
        NavPlacement.Pinned, 12.dp, RectangleShape, ElevationLevel.Level2, NavIndicator.Pill, LabelVisibility.Always,
        64.dp, DpSize(48.dp, 28.dp), RectangleShape, theme.typography.labelSmall, 24.dp, colors(theme),
    )

    @Composable
    override fun Container(
        state: NavBarState,
        theme: CamoTheme,
        style: NavigationBarStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        // The inset is read, so a test can see it reach the renderer.
        lastBarInsets = state.insets
        Row(modifier.fillMaxWidth().height(style.height).background(style.colors.container), content = content)
    }

    @Composable
    override fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationBarStyle,
        modifier: Modifier,
    ) = PlainItem(state, slots, modifier)
}

/** The inset the plain bar was last given, for tests. */
internal var lastBarInsets: WindowInsets? = null

internal object PlainNavigationRail : NavigationRailRenderer {
    override fun defaults(theme: CamoTheme) = NavigationRailStyle(
        NavIndicator.Pill, RailAlignment.Top, LabelVisibility.Always, 80.dp, 12.dp, DpSize(56.dp, 32.dp),
        RectangleShape, theme.typography.labelSmall, 24.dp, colors(theme),
    )

    @Composable
    override fun Container(
        state: NavRailState,
        theme: CamoTheme,
        style: NavigationRailStyle,
        modifier: Modifier,
        header: (@Composable () -> Unit)?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Column(modifier.fillMaxHeight().width(style.width).background(style.colors.container)) {
            header?.invoke()
            Column(content = content)
        }
    }

    @Composable
    override fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationRailStyle,
        modifier: Modifier,
    ) = PlainItem(state, slots, modifier)
}

internal object PlainNavigationDrawer : NavigationDrawerRenderer {
    override fun defaults(theme: CamoTheme): NavigationDrawerStyle {
        val c = theme.colors
        return NavigationDrawerStyle(
            NavIndicator.Pill, 280.dp, 48.dp, 24.dp, RectangleShape, RectangleShape, theme.typography.labelLarge,
            theme.typography.titleSmall, SCRIM,
            DrawerColors(
                c.surface, c.surfaceContainerLow, c.secondaryContainer, c.primary, c.onSurfaceVariant, c.onSurface,
            ),
        )
    }

    @Composable
    override fun Container(
        state: DrawerState,
        theme: CamoTheme,
        style: NavigationDrawerStyle,
        modifier: Modifier,
        header: (@Composable () -> Unit)?,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Column(modifier.fillMaxHeight().width(style.width).background(style.colors.container)) {
            header?.invoke()
            Box { Column(content = content) }
        }
    }

    @Composable
    override fun SectionTitle(title: String, theme: CamoTheme, style: NavigationDrawerStyle, modifier: Modifier) {
        BasicText(title, modifier)
    }

    @Composable
    override fun Item(
        state: NavItemState,
        slots: NavItemSlots,
        theme: CamoTheme,
        style: NavigationDrawerStyle,
        modifier: Modifier,
    ) = PlainItem(state, slots, modifier)
}
