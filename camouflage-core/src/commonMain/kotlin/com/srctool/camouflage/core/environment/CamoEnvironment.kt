package com.srctool.camouflage.core.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.theme.CamoTheme

/** The window's width class, from the theme's [CamoBreakpoints]: a phone, a tablet or foldable, a desktop window. */
public enum class WindowSizeClass { Compact, Medium, Expanded, Large }

/** How the user points: fingers (`Touch`, larger targets) or a mouse or trackpad (`Pointer`, hover, denser UI). */
public enum class InputMode { Touch, Pointer }

/** The operating system the app runs on (`Web` for a browser, whatever the OS underneath). */
public enum class CamoPlatform { Android, Ios, MacOs, Windows, Linux, Web }

/**
 * What the platform can draw and what the user asked for (ADR-42). Skins pick a material's fallback from it:
 * [reduceTransparency] always wins, then [backdropBlur] / [runtimeShaders] / [windowMaterial] decide how far
 * down the chain a surface goes.
 */
@Immutable
public data class CamoCapabilities(
    /** A blur of what's behind a surface (Acrylic, frosted bars). */
    val backdropBlur: Boolean,
    /** Custom fragment shaders (Liquid Glass's refraction). */
    val runtimeShaders: Boolean,
    /** A system window backdrop such as Windows 11's Mica. */
    val windowMaterial: Boolean,
    /** The user asked for opaque surfaces (Reduce Transparency, `prefers-reduced-transparency`). */
    val reduceTransparency: Boolean,
)

/**
 * Where the app runs (ADR-41): the window's [windowSize], the [input] in use, the [platform] and its
 * [capabilities]. The root `CamouflageProvider` computes it; any provider can override it. Adaptive themes and
 * skins read it, so a theme can get denser with a pointer or a skin follow a platform convention.
 */
@Immutable
public data class CamoEnvironment(
    val windowSize: WindowSizeClass,
    val input: InputMode,
    val platform: CamoPlatform,
    val capabilities: CamoCapabilities,
)

/**
 * The window widths where each [WindowSizeClass] starts: below [medium] is `Compact`. The defaults are Material's
 * window size classes (600 / 840 / 1200); a brand can move them in its theme.
 */
@Immutable
public data class CamoBreakpoints(
    val medium: Dp = 600.dp,
    val expanded: Dp = 840.dp,
    val large: Dp = 1200.dp,
) {
    /** The class of a window [width] wide. */
    public fun classify(width: Dp): WindowSizeClass = when {
        width >= large -> WindowSizeClass.Large
        width >= expanded -> WindowSizeClass.Expanded
        width >= medium -> WindowSizeClass.Medium
        else -> WindowSizeClass.Compact
    }
}

/**
 * A theme rule for an environment (ADR-41): returns the theme to use there, usually a copy with denser component
 * themes on `Pointer` or another type scale on `Expanded`. Set it with [adapt].
 */
public fun interface ThemeAdaptation {
    /** The theme for [environment], from [theme]. */
    public fun adapt(theme: CamoTheme, environment: CamoEnvironment): CamoTheme
}

/**
 * This theme with an adaptive [rule] added after any it already has. The provider applies the rules once per
 * environment change, so components still read one plain theme:
 * `minimalLight.adapt { env -> if (env.input == InputMode.Pointer) copy(components = dense) else this }`.
 */
public fun CamoTheme.adapt(rule: CamoTheme.(CamoEnvironment) -> CamoTheme): CamoTheme {
    val before = adaptation
    return copy(
        adaptation = ThemeAdaptation { theme, env ->
            val first = before?.adapt(theme, env) ?: theme
            first.rule(env)
        },
    )
}

/**
 * This theme for [environment]: its rules applied, the environment attached. A theme already adapted for that
 * environment (one read from a provider and passed on) is returned as is, so its rules never run twice.
 */
public fun CamoTheme.forEnvironment(environment: CamoEnvironment): CamoTheme =
    if (this.environment == environment) {
        this
    } else {
        (adaptation?.adapt(this, environment) ?: this).copy(environment = environment, adaptation = adaptation)
    }

internal val LocalCamoEnvironment = staticCompositionLocalOf<CamoEnvironment?> { null }

/** The environment of the nearest provider; outside one, the one the root would compute. */
public val Camouflage.environment: CamoEnvironment
    @Composable get() = LocalCamoEnvironment.current ?: rememberSystemEnvironment(CamoBreakpoints())

/**
 * The environment the platform reports: the window's width class by [breakpoints], the platform's usual input,
 * the platform and its capabilities. It follows window resizes.
 */
@Composable
public fun rememberSystemEnvironment(breakpoints: CamoBreakpoints): CamoEnvironment {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val capabilities = rememberCapabilities()
    val platform = remember { currentPlatform() }
    val windowSize = breakpoints.classify(width)
    return remember(windowSize, platform, capabilities) {
        CamoEnvironment(windowSize, defaultInput(platform), platform, capabilities)
    }
}

/** The usual input on [platform]: touch on phones and tablets, a pointer on desktops and in browsers. */
public fun defaultInput(platform: CamoPlatform): InputMode = when (platform) {
    CamoPlatform.Android, CamoPlatform.Ios -> InputMode.Touch
    else -> InputMode.Pointer
}

/** The platform this code runs on. */
internal expect fun currentPlatform(): CamoPlatform

/** The platform's capabilities and the user's transparency setting, updated when the setting changes. */
@Composable
internal expect fun rememberCapabilities(): CamoCapabilities
