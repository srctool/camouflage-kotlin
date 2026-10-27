package com.srctool.camouflage.core.segmented

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a segmented control uses. [container] is the track behind the segments. */
@Immutable
public data class SegmentedColors(
    val container: Color,
    val border: Color,
    val selectedContainer: Color,
    val selectedContent: Color,
    val content: Color,
)

/** Overrides for segmented controls. `null` keeps the skin's default. */
@Immutable
public data class SegmentedControlTheme(
    val height: Dp? = null,
    val shape: Shape? = null,
    val borderWidth: Dp? = null,
    val textStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val showCheckIcon: Boolean? = null,
    val colors: SegmentedColors? = null,
)

/** The resolved segmented control style: every field set. */
@Immutable
public data class SegmentedControlStyle(
    val height: Dp,
    val shape: Shape,
    val borderWidth: Dp,
    val textStyle: TextStyle,
    val iconSize: Dp,
    val showCheckIcon: Boolean,
    val colors: SegmentedColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SegmentedControlTheme?): SegmentedControlStyle = if (overrides == null) {
        this
    } else {
        SegmentedControlStyle(
            height = overrides.height ?: height,
            shape = overrides.shape ?: shape,
            borderWidth = overrides.borderWidth ?: borderWidth,
            textStyle = overrides.textStyle ?: textStyle,
            iconSize = overrides.iconSize ?: iconSize,
            showCheckIcon = overrides.showCheckIcon ?: showCheckIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * One option of a [CamoSegmentedControl]. It needs a [label] or an [icon]; an icon-only segment needs a
 * [contentDescription].
 */
@Immutable
public data class Segment<T>(
    val value: T,
    val label: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val contentDescription: String? = null,
    val enabled: Boolean = true,
)

/** What the container renderer draws: the track and outer shape around the segments. */
@Immutable
public data class SegmentedContainerState(val enabled: Boolean, val count: Int)

/** What a segment renderer draws: one segment at [position], selected or not. */
@Immutable
public data class SegmentState(
    val selected: Boolean,
    val enabled: Boolean,
    val position: ItemPosition,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** A segment's content: its [label] and [icon], or a building block's custom [content] instead of both. */
@Stable
public class SegmentSlots(
    public val label: String?,
    public val icon: (@Composable () -> Unit)?,
    public val content: (@Composable () -> Unit)?,
)

/**
 * Draws segmented controls in two parts: the [Container] (a `Row` of the segments) and each [Segment]. Apply
 * `modifier` (the behavior) to the outermost node of each. Core sizes the segments to equal widths.
 */
@Stable
public interface SegmentedControlRenderer {
    /** The complete segmented control style for [theme]. */
    public fun defaults(theme: CamoTheme): SegmentedControlStyle

    /** Draws the track around [content], which places the segments in a row. */
    @Composable
    public fun Container(
        state: SegmentedContainerState,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    )

    /** Draws one segment with [slots]. */
    @Composable
    public fun Segment(
        state: SegmentState,
        slots: SegmentSlots,
        theme: CamoTheme,
        style: SegmentedControlStyle,
        modifier: Modifier,
    )
}

private class SegmentedScope(val focus: RovingFocus, val enabled: Boolean)

private val LocalSegmentedScope = staticCompositionLocalOf<SegmentedScope?> { null }

/**
 * A compact choice between 2 to 5 related options ("Day / Week / Month"), each segment the same width. It's a
 * radio group: one Tab stop, the arrow keys move and select, and segments are announced "Week, 2 of 3". For
 * several selections, use filter chips.
 */
@Composable
public fun <T> CamoSegmentedControl(
    options: List<Segment<T>>,
    selected: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CamoSegmentedContainer(modifier, enabled = enabled, count = options.size) {
        options.forEachIndexed { index, segment ->
            CamoSegmentButton(
                segment = segment,
                selected = segment.value == selected,
                onClick = { onSelectionChange(segment.value) },
                position = ItemPosition(index, options.size),
            )
        }
    }
}

/**
 * The building block (ADR-30) for custom segments: the skin's track and outer shape, radio-group semantics and
 * the arrow keys across the [CamoSegmentButton]s in [content]. [count] is the number of segments.
 */
@Composable
public fun CamoSegmentedContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    count: Int,
    content: @Composable RowScope.() -> Unit,
) {
    val skin = requireSkin("CamoSegmentedContainer")
    val theme = requireTheme("CamoSegmentedContainer")
    val style = remember(skin, theme) {
        skin.segmentedControl.defaults(theme).merge(theme.components.segmentedControl)
    }
    val focus = rememberRovingFocus()
    val scope = remember(focus, enabled) { SegmentedScope(focus, enabled) }
    val behavior = modifier
        .minimumTouchTarget()
        .width(IntrinsicSize.Max)
        .height(IntrinsicSize.Min)
        .selectableGroup()
        .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = count) }
    CompositionLocalProvider(LocalSegmentedScope provides scope) {
        skin.segmentedControl.Container(SegmentedContainerState(enabled, count), theme, style, behavior, content)
    }
}

/**
 * One skin-styled segment inside a [CamoSegmentedContainer], with radio semantics. [content] replaces the
 * segment's label and icon (an icon with a count, two lines); the segment's label still names it.
 */
@Composable
public fun <T> RowScope.CamoSegmentButton(
    segment: Segment<T>,
    selected: Boolean,
    onClick: () -> Unit,
    position: ItemPosition,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    val skin = requireSkin("CamoSegmentButton")
    val theme = requireTheme("CamoSegmentButton")
    val scope = checkNotNull(LocalSegmentedScope.current) {
        "CamoSegmentButton must be inside a CamoSegmentedContainer."
    }
    if (DebugChecks.enabled) {
        check(segment.label != null || segment.icon != null || content != null) {
            "A Segment needs a label or an icon."
        }
        check(segment.label != null || segment.contentDescription != null) {
            "An icon-only Segment needs a contentDescription."
        }
    }
    val style = remember(skin, theme) {
        skin.segmentedControl.defaults(theme).merge(theme.components.segmentedControl)
    }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val enabled = scope.enabled && segment.enabled
    val behavior = modifier
        .weight(1f)
        .rovingFocusItem(scope.focus, position.index, enabled, selected, onClick)
        .semantics {
            segment.contentDescription?.let { contentDescription = it }
            collectionItemInfo = CollectionItemInfo(0, 1, position.index, 1)
        }
        .selectable(selected, source, indication = null, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    skin.segmentedControl.Segment(
        SegmentState(selected, enabled, position, interaction, source),
        SegmentSlots(segment.label, segment.icon, content),
        theme,
        style,
        behavior,
    )
}
