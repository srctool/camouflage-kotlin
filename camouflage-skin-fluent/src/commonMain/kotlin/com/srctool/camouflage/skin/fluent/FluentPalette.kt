@file:Suppress("MagicNumber") // Fluent's published color tables.

package com.srctool.camouflage.skin.fluent

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette

// Fluent 2's published color tokens (@fluentui/tokens 1.0.0-alpha.24: global/brandColors, global/colors).

/** Fluent's default brand color, `brandWeb[80]`. */
public val FluentBrandBlue: Color = Color(0xFF0F6CBD)

// brandWeb, steps 10..160.
private val BrandWeb = listOf(
    0xFF061724, 0xFF082338, 0xFF0A2E4A, 0xFF0C3B5E, 0xFF0E4775, 0xFF0F548C, 0xFF115EA3, 0xFF0F6CBD,
    0xFF2886DE, 0xFF479EF5, 0xFF62ABF5, 0xFF77B7F7, 0xFF96C6FA, 0xFFB4D6FA, 0xFFCFE4FA, 0xFFEBF3FC,
).map { Color(it) }

private const val BRAND_STEP = 80
private const val STEP = 10

/**
 * A 16-step brand ramp, read by step (`ramp[80]` is the brand color). The default brand is Fluent's own
 * `brandWeb`; another brand keeps its hue and chroma at the tones of `brandWeb`'s steps, with step 80 being the
 * brand color itself.
 */
internal class BrandRamp(private val steps: List<Color>) {
    operator fun get(step: Int): Color = steps[step / STEP - 1]

    companion object {
        fun of(brand: Color): BrandRamp {
            if (brand == FluentBrandBlue) return BrandRamp(BrandWeb)
            val hct = Hct.fromInt(brand.toArgb())
            val palette = TonalPalette.fromHueAndChroma(hct.hue, hct.chroma)
            return BrandRamp(
                BrandWeb.mapIndexed { i, web ->
                    val tone = Hct.fromInt(web.toArgb()).tone.toInt()
                    if ((i + 1) * STEP == BRAND_STEP) brand else Color(palette.tone(tone))
                },
            )
        }
    }
}

/** Fluent's neutral `grey[n]`: n is the lightness percentage. */
internal fun grey(n: Int): Color = Grey.getValue(n)

private val Grey = mapOf(
    2 to 0xFF050505, 4 to 0xFF0A0A0A, 6 to 0xFF0F0F0F, 8 to 0xFF141414, 10 to 0xFF1A1A1A, 12 to 0xFF1F1F1F,
    14 to 0xFF242424, 16 to 0xFF292929, 18 to 0xFF2E2E2E, 20 to 0xFF333333, 22 to 0xFF383838, 24 to 0xFF3D3D3D,
    26 to 0xFF424242, 28 to 0xFF474747, 30 to 0xFF4D4D4D, 32 to 0xFF525252, 34 to 0xFF575757, 36 to 0xFF5C5C5C,
    38 to 0xFF616161, 40 to 0xFF666666, 42 to 0xFF6B6B6B, 44 to 0xFF707070, 46 to 0xFF757575, 48 to 0xFF7A7A7A,
    50 to 0xFF808080, 60 to 0xFF999999, 68 to 0xFFADADAD, 70 to 0xFFB3B3B3, 74 to 0xFFBDBDBD, 78 to 0xFFC7C7C7,
    82 to 0xFFD1D1D1, 84 to 0xFFD6D6D6, 88 to 0xFFE0E0E0, 90 to 0xFFE6E6E6, 92 to 0xFFEBEBEB, 94 to 0xFFF0F0F0,
    96 to 0xFFF5F5F5, 98 to 0xFFFAFAFA,
).mapValues { Color(it.value) }

/** The steps of a Fluent shared color that the status roles use; [foreground] is light mode's foreground1. */
internal class Shared(
    val shade40: Color,
    val foreground: Color,
    val primary: Color,
    val tint30: Color,
    val tint40: Color,
    val tint60: Color,
)

// The status mapping (statusColorMapping): danger = cranberry, success = green, warning = orange. Light mode's
// foreground1 is shade10, except warning's, which Fluent patches to shade20.
internal val Cranberry = Shared(
    Color(0xFF3B0509), Color(0xFFB10E1C), Color(0xFFC50F1F), Color(0xFFDC626D), Color(0xFFEEACB2), Color(0xFFFDF3F4),
)
internal val Green = Shared(
    Color(0xFF052505), Color(0xFF0E700E), Color(0xFF107C10), Color(0xFF54B054), Color(0xFF9FD89F), Color(0xFFF1FAF1),
)
internal val Orange = Shared(
    Color(0xFF4A1E04), Color(0xFFBC4B09), Color(0xFFF7630C), Color(0xFFFAA06B), Color(0xFFFDCFB4), Color(0xFFFFF9F5),
)
