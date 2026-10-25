package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skin.IconStyle
import com.srctool.camouflage.core.theme.CamoTheme

// Core's root icon size until the icon renderer's defaults exist (M3, ADR-39): the Minimal value (ADR-24).
private val RootIconSize = 20.dp

/**
 * Puts a skin and a theme into the tree for every `Camo*` component below. A `null` argument inherits the
 * value from the provider above, so nested providers can override just the skin or just the theme.
 *
 * It applies reduced motion to the theme once, and resets the content color and icon style for its subtree.
 * A provider with nothing to provide doesn't throw: the first component that needs the value does, by name.
 */
@Composable
public fun CamouflageProvider(
    skin: CamoSkin? = null,
    theme: CamoTheme? = null,
    content: @Composable () -> Unit,
) {
    ProvideCamouflage(skin, theme, reduceMotion = rememberReducedMotion(), content = content)
}

@Composable
internal fun ProvideCamouflage(
    skin: CamoSkin?,
    theme: CamoTheme?,
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val effectiveSkin = skin ?: LocalCamoSkin.current
    val baseTheme = theme ?: LocalCamoTheme.current
    val effectiveTheme = remember(baseTheme, reduceMotion) {
        if (baseTheme != null && reduceMotion) baseTheme.copy(motion = baseTheme.motion.reduced()) else baseTheme
    }
    val onSurface = effectiveTheme?.colors?.onSurface
    CompositionLocalProvider(
        LocalCamoSkin provides effectiveSkin,
        LocalCamoTheme provides effectiveTheme,
        LocalCamoContentColor provides (onSurface ?: LocalCamoContentColor.current),
        LocalCamoIconStyle provides (onSurface?.let { IconStyle(RootIconSize, it) } ?: LocalCamoIconStyle.current),
        content = content,
    )
}

/**
 * A provider for `@Preview`s, on the theme's `surface`. Core has no default skin (ADR-9), so both are
 * required; skin modules ship their own shortcut, like `MinimalPreview`.
 */
@Composable
public fun CamouflagePreview(skin: CamoSkin, theme: CamoTheme, content: @Composable () -> Unit) {
    CamouflageProvider(skin = skin, theme = theme) {
        Box(Modifier.background(theme.colors.surface)) { content() }
    }
}

/** Whether the platform asks for reduced motion ("Remove animations", "Reduce Motion", `prefers-reduced-motion`). */
@Composable
internal expect fun rememberReducedMotion(): Boolean
