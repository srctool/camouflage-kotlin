package com.srctool.camouflage.skin.carbon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.popover.PopoverPlacement
import com.srctool.camouflage.core.popover.PopoverSlots
import com.srctool.camouflage.core.popover.PopoverState
import com.srctool.camouflage.core.popover.PopoverTheme
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.sidesheet.CamoSideSheet
import com.srctool.camouflage.core.sidesheet.SideSheetTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import kotlin.math.roundToInt
import kotlin.test.Test

// The M9 popover and side sheet renderers against the contract: theme fields honored.
@OptIn(ExperimentalTestApi::class)
class CarbonPanelsRendererTest {
    private val skin = CarbonSkin
    private val theme = carbonLight

    @Test
    fun aPopoverHonorsItsMaxWidth() = runComposeUiTest {
        assertNoLiterals(skin, theme, PopoverTheme(maxWidth = 211.dp), render = { odd ->
            ProvideComponentThemes(CamoComponentThemes(popover = odd)) {
                val t = Camouflage.theme
                val style = skin.popover.defaults(t).merge(t.components.popover)
                skin.popover.Render(
                    PopoverState(PopoverPlacement.Bottom, 0.dp),
                    PopoverSlots("Title", { Box(Modifier.width(600.dp)) { BasicText("wide") } }, null),
                    t,
                    style,
                    Modifier,
                )
            }
        }, measure = { it.size.width == px(211) })
    }

    @Test
    fun aSideSheetHonorsItsWidth() = runComposeUiTest {
        assertNoLiterals(skin, theme, SideSheetTheme(width = 277.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(sideSheet = it)) {
                CamoSideSheet(title = "Details") { BasicText("x") }
            }
        }, measure = { it.size.width == px(277) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
