package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.banner.BannerTheme
import com.srctool.camouflage.core.banner.CamoBanner
import com.srctool.camouflage.core.progress.CamoProgressBar
import com.srctool.camouflage.core.progress.ProgressTheme
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.snackbar.SnackbarSlots
import com.srctool.camouflage.core.snackbar.SnackbarState
import com.srctool.camouflage.core.snackbar.SnackbarTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import kotlin.math.roundToInt
import kotlin.test.Test

// The M7 feedback renderers against the contract: theme fields honored.
@OptIn(ExperimentalTestApi::class)
class FluentFeedbackRendererTest {
    private val skin = FluentSkin
    private val theme = fluentTheme()

    @Test
    fun aBarHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, ProgressTheme(barHeight = 9.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(progress = it)) {
                CamoProgressBar(Modifier.width(100.dp), progress = 0.5f)
            }
        }, measure = { it.size.height == px(9) })
    }

    @Test
    fun aBannerHonorsItsPadding() = runComposeUiTest {
        assertNoLiterals(skin, theme, BannerTheme(padding = PaddingValues(37.dp)), render = {
            ProvideComponentThemes(CamoComponentThemes(banner = it)) {
                CamoBanner("Message", Modifier.width(300.dp), icon = {})
            }
        }, measure = { it.size.height > px(74) })
    }

    @Test
    fun aSnackbarHonorsItsMinHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, SnackbarTheme(minHeight = 83.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(snackbar = it)) {
                val t = Camouflage.theme
                val style = Camouflage.skin.snackbar.defaults(t).merge(t.components.snackbar)
                Camouflage.skin.snackbar.Render(
                    SnackbarState("Saved", null, Tone.Default, false, false, style.position, InteractionState.None),
                    SnackbarSlots(null, null, null),
                    t,
                    style,
                    Modifier.width(300.dp),
                )
            }
        }, measure = { it.size.height == px(83) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
