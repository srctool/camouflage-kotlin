package com.srctool.camouflage.core.tabs

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.theme.Appearance

/**
 * Editor-style tabs for open documents: a close button on hover and on the selected tab (a dot while the
 * document is modified), middle-click to close, drag to reorder (Alt+Left and Alt+Right with the keyboard),
 * pinned tabs at the start, and a row that scrolls with a "show all tabs" menu when the tabs don't fit.
 *
 * [documents] and [selected] (a document id) are the app's; so is closing: [onClose] says the user asked, and the
 * app removes the document and picks the next selection. The order and pins live in [state]. [onContextMenu]
 * returns the entries of a tab's right-click menu ("Close others", "Close to the right", "Pin"). Ctrl+W is the
 * app's key. Screen readers get the tab-list semantics of `CamoTabs` plus "Close", "Move earlier", "Move later"
 * and "Pin" actions on every tab, and hear "modified" and "pinned" in its state.
 *
 * Built from the skin's tab bar (every skin draws it), so it needs no renderer of its own.
 */
@Composable
public fun CamoDocumentTabs(
    documents: List<CamoDocument>,
    selected: Any,
    onSelect: (Any) -> Unit,
    onClose: (Any) -> Unit,
    state: CamoDocumentTabsState,
    modifier: Modifier = Modifier,
    onContextMenu: ((CamoDocument) -> List<CamoMenuEntry>)? = null,
    appearance: Appearance? = null,
) {
    val ids = documents.map { it.id }
    LaunchedEffect(ids) { state.sync(ids) }
    val byId = documents.associateBy { it.id }
    val ordered = state.sorted(ids).mapNotNull { byId[it] }
    val registry = remember { TabsRegistry() }
    val scroll = rememberScrollState()
    val drag = remember { DocumentDrag() }
    val overflow = registry.overflows()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            TabsBar(
                selectedIndex = ordered.indexOfFirst { it.id == selected },
                registry = registry,
                scroll = scroll,
                modifier = Modifier,
                scrollable = true,
                appearance = appearance,
                indicatorPosition = null,
                enabled = true,
            ) {
                ordered.forEachIndexed { index, doc ->
                    key(doc.id) {
                        DocumentTab(
                            DocumentTabSpec(doc, ItemPosition(index, ordered.size), doc.id == selected),
                            DocumentTabActions(onSelect, onClose, onContextMenu),
                            state,
                            drag,
                        )
                    }
                }
            }
        }
        if (overflow) AllTabsMenu(ordered, selected, onSelect)
    }
}

// Whether the tabs are wider than the bar.
private fun TabsRegistry.overflows(): Boolean {
    val end = bounds.values.maxOfOrNull { it.start + it.width } ?: return false
    return end > viewport + 1
}

@Composable
private fun AllTabsMenu(ordered: List<CamoDocument>, selected: Any, onSelect: (Any) -> Unit) {
    val skin = requireSkin("CamoDocumentTabs")
    val theme = requireTheme("CamoDocumentTabs")
    val chevron = remember(skin, theme) {
        skin.splitButton.defaults(theme).merge(theme.components.splitButton).arrowIcon
    }
    // The selected document first, as a list of what is open.
    val items = (ordered.filter { it.id == selected } + ordered.filter { it.id != selected }).map { doc ->
        CamoMenuItem(
            text = doc.title,
            onClick = { onSelect(doc.id) },
            trailingText = if (doc.modified) "modified" else null,
        )
    }
    CamoMenuAnchor(items = items, speedSearch = true) { open ->
        CamoIconButton(
            icon = { CamoIcon(chevron, null) },
            contentDescription = "Show all tabs",
            onClick = open,
            appearance = Appearance.Ghost,
            tooltip = "All tabs",
        )
    }
}
