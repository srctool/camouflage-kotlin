package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.accordion.CamoAccordion
import com.srctool.camouflage.core.accordion.CamoAccordionGroup
import com.srctool.camouflage.core.accordion.CamoAccordionItem
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.card.CamoCard
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.list.CamoListSection
import com.srctool.camouflage.core.pagedlist.CamoPagedList
import com.srctool.camouflage.core.pagedlist.CamoPagedListState
import com.srctool.camouflage.core.pagedlist.PagedError
import com.srctool.camouflage.core.pagedlist.PagedErrorScope
import com.srctool.camouflage.core.pagedlist.PagedLoad
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.search.CamoSearchField
import com.srctool.camouflage.core.skeleton.CamoSkeletonCircle
import com.srctool.camouflage.core.skeleton.CamoSkeletonGroup
import com.srctool.camouflage.core.skeleton.CamoSkeletonText
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.textfield.CamoTextField
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.topbar.CamoTopBar
import com.srctool.camouflage.core.topbar.rememberTopBarScrollOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 10
private const val PAGES = 5
private const val FAKE_PAGE_MILLIS = 1000L

/** A settings screen: a top bar that separates when the list scrolls under it, sections with switches. */
@Composable
internal fun SettingsScreen() {
    val scroll = rememberScrollState()
    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(false) }
    var backup by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().height(420.dp)) {
        CamoTopBar(
            "Settings",
            navigation = { CamoIconButton({ AppIcon.Back() }, "Back", {}) },
            actions = { CamoIconButton({ AppIcon.Search() }, "Search settings", {}) },
            scrollOffset = rememberTopBarScrollOffset(scroll),
            windowInsets = WindowInsets(0),
        )
        Column(Modifier.verticalScroll(scroll)) {
            CamoListSection(title = "Network") {
                CamoListItem("Wi-Fi", leading = { AppIcon.Star() }, trailing = { CamoSwitch(wifi, null) },
                    onClick = { wifi = !wifi })
                CamoListItem("Bluetooth", supporting = if (bluetooth) "On" else "Off", leading = { AppIcon.Star() },
                    trailing = { CamoSwitch(bluetooth, null) }, onClick = { bluetooth = !bluetooth })
                CamoListItem("VPN", supporting = "Not connected", leading = { AppIcon.Star() },
                    trailing = { AppIcon.ChevronRight() }, onClick = {})
            }
            CamoListSection(title = "Account", footer = "Backups run at night on Wi-Fi.") {
                CamoListItem("Back up photos", trailing = { CamoCheckbox(backup, null) },
                    onClick = { backup = !backup })
                CamoListItem("Storage", overline = "Plan", supporting = "12 GB of 15 GB used", onClick = {})
                CamoListItem("Sign out", onClick = {})
            }
        }
    }
}

private enum class Plan(val label: String, val price: String) { Free("Free", "0"), Pro("Pro", "9"), Team("Team", "29") }

/** Cards: the four appearances, a status panel, pick-one plan cards, and a clickable card. */
@Composable
internal fun Cards() {
    val gap = Arrangement.spacedBy(Camouflage.spacing.s)
    Row(horizontalArrangement = gap) {
        for (a in listOf(Appearance.Solid, Appearance.Soft, Appearance.Raised, Appearance.Outline)) {
            CamoCard(Modifier.weight(1f), appearance = a) { CamoText(a.name, Modifier.padding(Camouflage.spacing.m)) }
        }
    }
    CamoCard(appearance = Appearance.Soft, tone = Tone.Warning) {
        CamoText("Only 3 left in stock.", Modifier.padding(Camouflage.spacing.l))
    }
    var plan by remember { mutableStateOf(Plan.Pro) }
    Row(Modifier.selectableGroup(), horizontalArrangement = gap) {
        Plan.entries.forEach {
            CamoCard(Modifier.weight(1f), onClick = { plan = it }, selected = plan == it) {
                Column(Modifier.padding(Camouflage.spacing.m)) {
                    CamoText(it.label, style = Camouflage.typography.titleSmall)
                    CamoText("$${it.price} a month", style = Camouflage.typography.bodySmall)
                }
            }
        }
    }
    var opened by remember { mutableIntStateOf(0) }
    CamoCard(onClick = { opened++ }) {
        Column(Modifier.padding(Camouflage.spacing.l)) {
            CamoText("Getting started", style = Camouflage.typography.titleMedium)
            CamoText(if (opened == 0) "Tap to open the guide." else "Opened $opened times.")
        }
    }
}

