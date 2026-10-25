@file:Suppress("MagicNumber") // Fluent's token tables: ramp steps, grey levels, sizes.

package com.srctool.camouflage.skin.fluent

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.iconbutton.IconButtonTheme
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoMotion
import com.srctool.camouflage.core.theme.CamoShapes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography

/**
 * Fluent 2's starter theme: the brand ramp for the `primary` family, Fluent's neutral ramp for surfaces and
 * text, the danger, success and warning palettes for status (info is the brand), Fluent's type ramp, radii and
 * motion, plus [FluentTokens] for the hover and press steps. [density] sets the controls' sizes through the
 * component themes. [fontFamily] `null` is the platform font (Segoe UI on Windows); fonts are never bundled.
 */
public fun fluentTheme(
    brand: Color = FluentBrandBlue,
    dark: Boolean = false,
    density: FluentDensity = FluentDensity.Medium,
    fontFamily: FontFamily? = null,
): CamoTheme {
    val ramp = BrandRamp.of(brand)
    val typography = fluentTypography(fontFamily ?: FontFamily.Default)
    return CamoTheme(
        colors = if (dark) darkColors(ramp) else lightColors(ramp),
        typography = typography,
        components = densityThemes(density, typography),
        shapes = FluentShapes,
        motion = FluentMotion,
        tokenSets = listOf(if (dark) darkTokens(ramp) else lightTokens(ramp)),
    )
}

// Fluent's colorNeutral*/colorBrand* aliases (alias/lightColor) mapped to core's roles.
private fun lightColors(b: BrandRamp) = CamoColors(
    primary = b[80], onPrimary = Color.White, primaryContainer = b[160], onPrimaryContainer = b[70],
    secondary = grey(38), onSecondary = Color.White, secondaryContainer = grey(94), onSecondaryContainer = grey(14),
    tertiary = b[60], onTertiary = Color.White, tertiaryContainer = b[150], onTertiaryContainer = b[40],
    surface = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = grey(98), surfaceContainer = grey(96),
    surfaceContainerHigh = grey(94), surfaceContainerHighest = grey(92),
    onSurface = grey(14), onSurfaceVariant = grey(26),
    outline = grey(38), outlineVariant = grey(82),
    error = Cranberry.primary, onError = Color.White,
    errorContainer = Cranberry.tint60, onErrorContainer = Cranberry.foreground,
    success = Green.primary, onSuccess = Color.White,
    successContainer = Green.tint60, onSuccessContainer = Green.foreground,
    warning = Orange.primary, onWarning = grey(14),
    warningContainer = Orange.tint60, onWarningContainer = Orange.foreground,
    info = b[80], onInfo = Color.White, infoContainer = b[160], onInfoContainer = b[70],
    inverseSurface = grey(16), inverseOnSurface = Color.White, inversePrimary = b[100],
    scrim = Color.Black,
)

// alias/darkColor. `primary` is the brand foreground (readable as text on dark surfaces); the filled brand
// background is darker and lives in FluentTokens.brandBackground.
private fun darkColors(b: BrandRamp) = CamoColors(
    primary = b[100], onPrimary = Color.Black, primaryContainer = b[20], onPrimaryContainer = b[110],
    secondary = grey(68), onSecondary = grey(14), secondaryContainer = grey(24), onSecondaryContainer = Color.White,
    tertiary = b[120], onTertiary = b[20], tertiaryContainer = b[40], onTertiaryContainer = b[150],
    surface = grey(16),
    surfaceContainerLowest = grey(12), surfaceContainerLow = grey(20), surfaceContainer = grey(22),
    surfaceContainerHigh = grey(24), surfaceContainerHighest = grey(28),
    onSurface = Color.White, onSurfaceVariant = grey(84),
    outline = grey(68), outlineVariant = grey(40),
    error = Cranberry.tint30, onError = Color.Black,
    errorContainer = Cranberry.shade40, onErrorContainer = Cranberry.tint30,
    success = Green.tint30, onSuccess = Color.Black,
    successContainer = Green.shade40, onSuccessContainer = Green.tint40,
    warning = Orange.tint30, onWarning = Color.Black,
    warningContainer = Orange.shade40, onWarningContainer = Orange.tint30,
    info = b[100], onInfo = Color.Black, infoContainer = b[30], onInfoContainer = b[130],
    inverseSurface = Color.White, inverseOnSurface = grey(14), inversePrimary = b[80],
    scrim = Color.Black,
)

