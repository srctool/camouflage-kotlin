package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.avatar.AvatarTheme
import com.srctool.camouflage.core.avatar.CamoAvatar
import com.srctool.camouflage.core.badge.BadgeTheme
import com.srctool.camouflage.core.badge.CamoBadge
import com.srctool.camouflage.core.chip.CamoChip
import com.srctool.camouflage.core.chip.ChipTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.assertRendererAppliesBehavior
import com.srctool.camouflage.skin.testing.setCamoContent
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

// The M7 data display renderers against the contract: behavior on the whole chip, theme fields honored.
@OptIn(ExperimentalTestApi::class)
class FluentDataDisplayRendererTest {
    private val skin = FluentSkin
    private val theme = fluentTheme()

    @Test
    fun aChipAppliesItsBehaviorToTheWholeChip() = runComposeUiTest {
        assertRendererAppliesBehavior(skin, theme) { CamoChip("Chip", onClick = it) }
    }

    @Test
    fun aChipHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, ChipTheme(height = 53.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(chip = it)) { CamoChip("Tag") }
        }, measure = { it.size.height == px(53) })
    }

    @Test
    fun aChipInAScrollingRowKeepsItsLabel() = runComposeUiTest {
        setCamoContent(skin, theme) {
            Row(Modifier.horizontalScroll(rememberScrollState())) { CamoChip("Relevance", Modifier.testTag("chip")) }
        }
        val width = onNodeWithTag("chip").fetchSemanticsNode().size.width
        assertTrue(width > px(60), "the label measured to nothing: $width px")
    }

    @Test
    fun aDotHonorsItsSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, BadgeTheme(dotSize = 13.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(badge = it)) { CamoBadge() }
        }, measure = { it.size.width == px(13) })
    }

    @Test
    fun aCountHonorsItsMinWidth() = runComposeUiTest {
        assertNoLiterals(skin, theme, BadgeTheme(minWidth = 31.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(badge = it)) { CamoBadge(count = 1) }
        }, measure = { it.size.width == px(31) })
    }

    @Test
    fun anAvatarHonorsItsSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, AvatarTheme(sizeMedium = 57.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(avatar = it)) { CamoAvatar(name = "Ada Lovelace") }
        }, measure = { it.size.width == px(57) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
