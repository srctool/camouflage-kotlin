package com.srctool.camouflage.skin.cupertino

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.navigation.CamoNavItem
import com.srctool.camouflage.core.navigation.CamoNavSection
import com.srctool.camouflage.core.navigation.CamoNavigationBar
import com.srctool.camouflage.core.navigation.CamoNavigationDrawer
import com.srctool.camouflage.core.navigation.CamoNavigationRail
import com.srctool.camouflage.core.navigation.NavigationBarTheme
import com.srctool.camouflage.core.navigation.NavigationDrawerTheme
import com.srctool.camouflage.core.navigation.NavigationRailTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.setCamoContent
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

// The M8 navigation chrome renderers against the contract: behavior on the whole item, theme fields honored.
@OptIn(ExperimentalTestApi::class)
class CupertinoNavigationChromeRendererTest {
    private val skin = CupertinoSkin
    private val theme = cupertinoLight
    private val items = listOf("A", "B", "C").map { CamoNavItem(it, it, icon = { BasicText("i") }) }

    @Test
    fun anItemTakesTapsAcrossItsWholeBox() = runComposeUiTest {
        var selected = ""
        setCamoContent(skin, theme) { CamoNavigationBar(items, "none", { selected += it }) }
        val box = onNodeWithText("A").fetchSemanticsNode().boundsInRoot
        onNodeWithText("A").performTouchInput {
            click(Offset(1f, 1f))
            click(Offset(box.width - 2f, box.height - 2f))
        }
        assertEquals("AA", selected)
    }

    @Test
    fun aBarHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, NavigationBarTheme(height = 71.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(navigationBar = it)) { CamoNavigationBar(items, "A", {}) }
        }, measure = { it.size.height == px(71) })
    }

    @Test
    fun aRailHonorsItsWidth() = runComposeUiTest {
        assertNoLiterals(skin, theme, NavigationRailTheme(width = 77.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(navigationRail = it)) { CamoNavigationRail(items, "A", {}) }
        }, measure = { it.size.width == px(77) })
    }

    @Test
    fun aDrawerHonorsItsWidth() = runComposeUiTest {
        assertNoLiterals(skin, theme, NavigationDrawerTheme(width = 301.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(navigationDrawer = it)) {
                CamoNavigationDrawer(listOf(CamoNavSection(items = items)), "A", {})
            }
        }, measure = { it.size.width == px(301) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
