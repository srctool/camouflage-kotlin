package com.srctool.camouflage.skin.fluent

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.levelOf

/**
 * Fluent's Mica: the window backdrop Windows 11 paints from the wallpaper. Camouflage does not draw it. Where the
 * platform reports `CamoCapabilities.windowMaterial` (Windows 11 on the desktop JVM), the base layers (the window,
 * the navigation chrome: elevation below `Level2`) draw nothing, so the backdrop the app enabled on its window
 * shows through them. Everywhere else, and with Reduce Transparency, they are the opaque Fluent surface
 * ([FluentSolid]), so the layout and behavior are the same (ADR-42). Raised surfaces (cards, flyouts, dialogs)
 * are always [FluentSolid].
 *
 * Choosing it is the opt-in: the app must also give its window the Mica backdrop (and a transparent Compose
 * background), or the base layers show the window's own color.
 */
public object FluentMica : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier {
        val capabilities = theme.environment?.capabilities
        val base = theme.elevation.levelOf(elevation) < ElevationLevel.Level2
        return when {
            base && capabilities?.windowMaterial == true && !capabilities.reduceTransparency -> Modifier
            else -> FluentSolid.modifier(color, shape, elevation, theme)
        }
    }
}
