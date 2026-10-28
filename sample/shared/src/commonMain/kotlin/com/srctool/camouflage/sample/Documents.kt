package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.dialog.CamoDialog
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.list.CamoListSection
import com.srctool.camouflage.core.menu.CamoContextMenuArea
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuDivider
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.menu.CamoMenuGroup
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.pagedlist.CamoPagedListState
import com.srctool.camouflage.core.pagedlist.PagedLoad
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.select.CamoAsyncSelect
import com.srctool.camouflage.core.select.CamoSelect
import com.srctool.camouflage.core.select.SelectOption
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.textfield.CamoTextField
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.topbar.CamoTopBar
import com.srctool.camouflage.navigation3.CamoBottomSheetSceneStrategy
import com.srctool.camouflage.navigation3.CamoDialogSceneStrategy
import com.srctool.camouflage.navigation3.CamoNavResults
import com.srctool.camouflage.navigation3.rememberCamoNavResults
import com.srctool.camouflage.navigation3.showForResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The M6 milestone screen: a documents list with a top bar (tooltips, a sort menu, an overflow menu), filters
// as selects (a menu on desktop and a sheet on phones, a searchable sheet, an API-backed sheet), rows with a
// context menu and a "More" menu, and overlays as Navigation 3 entries (rename in a sheet, delete in a dialog).
// Under Fluent Acrylic, the menus and the sheet are Acrylic flyouts.

private data object Docs

private data class RenameDoc(val name: String)

private data class DeleteDocs(val names: List<String>)

private class Doc(val name: String, val kind: String, val owner: String, val day: Int)

private val InitialDocs = listOf(
    Doc("Budget 2027", "Spreadsheet", "Ayu", 27),
    Doc("Launch plan", "Document", "Bima", 25),
    Doc("Team photo", "Image", "Citra", 22),
    Doc("Release notes", "Document", "Ayu", 20),
    Doc("Invoices Q3", "Spreadsheet", "Dewi", 14),
)

private val Kinds = listOf("All", "Document", "Spreadsheet", "Image").map { SelectOption(it, it) }

private val Owners = listOf(
    "Anyone", "Ayu", "Bima", "Citra", "Dewi", "Eko", "Fajar", "Gita", "Hadi", "Intan", "Joko", "Kartika", "Lestari",
).map { SelectOption(it, it) }

private enum class Sort(val label: String) { Newest("Newest"), Oldest("Oldest"), Name("Name") }

/** The documents screen on its own back stack, so its sheet and dialog are entries too. */
@Composable
internal fun DocumentsScreen() {
    val backStack = remember { mutableStateListOf<Any>(Docs) }
    val results = rememberCamoNavResults()
    val docs = remember { mutableStateListOf(*InitialDocs.toTypedArray()) }
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxWidth().height(620.dp),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<Any>(),
            rememberViewModelStoreNavEntryDecorator<Any>(),
        ),
        sceneStrategies = listOf(CamoDialogSceneStrategy(), CamoBottomSheetSceneStrategy()),
        entryProvider = entryProvider {
            entry<Docs> { DocumentList(docs, backStack, results) }
            entry<RenameDoc>(metadata = CamoBottomSheetSceneStrategy.bottomSheet()) { key ->
                RenameSheet(key.name) { name ->
                    if (name != null) results.set(key, name)
                    backStack.removeLastOrNull()
                }
            }
            entry<DeleteDocs>(metadata = CamoDialogSceneStrategy.dialog()) { key ->
                DeleteDocsDialog(key.names.size) { confirmed ->
                    if (confirmed) docs.removeAll { it.name in key.names }
                    backStack.removeLastOrNull()
                }
            }
        },
    )
}

@Composable
private fun DocumentList(docs: MutableList<Doc>, backStack: MutableList<Any>, results: CamoNavResults) {
    val scope = rememberCoroutineScope()
    var sort by rememberSaveable { mutableStateOf(Sort.Newest) }
    var showOwner by rememberSaveable { mutableStateOf(true) }
    var kind by rememberSaveable { mutableStateOf<String?>("All") }
    var owner by rememberSaveable { mutableStateOf<String?>("Anyone") }
    var city by rememberSaveable { mutableStateOf<String?>(null) }

    fun rename(doc: Doc) = scope.launch {
        val name = backStack.showForResult<Any, String>(RenameDoc(doc.name), results) ?: return@launch
        val at = docs.indexOf(doc)
        if (at >= 0 && name.isNotBlank()) docs[at] = Doc(name, doc.kind, doc.owner, doc.day)
    }

    fun rowMenu(doc: Doc): List<CamoMenuEntry> = listOf(
        CamoMenuItem("Rename", { rename(doc) }, leadingIcon = { AppIcon.Edit() }),
        CamoMenuItem("Share", {}, leadingIcon = { AppIcon.Share() }, trailingText = "Ctrl+S"),
        CamoMenuDivider,
        CamoMenuItem("Delete", { backStack.add(DeleteDocs(listOf(doc.name))) }, leadingIcon = { AppIcon.Delete() },
            tone = Tone.Error),
    )

    val shown = shown(docs, kind, owner, sort)
    Column {
        DocumentsTopBar(sort, { sort = it }, showOwner, { showOwner = !showOwner }, docs.isNotEmpty()) {
            backStack.add(DeleteDocs(docs.map { it.name }))
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Camouflage.spacing.m),
            verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m),
        ) {
            val gap = Arrangement.spacedBy(Camouflage.spacing.s)
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                val field = Modifier.widthIn(min = 180.dp, max = 240.dp)
                // Four options: a menu on a desktop window, a sheet on a phone.
                CamoSelect(kind, { kind = it }, Kinds, field, label = "Type")
                CamoSelect(owner, { owner = it }, Owners, field, label = "Owner", searchable = true)
                SharedFromCity(city, field) { city = it }
            }
            CamoListSection(title = if (shown.size == 1) "1 document" else "${shown.size} documents") {
                shown.forEach { doc ->
                    CamoContextMenuArea(rowMenu(doc)) {
                        CamoListItem(
                            doc.name,
                            supporting = listOfNotNull(doc.kind, doc.owner.takeIf { showOwner }, "Oct ${doc.day}")
                                .joinToString(" · "),
                            onClick = {},
                            trailing = {
                                CamoMenuAnchor(items = rowMenu(doc)) { open ->
                                    CamoIconButton({ AppIcon.More() }, "More for ${doc.name}", open)
                                }
                            },
                        )
                    }
                }
            }
            if (shown.isEmpty()) CamoText("No documents match.")
        }
    }
}

