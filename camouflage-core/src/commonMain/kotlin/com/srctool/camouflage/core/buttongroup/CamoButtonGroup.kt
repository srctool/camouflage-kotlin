package com.srctool.camouflage.core.buttongroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.button.DebugChecks
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a button group uses, for `Tone.Default`. [selectedContainer] fills a pressed toggle. */
@Immutable
public data class GroupColors(
    val content: Color,
    val border: Color,
    val selectedContainer: Color,
    val selectedContent: Color,
)

/** Overrides for button groups. `null` keeps the skin's default. */
@Immutable
public data class ButtonGroupTheme(
    val defaultAppearance: Appearance? = null,
    val height: Dp? = null,
    val shape: Shape? = null,
    val innerShape: Shape? = null,
    val spacing: Dp? = null,
    val borderWidth: Dp? = null,
    val iconSize: Dp? = null,
    val iconGap: Dp? = null,
    val textStyle: TextStyle? = null,
    val colors: GroupColors? = null,
)

/** The resolved button groups style: every field set. */
@Immutable
public data class ButtonGroupStyle(
    val defaultAppearance: Appearance,
    val height: Dp,
    val shape: Shape,
    val innerShape: Shape,
    val spacing: Dp,
    val borderWidth: Dp,
    val iconSize: Dp,
    val iconGap: Dp,
    val textStyle: TextStyle,
    val colors: GroupColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: ButtonGroupTheme?): ButtonGroupStyle = if (overrides == null) {
        this
    } else {
        ButtonGroupStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            height = overrides.height ?: height,
            shape = overrides.shape ?: shape,
            innerShape = overrides.innerShape ?: innerShape,
            spacing = overrides.spacing ?: spacing,
            borderWidth = overrides.borderWidth ?: borderWidth,
            iconSize = overrides.iconSize ?: iconSize,
            iconGap = overrides.iconGap ?: iconGap,
            textStyle = overrides.textStyle ?: textStyle,
            colors = overrides.colors ?: colors,
        )
    }


    /** The colors in [tone]: this style's for `Tone.Default`, the tone's family otherwise. */
    public fun colorsFor(tone: Tone, theme: CamoTheme): GroupColors {
        if (tone == Tone.Default) return colors
        val f = theme.colors.family(tone)
        return GroupColors(f.strong, f.strong, f.strong, f.onStrong)
    }
}

/**
 * One button of a [CamoButtonGroup]. It needs a [text] or an [icon]; an icon-only button needs a
 * [contentDescription]. A non-null [selected] makes it a toggle (Bold, Italic): the app owns the state and flips
 * it in [onClick].
 */
@Immutable
public data class CamoGroupButton(
    val key: Any,
    val onClick: () -> Unit,
    val text: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val contentDescription: String? = null,
    val selected: Boolean? = null,
    val enabled: Boolean = true,
)

/** What the container renderer draws: the row around the buttons. */
@Immutable
public data class ButtonGroupState(
    val count: Int,
    val connected: Boolean,
    val appearance: Appearance,
    val tone: Tone,
    val enabled: Boolean,
)

/**
 * What a button renderer draws. [selected] is `null` for a command button; draw the pressed look when `true`.
 * [position] is for the outer corners of a connected group.
 */
@Immutable
public data class GroupButtonState(
    val selected: Boolean?,
    val enabled: Boolean,
    val position: ItemPosition,
    val connected: Boolean,
    val appearance: Appearance,
    val tone: Tone,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** A button's content. */
@Stable
public class GroupButtonSlots(public val text: String?, public val icon: (@Composable () -> Unit)?)

/**
 * Draws button groups in two parts: the [Container] (a `Row`, spaced or connected) and each [Button]. Apply
 * `modifier` (the behavior) to the outermost node of each. Core sizes the buttons to their content.
 */
@Stable
public interface ButtonGroupRenderer {
    /** The complete button group style for [theme]. */
    public fun defaults(theme: CamoTheme): ButtonGroupStyle

    /** Draws the row around [content], which places the buttons. */
    @Composable
    public fun Container(
        state: ButtonGroupState,
        theme: CamoTheme,
        style: ButtonGroupStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    )

    /** Draws one button with [slots]. */
    @Composable
    public fun Button(
        state: GroupButtonState,
        slots: GroupButtonSlots,
        theme: CamoTheme,
        style: ButtonGroupStyle,
        modifier: Modifier,
    )
}

/**
 * Related buttons as one control: commands (Cut, Copy, Paste) or independent toggles (Bold, Italic). It is not a
 * segmented control, which chooses one value. [connected] buttons share borders and corners; otherwise they keep
 * a gap. One Tab stop: the arrow keys move between the enabled buttons, wrapping, and Space or Enter presses.
 * [contentDescription] names the group. A group never wraps: too many buttons belong in a toolbar.
 */
@Composable
public fun CamoButtonGroup(
    buttons: List<CamoGroupButton>,
    modifier: Modifier = Modifier,
    connected: Boolean = true,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val skin = requireSkin("CamoButtonGroup")
    val theme = requireTheme("CamoButtonGroup")
    val style = remember(skin, theme) { skin.buttonGroup.defaults(theme).merge(theme.components.buttonGroup) }
    val resolved = appearance ?: style.defaultAppearance
    val focus = rememberRovingFocus()
    val behavior = modifier
        .width(IntrinsicSize.Max)
        .height(IntrinsicSize.Min)
        .semantics {
            contentDescription?.let { this.contentDescription = it }
            collectionInfo = CollectionInfo(rowCount = 1, columnCount = buttons.size)
        }
    skin.buttonGroup.Container(
        ButtonGroupState(buttons.size, connected, resolved, tone, enabled),
        theme,
        style,
        behavior,
    ) {
        buttons.forEachIndexed { index, button ->
            GroupButton(button, ItemPosition(index, buttons.size), connected, resolved, tone, enabled, focus, style)
        }
    }
}

@Composable
private fun GroupButton(
    button: CamoGroupButton,
    position: ItemPosition,
    connected: Boolean,
    appearance: Appearance,
    tone: Tone,
    groupEnabled: Boolean,
    focus: RovingFocus,
    style: ButtonGroupStyle,
) {
    val skin = requireSkin("CamoButtonGroup")
    val theme = requireTheme("CamoButtonGroup")
    if (DebugChecks.enabled) {
        check(button.text != null || button.icon != null) { "A CamoGroupButton needs a text or an icon." }
        check(button.text != null || button.contentDescription != null) {
            "An icon-only CamoGroupButton needs a contentDescription."
        }
    }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val enabled = groupEnabled && button.enabled
    val selected = button.selected
    val click = if (selected == null) {
        Modifier.clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = button.onClick)
    } else {
        Modifier.toggleable(selected, source, indication = null, enabled = enabled, role = Role.Button) {
            button.onClick()
        }
    }
    val behavior = Modifier
        .rovingFocusItem(focus, position.index, enabled, selected = false, onSelect = {})
        .semantics {
            button.contentDescription?.let { contentDescription = it }
            collectionItemInfo = CollectionItemInfo(0, 1, position.index, 1)
        }
        .then(click)
        .minimumTouchTarget()
    skin.buttonGroup.Button(
        GroupButtonState(selected, enabled, position, connected, appearance, tone, interaction, source),
        GroupButtonSlots(button.text, button.icon),
        theme,
        style,
        behavior,
    )
}
