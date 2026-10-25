package com.srctool.camouflage.skin.fluent

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FluentThemeTest {
    @Test
    fun lightAndDarkValidate() {
        listOf(fluentTheme(), fluentTheme(dark = true)).forEach { assertEquals(emptyList(), it.validate()) }
    }

    // Pinned to @fluentui/tokens 1.0.0-alpha.24 (alias/lightColor, alias/darkColor).
    @Test
    fun theDefaultBrandMatchesFluentsPublishedTokens() {
        val light = fluentTheme()
        val dark = fluentTheme(dark = true)
        assertEquals(Color(0xFF0F6CBD), light.colors.primary)
        assertEquals(Color(0xFF242424), light.colors.onSurface)
        assertEquals(Color(0xFFD1D1D1), light.colors.outlineVariant)
        assertEquals(Color(0xFFC50F1F), light.colors.error)
        assertEquals(Color(0xFF292929), dark.colors.surface)
        val tokens = assertNotNull(light.tokens<FluentTokens>())
        assertEquals(Color(0xFF115EA3), tokens.brandBackgroundHover)
        assertEquals(Color(0xFF0C3B5E), tokens.brandBackgroundPressed)
        assertEquals(Color(0xFFF5F5F5), tokens.neutralBackground1Hover)
        assertEquals(Color(0xFF115EA3), assertNotNull(dark.tokens<FluentTokens>()).brandBackground)
    }

    @Test
    fun aCustomBrandIsStep80() {
        val brand = Color(0xFF8A3FFC)
        assertEquals(brand, fluentTheme(brand = brand).colors.primary)
    }

    @Test
    fun densitySetsControlHeights() {
        assertEquals(24.dp, fluentTheme(density = FluentDensity.Small).components.button?.height)
        assertEquals(32.dp, fluentTheme().components.button?.height)
        assertEquals(40.dp, fluentTheme(density = FluentDensity.Touch).components.iconButton?.containerSize)
    }
}
