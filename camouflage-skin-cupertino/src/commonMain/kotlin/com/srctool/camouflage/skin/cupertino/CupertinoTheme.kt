@file:Suppress("MagicNumber") // Apple's HIG tables: system colors, radii, durations and the text styles.

package com.srctool.camouflage.skin.cupertino

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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

// Continuous-corner radii of iOS controls and groups: 6, 8, 12 (grouped sections), 14 (alerts), 26 (sheets).
private val CupertinoShapes = CamoShapes(
    none = RoundedCornerShape(0.dp),
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(26.dp),
    full = RoundedCornerShape(percent = 50),
)

// iOS animations are springs of about 0.35 s; the durations are those, the easings the ease curves it settles on.
private val CupertinoMotion = CamoMotion(
    durationShort = 200,
    durationMedium = 350,
    durationLong = 500,
    easingStandard = CubicBezierEasing(a = 0.25f, b = 0.1f, c = 0.25f, d = 1f),
    easingEmphasized = CubicBezierEasing(a = 0.2f, b = 0.9f, c = 0.1f, d = 1f),
)

/** Apple's `systemBlue` (light): the default accent. The dark appearance's is `0xFF0A84FF`. */
public val SystemBlue: Color = Color(0xFF007AFF)

private val SystemBlueDark = Color(0xFF0A84FF)

/**
 * The Cupertino starter theme: Apple's system colors from the HIG tables, mapped to roles (base Cupertino Skin
 * §3): `primary` is the [accent] (systemBlue by default, and its dark variant in the dark appearance unless the
 * accent is custom), `error` systemRed, `success` systemGreen, `warning` systemOrange, `info` systemTeal;
 * `surface` is the grouped background, the containers step through the system backgrounds and fills. The
 * type scale is SF's text styles, set in [fontFamily] (`null`: the platform font; skins never bundle fonts,
 * ADR-33), the shapes iOS's radii, the motion its 0.35 s springs.
 */
public fun cupertinoTheme(
    accent: Color = SystemBlue,
    dark: Boolean = false,
    fontFamily: FontFamily? = null,
): CamoTheme = CamoTheme(
    colors = cupertinoColors(accent, dark),
    typography = cupertinoTypography(fontFamily ?: FontFamily.Default),
    shapes = CupertinoShapes,
    motion = CupertinoMotion,
)

/** [cupertinoTheme] with systemBlue, light. */
public val cupertinoLight: CamoTheme = cupertinoTheme()

/** [cupertinoTheme] with systemBlue, dark. */
public val cupertinoDark: CamoTheme = cupertinoTheme(dark = true)

// The system colors and the HIG backgrounds, written out (the tables at developer.apple.com/design/human-interface-
// guidelines/color). CupertinoThemeTest pins them.
private fun cupertinoColors(accent: Color, dark: Boolean): CamoColors {
    val blue = if (dark && accent == SystemBlue) SystemBlueDark else accent
    return if (dark) darkColors(blue) else lightColors(blue)
}

private fun lightColors(blue: Color): CamoColors {
    val surface = Color(0xFFF2F2F7) // systemGroupedBackground
    val tint = blue.copy(alpha = 0.15f).compositeOver(Color.White)
    return CamoColors.fromSeed(primary = blue, dark = false).copy(
        primary = blue, onPrimary = Color.White,
        primaryContainer = tint, onPrimaryContainer = blue,
        secondary = Color(0xFF8E8E93), onSecondary = Color.White, // systemGray
        secondaryContainer = Color(0xFFE5E5EA), onSecondaryContainer = Color.Black, // systemGray5
        tertiary = Color(0xFF5856D6), onTertiary = Color.White, // systemIndigo
        tertiaryContainer = Color(0xFFE0E0F8), onTertiaryContainer = Color(0xFF5856D6),
        surface = surface,
        surfaceContainerLowest = Color.White, // systemBackground
        surfaceContainerLow = Color.White, // secondarySystemGroupedBackground
        surfaceContainer = surface, // secondarySystemBackground
        surfaceContainerHigh = Color(0xFFE5E5EA), // systemGray5
        surfaceContainerHighest = Color(0xFFD1D1D6), // systemGray4
        onSurface = Color.Black, onSurfaceVariant = Color(0xFF8A8A8E), // label, secondaryLabel
        outline = Color(0xFFC7C7CC), outlineVariant = Color(0xFFC6C6C8), // systemGray3, opaqueSeparator
        error = Color(0xFFFF3B30), onError = Color.White,
        errorContainer = Color(0xFFFFE1DF), onErrorContainer = Color(0xFFFF3B30),
        success = Color(0xFF34C759), onSuccess = Color.White,
        successContainer = Color(0xFFDDF6E3), onSuccessContainer = Color(0xFF248A3D),
        warning = Color(0xFFFF9500), onWarning = Color.White,
        warningContainer = Color(0xFFFFEACC), onWarningContainer = Color(0xFFC93400),
        info = Color(0xFF30B0C7), onInfo = Color.White,
        infoContainer = Color(0xFFDAF1F5), onInfoContainer = Color(0xFF008299),
        inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color(0xFFF2F2F7),
        inversePrimary = SystemBlueDark, scrim = Color.Black,
    )
}

