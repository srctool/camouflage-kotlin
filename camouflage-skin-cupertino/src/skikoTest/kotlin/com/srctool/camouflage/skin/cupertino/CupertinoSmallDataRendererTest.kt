package com.srctool.camouflage.skin.cupertino

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.pagination.CamoPagination
import com.srctool.camouflage.core.pagination.PaginationTheme
import com.srctool.camouflage.core.rating.CamoRating
import com.srctool.camouflage.core.rating.RatingTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import kotlin.math.roundToInt
import kotlin.test.Test

// The M10 rating and pagination renderers against the contract: theme fields honored.
@OptIn(ExperimentalTestApi::class)
class CupertinoSmallDataRendererTest {
    private val skin = CupertinoSkin
    private val theme = cupertinoLight

    @Test
    fun aRatingHonorsItsIconSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, RatingTheme(mediumSize = 33.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(rating = it)) { CamoRating(3f, "Quality") }
        }, measure = { it.size.height == px(33) })
    }

    @Test
    fun paginationHonorsItsButtonSize() = runComposeUiTest {
        assertNoLiterals(skin, theme, PaginationTheme(buttonSize = 61.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(pagination = it)) { CamoPagination(5, 1, {}) }
        }, measure = { it.size.height == px(61) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
