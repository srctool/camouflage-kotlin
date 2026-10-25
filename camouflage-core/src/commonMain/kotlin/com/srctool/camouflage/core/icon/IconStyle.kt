package com.srctool.camouflage.core.icon

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The size and color icons take by default inside a container. Renderers set it for their slots, and it's
 * also the resolved icon component theme (the provider's root style).
 */
@Immutable
public data class IconStyle(val size: Dp, val color: Color) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: IconTheme?): IconStyle =
        if (overrides == null) this else IconStyle(overrides.size ?: size, overrides.color ?: color)
}

/** The icon component theme: overrides for the root icon style outside any component. `null` keeps the skin's. */
@Immutable
public data class IconTheme(val size: Dp? = null, val color: Color? = null)
