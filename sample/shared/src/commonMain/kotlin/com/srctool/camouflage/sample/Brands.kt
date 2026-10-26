@file:Suppress("MagicNumber") // the brands' token tables: scale steps and radii

package com.srctool.camouflage.sample

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.iconbutton.IconButtonTheme
import com.srctool.camouflage.core.theme.CamoColorScale
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTokenSet

/**
 * A brand's own vocabulary (Theme §3.14): a numbered color scale and two named radii. Hand-written here; the
 * `camo` generator writes these from a tokens file. Roles and component themes refer to it, and the sample's
 * screen reads it with `Camouflage.tokens<BrandTokens>()`.
 */
@Immutable
data class BrandTokens(val brand: CamoColorScale, val controlRadius: Dp, val panelRadius: Dp) :
    CamoTokenSet<BrandTokens> {
    override fun lerp(to: BrandTokens, fraction: Float) = BrandTokens(
        brand = brand.lerp(to.brand, fraction),
        controlRadius = lerp(controlRadius, to.controlRadius, fraction),
        panelRadius = lerp(panelRadius, to.panelRadius, fraction),
    )
}

// Controls rounder than this are drawn as pills; icon buttons become circles.
private val PILL_FROM = 8.dp

private fun scale(vararg steps: Pair<Int, Long>) = CamoColorScale(steps.associate { (k, v) -> k to Color(v) })

/** Two brands designed on top of a skin (ADR-28), plus the skin's own starter theme. */
enum class Brand(val label: String, val tokens: BrandTokens?, val boldLabels: Boolean = false) {
    Default("Default", null),
    Ocean(
        "Ocean",
        BrandTokens(
            brand = scale(
                50 to 0xFFE6F6F7, 100 to 0xFFC0E9EC, 200 to 0xFF8FD8DE, 300 to 0xFF57C3CC, 400 to 0xFF2AAEBA,
                500 to 0xFF0B98A6, 600 to 0xFF087F8C, 700 to 0xFF066570, 800 to 0xFF054D55, 900 to 0xFF03363C,
            ),
            controlRadius = 22.dp,
            panelRadius = 20.dp,
        ),
    ),
    Ember(
        "Ember",
        BrandTokens(
            brand = scale(
                50 to 0xFFFFF1EC, 100 to 0xFFFFDCCF, 200 to 0xFFFFB8A0, 300 to 0xFFFF8F6B, 400 to 0xFFF96A3F,
                500 to 0xFFE94E21, 600 to 0xFFC93D14, 700 to 0xFFA12F0F, 800 to 0xFF7A240C, 900 to 0xFF541908,
            ),
            controlRadius = 2.dp,
            panelRadius = 4.dp,
        ),
        boldLabels = true,
    ),
    ;

    /**
     * This brand on [base], a skin's starter theme: the primary family and the control shapes come from the
     * brand's tokens, everything else stays the skin's.
     */
    fun on(base: CamoTheme, dark: Boolean): CamoTheme {
        val t = tokens ?: return base
        val colors = base.colors.copy(
            primary = if (dark) t.brand[300] else t.brand[600],
            onPrimary = if (dark) t.brand[900] else Color.White,
            primaryContainer = if (dark) t.brand[800] else t.brand[100],
            onPrimaryContainer = if (dark) t.brand[100] else t.brand[900],
        )
        val button = base.components.button ?: ButtonTheme()
        val iconButton = base.components.iconButton ?: IconButtonTheme()
        val label = (button.textStyle ?: base.typography.labelLarge)
            .let { if (boldLabels) it.copy(fontWeight = FontWeight.Bold) else it }
        return base.copy(
            colors = colors,
            components = base.components.copy(
                button = button.copy(shape = RoundedCornerShape(t.controlRadius), textStyle = label),
                iconButton = iconButton.copy(
                    shape = if (t.controlRadius > PILL_FROM) CircleShape else RoundedCornerShape(t.controlRadius),
                ),
            ),
            tokenSets = base.tokenSets + t,
        )
    }
}
