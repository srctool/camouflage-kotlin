package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.avatar.CamoAvatar
import com.srctool.camouflage.core.avatar.CamoAvatarGroup
import com.srctool.camouflage.core.avatar.Presence
import com.srctool.camouflage.core.avatar.avatarColorSeed
import com.srctool.camouflage.core.avatar.avatarInitials
import com.srctool.camouflage.core.avatar.graphemes
import com.srctool.camouflage.core.badge.CamoBadge
import com.srctool.camouflage.core.badge.CamoBadgedBox
import com.srctool.camouflage.core.badge.badgeText
import com.srctool.camouflage.core.chip.CamoChip
import com.srctool.camouflage.core.chip.CamoChipGroup
import com.srctool.camouflage.core.chip.ChipKind
import com.srctool.camouflage.core.chip.ChipLayout
import com.srctool.camouflage.core.chip.ChipOption
import com.srctool.camouflage.core.chip.chipGroupSelection
import com.srctool.camouflage.core.chip.chipKind
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The M7 data display components' tests from their Kotlin notes: chip, chip group, badge, avatar.
@OptIn(ExperimentalTestApi::class)
class DataDisplayTest {
    private fun ComposeUiTest.camo(rtl: Boolean = false, content: @Composable () -> Unit) = setContent {
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            CamouflageProvider(skinA, themeT, content)
        }
    }

    private fun role(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun stateDescription(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun toggle(state: ToggleableState) =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)

    private val icon = IconSource.Vector(ImageVector.Builder("i", 24.dp, 24.dp, 24f, 24f).build())

    // Chips

    @Test
    fun theCallbacksDecideTheKind() {
        assertEquals(ChipKind.Filter, chipKind({}, selected = false, onRemove = {}))
        assertEquals(ChipKind.Input, chipKind(null, null, onRemove = {}))
        assertEquals(ChipKind.Action, chipKind({}, null, null))
        assertEquals(ChipKind.Tag, chipKind(null, null, null))
    }

    @Test
    fun aFilterChipIsAToggleThatAnnouncesItsState() = runComposeUiTest {
        camo {
            var on by remember { mutableStateOf(false) }
            CamoChip("Open now", onClick = { on = !on }, selected = on)
        }
        val chip = onNodeWithText("Open now")
        chip.assert(role(Role.Checkbox)).assert(toggle(ToggleableState.Off))
        chip.performClick()
        chip.assert(toggle(ToggleableState.On))
    }

    @Test
    fun anActionChipIsAButtonAndATagIsText() = runComposeUiTest {
        var taps = 0
        camo {
            Column {
                CamoChip("Add to calendar", onClick = { taps++ })
                CamoChip("Paid")
            }
        }
        onNodeWithText("Add to calendar").assert(role(Role.Button)).performClick()
        assertEquals(1, taps)
        onNodeWithText("Paid").assert(!hasClickAction())
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun anInputChipIsRemovedByItsButtonAndByTheDeleteKey() = runComposeUiTest {
        var removed = listOf<String>()
        camo {
            Column {
                CamoChip("Ada", onRemove = { removed = removed + "Ada" })
                CamoChip("Grace", onRemove = { removed = removed + "Grace" })
            }
        }
        onNodeWithContentDescription("Remove Ada").assert(role(Role.Button)).performClick()
        onNodeWithText("Grace").performSemanticsAction(SemanticsActions.RequestFocus)
        onNodeWithText("Grace").assertIsFocused().performKeyInput { pressKey(Key.Delete) }
        assertEquals(listOf("Ada", "Grace"), removed)
    }

    @Test
    fun anInteractiveChipHasAFullTouchTarget() = runComposeUiTest {
        camo { CamoChip("Filter", Modifier.testTag("chip"), onClick = {}) }
        onNodeWithTag("chip").assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun groupSelectionFollowsTheMode() {
        assertEquals(setOf(1, 2), chipGroupSelection(setOf(1), 2, multiSelect = true, required = false))
        assertEquals(emptySet(), chipGroupSelection(setOf(1), 1, multiSelect = true, required = false))
        assertEquals(setOf(2), chipGroupSelection(setOf(1), 2, multiSelect = false, required = false))
        assertEquals(emptySet(), chipGroupSelection(setOf(1), 1, multiSelect = false, required = false))
        assertEquals(setOf(1), chipGroupSelection(setOf(1), 1, multiSelect = false, required = true))
    }

    @Test
    fun aSingleChoiceGroupIsARadioGroupThatKeepsARequiredChoice() = runComposeUiTest {
        var picked = setOf("Newest")
        camo {
            var selected by remember { mutableStateOf(setOf("Newest")) }
            CamoChipGroup(
                listOf(ChipOption("Newest", "Newest"), ChipOption("Oldest", "Oldest")),
                selected,
                { selected = it; picked = it },
                multiSelect = false,
                required = true,
            )
        }
        onNodeWithText("Newest").assert(role(Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .performClick()
        assertEquals(setOf("Newest"), picked)
        onNodeWithText("Oldest").performClick()
        assertEquals(setOf("Oldest"), picked)
    }

    @Test
    fun aMultiSelectGroupTogglesEachChip() = runComposeUiTest {
        var picked = emptySet<Int>()
        camo {
            var selected by remember { mutableStateOf(emptySet<Int>()) }
            CamoChipGroup(
                (1..3).map { ChipOption(it, "Option $it") },
                selected,
                { selected = it; picked = it },
            )
        }
        onNodeWithText("Option 1").assert(role(Role.Checkbox)).performClick()
        onNodeWithText("Option 3").performClick()
        assertEquals(setOf(1, 3), picked)
        onNodeWithText("Option 1").performClick()
        assertEquals(setOf(3), picked)
    }

    @Test
    fun theArrowKeysBringAFocusedChipIntoViewInAScrollingGroup() = runComposeUiTest {
        camo {
            CamoChipGroup(
                (1..12).map { ChipOption(it, "Chip $it") },
                emptySet(),
                {},
                Modifier.width(200.dp),
                layout = ChipLayout.Scroll,
            )
        }
        onNodeWithText("Chip 1").performSemanticsAction(SemanticsActions.RequestFocus)
        // Left from the first chip wraps to the last one, far off screen.
        onNodeWithText("Chip 1").performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        onNodeWithText("Chip 12").assertIsFocused().assertIsDisplayed()
        // Moving in a multi-select group doesn't select.
        onNodeWithText("Chip 12").assert(toggle(ToggleableState.Off))
    }

    // Badges

    @Test
    fun theBadgeTextCapsAtMax() {
        assertNull(badgeText(null, 99))
        assertEquals("3", badgeText(3, 99))
        assertEquals("99+", badgeText(100, 99))
        assertEquals("9+", badgeText(10, 9))
    }

    @Test
    fun anIconButtonAnnouncesItsBadge() = runComposeUiTest {
        camo {
            CamoBadgedBox(badge = { CamoBadge(count = 3) }) {
                CamoIconButton({ CamoIcon(icon, null) }, "Notifications", {})
            }
        }
        onNodeWithContentDescription("Notifications").assert(role(Role.Button)).assert(stateDescription("3 new"))
    }

    @Test
    fun plainContentTakesTheBadgeDescriptionAndZeroHidesIt() = runComposeUiTest {
        var count by mutableStateOf(2)
        camo {
            CamoBadgedBox(badge = { CamoBadge(count = count, contentDescription = "$count unread") }) {
                CamoIcon(icon, "Inbox")
            }
        }
        onNodeWithContentDescription("Inbox").assert(stateDescription("2 unread"))
        count = 0
        waitForIdle()
        onNodeWithContentDescription("Inbox")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun theBadgeSitsOnTheTopEndCornerInBothDirections() {
        for (rtl in listOf(false, true)) {
            runComposeUiTest {
                camo(rtl) {
                    CamoBadgedBox(badge = { CamoBadge(Modifier.testTag("badge")) }, Modifier.testTag("anchor")) {
                        Box(Modifier.size(24.dp))
                    }
                }
                val anchor = onNodeWithTag("anchor").getUnclippedBoundsInRoot()
                val badge = onNodeWithTag("badge", useUnmergedTree = true).getUnclippedBoundsInRoot()
                // The test badge's dot is 6 wide, its center 4 in from the end edge and 4 down from the top.
                val expected = if (rtl) 1.dp else 17.dp
                assertEquals(expected, badge.left - anchor.left, "rtl = $rtl")
                assertEquals(1.dp, badge.top - anchor.top, "rtl = $rtl")
            }
        }
    }

    // Avatars

    @Test
    fun initialsComeFromTheFirstAndLastWords() {
        assertEquals("DR", avatarInitials("Dwiki Riyadi"))
        assertEquals("D", avatarInitials("dwiki"))
        assertEquals("AK", avatarInitials("  Ada  Lovelace King "))
        assertEquals("李", avatarInitials("李小龙"))
        assertEquals("ÉZ", avatarInitials("émile Zola"))
        assertNull(avatarInitials("😀 Smile"))
        assertNull(avatarInitials(""))
        assertNull(avatarInitials(null))
    }

    @Test
    fun graphemesKeepEmojiAndMarksWhole() {
        assertEquals(listOf("🇮🇩", "🇯🇵"), graphemes("🇮🇩🇯🇵"))
        assertEquals(listOf("👍🏽", "x"), graphemes("👍🏽x"))
        assertEquals(1, graphemes("👨‍👩‍👧").size)
        assertEquals(listOf("é", "a"), graphemes("éa"))
    }

    @Test
    fun theColorSeedIsFnv1aOfTheUtf8Bytes() {
        // The same vectors as the Flutter library's test, so a person gets the same color on both.
        assertEquals(0xCDC853E9u, avatarColorSeed("Dwiki Riyadi"))
        assertEquals(0xFE5E7CFEu, avatarColorSeed("Ada Lovelace"))
        assertEquals(0x785BAF5Eu, avatarColorSeed("李小龙"))
        assertEquals(0x811C9DC5u, avatarColorSeed(""))
    }

    @Test
    fun anAvatarShowsInitialsUnderTheImageAndSaysNameAndPresence() = runComposeUiTest {
        camo {
            Column {
                CamoAvatar(name = "Dwiki Riyadi", presence = Presence.Online)
                CamoAvatar(Modifier.testTag("photo"), name = "Ada Lovelace", image = { BasicText("photo") })
                CamoAvatar(Modifier.testTag("nobody"))
            }
        }
        onNodeWithContentDescription("Dwiki Riyadi, online").assert(role(Role.Image))
        onNodeWithText("DR", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("AL", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithText("photo", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("nobody").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun anAvatarInAListRowLeavesTheNameToTheRow() = runComposeUiTest {
        camo { CamoListItem("Ada Lovelace", leading = { CamoAvatar(name = "Ada Lovelace") }, onClick = {}) }
        onNodeWithText("Ada Lovelace").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        onNodeWithText("AL", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aGroupShowsMaxAvatarsAndTheRestAsACount() = runComposeUiTest {
        camo {
            CamoAvatarGroup(listOf("A B", "C D", "E F", "G H", "I J").map { n -> { CamoAvatar(name = n) } }, max = 3)
        }
        onNodeWithContentDescription("E F").assertIsDisplayed()
        onNodeWithContentDescription("2 more").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithContentDescription("G H").assertIsDisplayed() }.isFailure)
    }
}
