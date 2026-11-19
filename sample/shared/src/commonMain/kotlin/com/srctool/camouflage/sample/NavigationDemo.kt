package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.badge.CamoBadge
import com.srctool.camouflage.core.fab.CamoFab
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.tabs.CamoTab
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.tabs.CamoDocument
import com.srctool.camouflage.core.tabs.CamoDocumentTabs
import com.srctool.camouflage.core.tabs.CamoDocumentTabsState
import com.srctool.camouflage.core.tabs.CamoTabs
import com.srctool.camouflage.core.tabs.rememberCamoTabsPagerSync
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.topbar.rememberHideOnScroll

// The M8 tabs and FAB: a product page with fixed tabs over a pager, a scrollable category bar, and a list with an
// extended FAB that collapses while it scrolls (a second one hides on scroll).

private val ProductTabs = listOf(
    CamoTab("overview", "Overview"),
    CamoTab("reviews", "Reviews", badge = { CamoBadge(count = 12, contentDescription = "12 reviews") }),
    CamoTab("specs", "Specs"),
)
private val Categories = listOf("Phones", "Laptops", "Audio", "Cameras", "Wearables", "Gaming", "Home", "Accessories")
private val ListHeight = 280.dp
private val PagerHeight = 96.dp
private const val ROWS = 24
private const val DOCUMENTS = 20
private val DocumentBarWidth = 420.dp

@Composable
internal fun NavigationDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.l)) {
        ProductPage()
        CategoryBar()
        DocumentBar()
        MessageList()
    }
}

@Composable
private fun ProductPage() {
    val pager = rememberPagerState { ProductTabs.size }
    val sync = rememberCamoTabsPagerSync(pager, ProductTabs)
    Column(Modifier.fillMaxWidth()) {
        CamoTabs(sync.selected, sync.onSelect, ProductTabs, indicatorPosition = sync.position)
        HorizontalPager(pager, Modifier.fillMaxWidth().height(PagerHeight)) { page ->
            Box(Modifier.fillMaxSize().padding(Camouflage.spacing.m), contentAlignment = Alignment.CenterStart) {
                CamoText("${ProductTabs[page].label}: swipe or tap a tab")
            }
        }
    }
}

@Composable
private fun CategoryBar() {
    var selected by remember { mutableStateOf(Categories.first()) }
    CamoTabs(selected, { selected = it }, Categories.map { CamoTab(it, it) }, scrollable = true)
}

// Editor-style tabs: twenty documents, close, drag or Alt+arrows to reorder, pin from the right-click menu.
@Composable
private fun DocumentBar() {
    var documents by remember {
        mutableStateOf(List(DOCUMENTS) { CamoDocument(it, "Document ${it + 1}.kt", modified = it % 5 == 2) })
    }
    var selected by remember { mutableStateOf<Any>(0) }
    val state = remember { CamoDocumentTabsState() }
    CamoDocumentTabs(
        documents, selected, { selected = it },
        onClose = { id ->
            val order = state.order
            val at = order.indexOf(id)
            val next = order.getOrNull(at + 1) ?: order.getOrNull(at - 1)
            documents = documents.filter { it.id != id }
            if (selected == id && next != null) selected = next
        },
        state = state,
        onContextMenu = { doc ->
            val pinned = state.isPinned(doc.id)
            listOf(
                CamoMenuItem(
                    if (pinned) "Unpin" else "Pin",
                    { if (pinned) state.unpin(doc.id) else state.pin(doc.id) },
                ),
                CamoMenuItem("Close others", {
                    documents = documents.filter { it.id == doc.id }
                    selected = doc.id
                }),
            )
        },
        modifier = Modifier.width(DocumentBarWidth),
    )
}

@Composable
private fun MessageList() {
    val state = rememberLazyListState()
    val hideOnScroll = rememberHideOnScroll(state)
    Box(Modifier.fillMaxWidth().height(ListHeight)) {
        LazyColumn(Modifier.fillMaxSize(), state) {
            items((1..ROWS).toList()) { CamoListItem("Message $it", supporting = "Scroll to move the buttons") }
        }
        CamoFab(
            icon = { AppIcon.Edit() },
            contentDescription = "Compose message",
            onClick = {},
            modifier = Modifier.align(Alignment.BottomEnd).padding(Camouflage.spacing.m),
            text = "Compose",
            expanded = !state.isScrollInProgress,
            visible = hideOnScroll,
        )
    }
}
