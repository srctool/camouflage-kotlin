package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography

private val Seed = Color(0xFF0B57D0)
private val SampleLight = CamoTheme(colors = CamoColors.fromSeed(primary = Seed), typography = CamoTypography.system())
private val SampleDark = SampleLight.copy(colors = CamoColors.fromSeed(primary = Seed, dark = true))

/** The sample app. M1: every color role of a seed theme, in light and dark. */
@Composable
fun SampleApp() {
    Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Swatches("Light", SampleLight, Modifier.weight(1f))
        Swatches("Dark", SampleDark, Modifier.weight(1f))
    }
}

@Composable
private fun Swatches(title: String, theme: CamoTheme, modifier: Modifier) {
    val c = theme.colors
    Column(
        modifier.background(c.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText("$title · seed #0B57D0", style = theme.typography.titleMedium.copy(color = c.onSurface))
        roles(c).forEach { (name, color, content) ->
            Box(
                Modifier.fillMaxWidth().height(36.dp).background(color)
                    .border(1.dp, c.outlineVariant).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicText(name, style = theme.typography.labelMedium.copy(color = content))
            }
        }
    }
}

// Each role with the content color drawn on it.
private fun roles(c: CamoColors): List<Triple<String, Color, Color>> = listOf(
    Triple("primary", c.primary, c.onPrimary),
    Triple("primaryContainer", c.primaryContainer, c.onPrimaryContainer),
    Triple("secondary", c.secondary, c.onSecondary),
    Triple("secondaryContainer", c.secondaryContainer, c.onSecondaryContainer),
    Triple("tertiary", c.tertiary, c.onTertiary),
    Triple("tertiaryContainer", c.tertiaryContainer, c.onTertiaryContainer),
    Triple("surface", c.surface, c.onSurface),
    Triple("surfaceContainerLowest", c.surfaceContainerLowest, c.onSurface),
    Triple("surfaceContainerLow", c.surfaceContainerLow, c.onSurface),
    Triple("surfaceContainer", c.surfaceContainer, c.onSurface),
    Triple("surfaceContainerHigh", c.surfaceContainerHigh, c.onSurface),
    Triple("surfaceContainerHighest", c.surfaceContainerHighest, c.onSurfaceVariant),
    Triple("outline", c.outline, c.surface),
    Triple("outlineVariant", c.outlineVariant, c.onSurface),
    Triple("error", c.error, c.onError),
    Triple("errorContainer", c.errorContainer, c.onErrorContainer),
    Triple("success", c.success, c.onSuccess),
    Triple("successContainer", c.successContainer, c.onSuccessContainer),
    Triple("warning", c.warning, c.onWarning),
    Triple("warningContainer", c.warningContainer, c.onWarningContainer),
    Triple("info", c.info, c.onInfo),
    Triple("infoContainer", c.infoContainer, c.onInfoContainer),
    Triple("inverseSurface", c.inverseSurface, c.inverseOnSurface),
    Triple("inversePrimary", c.inversePrimary, c.inverseSurface),
    Triple("scrim", c.scrim, Color.White), // the scrim has no content role
)
