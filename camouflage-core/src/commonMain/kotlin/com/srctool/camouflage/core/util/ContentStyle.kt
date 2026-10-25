package com.srctool.camouflage.core.util

import androidx.compose.foundation.Indication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.LocalCamoContentColor
import com.srctool.camouflage.core.provider.LocalCamoIconStyle

/** Sets the content color and icon style for slots a renderer draws, e.g. `onPrimary` inside a solid button. */
@Composable
public fun ProvideContentStyle(color: Color, iconStyle: IconStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCamoContentColor provides color,
        LocalCamoIconStyle provides iconStyle,
        content = content,
    )
}

/**
 * The current skin's feedback as an [Indication], for `Modifier.indication`. Renderers read it from the
 * provider, never from a fixed skin, so `skin.copy(feedback = …)` reaches every renderer.
 */
@Composable
public fun rememberSkinIndication(contentColor: Color, shape: Shape): Indication {
    val feedback = Camouflage.skin.feedback
    val theme = Camouflage.theme
    return remember(feedback, contentColor, shape, theme) { feedback.indication(contentColor, shape, theme) }
}

/** This color at [alpha] of its own opacity, for disabled containers and content. */
public fun Color.disabled(alpha: Float): Color = copy(alpha = this.alpha * alpha)
