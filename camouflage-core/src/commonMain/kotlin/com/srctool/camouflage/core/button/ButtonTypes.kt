package com.srctool.camouflage.core.button

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family

/** A button's colors for one appearance. [border] is drawn only when set. */
@Immutable
public data class ButtonColors(val container: Color, val content: Color, val border: Color? = null)

/** Overrides for buttons. Every field is optional; `null` keeps the skin's default. */
@Immutable
public data class ButtonTheme(
    val defaultAppearance: Appearance? = null,
    val height: Dp? = null,
    val minWidth: Dp? = null,
    val shape: Shape? = null,
    val padding: PaddingValues? = null,
    val paddingWithIcon: PaddingValues? = null,
    val textStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val iconGap: Dp? = null,
    val borderWidth: Dp? = null,
    val raisedElevation: ElevationLevel? = null,
    val solidColors: ButtonColors? = null,
    val softColors: ButtonColors? = null,
    val raisedColors: ButtonColors? = null,
    val outlineColors: ButtonColors? = null,
    val ghostColors: ButtonColors? = null,
    val linkColors: ButtonColors? = null,
    val disabledContainerAlpha: Float? = null,
    val disabledContentAlpha: Float? = null,
)

/**
 * The resolved button style: every field set. [paddingWithIcon]'s start applies beside a prefix icon and its
 * end beside a suffix icon; the per-appearance colors are for `Tone.Default`.
 */
@Immutable
public data class ButtonStyle(
    val defaultAppearance: Appearance,
    val height: Dp,
    val minWidth: Dp,
    val shape: Shape,
    val padding: PaddingValues,
    val paddingWithIcon: PaddingValues,
    val textStyle: TextStyle,
    val iconSize: Dp,
    val iconGap: Dp,
    val borderWidth: Dp,
    val raisedElevation: ElevationLevel,
    val solidColors: ButtonColors,
    val softColors: ButtonColors,
    val raisedColors: ButtonColors,
    val outlineColors: ButtonColors,
    val ghostColors: ButtonColors,
    val linkColors: ButtonColors,
    val disabledContainerAlpha: Float,
    val disabledContentAlpha: Float,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: ButtonTheme?): ButtonStyle = if (overrides == null) {
        this
    } else {
        ButtonStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            height = overrides.height ?: height,
            minWidth = overrides.minWidth ?: minWidth,
            shape = overrides.shape ?: shape,
            padding = overrides.padding ?: padding,
            paddingWithIcon = overrides.paddingWithIcon ?: paddingWithIcon,
            textStyle = overrides.textStyle ?: textStyle,
            iconSize = overrides.iconSize ?: iconSize,
            iconGap = overrides.iconGap ?: iconGap,
            borderWidth = overrides.borderWidth ?: borderWidth,
            raisedElevation = overrides.raisedElevation ?: raisedElevation,
            solidColors = overrides.solidColors ?: solidColors,
            softColors = overrides.softColors ?: softColors,
            raisedColors = overrides.raisedColors ?: raisedColors,
            outlineColors = overrides.outlineColors ?: outlineColors,
            ghostColors = overrides.ghostColors ?: ghostColors,
            linkColors = overrides.linkColors ?: linkColors,
            disabledContainerAlpha = overrides.disabledContainerAlpha ?: disabledContainerAlpha,
            disabledContentAlpha = overrides.disabledContentAlpha ?: disabledContentAlpha,
        )
    }

    /** The colors for [appearance] in [tone]: this style's for `Tone.Default`, the tone's family otherwise. */
    public fun colorsFor(appearance: Appearance, tone: Tone, theme: CamoTheme): ButtonColors {
        if (tone == Tone.Default) {
            return when (appearance) {
                Appearance.Solid -> solidColors
                Appearance.Soft -> softColors
                Appearance.Raised -> raisedColors
                Appearance.Outline -> outlineColors
                Appearance.Ghost -> ghostColors
                Appearance.Link -> linkColors
            }
        }
        val f = theme.colors.family(tone)
        return when (appearance) {
            Appearance.Solid -> ButtonColors(f.strong, f.onStrong)
            Appearance.Soft -> ButtonColors(f.container, f.onContainer)
            Appearance.Raised -> ButtonColors(raisedColors.container, f.strong, raisedColors.border)
            Appearance.Outline -> ButtonColors(Color.Transparent, f.strong, border = f.strong)
            Appearance.Ghost, Appearance.Link -> ButtonColors(Color.Transparent, f.strong)
        }
    }
}

/** What a button renderer draws. [interactionSource] is for the skin's `Indication`. */
@Immutable
public data class ButtonState(
    val text: String,
    val enabled: Boolean,
    val loading: Boolean,
    val appearance: Appearance,
    val tone: Tone,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** The caller's icons, placed before and after the text. */
@Stable
public class ButtonSlots(
    public val prefixIcon: (@Composable () -> Unit)?,
    public val suffixIcon: (@Composable () -> Unit)?,
)

/** Draws buttons. Apply `modifier` (the component's behavior) to the outermost node, before any visual modifier. */
@Stable
public interface ButtonRenderer {
    /** The complete button style for [theme]. */
    public fun defaults(theme: CamoTheme): ButtonStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(state: ButtonState, slots: ButtonSlots, theme: CamoTheme, style: ButtonStyle, modifier: Modifier)
}
