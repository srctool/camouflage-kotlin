package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 15 text styles: five roles × three sizes. Each is complete, weight included, so a component reads one constant.
 * Sizes are in `sp`, so the user's font scale always applies.
 */
@Immutable
public data class CamoTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
) {
    public companion object {
        /** Camouflage's default scale (base Theme §3.5) in the platform font, or in [fontFamily]. */
        public fun system(fontFamily: FontFamily = FontFamily.Default): CamoTypography {
            fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
                fontFamily = fontFamily,
                fontWeight = weight,
                fontSize = size.sp,
                lineHeight = lineHeight.sp,
                letterSpacing = 0.sp,
            )
            val strong = FontWeight.SemiBold
            val label = FontWeight.Medium
            val body = FontWeight.Normal
            return CamoTypography(
                displayLarge = style(size = 48, lineHeight = 56, weight = strong),
                displayMedium = style(size = 40, lineHeight = 48, weight = strong),
                displaySmall = style(size = 32, lineHeight = 40, weight = strong),
                headlineLarge = style(size = 28, lineHeight = 36, weight = strong),
                headlineMedium = style(size = 24, lineHeight = 32, weight = strong),
                headlineSmall = style(size = 20, lineHeight = 28, weight = strong),
                titleLarge = style(size = 18, lineHeight = 26, weight = strong),
                titleMedium = style(size = 16, lineHeight = 24, weight = strong),
                titleSmall = style(size = 14, lineHeight = 20, weight = strong),
                bodyLarge = style(size = 16, lineHeight = 24, weight = body),
                bodyMedium = style(size = 14, lineHeight = 20, weight = body),
                bodySmall = style(size = 12, lineHeight = 16, weight = body),
                labelLarge = style(size = 14, lineHeight = 20, weight = label),
                labelMedium = style(size = 12, lineHeight = 16, weight = label),
                labelSmall = style(size = 11, lineHeight = 16, weight = label),
            )
        }
    }
}
