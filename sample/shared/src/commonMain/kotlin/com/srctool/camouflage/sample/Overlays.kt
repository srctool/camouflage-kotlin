package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.dialog.CamoDialog
import com.srctool.camouflage.core.dialog.CamoDialogFrame
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.radio.CamoRadioGroup
import com.srctool.camouflage.core.radio.RadioOption
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.navigation3.CamoBottomSheetSceneStrategy
import com.srctool.camouflage.navigation3.CamoDialogSceneStrategy
import com.srctool.camouflage.navigation3.CamoNavResults
import com.srctool.camouflage.navigation3.rememberCamoNavResults
import com.srctool.camouflage.navigation3.showForResult
import kotlinx.coroutines.launch

// The base Overlays samples: overlays registered as Navigation 3 entries, each in its own composable with its own
// ViewModel, and one standalone frame. The keys are data objects, so results are keyed by their names.
private data object Home

private data object CountryPicker

private data object DeleteItem

private data object SortOrderPicker

private val Countries = listOf("Indonesia", "Japan", "Netherlands", "New Zealand", "Singapore")

/** The country picker's own state, scoped to its entry: cleared when the sheet closes. */
internal class CountryPickerViewModel : ViewModel() {
    val countries = Countries
}

/** The overlay demos on a small Navigation 3 back stack. */
@Composable
internal fun OverlayDemo() {
    val backStack = remember { mutableStateListOf<Any>(Home) }
    val results = rememberCamoNavResults()
    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxWidth().height(260.dp),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<Any>(),
            rememberViewModelStoreNavEntryDecorator<Any>(),
        ),
        sceneStrategies = listOf(CamoDialogSceneStrategy(), CamoBottomSheetSceneStrategy()),
        entryProvider = entryProvider {
            entry<Home> { OverlayHome(backStack, results) }
            entry<CountryPicker>(metadata = CamoBottomSheetSceneStrategy.bottomSheet()) {
                CountryPickerSheet(onPick = {
                    results.set(CountryPicker, it)
                    backStack.removeLastOrNull()
                })
            }
            entry<DeleteItem>(metadata = CamoDialogSceneStrategy.dialog()) {
                DeleteItemDialog(onResult = {
                    results.set(DeleteItem, it)
                    backStack.removeLastOrNull()
                })
            }
            entry<SortOrderPicker>(metadata = CamoDialogSceneStrategy.dialog()) {
                SortOrderDialog(onResult = { order ->
                    if (order != null) results.set(SortOrderPicker, order)
                    backStack.removeLastOrNull()
                })
            }
        },
    )
}

@Composable
private fun OverlayHome(backStack: MutableList<Any>, results: CamoNavResults) {
    val scope = rememberCoroutineScope()
    var country by rememberSaveable { mutableStateOf<String?>(null) }
    var deleted by rememberSaveable { mutableStateOf(false) }
    var order by rememberSaveable { mutableStateOf("Newest") }
    var standalone by rememberSaveable { mutableStateOf(false) }
    // A result that must survive process death while the dialog is open: consumed, not awaited.
    results.consume<Boolean>(DeleteItem) { deleted = it }
    results.consume<String>(SortOrderPicker) { order = it }
    val gap = Arrangement.spacedBy(Camouflage.spacing.s)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = gap) {
        CamoText("Country: ${country ?: "none"} · Sort: $order · ${if (deleted) "Item deleted" else "Item kept"}")
        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
            CamoButton("Choose country", {
                scope.launch { backStack.showForResult<Any, String>(CountryPicker, results)?.let { country = it } }
            })
            CamoButton("Delete item", { backStack.add(DeleteItem) }, appearance = Appearance.Outline, tone = Tone.Error)
            CamoButton("Sort by", { backStack.add(SortOrderPicker) }, appearance = Appearance.Outline)
            CamoButton("Standalone dialog", { standalone = true }, appearance = Appearance.Ghost)
        }
    }
    if (standalone) {
        CamoDialogFrame(onDismissRequest = { standalone = false }) {
            CamoDialog(
                "This dialog is app state, shown without a navigator.",
                confirm = { CamoButton("Close", { standalone = false }) },
                title = "Standalone",
            )
        }
    }
}

/** The country picker: a sheet in its own composable, with its own ViewModel. */
@Composable
internal fun CountryPickerSheet(
    onPick: (String) -> Unit,
    vm: CountryPickerViewModel = viewModel { CountryPickerViewModel() },
) {
    CamoBottomSheet(title = "Choose country") {
        Column {
            vm.countries.forEach { CamoListItem(it, onClick = { onPick(it) }) }
        }
    }
}

/** A destructive confirmation: `true` to delete. */
@Composable
internal fun DeleteItemDialog(onResult: (Boolean) -> Unit) {
    CamoDialog(
        "This can't be undone.",
        confirm = { CamoButton("Delete", { onResult(true) }, tone = Tone.Error) },
        title = "Delete item?",
        dismiss = { CamoButton("Cancel", { onResult(false) }, appearance = Appearance.Ghost) },
    )
}

/** A choice in custom content: the chosen order, or `null` when cancelled. */
@Composable
internal fun SortOrderDialog(onResult: (String?) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Newest") }
    CamoDialog(
        "Choose how items are ordered.",
        confirm = { CamoButton("Apply", { onResult(selected) }) },
        title = "Sort by",
        dismiss = { CamoButton("Cancel", { onResult(null) }, appearance = Appearance.Ghost) },
    ) {
        CamoRadioGroup(selected, { selected = it }, listOf("Newest", "Price", "Rating").map { RadioOption(it, it) })
    }
}
