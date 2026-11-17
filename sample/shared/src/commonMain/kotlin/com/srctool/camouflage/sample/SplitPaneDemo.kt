package com.srctool.camouflage.sample

import androidx.compose.foundation.gestures.Orientation
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
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.splitpane.CamoPane
import com.srctool.camouflage.core.splitpane.CamoSplitPane
import com.srctool.camouflage.core.splitpane.CamoSplitSnapshot
import com.srctool.camouflage.core.splitpane.CamoSplitState
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance

// The M16 split pane: a list and a detail side by side, and a log below them that collapses. Drag a divider, focus it
// and press the arrow keys (Shift for a larger step, Home and End, Enter to collapse the log), or double-click to
// reset. The layout is app state: "Save" keeps a snapshot, "Restore" brings it back.

private val DemoHeight = 320.dp

@Composable
internal fun SplitPaneDemo() {
    val outer = remember { CamoSplitState(listOf(3f, 1f)) }
    val inner = remember { CamoSplitState(listOf(1f, 2f)) }
    var saved by remember { mutableStateOf<Pair<CamoSplitSnapshot, CamoSplitSnapshot>?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoButton("Save layout", { saved = outer.snapshot() to inner.snapshot() }, appearance = Appearance.Outline)
            CamoButton(
                "Restore",
                {
                    saved?.let {
                        outer.apply(it.first)
                        inner.apply(it.second)
                    }
                },
                appearance = Appearance.Outline,
            )
            CamoButton(
                "Reset",
                {
                    outer.reset()
                    inner.reset()
                },
                appearance = Appearance.Ghost,
            )
        }
        Box(Modifier.fillMaxWidth().height(DemoHeight)) {
            CamoSplitPane(
                Orientation.Vertical,
                listOf(
                    CamoPane(minSize = 120.dp) {
                        CamoSplitPane(
                            Orientation.Horizontal,
                            listOf(
                                CamoPane(minSize = 100.dp) { Panel("List", "Documents, in a column.") },
                                CamoPane(minSize = 120.dp) { Panel("Detail", "The selected document.") },
                            ),
                            inner,
                        )
                    },
                    CamoPane(minSize = 60.dp, collapsible = true) {
                        Panel("Log", "Collapses; Enter on its divider restores it.")
                    },
                ),
                outer,
                Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Panel(title: String, text: String) {
    CamoSurface(Modifier.fillMaxSize()) {
        Column(Modifier.padding(Camouflage.spacing.m)) {
            CamoText(title)
            CamoText(text)
        }
    }
}
