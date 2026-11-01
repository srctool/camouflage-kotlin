package com.srctool.camouflage.core.provider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.environment.CamoBreakpoints
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.LocalCamoEnvironment
import com.srctool.camouflage.core.environment.forEnvironment
import com.srctool.camouflage.core.environment.rememberSystemEnvironment
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.snackbar.ProvideMessenger
import com.srctool.camouflage.core.theme.CamoTheme

/**
 * Puts a skin and a theme into the tree for every `Camo*` component below. A `null` argument inherits the
 * value from the provider above, so nested providers can override just the skin or just the theme.
 *
 * The root provider computes the environment (window size, input, platform, capabilities; ADR-41) and installs
 * the app's `CamoMessenger` (snackbars); any provider can set its own (the overload with `environment`). Each
 * provider applies its theme's adaptive rules for the environment and reduced motion once, and resets the content
 * color and icon style for its subtree. A provider with nothing to provide doesn't throw: the first component
 * that needs the value does, by name.
 */
@Composable
public fun CamouflageProvider(
    skin: CamoSkin? = null,
    theme: CamoTheme? = null,
    content: @Composable () -> Unit,
) {
    ProvideCamouflage(skin, theme, environment = null, reduceMotion = rememberReducedMotion(), content = content)
}

/** A provider that also sets the [environment] for its subtree, instead of inheriting or computing it. */
@Composable
public fun CamouflageProvider(
    skin: CamoSkin? = null,
    theme: CamoTheme? = null,
    environment: CamoEnvironment,
    content: @Composable () -> Unit,
) {
    ProvideCamouflage(skin, theme, environment, reduceMotion = rememberReducedMotion(), content = content)
}

@Composable
internal fun ProvideCamouflage(
    skin: CamoSkin?,
    theme: CamoTheme?,
    environment: CamoEnvironment?,
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val effectiveSkin = skin ?: LocalCamoSkin.current
    // The theme as written, before its rules ran: nested providers adapt it again for their environment.
    val sourceTheme = theme ?: LocalCamoSourceTheme.current
    val parentEnvironment = LocalCamoEnvironment.current
    val effectiveEnvironment = environment
        ?: parentEnvironment
        ?: rememberSystemEnvironment(sourceTheme?.breakpoints ?: CamoBreakpoints())
    val effectiveTheme = remember(sourceTheme, effectiveEnvironment, reduceMotion) {
        sourceTheme?.forEnvironment(effectiveEnvironment)?.let {
            if (reduceMotion) it.copy(motion = it.motion.reduced()) else it
        }
    }
    // The root icon style is the icon component theme: the skin's defaults, then the theme's overrides.
    val rootIcon = remember(effectiveSkin, effectiveTheme) {
        if (effectiveSkin == null || effectiveTheme == null) {
            null
        } else {
            effectiveSkin.icon.defaults(effectiveTheme).merge(effectiveTheme.components.icon)
        }
    }
    CompositionLocalProvider(
        LocalCamoSkin provides effectiveSkin,
        LocalCamoSourceTheme provides sourceTheme,
        LocalCamoTheme provides effectiveTheme,
        LocalCamoEnvironment provides effectiveEnvironment,
        LocalCamoContentColor provides (effectiveTheme?.colors?.onSurface ?: LocalCamoContentColor.current),
        LocalCamoIconStyle provides (rootIcon ?: LocalCamoIconStyle.current),
    ) {
        // The root provider installs the app's messenger; nested ones use it.
        ProvideMessenger(content)
    }
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
