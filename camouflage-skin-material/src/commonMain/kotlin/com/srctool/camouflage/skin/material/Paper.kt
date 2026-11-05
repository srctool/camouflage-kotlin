package com.srctool.camouflage.skin.material

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme

/**
 * Material 3's surface: the given color clipped to the shape, with a shadow from `Level1`. Separation from the
 * background mostly comes from the surface level (`surfaceContainer*`); the shadow is the elevation of
 * raised components (1, 3, 6, 8 and 12 dp for `Level1` to `Level5`).
 */
public object Paper : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        Modifier.shadow(elevation, shape, clip = false).background(color, shape).clip(shape)
}