/** An FAQ that opens one answer at a time, and product panels, one with a form that keeps its input. */
@Composable
internal fun Faq() {
    var open by remember { mutableStateOf(setOf<String>()) }
    CamoAccordionGroup(
        open,
        { open = it },
        listOf(
            CamoAccordionItem("ship", "How long does shipping take?") { CamoText("Two to four working days.") },
            CamoAccordionItem("returns", "Can I return an item?") { CamoText("Within 30 days, unused.") },
            CamoAccordionItem("pay", "Which payment methods work?") { CamoText("Cards, bank transfer and wallets.") },
        ),
        singleExpand = true,
    )
    var details by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf(false) }
    CamoAccordion("Details", details, { details = it }, subtitle = "Size, material") {
        CamoText("Cotton, regular fit.")
    }
    CamoAccordion("Gift note", note, { note = it }, subtitle = "Kept when closed") {
        var text by remember { mutableStateOf("") }
        CamoTextField(text, { text = it }, Modifier.fillMaxWidth(), label = "Message")
    }
}

/** A profile header while it loads: skeletons sharing one shimmer. */
@Composable
internal fun LoadingProfile() {
    CamoSkeletonGroup {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamoSkeletonCircle(48.dp)
            CamoSkeletonText(Modifier.weight(1f), lines = 2, style = Camouflage.typography.titleMedium)
        }
    }
}

/** A fake paged API: [PAGES] pages of products, one second each, filtered by a query; page 3 can be set to fail. */
private class FakeProducts {
    var failPage3 = false

    suspend fun page(query: String, page: Int): List<String> {
        delay(FAKE_PAGE_MILLIS)
        check(!(failPage3 && page == 2)) { "Couldn't load page 3" }
        val all = (1..PAGES * PAGE_SIZE).map { "Product $it" }.filter { it.contains(query, ignoreCase = true) }
        return all.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}

/** A product list on the fake API: paging, a failing page 3 with retry, refresh, and a search in the header. */
@Composable
internal fun Products() {
    val api = remember { FakeProducts() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(CamoPagedListState<String>(loading = PagedLoad.Initial)) }
    var failPage3 by remember { mutableStateOf(false) }
    api.failPage3 = failPage3

    fun load(kind: PagedLoad) {
        val keep = if (kind == PagedLoad.More) state.items else emptyList()
        state = CamoPagedListState(keep, loading = kind)
        val q = query
        scope.launch {
            val page = keep.size / PAGE_SIZE
            state = try {
                val items = api.page(q, page)
                CamoPagedListState(keep + items, endReached = items.size < PAGE_SIZE)
            } catch (e: IllegalStateException) {
                val scopeOf = if (keep.isEmpty()) PagedErrorScope.FirstPage else PagedErrorScope.NextPage
                CamoPagedListState(keep, error = PagedError(e.message.orEmpty(), scopeOf))
            }
        }
    }
    LaunchedEffect(query) { load(PagedLoad.Initial) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CamoCheckbox(failPage3, { failPage3 = it }, label = "Fail page 3")
        CamoButton("Refresh", { load(PagedLoad.Refresh) }, appearance = Appearance.Outline)
    }
    CamoPagedList(
        state = state,
        itemKey = { it },
        onLoadMore = { load(PagedLoad.More) },
        onRetry = { load(if (state.items.isEmpty()) PagedLoad.Initial else PagedLoad.More) },
        item = { product, _ -> CamoListItem(product, supporting = "In stock", onClick = {}) },
        modifier = Modifier.fillMaxWidth().height(420.dp),
        onRefresh = { load(PagedLoad.Refresh) },
        header = {
            CamoSearchField(query, { query = it }, {}, Modifier.fillMaxWidth().padding(bottom = Camouflage.spacing.s),
                placeholder = "Search products")
        },
    )
}
