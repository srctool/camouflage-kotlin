package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** The size and color icons take by default inside a container. Renderers set it for their slots. */
@Immutable
public data class IconStyle(val size: Dp, val color: Color)
