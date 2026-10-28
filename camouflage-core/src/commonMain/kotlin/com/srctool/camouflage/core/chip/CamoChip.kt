package com.srctool.camouflage.core.chip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

/** A chip's colors for one appearance or for the selected state. [border] is drawn only when set. */
@Immutable
public data class ChipColors(val container: Color, val content: Color, val border: Color? = null)

/** The words chips say themselves. [remove] names an input chip's remove button from the chip's label. */
@Immutable
public data class ChipStrings(val remove: (label: String) -> String = { "Remove $it" })

/** Overrides for chips. `null` keeps the skin's default. */
@Immutable
public data class ChipTheme(
    val defaultAppearance: Appearance? = null,
    val height: Dp? = null,
    val shape: Shape? = null,
    val padding: PaddingValues? = null,
    /** The padding when the chip shows a leading icon, a check or a remove button (usually tighter). */
    val iconPadding: PaddingValues? = null,
    val iconGap: Dp? = null,
    val textStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val borderWidth: Dp? = null,
    val showCheckWhenSelected: Boolean? = null,
    val outlineColors: ChipColors? = null,
    val softColors: ChipColors? = null,
    val solidColors: ChipColors? = null,
    val selectedColors: ChipColors? = null,
    val groupSpacing: Dp? = null,
    val checkIcon: IconSource? = null,
    val removeIcon: IconSource? = null,
    val strings: ChipStrings? = null,
)

/** The resolved chip style: every field set. The per-appearance colors are for `Tone.Default`. */
@Immutable
public data class ChipStyle(
    val defaultAppearance: Appearance,
    val height: Dp,
    val shape: Shape,
    val padding: PaddingValues,
    val iconPadding: PaddingValues,
    val iconGap: Dp,
    val textStyle: TextStyle,
    val iconSize: Dp,
    val borderWidth: Dp,
    val showCheckWhenSelected: Boolean,
    val outlineColors: ChipColors,
    val softColors: ChipColors,
    val solidColors: ChipColors,
    val selectedColors: ChipColors,
    val groupSpacing: Dp,
    val checkIcon: IconSource,
    val removeIcon: IconSource,
    val strings: ChipStrings = ChipStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: ChipTheme?): ChipStyle = if (overrides == null) {
        this
    } else {
        ChipStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            height = overrides.height ?: height,
            shape = overrides.shape ?: shape,
            padding = overrides.padding ?: padding,
            iconPadding = overrides.iconPadding ?: iconPadding,
            iconGap = overrides.iconGap ?: iconGap,
            textStyle = overrides.textStyle ?: textStyle,
            iconSize = overrides.iconSize ?: iconSize,
            borderWidth = overrides.borderWidth ?: borderWidth,
            showCheckWhenSelected = overrides.showCheckWhenSelected ?: showCheckWhenSelected,
            outlineColors = overrides.outlineColors ?: outlineColors,
            softColors = overrides.softColors ?: softColors,
            solidColors = overrides.solidColors ?: solidColors,
            selectedColors = overrides.selectedColors ?: selectedColors,
            groupSpacing = overrides.groupSpacing ?: groupSpacing,
            checkIcon = overrides.checkIcon ?: checkIcon,
            removeIcon = overrides.removeIcon ?: removeIcon,
            strings = overrides.strings ?: strings,
        )
    }

    /**
     * The colors for a chip in [state]: the selected colors for a selected filter chip, this style's colors for
     * `Tone.Default`, and the tone's family otherwise (Solid a strong fill, Soft a container fill, Outline the
     * tone's strong color for the border and text).
     */
    public fun colorsFor(state: ChipState, theme: CamoTheme): ChipColors {
        val f = theme.colors.family(state.tone)
        return when {
            state.selected == true -> selectedColors
            state.tone == Tone.Default -> when (state.appearance) {
                Appearance.Solid -> solidColors
                Appearance.Soft -> softColors
                else -> outlineColors
            }
            state.appearance == Appearance.Solid -> ChipColors(f.strong, f.onStrong)
            state.appearance == Appearance.Soft -> ChipColors(f.container, f.onContainer)
            else -> outlineColors.copy(content = f.strong, border = f.strong)
        }
    }
}

/** The appearance a chip draws for [appearance]: Outline, Soft or Solid (the others fall back to Outline). */
public fun chipAppearance(appearance: Appearance): Appearance = when (appearance) {
    Appearance.Solid, Appearance.Soft -> appearance
    else -> Appearance.Outline
}

/** Which chip a [CamoChip] call makes, decided by the callbacks it passes. */
public enum class ChipKind {
    /** `onClick` only: an action or suggestion, a button. */
    Action,

    /** `selected` set: a filter that toggles. */
    Filter,

    /** `onRemove` set (without `selected`): an entered value with a remove button. */
    Input,

    /** No callbacks: a static tag, text only. */
    Tag,
}