private fun shown(docs: List<Doc>, kind: String?, owner: String?, sort: Sort): List<Doc> {
    val matching = docs.filter { (kind == "All" || it.kind == kind) && (owner == "Anyone" || it.owner == owner) }
    return when (sort) {
        Sort.Newest -> matching.sortedByDescending { it.day }
        Sort.Oldest -> matching.sortedBy { it.day }
        Sort.Name -> matching.sortedBy { it.name }
    }
}

// Tooltips on the icon buttons, an exclusive "Sort by" group, and an overflow menu with a checkable item and a
// destructive one.
@Composable
private fun DocumentsTopBar(
    sort: Sort,
    onSort: (Sort) -> Unit,
    showOwner: Boolean,
    onToggleOwner: () -> Unit,
    canDelete: Boolean,
    onDeleteAll: () -> Unit,
) {
    CamoTopBar(
        "Documents",
        windowInsets = WindowInsets(0),
        actions = {
            CamoIconButton({ AppIcon.Search() }, "Search", {}, tooltip = "Search documents")
            CamoMenuAnchor(
                items = listOf(
                    CamoMenuGroup(
                        Sort.entries.map { CamoMenuItem(it.label, { onSort(it) }, checked = sort == it) },
                        exclusive = true,
                    ),
                ),
            ) { open -> CamoIconButton({ AppIcon.Sort() }, "Sort by", open, tooltip = "Sort by") }
            CamoMenuAnchor(
                items = listOf(
                    CamoMenuItem("Show owner", onToggleOwner, checked = showOwner),
                    CamoMenuDivider,
                    CamoMenuItem("Delete all", onDeleteAll, tone = Tone.Error, enabled = canDelete),
                ),
            ) { open -> CamoIconButton({ AppIcon.More() }, "More options", open, tooltip = "More options") }
        },
    )
}

private const val CITY_PAGE = 8
private const val CITY_MILLIS = 600L

private val Cities = listOf(
    "Ambon", "Balikpapan", "Bandung", "Banjarmasin", "Batam", "Bekasi", "Bogor", "Denpasar", "Depok", "Jakarta",
    "Jambi", "Jayapura", "Kupang", "Makassar", "Malang", "Manado", "Mataram", "Medan", "Padang", "Palembang",
    "Pekanbaru", "Pontianak", "Samarinda", "Semarang", "Surabaya", "Surakarta", "Tangerang", "Yogyakarta",
)

// An API-backed select: a fake paged search over cities, one page per load.
@Composable
private fun SharedFromCity(city: String?, modifier: Modifier, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(CamoPagedListState<SelectOption<String>>()) }
    var request by remember { mutableStateOf(0) }
    LaunchedEffect(request) {
        if (request == 0) return@LaunchedEffect
        val keep = if (state.loading == PagedLoad.More) state.items else emptyList()
        delay(CITY_MILLIS)
        val page = Cities.filter { it.contains(query, ignoreCase = true) }.drop(keep.size).take(CITY_PAGE)
        state = CamoPagedListState(keep + page.map { SelectOption(it, it) }, endReached = page.size < CITY_PAGE)
    }

    fun load(kind: PagedLoad) {
        state = state.copy(items = if (kind == PagedLoad.More) state.items else emptyList(), loading = kind)
        request++
    }
    CamoAsyncSelect(
        value = city,
        onValueChange = onPick,
        options = state,
        onOpen = {
            query = ""
            load(PagedLoad.Initial)
        },
        onQueryChange = {
            query = it
            load(PagedLoad.Initial)
        },
        onLoadMore = { load(PagedLoad.More) },
        onRetry = { load(PagedLoad.Initial) },
        selectedLabel = city,
        modifier = modifier,
        label = "Shared from",
        placeholder = "Any city",
    )
}

@Composable
private fun RenameSheet(name: String, onDone: (String?) -> Unit) {
    var text by rememberSaveable { mutableStateOf(name) }
    CamoBottomSheet(title = "Rename") {
        Column(verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.m)) {
            CamoTextField(text, { text = it }, Modifier.fillMaxWidth(), label = "Name")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s)) {
                CamoButton("Save", { onDone(text.trim()) }, enabled = text.isNotBlank())
                CamoButton("Cancel", { onDone(null) }, appearance = Appearance.Ghost)
            }
        }
    }
}

@Composable
private fun DeleteDocsDialog(count: Int, onResult: (Boolean) -> Unit) {
    CamoDialog(
        "This can't be undone.",
        confirm = { CamoButton("Delete", { onResult(true) }, tone = Tone.Error) },
        title = if (count == 1) "Delete this document?" else "Delete $count documents?",
        dismiss = { CamoButton("Cancel", { onResult(false) }, appearance = Appearance.Ghost) },
    )
}
