package com.srctool.camouflage.skin.carbon

import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.theme.ThemeIssue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CarbonThemeTest {
    // Carbon's own tokens pass AA in the White, G10 and G100 families; G90's layer-03 (gray 60) is the one place
    // Carbon's text tokens fall short (text on gray 60 is 4.0), so that family may fail only on the two highest
    // container roles.
    private val g90Known = setOf(
        "onSurface/surfaceContainerHigh", "onSurfaceVariant/surfaceContainerHigh",
        "onSurface/surfaceContainerHighest", "onSurfaceVariant/surfaceContainerHighest",
    )

    @Test
    fun whiteG10AndG100Validate() {
        listOf(CarbonThemeFamily.White, CarbonThemeFamily.G10, CarbonThemeFamily.G100)
            .forEach { assertEquals(emptyList(), carbonTheme(it).validate(), it.name) }
        assertEquals(emptyList(), carbonLight.validate())
        assertEquals(emptyList(), carbonDark.validate())
    }

    @Test
    fun g90FailsOnlyOnItsHighestLayers() {
        val failing = carbonTheme(CarbonThemeFamily.G90).validate()
            .filterIsInstance<ThemeIssue.LowContrast>().map { it.pair }
        assertEquals(emptyList(), failing.filterNot { it in g90Known })
    }

    // Carbon's published tokens, pinned: a change here is a decision, not a side effect.
    @Test
    fun tokensArePinned() {
        val white = carbonLight.colors
        assertEquals(Color(0xFF0F62FE), white.primary) // blue 60
        assertEquals(Color(0xFF161616), white.onSurface) // text-primary
        assertEquals(Color.White, white.surface) // background
        assertEquals(Color(0xFFF4F4F4), white.surfaceContainerLow) // layer-01
        assertEquals(Color.White, white.surfaceContainer) // layer-02
        assertEquals(Color(0xFF8D8D8D), white.outline) // border-strong
        assertEquals(Color(0xFFC6C6C6), white.outlineVariant) // border-subtle
        val g100 = carbonDark.colors
        assertEquals(Color(0xFF161616), g100.surface)
        assertEquals(Color(0xFF262626), g100.surfaceContainerLow)
        assertEquals(Color(0xFF393939), g100.surfaceContainer)
        assertEquals(Color(0xFF525252), g100.surfaceContainerHigh)
        assertEquals(Color(0xFF4589FF), g100.primary)
    }

    @Test
    fun layersStepOneGrayPerNestingLevelInTheDarkFamilies() {
        listOf(CarbonThemeFamily.G90, CarbonThemeFamily.G100).forEach {
            val c = carbonTheme(it).colors
            val gray = { color: Color -> color.red }
            assertTrue(gray(c.surface) < gray(c.surfaceContainerLow))
            assertTrue(gray(c.surfaceContainerLow) < gray(c.surfaceContainer))
            assertTrue(gray(c.surfaceContainer) < gray(c.surfaceContainerHigh))
        }
    }

    @Test
    fun everyShapeIsSquareExceptFull() {
        val shapes = carbonLight.shapes
        listOf(shapes.none, shapes.extraSmall, shapes.small, shapes.medium, shapes.large, shapes.extraLarge)
            .forEach { assertEquals(androidx.compose.foundation.shape.RoundedCornerShape(0.dp), it) }
    }
}
