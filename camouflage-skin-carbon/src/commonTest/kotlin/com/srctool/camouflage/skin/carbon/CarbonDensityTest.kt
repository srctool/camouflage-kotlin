package com.srctool.camouflage.skin.carbon

import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.environment.forEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// Carbon's density (base Carbon Skin §3.5): one theme-level value that every control's size follows.
class CarbonDensityTest {
    private val caps = CamoCapabilities(
        backdropBlur = false,
        runtimeShaders = false,
        windowMaterial = false,
        reduceTransparency = false,
    )
    private val desktop = CamoEnvironment(WindowSizeClass.Expanded, InputMode.Pointer, CamoPlatform.Windows, caps)
    private val phone = CamoEnvironment(WindowSizeClass.Compact, InputMode.Touch, CamoPlatform.Android, caps)

    private fun heights(density: CarbonDensity) = carbonTheme(density = density).let { theme ->
        listOf(
            CarbonSkin.button.defaults(theme).height,
            CarbonSkin.iconButton.defaults(theme).containerSize,
            CarbonSkin.textField.defaults(theme).height,
            CarbonSkin.searchField.defaults(theme).height,
            CarbonSkin.stepper.defaults(theme).height,
            CarbonSkin.menu.defaults(theme).itemHeight,
            CarbonSkin.listItem.defaults(theme).minHeightOneLine,
            CarbonSkin.tree.defaults(theme).rowHeight,
            CarbonSkin.dataTable.defaults(theme).rowHeight,
            CarbonSkin.tabs.defaults(theme).height,
            CarbonSkin.toolbar.defaults(theme).actionSize,
        )
    }

    @Test
    fun everyControlFollowsTheDensity() {
        for (density in CarbonDensity.entries) {
            assertEquals(List(11) { density.size }, heights(density), "$density")
        }
        assertEquals(listOf(24.dp, 32.dp, 40.dp, 48.dp), CarbonDensity.entries.map { it.size })
    }

    @Test
    fun theDefaultFollowsTheInput() {
        val theme = carbonTheme()
        assertEquals(40.dp, CarbonSkin.button.defaults(theme.forEnvironment(desktop)).height)
        assertEquals(48.dp, CarbonSkin.button.defaults(theme.forEnvironment(phone)).height)
        assertEquals(40.dp, CarbonSkin.dataTable.defaults(theme.forEnvironment(desktop)).rowHeight)
    }

    @Test
    fun anExplicitDensityWinsOverTheInput() {
        val theme = carbonTheme(density = CarbonDensity.Sm)
        assertEquals(32.dp, CarbonSkin.button.defaults(theme.forEnvironment(phone)).height)
        assertEquals(32.dp, CarbonSkin.button.defaults(theme.forEnvironment(desktop)).height)
    }

    @Test
    fun theSmallestSizeDrawsTwelvePointText() {
        val xs = carbonTheme(density = CarbonDensity.Xs)
        assertEquals(12f, CarbonSkin.button.defaults(xs).textStyle.fontSize.value)
        assertNotNull(carbonLight.tokens<CarbonDensityTokens>())
        assertEquals(14f, CarbonSkin.button.defaults(carbonTheme(density = CarbonDensity.Md)).textStyle.fontSize.value)
    }

    @Test
    fun aThemeWithoutTheTokensKeepsTheLargeSize() {
        // A theme built by hand (or copied without the token set) draws today's 48.
        val bare = carbonTheme(density = CarbonDensity.Sm).copy(tokenSets = emptyList())
        assertEquals(48.dp, CarbonSkin.button.defaults(bare).height)
    }
}
