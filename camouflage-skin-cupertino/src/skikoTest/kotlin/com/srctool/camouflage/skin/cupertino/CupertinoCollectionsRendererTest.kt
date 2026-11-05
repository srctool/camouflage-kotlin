package com.srctool.camouflage.skin.cupertino

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.fileuploader.CamoFileUploader
import com.srctool.camouflage.core.fileuploader.UploaderTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import kotlin.math.roundToInt
import kotlin.test.Test

// The M10 file uploader renderer against the contract: theme fields honored.
@OptIn(ExperimentalTestApi::class)
class CupertinoCollectionsRendererTest {
    private val skin = CupertinoSkin
    private val theme = cupertinoLight

    @Test
    fun theZoneHonorsItsHeight() = runComposeUiTest {
        assertNoLiterals(skin, theme, UploaderTheme(zoneHeight = 147.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(fileUploader = it)) {
                CamoFileUploader(emptyList(), {}, {}, "Upload files")
            }
        }, measure = { it.size.height == px(147) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