private fun lightTokens(b: BrandRamp) = FluentTokens(
    brandBackground = b[80], brandBackgroundHover = b[70], brandBackgroundPressed = b[40],
    neutralForegroundOnBrand = Color.White,
    neutralBackground1Hover = grey(96), neutralBackground1Pressed = grey(88),
    subtleBackgroundHover = grey(96), subtleBackgroundPressed = grey(88),
    neutralForeground2Hover = grey(14),
    neutralStroke1 = grey(82), neutralStroke1Hover = grey(78), neutralStroke1Pressed = grey(70),
    neutralStroke2 = grey(88),
    brandForegroundLinkHover = b[60], brandForegroundLinkPressed = b[40],
    strokeFocus1 = Color.White, strokeFocus2 = Color.Black,
    shadowAmbient = Color.Black.copy(alpha = 0.12f), shadowKey = Color.Black.copy(alpha = 0.14f),
)

private fun darkTokens(b: BrandRamp) = FluentTokens(
    brandBackground = b[70], brandBackgroundHover = b[80], brandBackgroundPressed = b[40],
    neutralForegroundOnBrand = Color.White,
    neutralBackground1Hover = grey(24), neutralBackground1Pressed = grey(12),
    subtleBackgroundHover = grey(22), subtleBackgroundPressed = grey(18),
    neutralForeground2Hover = Color.White,
    neutralStroke1 = grey(40), neutralStroke1Hover = grey(46), neutralStroke1Pressed = grey(42),
    neutralStroke2 = grey(32),
    brandForegroundLinkHover = b[110], brandForegroundLinkPressed = b[90],
    strokeFocus1 = Color.Black, strokeFocus2 = Color.White,
    shadowAmbient = Color.Black.copy(alpha = 0.24f), shadowKey = Color.Black.copy(alpha = 0.28f),
)

// Fluent's type ramp (global/fonts): caption2 10/14, caption1 12/16, body1 14/20, body2 and subtitle2 16/22,
// subtitle1 20/28, title3 24/32, title2 28/36, title1 32/40, largeTitle 40/52, display 68/92.
private fun fluentTypography(family: FontFamily): CamoTypography {
    fun style(size: Int, line: Int, weight: FontWeight) =
        TextStyle(fontFamily = family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
    val semibold = FontWeight.SemiBold
    val regular = FontWeight.Normal
    return CamoTypography(
        displayLarge = style(size = 68, line = 92, semibold),
        displayMedium = style(size = 40, line = 52, semibold),
        displaySmall = style(size = 32, line = 40, semibold),
        headlineLarge = style(size = 28, line = 36, semibold),
        headlineMedium = style(size = 24, line = 32, semibold),
        headlineSmall = style(size = 20, line = 28, semibold),
        titleLarge = style(size = 20, line = 28, semibold),
        titleMedium = style(size = 16, line = 22, semibold),
        titleSmall = style(size = 14, line = 20, semibold),
        bodyLarge = style(size = 16, line = 22, regular),
        bodyMedium = style(size = 14, line = 20, regular),
        bodySmall = style(size = 12, line = 16, regular),
        labelLarge = style(size = 14, line = 20, semibold),
        labelMedium = style(size = 12, line = 16, semibold),
        labelSmall = style(size = 10, line = 14, regular),
    )
}

// global/borderRadius: small 2, medium 4, large 6, xLarge 8, 2xLarge 12, circular.
private val FluentShapes = CamoShapes(
    none = RoundedCornerShape(0.dp),
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
    full = RoundedCornerShape(percent = 50),
)

// global/durations (faster 100, normal 200, slow 300) and curves (easyEase, decelerateMid).
private val FluentMotion = CamoMotion(
    durationShort = 100,
    durationMedium = 200,
    durationLong = 300,
    easingStandard = CubicBezierEasing(a = 0.33f, b = 0f, c = 0.67f, d = 1f),
    easingEmphasized = CubicBezierEasing(a = 0.1f, b = 0.9f, c = 0.2f, d = 1f),
)

// Fluent Button's sizes: small 24 (caption1, padding 8), medium 32 (body1Strong, padding 12), large 40
// (subtitle2, padding 16); icon-only buttons are square at the same heights.
private fun densityThemes(density: FluentDensity, type: CamoTypography): CamoComponentThemes {
    data class Size(val height: Dp, val minWidth: Dp, val padding: Dp, val iconSize: Dp, val text: TextStyle)
    val s = when (density) {
        FluentDensity.Small -> Size(24.dp, 64.dp, 8.dp, 16.dp, type.bodySmall)
        FluentDensity.Medium -> Size(32.dp, 96.dp, 12.dp, 20.dp, type.labelLarge)
        FluentDensity.Touch -> Size(40.dp, 96.dp, 16.dp, 24.dp, type.titleMedium)
    }
    return CamoComponentThemes(
        button = ButtonTheme(
            height = s.height,
            minWidth = s.minWidth,
            padding = PaddingValues(horizontal = s.padding),
            paddingWithIcon = PaddingValues(horizontal = s.padding),
            textStyle = s.text,
            iconSize = s.iconSize,
        ),
        iconButton = IconButtonTheme(containerSize = s.height, iconSize = s.iconSize),
    )
}
