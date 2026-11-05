package com.srctool.camouflage.skin.material

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialThemeTest {
    @Test
    fun baselineAndSeededThemesValidate() {
        val seed = Color(0xFF0B57D0)
        listOf(
            materialBaselineLight,
            materialBaselineDark,
            materialTheme(seed),
            materialTheme(seed, dark = true),
        ).forEach { assertEquals(emptyList(), it.validate()) }
    }

    @Test
    fun baselineHasTheM3Roles() {
        val light = materialBaselineLight.colors
        assertEquals(Color(0xFF6750A4), light.primary)
        assertEquals(Color(0xFFFEF7FF), light.surface)
        assertEquals(Color(0xFFF3EDF7), light.surfaceContainer)
        assertEquals(Color(0xFFB3261E), light.error)
        val dark = materialBaselineDark.colors
        assertEquals(Color(0xFFD0BCFF), dark.primary)
        assertEquals(Color(0xFF141218), dark.surface)
        assertEquals(Color(0xFF6750A4), dark.inversePrimary)
    }

    @Test
    fun themeCarriesM3ShapesMotionAndType() {
        val theme = materialTheme()
        assertEquals(28f, (theme.shapes.extraLarge.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f),
            androidx.compose.ui.unit.Density(1f))))
        assertEquals(150, theme.motion.durationShort)
        assertEquals(57f, theme.typography.displayLarge.fontSize.value)
    }
}
