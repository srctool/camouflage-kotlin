package com.srctool.camouflage.core.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.button.DebugChecks
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

private const val HIDDEN_SCALE = 0.6f

/** The FAB sizes. Only Material has three, so a brand picks one in [FabTheme.size] (ADR-23). */
public enum class FabSize { Small, Regular, Large }

/** A FAB's colors for one appearance. */
@Immutable
public data class FabColors(val container: Color, val content: Color)

/** Overrides for FABs. `null` keeps the skin's default. */
@Immutable
public data class FabTheme(
    val defaultAppearance: Appearance? = null,
    val size: FabSize? = null,
    val sizeSmall: Dp? = null,
    val sizeRegular: Dp? = null,
    val sizeLarge: Dp? = null,
    val extendedHeight: Dp? = null,
    val shapeSmall: Shape? = null,
    val shapeRegular: Shape? = null,
    val shapeLarge: Shape? = null,
    val iconSizeRegular: Dp? = null,
    val iconSizeLarge: Dp? = null,
    val iconGap: Dp? = null,
    val extendedPadding: Dp? = null,
    val textStyle: TextStyle? = null,
    val elevation: ElevationLevel? = null,
    val softColors: FabColors? = null,
    val solidColors: FabColors? = null,
    val raisedColors: FabColors? = null,
)

/** The resolved FAB style: every field set. The per-appearance colors are for `Tone.Default`. */
@Immutable
public data class FabStyle(
    val defaultAppearance: Appearance,
    val size: FabSize,
    val sizeSmall: Dp,
    val sizeRegular: Dp,
    val sizeLarge: Dp,
    val extendedHeight: Dp,
    val shapeSmall: Shape,
    val shapeRegular: Shape,
    val shapeLarge: Shape,
    val iconSizeRegular: Dp,
    val iconSizeLarge: Dp,
    val iconGap: Dp,
    val extendedPadding: Dp,
    val textStyle: TextStyle,
    val elevation: ElevationLevel,
    val softColors: FabColors,
    val solidColors: FabColors,
    val raisedColors: FabColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: FabTheme?): FabStyle = if (overrides == null) {
        this
    } else {
        FabStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            size = overrides.size ?: size,
            sizeSmall = overrides.sizeSmall ?: sizeSmall,
            sizeRegular = overrides.sizeRegular ?: sizeRegular,
            sizeLarge = overrides.sizeLarge ?: sizeLarge,
            extendedHeight = overrides.extendedHeight ?: extendedHeight,
            shapeSmall = overrides.shapeSmall ?: shapeSmall,
            shapeRegular = overrides.shapeRegular ?: shapeRegular,
            shapeLarge = overrides.shapeLarge ?: shapeLarge,
            iconSizeRegular = overrides.iconSizeRegular ?: iconSizeRegular,
            iconSizeLarge = overrides.iconSizeLarge ?: iconSizeLarge,
            iconGap = overrides.iconGap ?: iconGap,
            extendedPadding = overrides.extendedPadding ?: extendedPadding,
            textStyle = overrides.textStyle ?: textStyle,
            elevation = overrides.elevation ?: elevation,
            softColors = overrides.softColors ?: softColors,
            solidColors = overrides.solidColors ?: solidColors,
            raisedColors = overrides.raisedColors ?: raisedColors,
        )
    }

    /** The container size of the chosen [size]. */
    public val containerSize: Dp
        get() = when (size) {
            FabSize.Small -> sizeSmall
            FabSize.Regular -> sizeRegular
            FabSize.Large -> sizeLarge
        }

    /** The corner shape of the chosen [size]. */
    public val shape: Shape
        get() = when (size) {
            FabSize.Small -> shapeSmall
            FabSize.Regular -> shapeRegular
            FabSize.Large -> shapeLarge
        }

    /** The icon size of the chosen [size] (Small uses the regular one). */
    public val iconSize: Dp get() = if (size == FabSize.Large) iconSizeLarge else iconSizeRegular

    /**
     * The colors for a FAB in [state]: this style's colors for `Tone.Default`, and the tone's family otherwise
     * (Solid a strong fill, Soft a container fill, Raised a lifted surface with the tone's strong content).
     */
    public fun colorsFor(state: FabState, theme: CamoTheme): FabColors {
        val f = theme.colors.family(state.tone)
        return when {
            state.tone == Tone.Default -> when (state.appearance) {
                Appearance.Solid -> solidColors
                Appearance.Raised -> raisedColors
                else -> softColors
            }
            state.appearance == Appearance.Solid -> FabColors(f.strong, f.onStrong)
            state.appearance == Appearance.Raised -> FabColors(raisedColors.container, f.strong)
            else -> FabColors(f.container, f.onContainer)
        }
    }
}

