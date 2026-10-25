package com.srctool.camouflage.skin.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.levelOf

/**
 * Minimal's surfaces: never a shadow. From `Level1` a 1 dp `outlineVariant` hairline shows depth, and from
 * `Level3` a surface fill also steps up one container level.
 */
public object Flat : SurfaceTreatment {
    private val Hairline = 1.dp

    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier {
        val level = theme.elevation.levelOf(elevation)
        val fill = if (level >= ElevationLevel.Level3) theme.colors.oneLevelUp(color) else color
        val hairline = if (level >= ElevationLevel.Level1) {
            Modifier.border(Hairline, theme.colors.outlineVariant, shape)
        } else {
            Modifier
        }
        return Modifier.background(fill, shape).then(hairline)
    }
}

// The next surface role up; colors that aren't surface roles (a solid button's ink) are unchanged.
internal fun CamoColors.oneLevelUp(color: Color): Color = when (color) {
    surface, surfaceContainerLowest -> surfaceContainerLow
    surfaceContainerLow -> surfaceContainer
    surfaceContainer -> surfaceContainerHigh
    surfaceContainerHigh, surfaceContainerHighest -> surfaceContainerHighest
    else -> color
}
