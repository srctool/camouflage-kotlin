@file:Suppress("MagicNumber") // IBM Carbon's published tokens: colors, type sets and motion.

package com.srctool.camouflage.skin.carbon

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

// Square everything; `full` is for toggles and tags, the only rounded elements.
private val CarbonShapes = CamoShapes(
    none = RoundedCornerShape(0.dp),
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
    full = RoundedCornerShape(percent = 50),
)

// Carbon's productive motion: fast-02 110 ms, moderate-01 150 ms, moderate-02 240 ms; the standard productive
// curve, and the expressive one for entrances.
private val CarbonMotion = CamoMotion(
    durationShort = 110,
    durationMedium = 150,
    durationLong = 240,
    easingStandard = CubicBezierEasing(a = 0.2f, b = 0f, c = 0.38f, d = 0.9f),
    easingEmphasized = CubicBezierEasing(a = 0.4f, b = 0.14f, c = 0.3f, d = 1f),
)

// Declared before the themes below: top-level values initialize in order.
private val Blue60 = Color(0xFF0F62FE)
private val Blue50 = Color(0xFF4589FF)

/** Carbon's gray 100: the UI shell header in every family. */
internal val Gray100: Color = Color(0xFF161616)

/** Carbon's gray 10 text on the dark header. */
internal val Gray10: Color = Color(0xFFF4F4F4)

/**
 * The Carbon starter theme for a [family] (base Carbon Skin §3): `background` is `surface`, `layer-01` to
 * `layer-03` the containers, `text-primary` and `text-secondary` `onSurface` and `onSurfaceVariant`,
 * `border-strong` and `border-subtle` `outline` and `outlineVariant`, `interactive` the `primary` (blue 60,
 * blue 50 in the dark families, or the brand [accent]), and the support colors the status families. Square
 * shapes, productive motion and the IBM Plex type set in [fontFamily] (`null`: the platform font; skins never
 * bundle fonts, ADR-33).
 */
public fun carbonTheme(
    family: CarbonThemeFamily = CarbonThemeFamily.White,
    accent: Color? = null,
    fontFamily: FontFamily? = null,
): CamoTheme = CamoTheme(
    colors = when (family) {
        CarbonThemeFamily.White -> whiteColors()
        CarbonThemeFamily.G10 -> g10Colors()
        CarbonThemeFamily.G90 -> g90Colors()
        CarbonThemeFamily.G100 -> g100Colors()
    }.withAccent(accent, dark = family == CarbonThemeFamily.G90 || family == CarbonThemeFamily.G100),
    typography = carbonTypography(fontFamily ?: FontFamily.Default),
    shapes = CarbonShapes,
    motion = CarbonMotion,
)

/** [carbonTheme] in the White family. */
public val carbonLight: CamoTheme = carbonTheme()

/** [carbonTheme] in the G100 family. */
public val carbonDark: CamoTheme = carbonTheme(CarbonThemeFamily.G100)

// A brand accent takes only the `primary` family; every other token stays Carbon's.
private fun CamoColors.withAccent(accent: Color?, dark: Boolean): CamoColors {
    if (accent == null) return this
    val seeded = CamoColors.fromSeed(primary = accent, dark = dark)
    return copy(
        primary = seeded.primary, onPrimary = seeded.onPrimary,
        primaryContainer = seeded.primaryContainer, onPrimaryContainer = seeded.onPrimaryContainer,
        inversePrimary = seeded.inversePrimary,
    )
}

// Status colors shared by the light families (support-error red 60, success green 60, info blue 70).
private fun lightColors(base: CamoColors, background: Color, l1: Color, l2: Color, l3: Color, subtle: Color) =
    base.copy(
        primary = Blue60, onPrimary = Color.White,
        primaryContainer = Color(0xFFD0E2FF), onPrimaryContainer = Color(0xFF001D6C),
        secondary = Color(0xFF393939), onSecondary = Color.White, // the secondary button
        secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Gray100,
        tertiary = Color(0xFF8A3FFC), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF6F2FF), onTertiaryContainer = Color(0xFF491D8B),
        surface = background,
        surfaceContainerLowest = background,
        surfaceContainerLow = l1, surfaceContainer = l2, surfaceContainerHigh = l3,
        surfaceContainerHighest = Color(0xFFE0E0E0),
        onSurface = Gray100, onSurfaceVariant = Color(0xFF525252),
        outline = Color(0xFF8D8D8D), outlineVariant = subtle,
        error = Color(0xFFDA1E28), onError = Color.White,
        errorContainer = Color(0xFFFFF1F1), onErrorContainer = Color(0xFFA2191F),
        success = Color(0xFF198038), onSuccess = Color.White,
        successContainer = Color(0xFFDEFBE6), onSuccessContainer = Color(0xFF0E6027),
        warning = Color(0xFFF1C21B), onWarning = Gray100,
        warningContainer = Color(0xFFFCF4D6), onWarningContainer = Color(0xFF5E4800),
        info = Color(0xFF0043CE), onInfo = Color.White,
        infoContainer = Color(0xFFEDF5FF), onInfoContainer = Color(0xFF002D9C),
        inverseSurface = Color(0xFF393939), inverseOnSurface = Gray10,
        inversePrimary = Blue50, scrim = Gray100,
    )

