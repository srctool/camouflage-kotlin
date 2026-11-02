package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.srctool.camouflage.core.popover.CamoPopover
import com.srctool.camouflage.core.popover.PopoverPlacement
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.sidesheet.CamoModalSideSheet
import com.srctool.camouflage.core.sidesheet.CamoSideSheet
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance

// The M9 popover and side sheet: a popover from a button, a standard side sheet beside a document, and the same
// sheet as a modal one.

private val DocumentHeight = 220.dp

@Composable
internal fun PanelsDemo() {
    var shared by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(true) }
    var modal by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoPopover(
                open = shared,
                onDismissRequest = { shared = false },
                anchor = { CamoButton("Share", { shared = !shared }) },
                title = "Share this file",
                actions = {
                    CamoButton("Cancel", { shared = false }, appearance = Appearance.Ghost)
                    CamoButton("Copy link", { shared = false })
                },
                placement = PopoverPlacement.Bottom,
            ) { CamoText("Anyone with the link can view it.") }
            CamoButton("Details (docked)", { details = !details }, appearance = Appearance.Outline)
            CamoButton("Details (modal)", { modal = true }, appearance = Appearance.Outline)
        }
        Row(Modifier.fillMaxWidth().height(DocumentHeight)) {
            Box(Modifier.weight(1f).padding(Camouflage.spacing.m)) {
                CamoText("The document: it stays usable beside the docked sheet.")
            }
            if (details) {
                CamoSideSheet(title = "Details", onClose = { details = false }) {
                    CamoText("Size: 4.2 MB")
                    CamoText("Modified: today")
                }
            }
        }
        CamoModalSideSheet(modal, { modal = false }) {
            CamoSideSheet(
                title = "Details",
                onClose = { modal = false },
                actions = { CamoButton("Done", { modal = false }) },
            ) {
                CamoText("Size: 4.2 MB")
                CamoText("Modified: today")
            }
        }
    }
}
