package com.srctool.camouflage.sample

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.buttongroup.CamoButtonGroup
import com.srctool.camouflage.core.buttongroup.CamoGroupButton
import com.srctool.camouflage.core.menu.CamoMenuDivider
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.popover.CamoPopover
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.sidesheet.CamoSideSheet
import com.srctool.camouflage.core.splitbutton.CamoSplitButton
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.toolbar.CamoToolbar
import com.srctool.camouflage.core.toolbar.CamoToolbarAction
import com.srctool.camouflage.core.toolbar.CamoToolbarContainer
import com.srctool.camouflage.core.toolbar.ToolbarPlacement

// The M9 milestone screen: an editor whose toolbars use a split button, command and toggle groups and an
// overflowing action bar, with a popover from the toolbar and a properties side sheet beside the document.

private val DocumentHeight = 240.dp

@Stable
private class EditorState {
    var bold by mutableStateOf(false)
    var italic by mutableStateOf(false)
    var underline by mutableStateOf(false)
    var starred by mutableStateOf(false)
    var properties by mutableStateOf(true)
    var linkOpen by mutableStateOf(false)
    var last by mutableStateOf("Nothing yet")

    val style: String
        get() = listOfNotNull("bold".takeIf { bold }, "italic".takeIf { italic }, "underline".takeIf { underline })
            .joinToString()
            .ifEmpty { "plain" }
}

@Composable
internal fun EditorDemo() {
    val editor = remember { EditorState() }
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
        CommandBar(editor)
        ActionBar(editor)
        Row(Modifier.fillMaxWidth().height(DocumentHeight)) {
            Box(Modifier.weight(1f).padding(Camouflage.spacing.m)) {
                CamoText("Last command: ${editor.last}. Text style: ${editor.style}.")
            }
            if (editor.properties) {
                CamoSideSheet(title = "Properties", onClose = { editor.properties = false }) {
                    CamoText("Starred: ${if (editor.starred) "yes" else "no"}")
                    CamoText("Words: 214")
                }
            }
        }
    }
}

@Composable
private fun CommandBar(editor: EditorState) {
    val saveMenu = listOf(
        CamoMenuItem("Save as…", { editor.last = "Save as" }),
        CamoMenuItem("Save a copy", { editor.last = "Save a copy" }),
        CamoMenuDivider,
        CamoMenuItem("Export as PDF", { editor.last = "Export" }),
    )
    val clipboard = listOf("Cut", "Copy", "Paste").map { CamoGroupButton(it, { editor.last = it }, text = it) }
    val formatting = listOf(
        CamoGroupButton(
            "b", { editor.bold = !editor.bold }, text = "B", contentDescription = "Bold", selected = editor.bold,
        ),
        CamoGroupButton(
            "i", { editor.italic = !editor.italic }, text = "I", contentDescription = "Italic",
            selected = editor.italic,
        ),
        CamoGroupButton(
            "u", { editor.underline = !editor.underline }, text = "U", contentDescription = "Underline",
            selected = editor.underline,
        ),
    )
    CamoToolbarContainer(contentDescription = "Document commands") {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoSplitButton("Save", { editor.last = "Save" }, saveMenu)
            CamoButtonGroup(clipboard, contentDescription = "Clipboard")
            CamoButtonGroup(formatting, connected = false, contentDescription = "Text style")
        }
    }
}

@Composable
private fun ActionBar(editor: EditorState) {
    val actions = listOf(
        CamoToolbarAction(
            "star", "Star", { AppIcon.Star() }, { editor.starred = !editor.starred }, selected = editor.starred,
        ),
        CamoToolbarAction("share", "Share", { AppIcon.Share() }, { editor.last = "Share" }),
        CamoToolbarAction("upload", "Upload", { AppIcon.Upload() }, { editor.last = "Upload" }),
        CamoToolbarAction("archive", "Archive", { AppIcon.Archive() }, { editor.last = "Archive" }),
        CamoToolbarAction("delete", "Delete", { AppIcon.Delete() }, { editor.last = "Delete" }),
    )
    CamoToolbar(
        actions,
        placement = ToolbarPlacement.Floating,
        contentDescription = "Document actions",
        leading = {
            CamoPopover(
                open = editor.linkOpen,
                onDismissRequest = { editor.linkOpen = false },
                anchor = {
                    CamoButton("Link", { editor.linkOpen = !editor.linkOpen }, appearance = Appearance.Ghost)
                },
                title = "Insert link",
                actions = {
                    CamoButton("Insert", {
                        editor.linkOpen = false
                        editor.last = "Insert link"
                    })
                },
            ) { CamoText("Paste a URL to link the selected text.") }
        },
        trailing = {
            CamoButton("Properties", { editor.properties = !editor.properties }, appearance = Appearance.Outline)
        },
    )
}
