package com.srctool.camouflage.core.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.menu.CamoContextMenuArea
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuDivider
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.menu.CamoMenuGroup
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.pagedlist.CamoPagedListState
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.select.CamoAsyncSelect
import com.srctool.camouflage.core.select.CamoSelect
import com.srctool.camouflage.core.select.SelectOption
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// CamoMenu and CamoSelect's tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class MenuTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) =
        setContent { CamouflageProvider(skinA, themeT, content) }

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun overflow(log: MutableList<String>): List<CamoMenuEntry> = listOf(
        CamoMenuItem("Share", { log += "share" }),
        CamoMenuItem("Edit", { log += "edit" }, trailingText = "⌘E"),
        CamoMenuItem("Archive", { log += "archive" }, enabled = false),
        CamoMenuDivider,
        CamoMenuItem("Delete", { log += "delete" }),
    )

    @Test
    fun selectingAnItemClosesTheMenuThenRunsItsAction() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo { CamoMenuAnchor(items = overflow(log)) { open -> CamoButton("More", open) } }
        onNodeWithText("More").performClick()
        onNodeWithText("Edit").assert(role(Role.Button))
        onNodeWithText("Edit").performClick()
        waitForIdle()
        assertEquals(listOf("edit"), log)
        onNodeWithText("Share").assertDoesNotExist()
    }

    @Test
    fun checkableItemsAreCheckboxesAndAnExclusiveGroupsAreRadios() = runComposeUiTest {
        camo {
            CamoMenuAnchor(
                items = listOf(
                    CamoMenuItem("Show hidden files", {}, checked = true),
                    CamoMenuDivider,
                    CamoMenuGroup(
                        listOf(CamoMenuItem("Newest", {}, checked = true), CamoMenuItem("Oldest", {}, checked = false)),
                        exclusive = true,
                    ),
                ),
            ) { open -> CamoButton("View", open) }
        }
        onNodeWithText("View").performClick()
        onNodeWithText("Show hidden files")
            .assert(role(Role.Checkbox))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
        onNodeWithText("Newest").assert(role(Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        onNodeWithText("Oldest").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun keysMoveFocusJumpAndTypeAheadAndEscReturnsToTheAnchor() = runComposeUiTest {
        camo { CamoMenuAnchor(items = overflow(mutableListOf())) { open -> CamoButton("More", open) } }
        onNodeWithText("More").requestFocus()
        onNodeWithText("More").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithText("Share").assertIsFocused()
        onNodeWithText("Share").performKeyInput { pressKey(Key.MoveEnd) }
        onNodeWithText("Delete").assertIsFocused()
        // The disabled item is skipped; "e" finds Edit.
        onNodeWithText("Delete").performKeyInput { pressKey(Key.E) }
        onNodeWithText("Edit").assertIsFocused()
        onNodeWithText("Edit").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithText("Delete").assertIsFocused()
        onNodeWithText("Delete").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNodeWithText("Share").assertDoesNotExist()
        onNodeWithText("More").assertIsFocused()
    }

    @Test
    fun theMenuOpensBelowItsAnchorAndAboveItNearTheBottomEdge() = runComposeUiTest {
        var bottom by mutableStateOf(false)
        camo {
            Box(Modifier.fillMaxSize(), contentAlignment = if (bottom) Alignment.BottomStart else Alignment.TopStart) {
                CamoMenuAnchor(items = overflow(mutableListOf())) { open -> CamoButton("More", open) }
            }
        }
        onNodeWithText("More").performClick()
        assertTrue(top("Share") > top("More"))
        onNodeWithText("Share").performKeyInput { pressKey(Key.Escape) }
        bottom = true
        waitForIdle()
        onNodeWithText("More").performClick()
        assertTrue(top("Share") < top("More"))
    }

    private fun ComposeUiTest.top(text: String) = onNodeWithText(text).fetchSemanticsNode().boundsInWindow.top

    @Test
    fun aContextMenuOpensOnRightClickAndOnLongPressWithoutClickingTheContent() = runComposeUiTest {
        val log = mutableListOf<String>()
        var clicks = 0
        camo {
            CamoContextMenuArea(overflow(log)) {
                Box(Modifier.size(200.dp).clickable { clicks++ }) { BasicText("Row") }
            }
        }
        onNodeWithText("Row").performMouseInput { rightClick() }
        onNodeWithText("Share").performClick()
        waitForIdle()
        onNodeWithText("Row").performTouchInput { longClick() }
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(listOf("share", "delete"), log)
        assertEquals(0, clicks)
    }

    private val sorts = listOf(
        SelectOption("new", "Newest"),
        SelectOption("old", "Oldest"),
        SelectOption("az", "Name"),
    )

    @Test
    fun aShortSelectOnAWideWindowIsAMenuAndPickingSetsTheValue() = runComposeUiTest {
        var value by mutableStateOf<String?>("new")
        camo { CamoSelect(value, { value = it }, sorts, label = "Sort by") }
        onNodeWithText("Newest").performClick()
        onNodeWithText("Oldest").assert(role(Role.RadioButton))
        onNodeWithText("Oldest").performClick()
        waitForIdle()
        assertEquals("old", value)
        onNodeWithText("Name").assertDoesNotExist()
    }

    @Test
    fun aNarrowWindowOpensASheetAndEscKeepsTheValue() = runComposeUiTest {
        var value by mutableStateOf<String?>("new")
        camo {
            Narrow { CamoSelect(value, { value = it }, sorts, label = "Sort by") }
        }
        onNodeWithText("Newest").performClick()
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Sort by")).assertExists()
        onNodeWithText("Name").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals("new", value)
        onNodeWithText("Name").assertDoesNotExist()
    }

    @Test
    fun aSearchableSelectFiltersIgnoringCaseAndAccents() = runComposeUiTest {
        val cities = listOf("Zürich", "Bern", "Genève", "Basel").map { SelectOption(it, it) }
        var value by mutableStateOf<String?>(null)
        camo { CamoSelect(value, { value = it }, cities, label = "City", searchable = true) }
        onNodeWithContentDescription("City").performClick()
        onNodeWithText("Search").performTextInput("zur")
        waitForIdle()
        onNodeWithText("Bern").assertDoesNotExist()
        onNodeWithText("Zürich").performClick()
        waitForIdle()
        assertEquals("Zürich", value)
    }

    @Test
    fun anAsyncSelectCallsOnOpenOncePerOpenAndDebouncesTheQuery() = runComposeUiTest {
        var opens = 0
        val queries = mutableListOf<String>()
        var value by mutableStateOf<Int?>(null)
        val page = CamoPagedListState(items = (1..3).map { SelectOption(it, "City $it") }, endReached = true)
        camo {
            CamoAsyncSelect(
                value = value,
                onValueChange = { value = it },
                options = page,
                onOpen = { opens++ },
                onQueryChange = { queries += it },
                onLoadMore = {},
                onRetry = {},
                selectedLabel = value?.let { "City $it" },
                label = "City",
            )
        }
        onNodeWithContentDescription("City").performClick()
        waitForIdle()
        assertEquals(1, opens)
        mainClock.autoAdvance = false
        onNodeWithText("Search").performTextInput("Ba")
        mainClock.advanceTimeBy(200)
        assertEquals(emptyList(), queries)
        mainClock.advanceTimeBy(400)
        assertEquals(listOf("Ba"), queries)
        mainClock.autoAdvance = true
        onNodeWithText("City 2").performClick()
        waitForIdle()
        assertEquals(2, value)
        onNodeWithText("City 2").performClick()
        waitForIdle()
        assertEquals(2, opens)
    }
}

// A phone-sized window for `Auto`.
@Composable
private fun Narrow(content: @Composable () -> Unit) {
    val real = LocalWindowInfo.current
    val narrow = object : WindowInfo {
        override val isWindowFocused: Boolean get() = real.isWindowFocused
        override val keyboardModifiers: PointerKeyboardModifiers get() = real.keyboardModifiers
        override val containerSize: IntSize get() = IntSize(360, 800)
    }
    CompositionLocalProvider(LocalWindowInfo provides narrow, content = content)
}
