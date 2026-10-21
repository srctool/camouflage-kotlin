package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The 42 color roles in 7 groups. `onX` is the color of content drawn on `x`.
 * Build them explicitly (every role), or from a few seeds with [fromSeed].
 */
@Immutable
public data class CamoColors(
    // brand
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color, val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color, val tertiaryContainer: Color, val onTertiaryContainer: Color,
    // surfaces, content, lines
    val surface: Color,
    val surfaceContainerLowest: Color, val surfaceContainerLow: Color, val surfaceContainer: Color,
    val surfaceContainerHigh: Color, val surfaceContainerHighest: Color,
    val onSurface: Color, val onSurfaceVariant: Color,
    val outline: Color, val outlineVariant: Color,
    // status
    val error: Color, val onError: Color, val errorContainer: Color, val onErrorContainer: Color,
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
    // inverse, utility
    val inverseSurface: Color, val inverseOnSurface: Color, val inversePrimary: Color,
    val scrim: Color,
) {
    public companion object {
        /** Camouflage's default status seeds (settled 2026-09-27). Only their hue and chroma matter. */
        internal val DefaultErrorSeed: Color = Color(0xFFB3261E)
        internal val DefaultSuccessSeed: Color = Color(0xFF2E7D32)
        internal val DefaultWarningSeed: Color = Color(0xFFF9A825)
        internal val DefaultInfoSeed: Color = Color(0xFF0288D1)

        /**
         * All 42 roles from a few seeds, with the tonal-palette algorithm (HCT) and Camouflage's tone table.
         *
         * - `secondary` / `tertiary`: `null` derives them from `primary`'s hue.
         * - Status seeds: `null` uses the defaults above.
         * - `neutral`: `null` gives true grays (ADR-24). Pass `primary` for brand-tinted surfaces (Material style).
         */
        public fun fromSeed(
            primary: Color,
            secondary: Color? = null,
            tertiary: Color? = null,
            error: Color? = null,
            success: Color? = null,
            warning: Color? = null,
            info: Color? = null,
            neutral: Color? = null,
            dark: Boolean = false,
        ): CamoColors =
            SeedScheme.build(primary, secondary, tertiary, error, success, warning, info, neutral, isDark = dark)
    }
}

/** One color family: the strong color, the content on it, the container, and the content on the container. */
@Immutable
public data class ToneColors(val strong: Color, val onStrong: Color, val container: Color, val onContainer: Color)

/** The family a [Tone] reads. `Default` pairs `primary` with `secondaryContainer`; skins may document their own. */
public fun CamoColors.family(tone: Tone): ToneColors = when (tone) {
    Tone.Default -> ToneColors(primary, onPrimary, secondaryContainer, onSecondaryContainer)
    Tone.Error -> ToneColors(error, onError, errorContainer, onErrorContainer)
    Tone.Success -> ToneColors(success, onSuccess, successContainer, onSuccessContainer)
    Tone.Warning -> ToneColors(warning, onWarning, warningContainer, onWarningContainer)
    Tone.Info -> ToneColors(info, onInfo, infoContainer, onInfoContainer)
}
