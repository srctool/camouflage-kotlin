package com.srctool.camouflage.core.switch

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.LabelPosition
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.ReportRowToggle
import com.srctool.camouflage.core.util.camoError
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

/** Every color a switch uses. Disabled and error colors come from the theme. */
@Immutable
public data class SwitchColors(
    val trackOn: Color,
    val trackOff: Color,
    val thumbOn: Color,
    val thumbOff: Color,
    val trackOffBorder: Color,
    val label: Color,
)

/** The icons inside the thumb when [SwitchStyle.showThumbIcons] is on. A `null` [unchecked] leaves "off" plain. */
@Immutable
public data class ThumbIcons(val checked: IconSource, val unchecked: IconSource? = null)

/** Overrides for switches. `null` keeps the skin's default. */
@Immutable
public data class SwitchTheme(
    val showThumbIcons: Boolean? = null,
    val thumbIcons: ThumbIcons? = null,
    val trackWidth: Dp? = null,
    val trackHeight: Dp? = null,
    val thumbSizeOff: Dp? = null,
    val thumbSizeOn: Dp? = null,
    val thumbSizePressed: Dp? = null,
    val labelStyle: TextStyle? = null,
    val labelPosition: LabelPosition? = null,
    val labelGap: Dp? = null,
    /** The description line, color included. */
    val descriptionStyle: TextStyle? = null,
    val colors: SwitchColors? = null,
)

/** The resolved switch style: every field set except [thumbIcons], which a skin without icons leaves `null`. */
@Immutable
public data class SwitchStyle(
    val showThumbIcons: Boolean,
    val thumbIcons: ThumbIcons?,
    val trackWidth: Dp,
    val trackHeight: Dp,
    val thumbSizeOff: Dp,
    val thumbSizeOn: Dp,
    val thumbSizePressed: Dp,
    val labelStyle: TextStyle,
    val labelPosition: LabelPosition,
    val labelGap: Dp,
    val descriptionStyle: TextStyle,
    val colors: SwitchColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: SwitchTheme?): SwitchStyle = if (overrides == null) {
        this
    } else {
        SwitchStyle(
            showThumbIcons = overrides.showThumbIcons ?: showThumbIcons,
            thumbIcons = overrides.thumbIcons ?: thumbIcons,
            trackWidth = overrides.trackWidth ?: trackWidth,
            trackHeight = overrides.trackHeight ?: trackHeight,
            thumbSizeOff = overrides.thumbSizeOff ?: thumbSizeOff,
            thumbSizeOn = overrides.thumbSizeOn ?: thumbSizeOn,
            thumbSizePressed = overrides.thumbSizePressed ?: thumbSizePressed,
            labelStyle = overrides.labelStyle ?: labelStyle,
            labelPosition = overrides.labelPosition ?: labelPosition,
            labelGap = overrides.labelGap ?: labelGap,
            descriptionStyle = overrides.descriptionStyle ?: descriptionStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a switch renderer draws: the track and thumb, with the label and [description] on the
 * [SwitchStyle.labelPosition] side and [error] below. While the user drags the thumb, [dragFraction] is its
 * position from the "off" side (0) to the "on" side (1), measured from the start in the reading direction, and
 * the renderer draws it there without animating. Otherwise the thumb animates to [checked].
 */
@Immutable
public data class SwitchState(
    val checked: Boolean,
    val label: List<CamoSpan>?,
    val description: String?,
    val error: String?,
    val enabled: Boolean,
    val dragFraction: Float?,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** Draws switches. Apply `modifier` (the component's behavior) to the outermost node: the whole row. */
@Stable
public interface SwitchRenderer {
    /** The complete switch style for [theme]. */
    public fun defaults(theme: CamoTheme): SwitchStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: SwitchState, theme: CamoTheme, style: SwitchStyle, modifier: Modifier)
}

private const val HALFWAY = 0.5f

/**
 * An on/off switch that applies immediately (Wi-Fi on), announced as "Wi-Fi, switch, on". With a [label] the
 * whole row is one target. The thumb can be dragged: released past the middle it toggles, otherwise it springs
 * back. A `null` [onCheckedChange] makes it display-only.
 */
@Composable
public fun CamoSwitch(
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
    val skin = requireSkin("CamoSwitch")
    val theme = requireTheme("CamoSwitch")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.switch.defaults(theme).merge(theme.components.switch) }
    val spans = labelSpans ?: label?.let { listOf(CamoSpan(it)) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val current by rememberUpdatedState(checked)
    val onChange by rememberUpdatedState(onCheckedChange)
    // The thumb's center travels from trackHeight/2 to trackWidth - trackHeight/2.
    val travel = with(LocalDensity.current) { (style.trackWidth - style.trackHeight).toPx() }.coerceAtLeast(1f)
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val drag = rememberDraggableState { delta ->
        val from = dragFraction ?: if (current) 1f else 0f
        dragFraction = (from + direction * delta / travel).coerceIn(0f, 1f)
    }
    val behavior = if (onCheckedChange == null) {
        ReportRowToggle(Role.Switch, ToggleableState(checked))
        modifier
    } else {
        modifier
            .camoError(error)
            .toggleable(checked, source, indication = null, enabled = enabled, role = Role.Switch) {
                onCheckedChange(it)
            }
            .draggable(
                drag,
                Orientation.Horizontal,
                enabled = enabled,
                interactionSource = source,
                onDragStarted = { dragFraction = if (current) 1f else 0f },
                onDragStopped = {
                    val on = (dragFraction ?: 0f) > HALFWAY
                    dragFraction = null
                    if (on != current) onChange?.invoke(on)
                },
            )
            .minimumTouchTarget()
    }
    skin.switch.Render(
        SwitchState(checked, spans, description, error, enabled, dragFraction, interaction, source),
        theme,
        style,
        behavior,
    )
}
