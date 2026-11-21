package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

private val DemoHeight = 420.dp

private val Windows = listOf(
    Triple("files", "Files", DockEdge.Left),
    Triple("search", "Search", DockEdge.Left),
    Triple("details", "Details", DockEdge.Right),
    Triple("log", "Log", DockEdge.Bottom),
    Triple("terminal", "Terminal", DockEdge.Bottom),
)

@Composable
internal fun DockLayoutDemo() {
    val state = remember {
        CamoDockState(Windows.associate { (id, _, edge) -> id to edge }, open = setOf("files", "log"))
    }
    var saved by remember { mutableStateOf<CamoDockSnapshot?>(null) }
    val toolWindows = remember {
        Windows.map { (id, title, edge) ->
            CamoToolWindow(id, title, { CamoText(title.take(1)) }, edge) { Panel(title) }
        }
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
                    Column(Modifier.padding(Camouflage.spacing.m)) {
                        CamoText("Editor")
                        CamoText("The center. Esc inside a tool window comes back here.")
                    }
                }
            }
        }
    }
}

@Composable
private fun Panel(title: String) {
    Column(Modifier.padding(Camouflage.spacing.m)) {
        CamoText("$title tool window")
    }
}
