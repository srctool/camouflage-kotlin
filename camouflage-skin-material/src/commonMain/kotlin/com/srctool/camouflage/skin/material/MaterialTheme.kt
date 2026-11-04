@file:Suppress("MagicNumber") // Material 3's token tables: tones, radii, durations and the type scale.

package com.srctool.camouflage.skin.material

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoMotion
import com.srctool.camouflage.core.theme.CamoShapes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography

// M3 shape scale: extra small 4, small 8, medium 12, large 16, extra large 28, full.
private val MaterialShapes = CamoShapes(
    none = RoundedCornerShape(0.dp),
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
    full = RoundedCornerShape(percent = 50),
)

// M3 durations (short 150 / medium 300 / long 500) and easings (standard, emphasized decelerate).
private val MaterialMotion = CamoMotion(
    durationShort = 150,
    durationMedium = 300,
    durationLong = 500,
    easingStandard = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f),
    easingEmphasized = CubicBezierEasing(a = 0.05f, b = 0.7f, c = 0.1f, d = 1f),
)

/** The seed of Material 3's baseline scheme (`#6750A4`). */
public val MaterialBaselineSeed: Color = Color(0xFF6750A4)

/**
 * Material 3's starter theme: the baseline scheme when [seed] is `null`, else M3's tonal-spot colors from
 * [seed]; M3's shapes (4, 8, 12, 16, 28), motion (150, 300, 500 ms) and type scale. [fontFamily] `null` is the
 * platform font: skins never bundle fonts, so pass Roboto for the authentic look.
 */
public fun materialTheme(
    seed: Color? = null,
    dark: Boolean = false,
    fontFamily: FontFamily? = null,
): CamoTheme = CamoTheme(
    colors = if (seed == null) baselineColors(dark) else materialColors(seed, dark),
    typography = materialTypography(fontFamily ?: FontFamily.Default),
    shapes = MaterialShapes,
    motion = MaterialMotion,
)

/**
 * M3 "tonal spot" colors from [seed]: `fromSeed(primary = seed, neutral = seed)`, so the surfaces carry the
 * seed's hue (surface 98, containers 100 to 90 in light). Status families are Camouflage's.
 */
public fun materialColors(seed: Color, dark: Boolean = false): CamoColors =
    CamoColors.fromSeed(primary = seed, neutral = seed, dark = dark)

/** [materialTheme] with the M3 baseline scheme, light. */
public val materialBaselineLight: CamoTheme = materialTheme()

/** [materialTheme] with the M3 baseline scheme, dark. */
public val materialBaselineDark: CamoTheme = materialTheme(dark = true)

// The M3 baseline roles (m3.material.io, seed #6750A4). The status families aren't M3's: they come from the
// default status seeds, written out here once; MaterialBaselineTest fails if a color-library update would change them.
private fun baselineColors(dark: Boolean): CamoColors {
    val seeded = materialColors(MaterialBaselineSeed, dark)
    return if (dark) {
        seeded.copy(
            primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8), onTertiary = Color(0xFF492532),
            tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
            surface = Color(0xFF141218),
            surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1B20),
            surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B),
            onSurface = Color(0xFFE6E0E9), onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
            error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
            inverseSurface = Color(0xFFE6E0E9), inverseOnSurface = Color(0xFF322F35),
            inversePrimary = Color(0xFF6750A4), scrim = Color.Black,
            success = Color(0xFF88D982), onSuccess = Color(0xFF003909),
            successContainer = Color(0xFF005312), onSuccessContainer = Color(0xFFA3F69C),
            warning = Color(0xFFFFB957), onWarning = Color(0xFF462B00),
            warningContainer = Color(0xFF643F00), onWarningContainer = Color(0xFFFFDDB5),
            info = Color(0xFF96CCFF), onInfo = Color(0xFF003353),
            infoContainer = Color(0xFF004A75), onInfoContainer = Color(0xFFCEE5FF),
        )
    } else {
        seeded.copy(
            primary = Color(0xFF6750A4), onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260), onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF31111D),
            surface = Color(0xFFFEF7FF),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
            onSurface = Color(0xFF1D1B20), onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
            error = Color(0xFFB3261E), onError = Color.White,
            errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
            inverseSurface = Color(0xFF322F35), inverseOnSurface = Color(0xFFF5EFF7),
            inversePrimary = Color(0xFFD0BCFF), scrim = Color.Black,
            success = Color(0xFF1B6D24), onSuccess = Color.White,
            successContainer = Color(0xFFA3F69C), onSuccessContainer = Color(0xFF002204),
            warning = Color(0xFF835400), onWarning = Color.White,
            warningContainer = Color(0xFFFFDDB5), onWarningContainer = Color(0xFF2A1800),
            info = Color(0xFF00639A), onInfo = Color.White,
            infoContainer = Color(0xFFCEE5FF), onInfoContainer = Color(0xFF001D32),
        )
    }
}

// The M3 baseline type scale (size / line height, weight, tracking).
private fun materialTypography(family: FontFamily): CamoTypography {
    fun style(size: Int, line: Int, weight: Int, tracking: Float) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = FontWeight(weight),
        letterSpacing = tracking.sp,
    )
    return CamoTypography(
        displayLarge = style(57, 64, 400, -0.25f),
        displayMedium = style(45, 52, 400, 0f),
        displaySmall = style(36, 44, 400, 0f),
        headlineLarge = style(32, 40, 400, 0f),
        headlineMedium = style(28, 36, 400, 0f),
        headlineSmall = style(24, 32, 400, 0f),
        titleLarge = style(22, 28, 400, 0f),
        titleMedium = style(16, 24, 500, 0.15f),
        titleSmall = style(14, 20, 500, 0.1f),
        bodyLarge = style(16, 24, 400, 0.5f),
        bodyMedium = style(14, 20, 400, 0.25f),
        bodySmall = style(12, 16, 400, 0.4f),
        labelLarge = style(14, 20, 500, 0.1f),
        labelMedium = style(12, 16, 500, 0.5f),
        labelSmall = style(11, 16, 500, 0.5f),
    )
}
