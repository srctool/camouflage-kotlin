package com.srctool.camouflage.skin.minimal

import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTypography

/** The Minimal skin's starter theme: monochrome ink on true grays. A placeholder until milestone M3 builds the skin. */
public val minimalTheme: CamoTheme = CamoTheme(
    colors = CamoColors.fromSeed(primary = Color(0xFF1F1F1F)),
    typography = CamoTypography.system(),
)