/**
 * What a chip renderer draws. [appearance] is already resolved (Outline, Soft or Solid). [selected] is `null`
 * for chips that aren't filters. Draw press feedback for every kind but [ChipKind.Tag].
 */
@Immutable
public data class ChipState(
    val label: String,
    val kind: ChipKind,
    val selected: Boolean?,
    val appearance: Appearance,
    val tone: Tone,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/**
 * A chip's parts besides its label: the app's [leading] icon or avatar, and the [remove] button core builds
 * for input chips (its own focusable button; the renderer only places it, after the label).
 */
@Stable
public class ChipSlots(
    public val leading: (@Composable () -> Unit)?,
    public val remove: (@Composable () -> Unit)?,
)

/** Draws chips. Apply `modifier` (the component's behavior) to the outermost node, before any visual modifier. */
@Stable
public interface ChipRenderer {
    /** The complete chip style for [theme]. */
    public fun defaults(theme: CamoTheme): ChipStyle

    /** Draws [state] with [slots]. A selected filter shows [ChipStyle.checkIcon] when the style asks. */
    @Composable
    public fun Render(state: ChipState, slots: ChipSlots, theme: CamoTheme, style: ChipStyle, modifier: Modifier)
}

/** The kind a chip with these callbacks is. */
public fun chipKind(onClick: (() -> Unit)?, selected: Boolean?, onRemove: (() -> Unit)?): ChipKind = when {
    selected != null -> ChipKind.Filter
    onRemove != null -> ChipKind.Input
    onClick != null -> ChipKind.Action
    else -> ChipKind.Tag
}

/**
 * A compact element whose callbacks decide its kind: a filter when [selected] is set (it toggles through
 * [onClick]), an input chip with a remove button when [onRemove] is set, an action chip with [onClick] only,
 * and a static tag with none (a status word in a [tone]: `Soft` + `Success` "Paid"). A focused input chip is
 * also removed with Backspace or Delete. Counts and dots are `CamoBadge`s, not chips.
 */
@Composable
public fun CamoChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    onRemove: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    ChipImpl(label, modifier, onClick, selected, onRemove, leading, appearance, tone, enabled, interactionSource, null)
}

// The chip, optionally as one radio of a single-choice group (`groupRole`), which `CamoChipGroup` uses.
@Composable
internal fun ChipImpl(
    label: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    selected: Boolean?,
    onRemove: (() -> Unit)?,
    leading: (@Composable () -> Unit)?,
    appearance: Appearance?,
    tone: Tone,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    groupRole: Role?,
) {
    val skin = requireSkin("CamoChip")
    val theme = requireTheme("CamoChip")
    val style = remember(skin, theme) { skin.chip.defaults(theme).merge(theme.components.chip) }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val kind = chipKind(onClick, selected, onRemove)
    val behavior = chipBehavior(modifier, onClick, selected, onRemove, enabled, source, groupRole)
    val remove: (@Composable () -> Unit)? = onRemove?.let {
        {
            CamoFieldAction(
                icon = { CamoIcon(style.removeIcon, contentDescription = null) },
                contentDescription = style.strings.remove(label),
                onClick = it,
                enabled = enabled,
            )
        }
    }
    skin.chip.Render(
        ChipState(
            label = label,
            kind = kind,
            selected = selected,
            appearance = chipAppearance(appearance ?: style.defaultAppearance),
            tone = tone,
            enabled = enabled,
            interaction = interaction,
            interactionSource = source,
        ),
        ChipSlots(leading, remove),
        theme,
        style,
        behavior,
    )
}

// The chip's behavior for its kind: toggle, radio, button, focusable input chip or plain merged text, with the
// 48 target for interactive chips and Delete / Backspace removal above the focus target.
private fun chipBehavior(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    selected: Boolean?,
    onRemove: (() -> Unit)?,
    enabled: Boolean,
    source: MutableInteractionSource,
    groupRole: Role?,
): Modifier {
    val keyed = if (onRemove != null && enabled) {
        modifier.onKeyEvent { event ->
            val delete = event.key == Key.Delete || event.key == Key.Backspace
            if (delete && event.type == KeyEventType.KeyDown) onRemove()
            delete
        }
    } else {
        modifier
    }
    return when {
        selected != null && onClick != null && groupRole == Role.RadioButton -> keyed
            .selectable(selected, source, indication = null, enabled = enabled, role = groupRole, onClick = onClick)
            .minimumTouchTarget()
        selected != null && onClick != null -> keyed
            .toggleable(selected, source, indication = null, enabled = enabled, role = Role.Checkbox) { onClick() }
            .minimumTouchTarget()
        onClick != null -> keyed
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .minimumTouchTarget()
        onRemove != null -> keyed.focusable(enabled, source).semantics(mergeDescendants = true) {}.minimumTouchTarget()
        else -> modifier.semantics(mergeDescendants = true) {}
    }
}
