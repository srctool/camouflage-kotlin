package com.srctool.camouflage.core.checkbox

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.LabelPosition
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.camoError
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

/** Every color a checkbox uses. Disabled and error colors come from the theme. */
@Immutable
public data class CheckboxColors(
    val checkedBox: Color,
    val checkmark: Color,
    val uncheckedBorder: Color,
    val label: Color,
)

/** Overrides for checkboxes. `null` keeps the skin's default. */
@Immutable
public data class CheckboxTheme(
    val boxSize: Dp? = null,
    val shape: Shape? = null,
    val borderWidth: Dp? = null,
    val labelStyle: TextStyle? = null,
    val labelPosition: LabelPosition? = null,
    val labelGap: Dp? = null,
    /** The description line, color included. */
    val descriptionStyle: TextStyle? = null,
    val colors: CheckboxColors? = null,
)

/** The resolved checkbox style: every field set. */
@Immutable
public data class CheckboxStyle(
    val boxSize: Dp,
    val shape: Shape,
    val borderWidth: Dp,
    val labelStyle: TextStyle,
    val labelPosition: LabelPosition,
    val labelGap: Dp,
    val descriptionStyle: TextStyle,
    val colors: CheckboxColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: CheckboxTheme?): CheckboxStyle = if (overrides == null) {
        this
    } else {
        CheckboxStyle(
            boxSize = overrides.boxSize ?: boxSize,
            shape = overrides.shape ?: shape,
            borderWidth = overrides.borderWidth ?: borderWidth,
            labelStyle = overrides.labelStyle ?: labelStyle,
            labelPosition = overrides.labelPosition ?: labelPosition,
            labelGap = overrides.labelGap ?: labelGap,
            descriptionStyle = overrides.descriptionStyle ?: descriptionStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a checkbox renderer draws: the box, then (when [label] is set) the label and [description] on the
 * [CheckboxStyle.labelPosition] side, and [error] below the row. A plain label arrives as one span. The error
 * text hides its own semantics: core already announces it.
 */
@Immutable
public data class CheckboxState(
    val checked: Boolean,
    val label: List<CamoSpan>?,
    val description: String?,
    val error: String?,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** Draws checkboxes. Apply `modifier` (the component's behavior) to the outermost node: the whole row. */
@Stable
public interface CheckboxRenderer {
    /** The complete checkbox style for [theme]. */
    public fun defaults(theme: CamoTheme): CheckboxStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: CheckboxState, theme: CamoTheme, style: CheckboxStyle, modifier: Modifier)
}

/**
 * A checkbox: selects something for later (a form that is submitted). With a [label] (or [labelSpans], for
 * links and bold), the whole row is one target, announced as "Remember me, checkbox, not checked". A link in
 * the spans stays its own target. A `null` [onCheckedChange] makes it display-only, for a parent that handles
 * clicks and semantics. [error] marks it invalid and shows below the row.
 */
@Composable
public fun CamoCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelSpans: List<CamoSpan>? = null,
    description: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = requireSkin("CamoCheckbox")
    val theme = requireTheme("CamoCheckbox")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.checkbox.defaults(theme).merge(theme.components.checkbox) }
    val spans = labelSpans ?: label?.let { listOf(CamoSpan(it)) }
    val behavior = if (onCheckedChange == null) {
        modifier
    } else {
        modifier
            .camoError(error)
            .toggleable(checked, source, indication = null, enabled = enabled, role = Role.Checkbox) {
                onCheckedChange(it)
            }
            .minimumTouchTarget()
    }
    skin.checkbox.Render(
        CheckboxState(checked, spans, description, error, enabled, interaction, source),
        theme,
        style,
        behavior,
    )
}
