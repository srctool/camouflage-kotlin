package com.srctool.camouflage.skin.testing

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography

// Ahem, the W3C test font (public domain): every glyph is a box, so goldens match on every OS.
private val Ahem = FontFamily(Font(resource = "camouflage/Ahem.ttf"))

/**
 * This theme with every text style in the Ahem test font, for screenshot goldens shared across machines: the
 * typography and the text styles set in component themes (a density preset's button text, for example).
 */
public fun CamoTheme.withGoldenFont(): CamoTheme {
    fun TextStyle.ahem() = copy(fontFamily = Ahem)
    return copy(
        typography = typography.map { it.ahem() },
        components = components.copy(
            button = components.button?.let { it.copy(textStyle = it.textStyle?.ahem()) },
            tooltip = components.tooltip?.let { it.copy(textStyle = it.textStyle?.ahem()) },
        ),
    )
}

private fun CamoTypography.map(f: (TextStyle) -> TextStyle) = CamoTypography(
    f(displayLarge), f(displayMedium), f(displaySmall),
    f(headlineLarge), f(headlineMedium), f(headlineSmall),
    f(titleLarge), f(titleMedium), f(titleSmall),
    f(bodyLarge), f(bodyMedium), f(bodySmall),
    f(labelLarge), f(labelMedium), f(labelSmall),
)
