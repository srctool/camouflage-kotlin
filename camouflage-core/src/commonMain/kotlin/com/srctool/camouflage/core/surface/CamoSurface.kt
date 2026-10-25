package com.srctool.camouflage.core.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.SurfaceLevel

/** Overrides for surfaces' borders. `null` keeps the skin's default. */
@Immutable
public data class SurfaceTheme(val borderWidth: Dp? = null, val borderColor: Color? = null)

/** The resolved surface style: every field set. */
@Immutable
public data class SurfaceStyle(val borderWidth: Dp, val borderColor: Color) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SurfaceTheme?): SurfaceStyle = if (overrides == null) {
        this
    } else {
        SurfaceStyle(overrides.borderWidth ?: borderWidth, overrides.borderColor ?: borderColor)
    }
}

/** What a surface renderer draws. */
@Immutable
public data class SurfaceState(
    val level: SurfaceLevel,
    val shape: ShapeRole,
    val elevation: ElevationLevel,
    val border: Boolean,
)

/** The surface's content. */
@Stable
public class SurfaceSlots(public val content: @Composable () -> Unit)

/** Draws a container: a surface role, a shape and a depth, through the skin's surface treatment. */
@Stable
public interface SurfaceRenderer {
    /** The complete surface style for [theme]. */
    public fun defaults(theme: CamoTheme): SurfaceStyle

    /** Draws [state] around [slots]. */
    @Composable
    public fun Render(
        state: SurfaceState,
        slots: SurfaceSlots,
        theme: CamoTheme,
        style: SurfaceStyle,
        modifier: Modifier,
    )
}

/** The color of a surface level. */
public fun CamoColors.surfaceOf(level: SurfaceLevel): Color = when (level) {
    SurfaceLevel.Surface -> surface
    SurfaceLevel.Lowest -> surfaceContainerLowest
    SurfaceLevel.Low -> surfaceContainerLow
    SurfaceLevel.Container -> surfaceContainer
    SurfaceLevel.High -> surfaceContainerHigh
    SurfaceLevel.Highest -> surfaceContainerHighest
}

/**
 * A neutral container: one of the six surface levels, clipped to a shape, at a semantic depth. It sets
 * `onSurface` as the content color for what's inside. Not interactive, and touches pass through it.
 */
@Composable
public fun CamoSurface(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Surface,
    shape: ShapeRole = ShapeRole.None,
    elevation: ElevationLevel = ElevationLevel.Level0,
    border: Boolean = false,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoSurface")
    val theme = requireTheme("CamoSurface")
    val style = remember(skin, theme) { skin.surface.defaults(theme).merge(theme.components.surface) }
    skin.surface.Render(SurfaceState(level, shape, elevation, border), SurfaceSlots(content), theme, style, modifier)
}
