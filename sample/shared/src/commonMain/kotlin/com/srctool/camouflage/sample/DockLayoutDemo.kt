package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.dock.CamoDockLayout
import com.srctool.camouflage.core.dock.CamoDockSnapshot
import com.srctool.camouflage.core.dock.CamoDockState
import com.srctool.camouflage.core.dock.CamoToolWindow
import com.srctool.camouflage.core.dock.DockEdge
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance

// The M16 dock layout: five tool windows on three edges. Click a stripe button to toggle a window, drag a divider,
// use a window's menu to move it, maximize or hide it. "Show Log" moves keyboard focus into a window; Esc returns it
// to the editor. The layout is app state: "Save" keeps a snapshot, "Restore" brings it back.

private val DemoHeight = 480.dp

private class SampleWindow(val id: String, val title: String, val edge: DockEdge, val icon: @Composable () -> Unit)

// A stripe button's icon takes the skin's tint, so an open window shows in every skin (Material colors the icon).
private val Windows = listOf(
    SampleWindow("files", "Files", DockEdge.Left) { AppIcon.Archive() },
    SampleWindow("search", "Search", DockEdge.Left) { AppIcon.Search() },
    SampleWindow("details", "Details", DockEdge.Right) { AppIcon.Edit() },
    SampleWindow("log", "Log", DockEdge.Bottom) { AppIcon.Sort() },
    SampleWindow("terminal", "Terminal", DockEdge.Bottom) { AppIcon.ChevronRight() },
)

@Composable
internal fun DockLayoutDemo() {
    val state = remember {
        CamoDockState(Windows.associate { it.id to it.edge }, open = setOf("files", "log"))
    }
    var saved by remember { mutableStateOf<CamoDockSnapshot?>(null) }
    var opened by remember { mutableIntStateOf(0) }
    val toolWindows = remember {
        Windows.map { w -> CamoToolWindow(w.id, w.title, w.icon, w.edge) { Panel(w.title) } }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoButton("Save layout", { saved = state.snapshot() }, appearance = Appearance.Outline)
            CamoButton("Restore", { saved?.let(state::apply) }, appearance = Appearance.Outline)
            CamoButton("Show Log", { state.focus("log") }, appearance = Appearance.Outline)
        }
        Box(Modifier.fillMaxWidth().height(DemoHeight)) {
            CamoDockLayout(state, toolWindows, Modifier.fillMaxSize()) {
                CamoSurface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(Camouflage.spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
                    ) {
                        CamoText("Editor")
                        CamoText("The center. Esc inside a tool window comes back here.")
                        CamoButton("Open file", { opened++ }, appearance = Appearance.Outline)
                        CamoText("Files opened: $opened")
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(title: String) {
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(Camouflage.spacing.m),
        verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
    ) {
        CamoText("$title tool window")
        // Something to focus: `focus(id)` lands here, Esc leaves.
        CamoButton("Refresh", {}, appearance = Appearance.Outline)
    }
}
