package com.srctool.camouflage.skin.minimal

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class MinimalThemeTest {
    @Test
    fun lightDarkAndAccentThemesValidate() {
        val accent = Color(0xFF0B57D0)
        listOf(minimalLight, minimalDark, minimalTheme(accent), minimalTheme(accent, dark = true))
            .forEach { assertEquals(emptyList(), it.validate()) }
    }
}
