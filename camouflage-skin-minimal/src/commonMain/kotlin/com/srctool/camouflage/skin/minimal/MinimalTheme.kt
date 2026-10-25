package com.srctool.camouflage.skin.minimal

import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography

// A seed with no chroma: the neutral palette, for the ink primary and the neutral secondary and tertiary.
private val Gray = Color(0xFF777777)

/**
 * The Minimal starter theme: true-gray surfaces, status colors only for status, and an **ink** primary
 * (near-black in light, near-white in dark) unless an [accent] is given. With an accent, only the `primary`
 * family takes its color; secondary and tertiary stay neutral.
 */
public fun minimalTheme(
    accent: Color? = null,
    dark: Boolean = false,
    typography: CamoTypography = CamoTypography.system(),
): CamoTheme {
    val seeded = CamoColors.fromSeed(primary = accent ?: Gray, secondary = Gray, tertiary = Gray, dark = dark)
    // Ink is the neutral palette's text tones (base Minimal Skin §3.2): primary 10 / 92, onPrimary 100 / 10,
    // primaryContainer 92 / 22, onPrimaryContainer 10 / 92 — the same tones as these surface roles.
    val colors = if (accent != null) {
        seeded
    } else {
        seeded.copy(
            primary = seeded.onSurface,
            onPrimary = if (dark) seeded.surfaceContainerLow else seeded.surface,
            primaryContainer = seeded.surfaceContainerHighest,
            onPrimaryContainer = seeded.onSurface,
        )
    }
    return CamoTheme(colors = colors, typography = typography)
}

/** [minimalTheme] in light mode, with the ink primary. */
public val minimalLight: CamoTheme = minimalTheme()

/** [minimalTheme] in dark mode, with the ink primary. */
public val minimalDark: CamoTheme = minimalTheme(dark = true)
