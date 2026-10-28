package com.srctool.camouflage.core.radio

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.LabelPosition
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.ReportRowToggle
import com.srctool.camouflage.core.util.camoError
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.minimumTouchTarget
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a radio uses. */
@Immutable
public data class RadioColors(val selected: Color, val unselected: Color, val error: Color, val label: Color)

/** Overrides for radios. `null` keeps the skin's default. */
@Immutable
public data class RadioTheme(
    val size: Dp? = null,
    val dotSize: Dp? = null,
    val borderWidth: Dp? = null,
    val labelStyle: TextStyle? = null,
    /** An option's description line, color included. */
    val descriptionStyle: TextStyle? = null,
    val labelPosition: LabelPosition? = null,
    val labelGap: Dp? = null,
    val colors: RadioColors? = null,
)

/** The resolved radio style: every field set. */
@Immutable
public data class RadioStyle(
    val size: Dp,
    val dotSize: Dp,
    val borderWidth: Dp,
    val labelStyle: TextStyle,
    val descriptionStyle: TextStyle,
    val labelPosition: LabelPosition,
    val labelGap: Dp,
    val colors: RadioColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: RadioTheme?): RadioStyle = if (overrides == null) {
        this
    } else {
        RadioStyle(
            size = overrides.size ?: size,
            dotSize = overrides.dotSize ?: dotSize,
            borderWidth = overrides.borderWidth ?: borderWidth,
            labelStyle = overrides.labelStyle ?: labelStyle,
            descriptionStyle = overrides.descriptionStyle ?: descriptionStyle,
            labelPosition = overrides.labelPosition ?: labelPosition,
            labelGap = overrides.labelGap ?: labelGap,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a radio renderer draws: the circle, and (when [label] is set) the label and [description] on the
 * [RadioStyle.labelPosition] side. [error] draws the ring in the error color; the group shows the message.
 */
@Immutable
public data class RadioState(
    val selected: Boolean,
    val label: List<CamoSpan>?,
    val description: String?,
    val enabled: Boolean,
    val error: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** Draws radios. Apply `modifier` (the component's behavior) to the outermost node: the whole row. */
@Stable
public interface RadioRenderer {
    /** The complete radio style for [theme]. */
    public fun defaults(theme: CamoTheme): RadioStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: RadioState, theme: CamoTheme, style: RadioStyle, modifier: Modifier)
}

/** One choice in a [CamoRadioGroup]. */
@Immutable
public data class RadioOption<T>(
    val value: T,
    val label: String,
    val description: String? = null,
    val enabled: Boolean = true,
)

/**
 * A single radio, for custom layouts (a radio inside a card). Prefer [CamoRadioGroup], which adds the keyboard
 * and the "2 of 3" announcement. A `null` [onClick] makes it display-only, for a parent row that handles it.
 */
@Composable
public fun CamoRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelSpans: List<CamoSpan>? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    RadioImpl(selected, onClick, modifier, labelSpans ?: label?.let { listOf(CamoSpan(it)) }, null, enabled, false,
        interactionSource)
}

@Composable
private fun RadioImpl(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    label: List<CamoSpan>?,
    description: String?,
    enabled: Boolean,
    error: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    val skin = requireSkin("CamoRadio")
    val theme = requireTheme("CamoRadio")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.radio.defaults(theme).merge(theme.components.radio) }
    val behavior = if (onClick == null) {
        ReportRowToggle(Role.RadioButton, selected = selected)
        modifier
    } else {
        modifier
            .selectable(selected, source, null, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .minimumTouchTarget()
    }
    skin.radio.Render(RadioState(selected, label, description, enabled, error, interaction, source), theme, style,
        behavior)
}

/**
 * Exactly one choice from a small visible set (2 to 5 options; more belong in a select). Tab enters the group on
 * the selected option, the arrow keys move and select, and each option is announced "Weekly, radio button, 2 of
 * 3". `value = null` selects nothing yet. A horizontal group wraps when it doesn't fit.
 */
@Composable
public fun <T> CamoRadioGroup(
    value: T?,
    onValueChange: (T) -> Unit,
    options: List<RadioOption<T>>,
    modifier: Modifier = Modifier,
    label: String? = null,
    error: String? = null,
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
) {
    val theme = requireTheme("CamoRadioGroup")
    val focus = rememberRovingFocus()
    val vertical = orientation == Orientation.Vertical
    val group = Modifier
        .selectableGroup()
        .semantics {
            if (label != null) contentDescription = label
            collectionInfo = CollectionInfo(
                rowCount = if (vertical) options.size else 1,
                columnCount = if (vertical) 1 else options.size,
            )
        }
        .camoError(error)
    Column(modifier) {
        if (label != null) GroupLabel(label, enabled)
        val items: @Composable () -> Unit = {
            options.forEachIndexed { index, option ->
                val on = enabled && option.enabled
                val chosen = option.value == value
                RadioImpl(
                    selected = chosen,
                    onClick = { onValueChange(option.value) },
                    modifier = Modifier
                        .rovingFocusItem(focus, index, on, chosen) { onValueChange(option.value) }
                        .semantics {
                            collectionItemInfo = CollectionItemInfo(
                                rowIndex = if (vertical) index else 0,
                                rowSpan = 1,
                                columnIndex = if (vertical) 0 else index,
                                columnSpan = 1,
                            )
                        },
                    label = listOf(CamoSpan(option.label)),
                    description = option.description,
                    enabled = on,
                    error = error != null,
                    interactionSource = null,
                )
            }
        }
        if (vertical) {
            Column(group) { items() }
        } else {
            FlowRow(group, horizontalArrangement = Arrangement.spacedBy(theme.spacing.l)) { items() }
        }
        if (error != null) {
            CamoText(
                error,
                Modifier.clearAndSetSemantics {}.padding(top = theme.spacing.xs),
                style = theme.typography.bodySmall,
                color = theme.colors.error,
            )
        }
    }
}

// The group's visible label. The group's semantics carry it, so the text hides its own.
@Composable
private fun GroupLabel(label: String, enabled: Boolean) {
    val theme = requireTheme("CamoRadioGroup")
    CamoText(
        label,
        Modifier.clearAndSetSemantics {}.padding(bottom = theme.spacing.xs),
        style = theme.typography.labelLarge,
        color = theme.colors.onSurface.let { if (enabled) it else it.disabled(DISABLED_ALPHA) },
    )
}

private const val DISABLED_ALPHA = 0.38f