/** The appearance a FAB draws for [appearance]: Soft, Solid or Raised (the others fall back to Soft). */
public fun fabAppearance(appearance: Appearance): Appearance = when (appearance) {
    Appearance.Solid, Appearance.Raised -> appearance
    else -> Appearance.Soft
}

/**
 * What a FAB renderer draws. [appearance] is already resolved (Soft, Solid or Raised). A FAB with a [text] is
 * extended; while it isn't [expanded] only the icon shows, and the renderer animates the width between the two.
 * Core hides the FAB itself (`visible = false`), so the renderer only draws a visible one.
 */
@Immutable
public data class FabState(
    val text: String?,
    val expanded: Boolean,
    val appearance: Appearance,
    val tone: Tone,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
) {
    /** Whether this FAB has a label (shown while [expanded]). */
    val extended: Boolean get() = text != null
}

/** A FAB's icon. */
@Stable
public class FabSlots(public val icon: @Composable () -> Unit)

/** Draws FABs. Apply `modifier` (the component's behavior) to the outermost node, before any visual modifier. */
@Stable
public interface FabRenderer {
    /** The complete FAB style for [theme]. */
    public fun defaults(theme: CamoTheme): FabStyle

    /** Draws [state] with [slots], with press feedback. */
    @Composable
    public fun Render(state: FabState, slots: FabSlots, theme: CamoTheme, style: FabStyle, modifier: Modifier)
}

/**
 * The screen's one main action: an [icon] button, or an extended FAB with a [text] label. The accessible name is
 * the [text] when there is one and [contentDescription] otherwise, in both states, so collapsing it while the
 * list scrolls (`expanded = false`) doesn't rename it. `visible = false` scales it out and takes it out of the
 * tree, so a hidden FAB can't be focused (pair it with `rememberHideOnScroll`). Placement is the app's layout.
 */
@Composable
public fun CamoFab(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    expanded: Boolean = true,
    visible: Boolean = true,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    if (DebugChecks.enabled) check(contentDescription.isNotBlank()) { "CamoFab needs a contentDescription." }
    val skin = requireSkin("CamoFab")
    val theme = requireTheme("CamoFab")
    val style = remember(skin, theme) { skin.fab.defaults(theme).merge(theme.components.fab) }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val spec = tween<Float>(theme.motion.durationMedium, easing = theme.motion.easingStandard)
    val name = text ?: contentDescription
    val behavior = Modifier
        .semantics { this.contentDescription = name }
        .clickable(source, indication = null, role = Role.Button, onClick = onClick)
        .minimumTouchTarget()
    AnimatedVisibility(
        visible,
        modifier,
        enter = scaleIn(spec, HIDDEN_SCALE) + fadeIn(spec),
        exit = scaleOut(spec, HIDDEN_SCALE) + fadeOut(spec),
    ) {
        skin.fab.Render(
            FabState(text, expanded, fabAppearance(appearance ?: style.defaultAppearance), tone, interaction, source),
            FabSlots(icon),
            theme,
            style,
            behavior,
        )
    }
}
