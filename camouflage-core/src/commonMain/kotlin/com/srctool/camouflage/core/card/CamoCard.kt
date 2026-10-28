package com.srctool.camouflage.core.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.util.collectInteractionState

/** A card's colors for one appearance. [border] is drawn only when set. */
@Immutable
public data class CardColors(val container: Color, val content: Color, val border: Color? = null)

/** Overrides for cards. `null` keeps the skin's default. */
@Immutable
public data class CardTheme(
    val defaultAppearance: Appearance? = null,
    val shape: Shape? = null,
    val contentPadding: PaddingValues? = null,
    val solidColors: CardColors? = null,
    val softColors: CardColors? = null,
    val raisedColors: CardColors? = null,
    val outlineColors: CardColors? = null,
    val raisedElevation: ElevationLevel? = null,
    /** The Raised card's depth while hovered (clickable cards only). */
    val hoveredElevation: ElevationLevel? = null,
    val borderWidth: Dp? = null,
    /** The start border a Raised card gets in a status tone. */
    val toneBorderWidth: Dp? = null,
    val selectedBorderColor: Color? = null,
    val selectedBorderWidth: Dp? = null,
    val showSelectedCheck: Boolean? = null,
)

/** The resolved card style: every field set. The per-appearance colors are for `Tone.Default`. */
@Immutable
public data class CardStyle(
    val defaultAppearance: Appearance,
    val shape: Shape,
    val contentPadding: PaddingValues,
    val solidColors: CardColors,
    val softColors: CardColors,
    val raisedColors: CardColors,
    val outlineColors: CardColors,
    val raisedElevation: ElevationLevel,
    val hoveredElevation: ElevationLevel,
    val borderWidth: Dp,
    val toneBorderWidth: Dp,
    val selectedBorderColor: Color,
    val selectedBorderWidth: Dp,
    val showSelectedCheck: Boolean,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: CardTheme?): CardStyle = if (overrides == null) {
        this
    } else {
        CardStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            shape = overrides.shape ?: shape,
            contentPadding = overrides.contentPadding ?: contentPadding,
            solidColors = overrides.solidColors ?: solidColors,
            softColors = overrides.softColors ?: softColors,
            raisedColors = overrides.raisedColors ?: raisedColors,
            outlineColors = overrides.outlineColors ?: outlineColors,
            raisedElevation = overrides.raisedElevation ?: raisedElevation,
            hoveredElevation = overrides.hoveredElevation ?: hoveredElevation,
            borderWidth = overrides.borderWidth ?: borderWidth,
            toneBorderWidth = overrides.toneBorderWidth ?: toneBorderWidth,
            selectedBorderColor = overrides.selectedBorderColor ?: selectedBorderColor,
            selectedBorderWidth = overrides.selectedBorderWidth ?: selectedBorderWidth,
            showSelectedCheck = overrides.showSelectedCheck ?: showSelectedCheck,
        )
    }

    /**
     * The colors for [appearance] in [tone]: this style's for `Tone.Default`, the tone's family otherwise (Solid a
     * strong fill, Soft a container fill, Raised the Raised colors with a tone border, Outline a tone border).
     * `Ghost` and `Link` fall back to `Outline`.
     */
    public fun colorsFor(appearance: Appearance, tone: Tone, theme: CamoTheme): CardColors {
        val a = cardAppearance(appearance)
        if (tone == Tone.Default) {
            return when (a) {
                Appearance.Solid -> solidColors
                Appearance.Soft -> softColors
                Appearance.Raised -> raisedColors
                else -> outlineColors
            }
        }
        val f = theme.colors.family(tone)
        return when (a) {
            Appearance.Solid -> CardColors(f.strong, f.onStrong)
            Appearance.Soft -> CardColors(f.container, f.onContainer)
            Appearance.Raised -> raisedColors.copy(border = f.strong)
            else -> outlineColors.copy(border = f.strong)
        }
    }
}

/** The appearance a card draws for [appearance]: `Ghost` and `Link` fall back to `Outline`. */
public fun cardAppearance(appearance: Appearance): Appearance = when (appearance) {
    Appearance.Ghost, Appearance.Link -> Appearance.Outline
    else -> appearance
}

/**
 * What a card renderer draws. [appearance] is already resolved (never `Ghost` or `Link`). [selected] is `null` for
 * a card that isn't selectable. Draw press feedback only when [clickable].
 */
@Immutable
public data class CardState(
    val appearance: Appearance,
    val tone: Tone,
    val clickable: Boolean,
    val enabled: Boolean,
    val selected: Boolean?,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** The card's content. */
@Stable
public class CardSlots(public val content: @Composable () -> Unit)

/** Draws cards. Apply `modifier` (the component's behavior) to the outermost node, before any visual modifier. */
@Stable
public interface CardRenderer {
    /** The complete card style for [theme]. */
    public fun defaults(theme: CamoTheme): CardStyle

    /** Draws [state] around [slots]. */
    @Composable
    public fun Render(state: CardState, slots: CardSlots, theme: CamoTheme, style: CardStyle, modifier: Modifier)
}

/**
 * A group of related content (a product, an article, a setting) on a surface. With [onClick] the whole card is
 * one button, its content the name; without it the card is plain content. A non-null [selected] makes it a
 * selectable card (plans, payment methods). It has no padding of its own by default, so images can reach its
 * edges: pad the content.
 */
@Composable
public fun CamoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    enabled: Boolean = true,
    selected: Boolean? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoCard")
    val theme = requireTheme("CamoCard")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.card.defaults(theme).merge(theme.components.card) }
    val behavior = when {
        onClick != null -> modifier
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { if (selected != null) this.selected = selected }
        selected != null -> modifier.semantics(mergeDescendants = true) { this.selected = selected }
        else -> modifier
    }
    skin.card.Render(
        CardState(
            cardAppearance(appearance ?: style.defaultAppearance),
            tone,
            clickable = onClick != null,
            enabled = enabled,
            selected = selected,
            interaction = interaction,
            interactionSource = source,
        ),
        CardSlots(content),
        theme,
        style,
        behavior,
    )
}
