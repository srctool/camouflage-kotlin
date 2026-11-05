package com.srctool.camouflage.skin.carbon

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.levelOf

/**
 * Carbon's surfaces: opaque layer tokens, never a hairline and no shadow, except that overlays (from `Level3`:
 * dialogs, menus, popovers) get Carbon's one overlay shadow. Depth is the gray of the layer the renderer asks
 * for (`layer-01` to `layer-03`), which steps with the nesting.
 */
public object Layered : SurfaceTreatment {
    private val OverlayShadow = 6.dp
    private const val SHADOW_ALPHA = 0.3f

    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier {
        val fill = Modifier.background(color, shape)
        if (theme.elevation.levelOf(elevation) < ElevationLevel.Level3) return fill
        val shadow = Color.Black.copy(alpha = SHADOW_ALPHA)
        return Modifier.shadow(OverlayShadow, shape, ambientColor = shadow, spotColor = shadow).then(fill)
    }
}
