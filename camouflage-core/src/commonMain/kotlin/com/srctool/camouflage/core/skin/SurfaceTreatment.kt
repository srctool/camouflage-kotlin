package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.theme.CamoTheme

/** What a skin's surfaces are made of: how a container is filled, shown at depth and clipped. */
@Stable
public interface SurfaceTreatment {
    /** Background, depth and clip for a container. Renderers apply it after the component's behavior modifier. */
    public fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier
}
