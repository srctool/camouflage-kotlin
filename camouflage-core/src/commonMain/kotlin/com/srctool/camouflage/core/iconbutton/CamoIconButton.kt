package com.srctool.camouflage.core.iconbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.badge.badgeAnchor
import com.srctool.camouflage.core.button.BUSY
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.ToneColors
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.tooltip.TooltipBox
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

/** An icon button's colors for one appearance. [border] is drawn only when set. */
@Immutable
public data class IconButtonColors(val container: Color, val content: Color, val border: Color? = null)

/** Overrides for icon buttons. `null` keeps the skin's default. */
@Immutable
public data class IconButtonTheme(
    val defaultAppearance: Appearance? = null,
    val containerSize: Dp? = null,
    val shape: Shape? = null,
    val iconSize: Dp? = null,
    val borderWidth: Dp? = null,
    val solidColors: IconButtonColors? = null,
    val softColors: IconButtonColors? = null,
    val outlineColors: IconButtonColors? = null,
    val ghostColors: IconButtonColors? = null,
    val checkedColors: IconButtonColors? = null,
    val disabledContainerAlpha: Float? = null,
    val disabledContentAlpha: Float? = null,
)

/** The resolved icon button style: every field set. The per-appearance colors are for `Tone.Default`. */
@Immutable
public data class IconButtonStyle(
    val defaultAppearance: Appearance,
    val containerSize: Dp,
    val shape: Shape,
    val iconSize: Dp,
    val borderWidth: Dp,
    val solidColors: IconButtonColors,
    val softColors: IconButtonColors,
    val outlineColors: IconButtonColors,
    val ghostColors: IconButtonColors,
    val checkedColors: IconButtonColors,
    val disabledContainerAlpha: Float,
    val disabledContentAlpha: Float,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: IconButtonTheme?): IconButtonStyle = if (overrides == null) {
        this
    } else {
        IconButtonStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            containerSize = overrides.containerSize ?: containerSize,
            shape = overrides.shape ?: shape,
            iconSize = overrides.iconSize ?: iconSize,
            borderWidth = overrides.borderWidth ?: borderWidth,
            solidColors = overrides.solidColors ?: solidColors,
            softColors = overrides.softColors ?: softColors,
            outlineColors = overrides.outlineColors ?: outlineColors,
            ghostColors = overrides.ghostColors ?: ghostColors,
            checkedColors = overrides.checkedColors ?: checkedColors,
            disabledContainerAlpha = overrides.disabledContainerAlpha ?: disabledContainerAlpha,
            disabledContentAlpha = overrides.disabledContentAlpha ?: disabledContentAlpha,
        )
    }

    /**
     * The colors for [appearance] in [tone], or [checkedColors] when [checked]. Icon buttons have no `Raised`
     * or `Link`: core maps them to `Soft` and `Ghost` before this is called.
     */
    public fun colorsFor(appearance: Appearance, tone: Tone, checked: Boolean, theme: CamoTheme): IconButtonColors =
        when {
            checked -> checkedColors
            tone == Tone.Default -> when (appearance) {
                Appearance.Solid -> solidColors
                Appearance.Soft, Appearance.Raised -> softColors
                Appearance.Outline -> outlineColors
                Appearance.Ghost, Appearance.Link -> ghostColors
            }
            else -> toneColors(appearance, theme.colors.family(tone))
        }

    private fun toneColors(appearance: Appearance, f: ToneColors) = when (appearance) {
        Appearance.Solid -> IconButtonColors(f.strong, f.onStrong)
        Appearance.Soft, Appearance.Raised -> IconButtonColors(f.container, f.onContainer)
        Appearance.Outline -> IconButtonColors(Color.Transparent, f.strong, border = f.strong)
        Appearance.Ghost, Appearance.Link -> IconButtonColors(Color.Transparent, f.strong)
    }
}

/** What an icon button renderer draws. `checked != null` makes it a toggle. */
@Immutable
public data class IconButtonState(
    val enabled: Boolean,
    val checked: Boolean?,
    val loading: Boolean,
    val appearance: Appearance,
    val tone: Tone,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** The caller's icon. Core hides its own semantics: the button carries the label. */
@Stable
public class IconButtonSlots(public val icon: @Composable () -> Unit)

/** Draws icon buttons. Apply `modifier` (the component's behavior) to the outermost node. */
@Stable
public interface IconButtonRenderer {
    /** The complete icon button style for [theme]. */
    public fun defaults(theme: CamoTheme): IconButtonStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(
        state: IconButtonState,
        slots: IconButtonSlots,
        theme: CamoTheme,
        style: IconButtonStyle,
        modifier: Modifier,
    )
}

/**
 * An icon-only button. [contentDescription] names the action ("Close", "Add to favorites"), not the icon.
 * A non-null [checked] makes it a toggle; [tooltip] shows on hover, keyboard focus and long press.
 */
@Composable
public fun CamoIconButton(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    enabled: Boolean = true,
    checked: Boolean? = null,
    loading: Boolean = false,
    tooltip: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    if (tooltip == null) {
        IconButtonImpl(icon, contentDescription, onClick, modifier, appearance, tone, enabled, checked, loading,
            interactionSource)
    } else {
        TooltipBox(tooltip, enabled = true, anchorLabel = contentDescription, modifier = modifier) {
            IconButtonImpl(icon, contentDescription, onClick, Modifier, appearance, tone, enabled, checked, loading,
                interactionSource)
        }
    }
}

@Composable
private fun IconButtonImpl(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    appearance: Appearance?,
    tone: Tone,
    enabled: Boolean,
    checked: Boolean?,
    loading: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    val skin = requireSkin("CamoIconButton")
    val theme = requireTheme("CamoIconButton")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.iconButton.defaults(theme).merge(theme.components.iconButton) }
    val resolved = when (val a = appearance ?: style.defaultAppearance) {
        Appearance.Raised -> Appearance.Soft
        Appearance.Link -> Appearance.Ghost
        else -> a
    }
    val behavior = modifier
        .badgeAnchor()
        .semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
            if (checked != null) toggleableState = ToggleableState(checked)
            if (loading && enabled) stateDescription = BUSY
        }
        .clickable(source, indication = null, enabled = enabled, role = Role.Button) { if (!loading) onClick() }
        .minimumTouchTarget()
    skin.iconButton.Render(
        state = IconButtonState(enabled, checked, loading && enabled, resolved, tone, interaction, source),
        slots = IconButtonSlots(icon),
        theme = theme,
        style = style,
        modifier = behavior,
    )
}
