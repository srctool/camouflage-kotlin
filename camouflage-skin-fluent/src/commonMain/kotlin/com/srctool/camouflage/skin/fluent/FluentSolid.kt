package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.levelOf

/**
 * Fluent's default surface: the fill plus Fluent's two-layer shadow (an ambient glow and a key shadow below).
 * `Level0` and `Level1` are flat; `Level2` is shadow4 (cards), `Level3` shadow8, `Level4` shadow16 (flyouts),
 * `Level5` shadow64 (dialogs). Shadow colors come from [FluentTokens], or default black alphas without them.
 */
public object FluentSolid : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        shadows(shape, elevation, theme).background(color, shape)

    /** Fluent's shadow for [elevation], without the fill (shared with [FluentAcrylic]). */
    internal fun shadows(shape: Shape, elevation: Dp, theme: CamoTheme): Modifier {
        val ramp = ShadowRamp[theme.elevation.levelOf(elevation)] ?: return Modifier
        val tokens = theme.tokens<FluentTokens>()
        val ambient = tokens?.shadowAmbient ?: DefaultAmbient
        val key = tokens?.shadowKey ?: DefaultKey
        return Modifier
            .dropShadow(shape, Shadow(radius = ramp.ambientBlur, color = ambient))
            .dropShadow(shape, Shadow(radius = ramp.keyBlur, color = key, offset = DpOffset(0.dp, ramp.keyOffset)))
    }

    // utils/shadows: shadowN = "0 0 <ambient>px ambient, 0 <N/2>px <N>px key".
    private class Layers(val ambientBlur: Dp, val keyOffset: Dp, val keyBlur: Dp)

    private val ShadowRamp = mapOf(
        ElevationLevel.Level2 to Layers(2.dp, 2.dp, 4.dp),
        ElevationLevel.Level3 to Layers(2.dp, 4.dp, 8.dp),
        ElevationLevel.Level4 to Layers(2.dp, 8.dp, 16.dp),
        ElevationLevel.Level5 to Layers(8.dp, 32.dp, 64.dp),
    )

    private val DefaultAmbient = Color.Black.copy(alpha = 0.12f)
    private val DefaultKey = Color.Black.copy(alpha = 0.14f)
}
