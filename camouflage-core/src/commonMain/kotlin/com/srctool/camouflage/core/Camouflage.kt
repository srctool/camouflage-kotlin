package com.srctool.camouflage.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skin.IconStyle
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoElevation
import com.srctool.camouflage.core.theme.CamoMotion
import com.srctool.camouflage.core.theme.CamoShapes
import com.srctool.camouflage.core.theme.CamoSpacing
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTokenSet
import com.srctool.camouflage.core.theme.CamoTypography

/** The nearest [CamouflageProvider]'s values. Every accessor fails fast when there's no provider. */
public object Camouflage {
    /** The nearest effective skin. */
    public val skin: CamoSkin
        @Composable @ReadOnlyComposable get() = requireSkin("Camouflage.skin")

    /** The nearest effective theme, with reduced motion already applied. */
    public val theme: CamoTheme
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.theme")

    /** `theme.colors`. */
    public val colors: CamoColors
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.colors").colors

    /** `theme.typography`. */
    public val typography: CamoTypography
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.typography").typography

    /** `theme.shapes`. */
    public val shapes: CamoShapes
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.shapes").shapes

    /** `theme.spacing`. */
    public val spacing: CamoSpacing
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.spacing").spacing

    /** `theme.elevation`. */
    public val elevation: CamoElevation
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.elevation").elevation

    /** `theme.motion`. */
    public val motion: CamoMotion
        @Composable @ReadOnlyComposable get() = requireTheme("Camouflage.motion").motion

    /** The content color set by the nearest container (`onSurface` at a provider). */
    public val contentColor: Color
        @Composable @ReadOnlyComposable get() = LocalCamoContentColor.current

    /** The icon size and color set by the nearest container. */
    public val iconStyle: IconStyle
        @Composable @ReadOnlyComposable get() = LocalCamoIconStyle.current
            ?: throw CamouflageMissingProviderException("Camouflage.iconStyle", "icon style")

    /** The skin, or `null` without a provider. For family packages only; `Camo*` components fail fast. */
    public val skinOrNull: CamoSkin?
        @Composable @ReadOnlyComposable get() = LocalCamoSkin.current

    /** The theme, or `null` without a provider. For family packages only; `Camo*` components fail fast. */
    public val themeOrNull: CamoTheme?
        @Composable @ReadOnlyComposable get() = LocalCamoTheme.current

    /** The theme's token set of type [T] (ADR-32). Fails fast, naming the set, when the theme lacks it. */
    @Composable
    @ReadOnlyComposable
    public inline fun <reified T : CamoTokenSet<T>> tokens(): T =
        requireTheme("Camouflage.tokens").tokens<T>() ?: missingTokenSet(T::class.simpleName)
}

/** Thrown when a component reads the skin or theme with no provider above it that sets it. */
public class CamouflageMissingProviderException(component: String, missing: String) : IllegalStateException(
    "No CamouflageProvider found above $component: missing $missing. " +
        "Wrap your app in CamouflageProvider(skin = …, theme = …).",
)

/** The skin for [component]; the error names the component. */
@Composable
@ReadOnlyComposable
@PublishedApi
internal fun requireSkin(component: String): CamoSkin =
    LocalCamoSkin.current ?: throw CamouflageMissingProviderException(component, "skin")

/** The theme for [component]; the error names the component. */
@Composable
@ReadOnlyComposable
@PublishedApi
internal fun requireTheme(component: String): CamoTheme =
    LocalCamoTheme.current ?: throw CamouflageMissingProviderException(component, "theme")

@PublishedApi
internal fun missingTokenSet(name: String?): Nothing =
    throw CamouflageMissingProviderException("Camouflage.tokens", "token set $name in the theme")
