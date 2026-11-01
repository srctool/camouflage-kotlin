package com.srctool.camouflage.skin.minimal

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.fab.CamoFab
import com.srctool.camouflage.core.fab.FabTheme
import com.srctool.camouflage.core.tabs.CamoTab
import com.srctool.camouflage.core.tabs.CamoTabs
import com.srctool.camouflage.core.tabs.TabsTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.assertRendererAppliesBehavior
import kotlin.math.roundToInt
import kotlin.test.Test

// The M8 tabs and FAB renderers against the contract: behavior on the whole node, theme fields honored.
@OptIn(ExperimentalTestApi::class)
class MinimalNavigationRendererTest {
    private val skin = MinimalSkin
    private val theme = minimalLight

    @Test
    fun aTabAppliesItsBehaviorToTheWholeTab() = runComposeUiTest {
        assertRendererAppliesBehavior(skin, theme) { onClick ->
            CamoTabs("none", { onClick() }, listOf(CamoTab("one", "One")))
        }
    }

    @Test
    fun aFabAppliesItsBehaviorToTheWholeFab() = runComposeUiTest {
        assertRendererAppliesBehavior(skin, theme) { CamoFab({}, "Add", it) }
    }

    @Test
    fun aTabBarHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, TabsTheme(height = 61.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(tabs = it)) { CamoTabs("a", {}, listOf(CamoTab("a", "One"))) }
        }, measure = { it.size.height == px(61) })
    }

    @Test
    fun aFabHonorsItsSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, FabTheme(sizeRegular = 57.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(fab = it)) { CamoFab({}, "Add", {}) }
        }, measure = { it.size.width == px(57) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
