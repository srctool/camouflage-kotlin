package com.srctool.camouflage.core.provider

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.theme.CamoTheme

// Skin and theme change rarely and are read everywhere: static locals, so reads are cheap.
internal val LocalCamoSkin = staticCompositionLocalOf<CamoSkin?> { null }
internal val LocalCamoTheme = staticCompositionLocalOf<CamoTheme?> { null }

// Content style is set by every container: dynamic locals, so only readers recompose.
internal val LocalCamoContentColor = compositionLocalOf { Color.Unspecified }
internal val LocalCamoIconStyle = compositionLocalOf<IconStyle?> { null }
