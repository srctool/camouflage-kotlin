package com.srctool.camouflage.navigation3

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.dialog.CamoDialog
import com.srctool.camouflage.core.overlay.OnDismissRequest
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography
import com.srctool.camouflage.skin.minimal.MinimalSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data object Home

private data object CountryPicker

private data object DeleteItem

private class PickerViewModel : ViewModel() {
    override fun onCleared() {
        cleared++
    }

    companion object {
        var cleared = 0
    }
}

// Registered overlays on a real NavDisplay (base Overlays §4): shown over Home, results, dismissal, the veto.
@OptIn(ExperimentalTestApi::class)
class SceneStrategyTest {
    private val theme =
        CamoTheme(colors = CamoColors.fromSeed(primary = Color(0xFF0B57D0)), typography = CamoTypography.system())

    private fun ComposeUiTest.app(
        backStack: SnapshotStateList<Any>,
        results: (CamoNavResults, CoroutineScope) -> Unit = { _, _ -> },
        keepOpen: () -> Boolean = { false },
    ) = setContent {
        CamouflageProvider(MinimalSkin, theme) {
            val store = rememberCamoNavResults()
            results(store, rememberCoroutineScope())
            NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator<Any>(),
                    rememberViewModelStoreNavEntryDecorator<Any>(),
                ),
                sceneStrategies = listOf(CamoDialogSceneStrategy(), CamoBottomSheetSceneStrategy()),
                entryProvider = entryProvider {
                    entry<Home> { BasicText("Home") }
                    entry<CountryPicker>(metadata = CamoBottomSheetSceneStrategy.bottomSheet()) {
                        viewModel { PickerViewModel() }
                        CamoBottomSheet(title = "Choose country") {
                            CamoButton("Indonesia", {
                                store.set(CountryPicker, "ID")
                                backStack.removeLastOrNull()
                            })
                        }
                    }
                    entry<DeleteItem>(metadata = CamoDialogSceneStrategy.dialog()) {
                        OnDismissRequest { !keepOpen() }
                        CamoDialog("This can't be undone.", confirm = { CamoButton("Delete", {}) }, title = "Delete?")
                    }
                },
            )
        }
    }

    private fun pane(title: String) = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)

    @Test
    fun aSheetShowsOverHomeAndItsResultReachesTheCaller() = runComposeUiTest {
        val backStack = mutableStateListOf<Any>(Home)
        lateinit var results: CamoNavResults
        lateinit var scope: CoroutineScope
        app(backStack, { r, s -> results = r; scope = s })
        var picked: String? = "unset"
        waitForIdle()
        scope.launch { picked = backStack.showForResult(CountryPicker, results) }
        waitForIdle()
        onNodeWithText("Home").assertExists()
        onNodeWithText("Indonesia").performClick()
        waitForIdle()
        assertEquals("ID", picked)
        assertEquals(listOf<Any>(Home), backStack.toList())
        onNodeWithText("Indonesia").assertDoesNotExist()
    }

    @Test
    fun aDismissedSheetGivesNullAndClearsItsViewModel() = runComposeUiTest {
        val backStack = mutableStateListOf<Any>(Home)
        lateinit var results: CamoNavResults
        lateinit var scope: CoroutineScope
        app(backStack, { r, s -> results = r; scope = s })
        val before = PickerViewModel.cleared
        var picked: String? = "unset"
        waitForIdle()
        scope.launch { picked = backStack.showForResult(CountryPicker, results) }
        waitForIdle()
        onNode(pane("Choose country")).performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertNull(picked)
        assertEquals(listOf<Any>(Home), backStack.toList())
        assertTrue(PickerViewModel.cleared > before)
    }

    @Test
    fun escClosesADialogUnlessItVetoes() = runComposeUiTest {
        val backStack = mutableStateListOf<Any>(Home, DeleteItem)
        var keep = true
        app(backStack, keepOpen = { keep })
        waitForIdle()
        onNodeWithText("Delete").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(listOf<Any>(Home, DeleteItem), backStack.toList())
        keep = false
        onNodeWithText("Delete").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(listOf<Any>(Home), backStack.toList())
        onNode(pane("Delete?")).assertDoesNotExist()
    }

    @Test
    fun consumeDeliversAResultOnce() = runComposeUiTest {
        val backStack = mutableStateListOf<Any>(Home)
        val seen = mutableListOf<String>()
        setContent {
            CamouflageProvider(MinimalSkin, theme) {
                val results = rememberCamoNavResults()
                results.consume<String>(CountryPicker) { seen += it }
                CamoButton("Pick", { results.set(CountryPicker, "ID") })
            }
        }
        onNodeWithText("Pick").performClick()
        waitForIdle()
        assertEquals(listOf("ID"), seen)
        waitForIdle()
        assertEquals(listOf("ID"), seen)
        assertEquals(listOf<Any>(Home), backStack.toList())
    }
}
