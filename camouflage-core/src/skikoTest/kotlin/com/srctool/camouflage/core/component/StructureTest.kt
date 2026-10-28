package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.accordion.CamoAccordion
import com.srctool.camouflage.core.accordion.CamoAccordionGroup
import com.srctool.camouflage.core.accordion.CamoAccordionItem
import com.srctool.camouflage.core.card.CamoCard
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.list.CamoListSection
import com.srctool.camouflage.core.pagedlist.CamoPagedList
import com.srctool.camouflage.core.pagedlist.CamoPagedListState
import com.srctool.camouflage.core.pagedlist.PagedError
import com.srctool.camouflage.core.pagedlist.PagedErrorScope
import com.srctool.camouflage.core.pagedlist.PagedLoad
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skeleton.CamoSkeleton
import com.srctool.camouflage.core.skeleton.CamoSkeletonText
import com.srctool.camouflage.core.skeleton.SkeletonAnimation
import com.srctool.camouflage.core.skeleton.SkeletonRenderer
import com.srctool.camouflage.core.skeleton.SkeletonState
import com.srctool.camouflage.core.skeleton.SkeletonStyle
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.topbar.CamoTopBar
import com.srctool.camouflage.core.PlainSkeleton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The M6 structure components' tests from their Kotlin notes: card, list item, accordion, skeleton, top bar,
// paged list.
@OptIn(ExperimentalTestApi::class)
class StructureTest {
    private fun ComposeUiTest.camo(
        skin: CamoSkin = skinA,
        theme: CamoTheme = themeT,
        content: @Composable () -> Unit,
    ) = setContent { CamouflageProvider(skin, theme, content) }

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private val noRole = SemanticsMatcher.keyNotDefined(SemanticsProperties.Role)

    // CamoCard

    @Test
    fun aCardWithoutOnClickHasNoClickSemantics() = runComposeUiTest {
        camo { CamoCard { BasicText("Plain") } }
        onNodeWithText("Plain").assert(!hasClickAction())
        onNodeWithTag("card-Raised", useUnmergedTree = true).assert(noRole).assert(!hasClickAction())
    }

    @Test
    fun aClickableCardIsOneButtonAndFiresOncePerTap() = runComposeUiTest {
        var taps = 0
        camo { CamoCard(onClick = { taps++ }) { Column { BasicText("Pro"); BasicText("12 a month") } } }
        onNodeWithText("Pro").assert(role(Role.Button)).performClick()
        assertEquals(1, taps)
    }

