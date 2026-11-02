package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.badge.CamoBadge
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.environment.environment
import com.srctool.camouflage.core.fab.CamoFab
import com.srctool.camouflage.core.navigation.CamoAdaptiveNavigation
import com.srctool.camouflage.core.navigation.CamoModalDrawer
import com.srctool.camouflage.core.navigation.CamoNavItem
import com.srctool.camouflage.core.navigation.CamoNavSection
import com.srctool.camouflage.core.navigation.CamoNavigationDrawer
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.segmented.CamoSegmentedControl
import com.srctool.camouflage.core.segmented.Segment
import com.srctool.camouflage.core.text.CamoText

// The M8 milestone screen: one set of destinations as a bar on a phone, a rail on a tablet and a drawer on a
// desktop window, with the same selection and the same screen state. The window sizes are simulated by the
// segmented control (a window can't resize inside this page); a real app just reads the window.

private enum class Destination(val label: String) { Home("Home"), Inbox("Inbox"), Search("Search"), Profile("Profile") }

private val Sizes = listOf(
    WindowSizeClass.Compact to 360.dp,
    WindowSizeClass.Medium to 640.dp,
    WindowSizeClass.Large to 960.dp,
)

@Composable
internal fun NavigationShellDemo() {
    var size by remember { mutableStateOf(WindowSizeClass.Compact) }
    var menuOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CamoSegmentedControl(
            Sizes.map { (s, _) -> Segment(s, s.name) },
            size,
            { size = it },
        )
        val width = Sizes.first { it.first == size }.second
        val base = Camouflage.environment
        // The simulated window: its own environment, so the chrome picks the layout for the chosen size.
        CamouflageProvider(environment = base.copy(windowSize = size)) {
            Box(Modifier.width(width).height(ShellHeight).clipToBounds()) {
                Shell(size, onMenu = { menuOpen = true })
                if (size == WindowSizeClass.Compact) {
                    CamoModalDrawer(menuOpen, { menuOpen = false }) {
                        CamoNavigationDrawer(
                            listOf(CamoNavSection(items = destinations())),
                            Destination.Home,
                            { menuOpen = false },
                        )
                    }
                }
            }
        }
    }
}

private val ShellHeight = 360.dp

@Composable
private fun destinations() = Destination.entries.map {
    CamoNavItem(
        it,
        it.label,
        icon = { AppIcon.Star() },
        selectedIcon = { AppIcon.Star() },
        badge = if (it == Destination.Inbox) ({ CamoBadge(count = 3, contentDescription = "3 unread") }) else null,
    )
}

@Composable
private fun Shell(size: WindowSizeClass, onMenu: () -> Unit) {
    var selected by remember { mutableStateOf(Destination.Home) }
    var taps by remember { mutableIntStateOf(0) }
    CamoAdaptiveNavigation(
        items = destinations(),
        selected = selected,
        onSelect = { selected = it },
        header = { CamoFab({ AppIcon.Edit() }, "Compose", {}) },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(Camouflage.spacing.l),
            verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
        ) {
            CamoText("${selected.label} (${size.name} window)", style = Camouflage.typography.titleMedium)
            CamoText("The screen keeps its state when the layout changes: $taps taps.")
            CamoButton("Tap", { taps++ })
            if (size == WindowSizeClass.Compact) CamoButton("Menu (modal drawer)", onMenu)
        }
    }
}
