package com.srctool.camouflage.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.palettes.TonalPalette

/** Builds [CamoColors] from seeds: MaterialKolor supplies the tonal palettes, Camouflage the tone table. */
internal object SeedScheme {
    private data class Tones(val strong: Int, val onStrong: Int, val container: Int, val onContainer: Int)

    private val Light = Tones(strong = 40, onStrong = 100, container = 90, onContainer = 10)
    private val Dark = Tones(strong = 80, onStrong = 20, container = 30, onContainer = 90)

    // Material's "tonal spot" derivation when no secondary / tertiary seed is given.
    private const val SECONDARY_CHROMA = 16.0
    private const val TERTIARY_CHROMA = 24.0
    private const val TERTIARY_HUE_SHIFT = 60.0

    // A `neutral` seed keeps only a hint of its hue, so surfaces stay readable (Material uses the same chroma).
    private const val TINTED_NEUTRAL_CHROMA = 6.0

    private const val FULL_TURN = 360.0
    private const val INVERSE_PRIMARY_LIGHT = 80
    private const val INVERSE_PRIMARY_DARK = 40

    fun family(palette: TonalPalette, dark: Boolean): ToneColors {
        val t = if (dark) Dark else Light
        return ToneColors(
            strong = palette.color(t.strong),
            onStrong = palette.color(t.onStrong),
            container = palette.color(t.container),
            onContainer = palette.color(t.onContainer),
        )
    }

    fun build(
        primary: Color,
        secondary: Color?,
        tertiary: Color?,
        error: Color?,
        success: Color?,
        warning: Color?,
        info: Color?,
        neutral: Color?,
        isDark: Boolean,
    ): CamoColors {
        val p = palette(primary)
        val s = secondary?.let(::palette) ?: TonalPalette.fromHueAndChroma(p.hue, SECONDARY_CHROMA)
        val t = tertiary?.let(::palette)
            ?: TonalPalette.fromHueAndChroma((p.hue + TERTIARY_HUE_SHIFT) % FULL_TURN, TERTIARY_CHROMA)
        val n = neutral?.let { TonalPalette.fromHueAndChroma(palette(it).hue, TINTED_NEUTRAL_CHROMA) }
            ?: TonalPalette.fromHueAndChroma(0.0, 0.0)

        val pf = family(p, isDark)
        val sf = family(s, isDark)
        val tf = family(t, isDark)
        val ef = family(palette(error ?: CamoColors.DefaultErrorSeed), isDark)
        val okf = family(palette(success ?: CamoColors.DefaultSuccessSeed), isDark)
        val wf = family(palette(warning ?: CamoColors.DefaultWarningSeed), isDark)
        val inf = family(palette(info ?: CamoColors.DefaultInfoSeed), isDark)

        // Camouflage's neutral tones (base Theme §3.4).
        fun nt(light: Int, dark: Int) = n.color(if (isDark) dark else light)

        return CamoColors(
            primary = pf.strong,
            onPrimary = pf.onStrong,
            primaryContainer = pf.container,
            onPrimaryContainer = pf.onContainer,
            secondary = sf.strong,
            onSecondary = sf.onStrong,
            secondaryContainer = sf.container,
            onSecondaryContainer = sf.onContainer,
            tertiary = tf.strong,
            onTertiary = tf.onStrong,
            tertiaryContainer = tf.container,
            onTertiaryContainer = tf.onContainer,
            surface = nt(light = 100, dark = 6),
            surfaceContainerLowest = nt(light = 100, dark = 4),
            surfaceContainerLow = nt(light = 98, dark = 10),
            surfaceContainer = nt(light = 96, dark = 12),
            surfaceContainerHigh = nt(light = 94, dark = 17),
            surfaceContainerHighest = nt(light = 92, dark = 22),
            onSurface = nt(light = 10, dark = 92),
            onSurfaceVariant = nt(light = 35, dark = 75),
            outline = nt(light = 50, dark = 60),
            outlineVariant = nt(light = 90, dark = 25),
            error = ef.strong,
            onError = ef.onStrong,
            errorContainer = ef.container,
            onErrorContainer = ef.onContainer,
            success = okf.strong,
            onSuccess = okf.onStrong,
            successContainer = okf.container,
            onSuccessContainer = okf.onContainer,
            warning = wf.strong,
            onWarning = wf.onStrong,
            warningContainer = wf.container,
            onWarningContainer = wf.onContainer,
            info = inf.strong,
            onInfo = inf.onStrong,
            infoContainer = inf.container,
            onInfoContainer = inf.onContainer,
            inverseSurface = nt(light = 20, dark = 90),
            inverseOnSurface = nt(light = 95, dark = 20),
            inversePrimary = p.color(tone = if (isDark) INVERSE_PRIMARY_DARK else INVERSE_PRIMARY_LIGHT),
            scrim = n.color(0),
        )
    }

    private fun palette(seed: Color): TonalPalette = TonalPalette.fromInt(seed.toArgb())

    private fun TonalPalette.color(tone: Int): Color = Color(tone(tone))
}
