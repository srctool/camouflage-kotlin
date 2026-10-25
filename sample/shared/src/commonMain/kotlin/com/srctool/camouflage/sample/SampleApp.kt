package com.srctool.camouflage.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.Camouflage
import com.srctool.camouflage.core.CamouflageProvider
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography
import com.srctool.camouflage.core.theme.animateCamoTheme

private val BlueSeed = Color(0xFF0B57D0)
private val GreenSeed = Color(0xFF146C2E)
private val PromoSeed = Color(0xFFE8710A)

private enum class Brand(val label: String, val seed: Color) {
    Blue("blue", BlueSeed),
    Green("green", GreenSeed),
}

private fun theme(seed: Color, dark: Boolean) =
    CamoTheme(colors = CamoColors.fromSeed(primary = seed, dark = dark), typography = CamoTypography.system())

/** The sample app. M2: placeholders drawn through DebugSkin, with nested providers and live switching. */
@Composable
fun SampleApp() {
    val systemDark = isSystemInDarkTheme()
    var dark by remember { mutableStateOf(systemDark) }
    var brand by remember { mutableStateOf(Brand.Blue) }
    var skin by remember { mutableStateOf(DebugSkin) }
    val otherSkin = if (skin == DebugSkin) DebugOutlineSkin else DebugSkin

    CamouflageProvider(skin = skin, theme = animateCamoTheme(theme(brand.seed, dark))) {
        Section("Root provider", Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
                verticalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
            ) {
                Placeholder("Skin: ${skin.name}", onClick = { skin = otherSkin })
                Placeholder("Brand: ${brand.label}", onClick = {
                    brand = Brand.entries[(brand.ordinal + 1) % Brand.entries.size]
                })
                Placeholder(if (dark) "Dark" else "Light", onClick = { dark = !dark })
            }
            Placeholder("Inherits the root skin and theme")
            CamouflageProvider(theme = animateCamoTheme(theme(PromoSeed, dark))) {
                Section("Nested provider: theme only") {
                    Placeholder("Inherits the root skin, uses the promo theme")
                }
            }
            CamouflageProvider(skin = otherSkin) {
                Section("Nested provider: skin only") {
                    Placeholder("Uses the other skin, inherits the root theme")
                }
            }
        }
    }
}

// A region on the theme's surface, drawn with the current skin's surface treatment.
@Composable
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val theme = Camouflage.theme
    Column(
        modifier
            .fillMaxWidth()
            .then(Camouflage.skin.surfaceTreatment.modifier(theme.colors.surface, theme.shapes.large, 0.dp, theme))
            .padding(theme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.m),
    ) {
        BasicText(title, style = theme.typography.titleMedium.copy(color = Camouflage.contentColor))
        content()
    }
}
