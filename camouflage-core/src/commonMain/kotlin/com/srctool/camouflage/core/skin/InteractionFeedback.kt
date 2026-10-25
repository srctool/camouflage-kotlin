package com.srctool.camouflage.core.skin

import androidx.compose.foundation.Indication
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.theme.CamoTheme

/** How a skin responds to press, hover and keyboard focus. It must always make keyboard focus visible. */
@Stable
public interface InteractionFeedback {
    /** A Compose [Indication] drawing press, hover and focus feedback in [contentColor]. */
    public fun indication(contentColor: Color, theme: CamoTheme): Indication
}
