package com.srctool.camouflage.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.environment.environment
import com.srctool.camouflage.core.provider.Camouflage

/**
 * Picks the navigation chrome for the window (ADR-41): a bottom [CamoNavigationBar] on a compact window, a
 * [CamoNavigationRail] on medium and expanded ones, and a permanent [CamoNavigationDrawer] on large ones, by
 * the theme `breakpoints`. The [content] keeps its state when the window resizes and the layout switches.
 * [header] (a FAB, a logo) goes on the rail and the drawer.
 */
@Composable
public fun <T> CamoAdaptiveNavigation(
    items: List<CamoNavItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val current by rememberUpdatedState(content)
    val body = remember { movableContentOf { Box(Modifier.fillMaxSize()) { current() } } }
    when (Camouflage.environment.windowSize) {
        WindowSizeClass.Compact -> Column(modifier) {
            Box(Modifier.weight(1f)) { body() }
            CamoNavigationBar(items, selected, onSelect)
        }
        WindowSizeClass.Medium, WindowSizeClass.Expanded -> Row(modifier) {
            CamoNavigationRail(items, selected, onSelect, header = header)
            Box(Modifier.weight(1f)) { body() }
        }
        WindowSizeClass.Large -> Row(modifier) {
            CamoNavigationDrawer(listOf(CamoNavSection(items = items)), selected, onSelect, header = header)
            Box(Modifier.weight(1f)) { body() }
        }
    }
}