private fun whiteColors(): CamoColors = lightColors(
    CamoColors.fromSeed(primary = Blue60, dark = false),
    background = Color.White, l1 = Gray10, l2 = Color.White, l3 = Gray10, subtle = Color(0xFFC6C6C6),
)

private fun g10Colors(): CamoColors = lightColors(
    CamoColors.fromSeed(primary = Blue60, dark = false),
    background = Gray10, l1 = Color.White, l2 = Gray10, l3 = Color.White, subtle = Color(0xFFE0E0E0),
)

private fun darkColors(
    background: Color,
    l1: Color,
    l2: Color,
    l3: Color,
    subtle: Color,
    highest: Color,
) = CamoColors.fromSeed(primary = Blue50, dark = true).copy(
    primary = Blue50, onPrimary = Gray100,
    primaryContainer = Color(0xFF001D6C), onPrimaryContainer = Color(0xFFA6C8FF),
    secondary = Color(0xFF6F6F6F), onSecondary = Color.White,
    secondaryContainer = Color(0xFF393939), onSecondaryContainer = Gray10,
    tertiary = Color(0xFFA56EFF), onTertiary = Gray100,
    tertiaryContainer = Color(0xFF31135E), onTertiaryContainer = Color(0xFFD4BBFF),
    surface = background,
    surfaceContainerLowest = background,
    surfaceContainerLow = l1, surfaceContainer = l2, surfaceContainerHigh = l3,
    surfaceContainerHighest = highest,
    onSurface = Gray10, onSurfaceVariant = Color(0xFFC6C6C6),
    outline = Color(0xFF8D8D8D), outlineVariant = subtle,
    error = Color(0xFFFA4D56), onError = Gray100,
    errorContainer = Color(0xFF520408), onErrorContainer = Color(0xFFFFB3B8),
    success = Color(0xFF42BE65), onSuccess = Gray100,
    successContainer = Color(0xFF022D0D), onSuccessContainer = Color(0xFFA7F0BA),
    warning = Color(0xFFF1C21B), onWarning = Gray100,
    warningContainer = Color(0xFF302400), onWarningContainer = Color(0xFFF1C21B),
    info = Color(0xFF78A9FF), onInfo = Gray100,
    infoContainer = Color(0xFF001141), onInfoContainer = Color(0xFFA6C8FF),
    inverseSurface = Gray10, inverseOnSurface = Gray100,
    inversePrimary = Blue60, scrim = Color.Black,
)

private fun g90Colors(): CamoColors = darkColors(
    background = Color(0xFF262626), l1 = Color(0xFF393939), l2 = Color(0xFF525252), l3 = Color(0xFF6F6F6F),
    subtle = Color(0xFF525252), highest = Color(0xFF6F6F6F),
)

private fun g100Colors(): CamoColors = darkColors(
    background = Gray100, l1 = Color(0xFF262626), l2 = Color(0xFF393939), l3 = Color(0xFF525252),
    subtle = Color(0xFF393939), highest = Color(0xFF525252),
)

// IBM Plex Sans in Carbon's type sets: size / line height, weight and the tracking at that size.
private fun carbonTypography(family: FontFamily): CamoTypography {
    fun style(size: Int, line: Int, weight: Int, tracking: Float = 0f) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = FontWeight(weight),
        letterSpacing = tracking.sp,
    )
    return CamoTypography(
        displayLarge = style(54, 60, 300), // heading-07
        displayMedium = style(42, 50, 300), // heading-06
        displaySmall = style(32, 40, 400), // heading-05
        headlineLarge = style(28, 36, 400), // heading-04
        headlineMedium = style(20, 28, 400), // heading-03
        headlineSmall = style(16, 24, 600), // heading-02
        titleLarge = style(20, 28, 400), // heading-03
        titleMedium = style(16, 22, 600, 0.16f), // heading-compact-02
        titleSmall = style(14, 18, 600, 0.16f), // heading-compact-01
        bodyLarge = style(16, 24, 400), // body-02
        bodyMedium = style(14, 18, 400, 0.16f), // body-compact-01
        bodySmall = style(12, 16, 400, 0.32f), // helper-text-01
        labelLarge = style(14, 18, 400, 0.16f), // the button text
        labelMedium = style(12, 16, 400, 0.32f), // label-01
        labelSmall = style(12, 16, 400, 0.32f),
    )
}