private fun darkColors(blue: Color): CamoColors {
    val tint = blue.copy(alpha = 0.22f).compositeOver(Color.Black)
    return CamoColors.fromSeed(primary = blue, dark = true).copy(
        primary = blue, onPrimary = Color.White,
        primaryContainer = tint, onPrimaryContainer = blue,
        secondary = Color(0xFF8E8E93), onSecondary = Color.Black,
        secondaryContainer = Color(0xFF2C2C2E), onSecondaryContainer = Color.White,
        tertiary = Color(0xFF5E5CE6), onTertiary = Color.White,
        tertiaryContainer = Color(0xFF26264F), onTertiaryContainer = Color(0xFF7D7AFF),
        surface = Color.Black, // systemGroupedBackground
        surfaceContainerLowest = Color.Black, // systemBackground
        surfaceContainerLow = Color(0xFF1C1C1E), // secondarySystemGroupedBackground
        surfaceContainer = Color(0xFF1C1C1E), // secondarySystemBackground
        surfaceContainerHigh = Color(0xFF2C2C2E), // tertiarySystemBackground
        surfaceContainerHighest = Color(0xFF3A3A3C), // systemGray4
        onSurface = Color.White, onSurfaceVariant = Color(0xFF8E8E93),
        outline = Color(0xFF48484A), outlineVariant = Color(0xFF38383A),
        error = Color(0xFFFF453A), onError = Color.White,
        errorContainer = Color(0xFF4A1A17), onErrorContainer = Color(0xFFFF453A),
        success = Color(0xFF30D158), onSuccess = Color.Black,
        successContainer = Color(0xFF123F1D), onSuccessContainer = Color(0xFF30D158),
        warning = Color(0xFFFF9F0A), onWarning = Color.Black,
        warningContainer = Color(0xFF4A2F05), onWarningContainer = Color(0xFFFF9F0A),
        info = Color(0xFF40CBE0), onInfo = Color.Black,
        infoContainer = Color(0xFF123C44), onInfoContainer = Color(0xFF40CBE0),
        inverseSurface = Color(0xFFF2F2F7), inverseOnSurface = Color(0xFF1C1C1E),
        inversePrimary = SystemBlue, scrim = Color.Black,
    )
}

// SF's text styles (largeTitle 34, title1 28, title2 22, title3 20, headline 17, body 17, callout 16, subheadline
// 15, footnote 13, caption1 12, caption2 11), size / line height, weight and the SF tracking at that size.
private fun cupertinoTypography(family: FontFamily): CamoTypography {
    fun style(size: Int, line: Int, weight: Int, tracking: Float) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = FontWeight(weight),
        letterSpacing = tracking.sp,
    )
    return CamoTypography(
        displayLarge = style(48, 56, 700, 0.37f),
        displayMedium = style(40, 48, 700, 0.37f),
        displaySmall = style(34, 41, 700, 0.37f), // largeTitle
        headlineLarge = style(28, 34, 700, 0.36f), // title1
        headlineMedium = style(22, 28, 700, 0.35f), // title2
        headlineSmall = style(20, 25, 600, 0.38f), // title3
        titleLarge = style(20, 25, 600, 0.38f),
        titleMedium = style(17, 22, 600, -0.41f), // headline
        titleSmall = style(15, 20, 600, -0.24f),
        bodyLarge = style(17, 22, 400, -0.41f), // body
        bodyMedium = style(15, 20, 400, -0.24f), // subheadline
        bodySmall = style(13, 18, 400, -0.08f), // footnote
        labelLarge = style(16, 21, 500, -0.32f), // callout
        labelMedium = style(12, 16, 500, 0f), // caption1
        labelSmall = style(11, 13, 500, 0.07f), // caption2
    )
}
