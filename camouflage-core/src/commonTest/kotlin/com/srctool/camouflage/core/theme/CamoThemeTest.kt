package com.srctool.camouflage.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.materialkolor.palettes.TonalPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CamoThemeTest {
    private val seed = Color(0xFF6750A4)
    private val light = CamoTheme(colors = CamoColors.fromSeed(primary = seed), typography = CamoTypography.system())
    private val dark = light.copy(colors = CamoColors.fromSeed(primary = seed, dark = true))

    @Test
    fun blackOnWhiteIs21() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.0001)
    }

    @Test
    fun fromSeedPrimaryIsTone40OfTheSeedPalette() {
        val palette = TonalPalette.fromInt(seed.toArgb())
        assertEquals(Color(palette.tone(40)), light.colors.primary)
        assertEquals(Color(palette.tone(80)), dark.colors.primary)
    }

    @Test
    fun defaultNeutralsAreTrueGrays() {
        val c = light.colors
        listOf(c.surface, c.surfaceContainer, c.onSurface, c.outline).forEach {
            assertEquals(it.red, it.green, 0.002f)
            assertEquals(it.green, it.blue, 0.002f)
        }
        assertEquals(Color.White, c.surface)
    }

    @Test
    fun seedThemesValidateInLightAndDark() {
        assertEquals(emptyList(), light.validate())
        assertEquals(emptyList(), dark.validate())
    }

    @Test
    fun tintedNeutralSeedStillValidates() {
        val tinted = light.copy(colors = CamoColors.fromSeed(primary = seed, neutral = seed))
        assertEquals(emptyList(), tinted.validate())
    }

    @Test
    fun plantedLowContrastPairIsReportedWithItsRatio() {
        val bad = light.copy(colors = light.colors.copy(onWarning = light.colors.warning))
        val issue = bad.validate().single() as ThemeIssue.LowContrast
        assertEquals("onWarning/warning", issue.pair)
        assertEquals(1.0, issue.ratio, 0.0001)
        assertEquals(4.5, issue.required)
    }

    @Test
    fun negativeValuesAreReported() {
        val bad = light.copy(
            spacing = CamoSpacing(m = (-4).dp),
            motion = CamoMotion(durationLong = -1),
            shapes = CamoShapes(small = RoundedCornerShape((-2).dp)),
        )
        val tokens = bad.validate().filterIsInstance<ThemeIssue.InvalidValue>().map { it.token }
        assertEquals(listOf("spacing.m", "motion.durationLong", "shapes.small"), tokens)
    }

    @Test
    fun duplicateTokenSetsAreReported() {
        val bad = light.copy(tokenSets = listOf(Brand(Color.Red), Brand(Color.Blue)))
        assertEquals(listOf<ThemeIssue>(ThemeIssue.ShadowedRole("tokenSets.Brand")), bad.validate())
    }

    @Test
    fun copyKeepsUntouchedGroups() {
        val changed = light.copy(colors = dark.colors)
        assertSame(light.typography, changed.typography)
        assertEquals(light.shapes, changed.shapes)
    }

    @Test
    fun unknownTokenSetIsNull() {
        assertNull(light.tokens<Brand>())
        assertEquals(Brand(Color.Red), light.copy(tokenSets = listOf(Brand(Color.Red))).tokens<Brand>())
    }

    @Test
    fun toneFamilies() {
        val c = light.colors
        val expected = ToneColors(c.primary, c.onPrimary, c.secondaryContainer, c.onSecondaryContainer)
        assertEquals(expected, c.family(Tone.Default))
        assertEquals(light.colors.warningContainer, light.colors.family(Tone.Warning).container)
    }

    @Test
    fun lerpEndpointsAndMidpoint() {
        assertSame(light, lerp(light, dark, 0f))
        assertSame(dark, lerp(light, dark, 1f))
        val mid = lerp(light, dark, 0.5f)
        assertTrue(mid.colors.surface != light.colors.surface && mid.colors.surface != dark.colors.surface)
    }

    @Test
    fun lerpBlendsTokenSetsOfTheSameType() {
        val a = light.copy(tokenSets = listOf(Brand(Color.Black)))
        val b = light.copy(tokenSets = listOf(Brand(Color.White)))
        val mid = lerp(a, b, 0.5f).tokens<Brand>()!!
        assertTrue(mid.accent.red > 0f && mid.accent.red < 1f)
    }

    private data class Brand(val accent: Color) : CamoTokenSet<Brand> {
        override fun lerp(to: Brand, fraction: Float): Brand = Brand(lerp(accent, to.accent, fraction))
    }
}