    @Test
    fun aDisabledCardIgnoresClicksAndASelectableCardAnnouncesItsState() = runComposeUiTest {
        var taps = 0
        camo {
            Column {
                CamoCard(onClick = { taps++ }, enabled = false) { BasicText("Off") }
                CamoCard(onClick = {}, selected = true) { BasicText("Chosen") }
            }
        }
        onNodeWithText("Off").performClick()
        assertEquals(0, taps)
        onNodeWithText("Chosen").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    // CamoListItem

    @Test
    fun rowHeightsFollowTheLineCount() = runComposeUiTest {
        camo {
            Column {
                CamoListItem("One", Modifier.width(300.dp).testTag("one"))
                CamoListItem("Two", Modifier.width(300.dp).testTag("two"), supporting = "Detail")
                CamoListItem("Three", Modifier.width(300.dp).testTag("three"), supporting = "Detail", overline = "Over")
            }
        }
        onNodeWithTag("one").assertHeightIsEqualTo(56.dp)
        onNodeWithTag("two").assertHeightIsEqualTo(72.dp)
        onNodeWithTag("three").assertHeightIsEqualTo(88.dp)
    }

    @Test
    fun aNonClickableRowHasNoRoleAndASwitchRowIsAnnouncedAsTheSwitch() = runComposeUiTest {
        var wifi by mutableStateOf(false)
        camo {
            Column {
                CamoListItem("Plain row")
                CamoListItem(
                    "Wi-Fi",
                    trailing = { CamoSwitch(wifi, onCheckedChange = null) },
                    onClick = { wifi = !wifi },
                )
            }
        }
        onNodeWithText("Plain row").assert(noRole).assert(!hasClickAction())
        onNodeWithText("Wi-Fi")
            .assert(role(Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .performClick()
        assertTrue(wifi)
        onNodeWithText("Wi-Fi")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    @Test
    fun aRowWithoutAControlIsAButtonAndTheSectionTitleIsAHeading() = runComposeUiTest {
        camo {
            CamoListSection(title = "Network") {
                CamoListItem("Proxy", onClick = {})
                CamoListItem("DNS", onClick = {}, selected = true)
            }
        }
        onNodeWithText("Network").assert(isHeading())
        onNodeWithText("Proxy").assert(role(Role.Button))
        onNodeWithText("DNS").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
    }

    // CamoAccordion

    @Test
    fun theHeaderTogglesAndIsAHeadingWithItsState() = runComposeUiTest {
        var open by mutableStateOf(false)
        camo { CamoAccordion("Shipping", open, { open = it }) { BasicText("Ships in 2 days") } }
        val header = onNodeWithText("Shipping")
        header.assert(isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
        onNodeWithText("Ships in 2 days").assertDoesNotExist()
        header.performClick()
        assertTrue(open)
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        onNodeWithText("Ships in 2 days").assertExists()
        header.assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse))
    }

    @Test
    fun collapsedContentKeepsItsState() = runComposeUiTest {
        var open by mutableStateOf(true)
        camo {
            CamoAccordion("Details", open, { open = it }) {
                var text by remember { mutableStateOf("") }
                BasicTextField(text, { text = it }, Modifier.width(200.dp))
            }
        }
        onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("kept")
        open = false
        waitForIdle()
        onNodeWithText("kept").assertDoesNotExist()
        open = true
        waitForIdle()
        onNodeWithText("kept").assertExists()
    }

    @Test
    fun singleExpandClosesTheOthers() = runComposeUiTest {
        var open by mutableStateOf(setOf("a"))
        camo {
            CamoAccordionGroup(
                open,
                { open = it },
                listOf(
                    CamoAccordionItem("a", "First") { BasicText("A") },
                    CamoAccordionItem("b", "Second") { BasicText("B") },
                ),
                singleExpand = true,
            )
        }
        onNodeWithText("Second").performClick()
        assertEquals(setOf("b"), open)
        onNodeWithText("Second").performClick()
        assertEquals(emptySet(), open)
    }

    // CamoSkeleton

    @Test
    fun skeletonsHaveNoSemanticsAndReducedMotionIsStatic() = runComposeUiTest {
        val seen = mutableListOf<SkeletonAnimation>()
        val recording = object : SkeletonRenderer by PlainSkeleton {
            @Composable
            override fun Render(state: SkeletonState, theme: CamoTheme, style: SkeletonStyle, modifier: Modifier) {
                seen += state.animation
                PlainSkeleton.Render(state, theme, style, modifier)
            }
        }
        camo(skinA.copy(skeleton = recording), themeT.copy(motion = themeT.motion.reduced())) {
            Column {
                CamoSkeleton(height = 24.dp)
                CamoSkeletonText(lines = 3)
            }
        }
        assertEquals(4, seen.size)
        assertTrue(seen.all { it == SkeletonAnimation.None })
        // Each block is a cleared node: nothing to announce.
        assertTrue(onRoot().fetchSemanticsNode().children.all { it.config.toList().isEmpty() })
    }

    // CamoTopBar

    @Test
    fun theTitleIsAHeadingAndTheScrollOffsetReachesTheRenderer() = runComposeUiTest {
        var offset by mutableStateOf(0.dp)
        camo { CamoTopBar("Settings", scrollOffset = offset) }
        onNodeWithText("Settings").assert(isHeading())
        onNodeWithTag("bar-resting").assertExists()
        offset = 8.dp
        waitForIdle()
        onNodeWithTag("bar-scrolled").assertExists()
        offset = androidx.compose.ui.unit.Dp.Infinity
        waitForIdle()
        onNodeWithTag("bar-scrolled").assertExists()
    }

    @Test
    fun aCustomTitleKeepsTheTitleAsTheHeading() = runComposeUiTest {
        camo { CamoTopBar("Search", titleContent = { BasicText("logo") }) }
        onNodeWithContentDescription("Search").assert(isHeading())
        onNodeWithText("logo").assertExists()
    }

    // CamoPagedList

    private val page = (1..30).map { "Item $it" }

    @Composable
    private fun Paged(state: CamoPagedListState<String>, onLoadMore: () -> Unit = {}, onRetry: () -> Unit = {}) =
        CamoPagedList(state, { it }, onLoadMore, onRetry, { value, _ -> BasicText(value, Modifier.height(48.dp)) },
            Modifier.height(400.dp))

    @Test
    fun eachLoadingStateDrawsItsRows() = runComposeUiTest {
        var state by mutableStateOf(CamoPagedListState<String>(loading = PagedLoad.Initial))
        camo { Paged(state) }
        onNodeWithText("Item 1").assertDoesNotExist()
        onNodeWithContentDescription("Loading").assertExists()
        state = CamoPagedListState(page.take(3))
        waitForIdle()
        onNodeWithText("Item 1").assertExists()
        state = CamoPagedListState(emptyList())
        waitForIdle()
        onNodeWithText("Nothing here yet").assertExists()
        state = CamoPagedListState(page.take(3), loading = PagedLoad.More)
        waitForIdle()
        onNodeWithText("Item 3").assertExists()
        onNodeWithContentDescription("Loading more").assertExists()
    }

    @Test
    fun aFirstPageErrorFocusesRetryAndANextPageErrorShowsARetryRow() = runComposeUiTest {
        var retries = 0
        var state by mutableStateOf(
            CamoPagedListState<String>(error = PagedError("Offline", PagedErrorScope.FirstPage)),
        )
        camo { Paged(state, onRetry = { retries++ }) }
        onNodeWithText("Offline").assertExists()
        onNodeWithText("Retry").assertIsFocused().performClick()
        assertEquals(1, retries)
        state = CamoPagedListState(page.take(2), error = PagedError("Couldn't load more", PagedErrorScope.NextPage))
        waitForIdle()
        onNodeWithText("Item 2").assertExists()
        onNodeWithText("Retry").performClick()
        assertEquals(2, retries)
    }

    @Test
    fun loadMoreFiresOnceNearTheEndAndNeverWhileLoadingOrAtTheEnd() = runComposeUiTest {
        var calls = 0
        var state by mutableStateOf(CamoPagedListState(page))
        camo { Paged(state, onLoadMore = { calls++ }) }
        assertEquals(0, calls)
        onNode(androidx.compose.ui.test.hasScrollToIndexAction()).performScrollToIndex(29)
        waitForIdle()
        assertEquals(1, calls)
        state = state.copy(loading = PagedLoad.More)
        waitForIdle()
        assertEquals(1, calls)
        state = CamoPagedListState(page, endReached = true)
        waitForIdle()
        assertEquals(1, calls)
    }

    @Test
    fun aShortFirstPageLoadsMoreRightAway() = runComposeUiTest {
        var calls = 0
        camo { Paged(CamoPagedListState(page.take(2)), onLoadMore = { calls++ }) }
        waitForIdle()
        assertEquals(1, calls)
    }
}
