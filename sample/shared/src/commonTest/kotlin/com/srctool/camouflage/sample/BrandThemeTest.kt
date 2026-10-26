package com.srctool.camouflage.sample

import com.srctool.camouflage.skin.fluent.FluentTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BrandThemeTest {
    // Issue acceptance: every brand passes the contrast check, on every skin, light and dark.
    @Test
    fun everyBrandOnEverySkinValidates() {
        for (skin in SampleSkin.entries) for (brand in Brand.entries) for (dark in listOf(false, true)) {
            assertEquals(emptyList(), sampleTheme(skin, brand, dark).validate(), "$brand on $skin, dark = $dark")
        }
    }

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
