package com.srctool.camouflage.core.banner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.button.ButtonColors
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family

/** The default status icon per tone, so a brand can use its own icon set. Snackbars use the same icons. */
@Immutable
public data class ToneIcons(
    val info: IconSource,
    val success: IconSource,
    val warning: IconSource,
    val error: IconSource,
)

/** The icon for [tone], or `null` for `Tone.Default`. */
public fun ToneIcons.ofTone(tone: Tone): IconSource? = when (tone) {
    Tone.Info -> info
    Tone.Success -> success
    Tone.Warning -> warning
    Tone.Error -> error
    Tone.Default -> null
}

/** A banner's colors: [accent] is the tone's strong color (actions, the accent border, an outline). */
@Immutable
public data class BannerColors(
    val container: Color,
    val content: Color,
    val accent: Color,
    val border: Color? = null,
)

/** The words banners say themselves: the ✕ button's name. */
@Immutable
public data class BannerStrings(val dismiss: String = "Dismiss")

/** Overrides for banners. `null` keeps the skin's default. */
@Immutable
public data class BannerTheme(
    val defaultAppearance: Appearance? = null,
    val shape: Shape? = null,
    val padding: PaddingValues? = null,
    val titleStyle: TextStyle? = null,
    val messageStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    /** A start border in the tone's strong color (Carbon style). */
    val accentBorder: Boolean? = null,
    val accentWidth: Dp? = null,
    val borderWidth: Dp? = null,
    val toneIcons: ToneIcons? = null,
    val dismissIcon: IconSource? = null,
    val strings: BannerStrings? = null,
)

/** The resolved banner style: every field set. */
@Immutable
public data class BannerStyle(
    val defaultAppearance: Appearance,
    val shape: Shape,
    val padding: PaddingValues,
    val titleStyle: TextStyle,
    val messageStyle: TextStyle,
    val iconSize: Dp,
    val accentBorder: Boolean,
    val accentWidth: Dp,
    val borderWidth: Dp,
    val toneIcons: ToneIcons,
    val dismissIcon: IconSource,
    val strings: BannerStrings = BannerStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: BannerTheme?): BannerStyle = if (overrides == null) {
        this
    } else {
        BannerStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            shape = overrides.shape ?: shape,
            padding = overrides.padding ?: padding,
            titleStyle = overrides.titleStyle ?: titleStyle,
            messageStyle = overrides.messageStyle ?: messageStyle,
            iconSize = overrides.iconSize ?: iconSize,
            accentBorder = overrides.accentBorder ?: accentBorder,
            accentWidth = overrides.accentWidth ?: accentWidth,
            borderWidth = overrides.borderWidth ?: borderWidth,
            toneIcons = overrides.toneIcons ?: toneIcons,
            dismissIcon = overrides.dismissIcon ?: dismissIcon,
            strings = overrides.strings ?: strings,
        )
    }

    /**
     * The colors for [appearance] in [tone] (`Tone.Default` is the primary family): Soft the tone's container,
     * Solid its strong fill, Outline the surface with a border in the strong color.
     */
    public fun colorsFor(appearance: Appearance, tone: Tone, theme: CamoTheme): BannerColors {
        val f = theme.colors.family(tone)
        return when (appearance) {
            Appearance.Solid -> BannerColors(f.strong, f.onStrong, f.onStrong)
            Appearance.Outline -> BannerColors(theme.colors.surface, theme.colors.onSurface, f.strong, f.strong)
            else -> BannerColors(f.container, f.onContainer, f.strong)
        }
    }
}

/** The appearance a banner draws for [appearance]: Soft, Solid or Outline (the others fall back to Soft). */
public fun bannerAppearance(appearance: Appearance): Appearance = when (appearance) {
    Appearance.Solid, Appearance.Outline -> appearance
    else -> Appearance.Soft
}

/** What a banner renderer draws. [appearance] is resolved (Soft, Solid or Outline). */
@Immutable
public data class BannerState(
    val title: String?,
    val message: String,
    val tone: Tone,
    val appearance: Appearance,
    val dismissible: Boolean,
)

/**
 * A banner's parts: the status [icon] (`null` for none), the app's [actions] (usually Ghost `CamoButton`s; the
 * renderer lays them out, below the texts or at the end), and the [dismiss] ✕ core builds when it's dismissible.
 */
@Stable
public class BannerSlots(
    public val icon: (@Composable () -> Unit)?,
    public val actions: (@Composable RowScope.() -> Unit)?,
    public val dismiss: (@Composable () -> Unit)?,
)

/** Draws banners. Apply `modifier` (core's semantics) to the outermost node. */
@Stable
public interface BannerRenderer {
    /** The complete banner style for [theme]. */
    public fun defaults(theme: CamoTheme): BannerStyle

    /**
     * Draws [state] with [slots]. Provide the texts' color to the slots, and the accent color around the actions:
     * core gives their Ghost and Link buttons that color.
     */
    @Composable
    public fun Render(state: BannerState, slots: BannerSlots, theme: CamoTheme, style: BannerStyle, modifier: Modifier)
}

/**
 * An inline message that stays in the page until it's resolved ("Couldn't sync · Retry", a form's error summary),
 * in a [tone] with its status icon (the theme's by default; `icon = {}` shows none). [actions] are usually Ghost
 * `CamoButton`s, at most two, and take the banner's accent color; [onDismiss] adds a ✕. Tone isn't carried by
 * color alone: the icon and the texts carry it too. A warning or error banner that appears after the page
 * loaded is announced.
 */
@Composable
public fun CamoBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Info,
    title: String? = null,
    icon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    appearance: Appearance? = null,
) {
    val skin = requireSkin("CamoBanner")
    val theme = requireTheme("CamoBanner")
    val style = remember(skin, theme) { skin.banner.defaults(theme).merge(theme.components.banner) }
    val toneIcon = style.toneIcons.ofTone(tone)
    val shownIcon = icon ?: toneIcon?.let { { CamoIcon(it, contentDescription = null) } }
    val dismiss: (@Composable () -> Unit)? = onDismiss?.let {
        { CamoFieldAction({ CamoIcon(style.dismissIcon, null) }, style.strings.dismiss, it) }
    }
    // Ghost and Link buttons among the actions take the accent the renderer provides: the tone on a Soft or
    // Outline banner, the on-color on a Solid one.
    val tonedActions: (@Composable RowScope.() -> Unit)? = actions?.let { content ->
        {
            val accent = Camouflage.contentColor
            val buttons = (theme.components.button ?: ButtonTheme()).copy(
                ghostColors = ButtonColors(Color.Transparent, accent),
                linkColors = ButtonColors(Color.Transparent, accent),
            )
            CamouflageProvider(theme = theme.copy(components = theme.components.copy(button = buttons))) { content() }
        }
    }
    val alert = tone == Tone.Error || tone == Tone.Warning
    // The texts and icon read as one node; the action buttons and the ✕ stay their own.
    val semantics = modifier.semantics(mergeDescendants = true) { if (alert) liveRegion = LiveRegionMode.Polite }
    skin.banner.Render(
        BannerState(title, message, tone, bannerAppearance(appearance ?: style.defaultAppearance), onDismiss != null),
        BannerSlots(shownIcon, tonedActions, dismiss),
        theme,
        style,
        semantics,
    )
}
