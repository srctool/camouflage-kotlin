package com.srctool.camouflage.sample

import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ThemeIssue
import com.srctool.camouflage.skin.fluent.FluentTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BrandThemeTest {
    // Issue acceptance: every brand passes the contrast check, on every skin, light and dark. Cupertino keeps Apple's
    // published system colors, a few of which are below AA by themselves (its own theme test lists them): a brand
    // there may not add a failing pair beyond those of the plain Cupertino theme.
    @Test
    fun everyBrandOnEverySkinValidates() {
        for (skin in SampleSkin.entries) for (brand in Brand.entries) for (dark in listOf(false, true)) {
            val issues = failingPairs(sampleTheme(skin, brand, dark))
            val allowed = if (skin.name.startsWith("Cupertino")) {
                failingPairs(sampleTheme(skin, Brand.Default, dark))
            } else {
                emptySet()
            }
            assertEquals(emptySet(), issues - allowed, "$brand on $skin, dark = $dark")
        }
    }

    private fun failingPairs(theme: CamoTheme): Set<String> =
        theme.validate().map { if (it is ThemeIssue.LowContrast) it.pair else it.toString() }.toSet()

    @Test
    fun aBrandCarriesItsTokensAndTheDefaultNone() {
        assertNotNull(sampleTheme(SampleSkin.Minimal, Brand.Ocean, dark = false).tokens<BrandTokens>())
        assertNull(sampleTheme(SampleSkin.Minimal, Brand.Default, dark = false).tokens<BrandTokens>())
    }

    // Fluent fills primary buttons from its own tokens: a brand must reach them, not only the roles.
    @Test
    fun aBrandReachesFluentsOwnTokens() {
        val ember = Brand.Ember.tokens!!.brand[600]
        val fluent = sampleTheme(SampleSkin.Fluent, Brand.Ember, dark = false)
        assertEquals(ember, assertNotNull(fluent.tokens<FluentTokens>()).brandBackground)
    }
}
