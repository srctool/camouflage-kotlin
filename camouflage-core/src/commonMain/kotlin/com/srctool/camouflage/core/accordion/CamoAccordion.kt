package com.srctool.camouflage.core.accordion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.list.DividedColumn
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState
import kotlin.math.roundToInt

/** The icon that shows whether a section is open: a chevron that turns, or a plus that becomes a minus. */
public enum class AccordionIndicator { Chevron, PlusMinus }

/** Which end of the header the indicator sits at, in reading order. */
public enum class IndicatorPosition { Start, End }

/** The words an accordion header announces. */
@Immutable
public data class AccordionStrings(
    val expanded: String = "Expanded",
    val collapsed: String = "Collapsed",
    val expand: String = "Expand",
    val collapse: String = "Collapse",
)

/** Every color an accordion uses. [expandedContainer] fills an open section when set. */
@Immutable
public data class AccordionColors(
    val title: Color,
    val subtitle: Color,
    val indicator: Color,
    val expandedContainer: Color? = null,
)

/** Overrides for accordions. `null` keeps the skin's default. */
@Immutable
public data class AccordionTheme(
    val indicator: AccordionIndicator? = null,
    val indicatorPosition: IndicatorPosition? = null,
    val headerMinHeight: Dp? = null,
    val headerPadding: PaddingValues? = null,
    val contentPadding: PaddingValues? = null,
    val titleStyle: TextStyle? = null,
    val subtitleStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val colors: AccordionColors? = null,
    val strings: AccordionStrings? = null,
)

/** The resolved accordion style: every field set. */
@Immutable
public data class AccordionStyle(
    val indicator: AccordionIndicator,
    val indicatorPosition: IndicatorPosition,
    val headerMinHeight: Dp,
    val headerPadding: PaddingValues,
    val contentPadding: PaddingValues,
    val titleStyle: TextStyle,
    val subtitleStyle: TextStyle,
    val iconSize: Dp,
    val colors: AccordionColors,
    val strings: AccordionStrings,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: AccordionTheme?): AccordionStyle = if (overrides == null) {
        this
    } else {
        AccordionStyle(
            indicator = overrides.indicator ?: indicator,
            indicatorPosition = overrides.indicatorPosition ?: indicatorPosition,
            headerMinHeight = overrides.headerMinHeight ?: headerMinHeight,
            headerPadding = overrides.headerPadding ?: headerPadding,
            contentPadding = overrides.contentPadding ?: contentPadding,
            titleStyle = overrides.titleStyle ?: titleStyle,
            subtitleStyle = overrides.subtitleStyle ?: subtitleStyle,
            iconSize = overrides.iconSize ?: iconSize,
            colors = overrides.colors ?: colors,
            strings = overrides.strings ?: strings,
        )
    }
}

/**
 * What an accordion renderer draws: a header with [title], [subtitle] and the indicator, and the content below.
 * [openFraction] runs from 0 (closed) to 1 (open) with the expand animation: turn or morph the indicator with it.
 */
@Immutable
public data class AccordionState(
    val title: String,
    val subtitle: String?,
    val expanded: Boolean,
    val openFraction: Float,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/**
 * The parts an accordion renderer places: the caller's [leading] icon, and [content], which core already pads by
 * `style.contentPadding`, clips and animates to its open height (place it below the header, full width).
 */
@Stable
public class AccordionSlots(
    public val leading: (@Composable () -> Unit)?,
    public val content: @Composable () -> Unit,
)

/** Draws accordions. */
@Stable
public interface AccordionRenderer {
    /** The complete accordion style for [theme]. */
    public fun defaults(theme: CamoTheme): AccordionStyle

    /**
     * Draws the header and places the content below it. Apply `headerModifier` (the header's behavior and
     * semantics) to the header row, and `modifier` to the outermost node.
     */
    @Composable
    public fun Render(
        state: AccordionState,
        slots: AccordionSlots,
        theme: CamoTheme,
        style: AccordionStyle,
        headerModifier: Modifier,
        modifier: Modifier,
    )
}

/**
 * An expandable section (an FAQ answer, a "Shipping" panel). It's controlled: the app owns [expanded]. The
 * header is a button and a heading, announced "Shipping, collapsed". Collapsed content stays composed, keeping
 * its state (a half-filled form), but takes no space and is neither focusable nor announced.
 */
@Composable
public fun CamoAccordion(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = requireSkin("CamoAccordion")
    val theme = requireTheme("CamoAccordion")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.accordion.defaults(theme).merge(theme.components.accordion) }
    val fraction by animateFloatAsState(
        if (expanded) 1f else 0f,
        tween(theme.motion.durationMedium, easing = theme.motion.easingStandard),
        label = "accordion",
    )
    val strings = style.strings
    val header = Modifier
        .clickable(source, indication = null, enabled = enabled) { onExpandedChange(!expanded) }
        .semantics(mergeDescendants = true) {
            heading()
            role = Role.Button
            stateDescription = if (expanded) strings.expanded else strings.collapsed
            onClick(if (expanded) strings.collapse else strings.expand) {
                if (enabled) onExpandedChange(!expanded)
                enabled
            }
            if (enabled) {
                if (expanded) {
                    collapse { onExpandedChange(false); true }
                } else {
                    expand { onExpandedChange(true); true }
                }
            }
        }
    val body: @Composable () -> Unit = { CollapsingContent(expanded, { fraction }, style.contentPadding, content) }
    skin.accordion.Render(
        AccordionState(title, subtitle, expanded, fraction, enabled, interaction, source),
        AccordionSlots(leading, body),
        theme,
        style,
        header,
        modifier,
    )
}

/** Content measured at full height and shown at [fraction] of it, clipped. Collapsed: no focus, no semantics. */
@Composable
private fun CollapsingContent(
    expanded: Boolean,
    fraction: () -> Float,
    padding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hidden = if (expanded) {
        Modifier
    } else {
        Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {}
    }
    Column(
        Modifier
            .clipToBounds()
            .layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minHeight = 0))
                val h = (p.height * fraction()).roundToInt()
                layout(p.width, h) { if (h > 0) p.placeRelative(0, 0) }
            }
            .then(hidden)
            .padding(padding),
        content = content,
    )
}

/** One section of a [CamoAccordionGroup], identified by [key]. */
@Immutable
public class CamoAccordionItem<K>(
    public val key: K,
    public val title: String,
    public val subtitle: String? = null,
    public val leading: (@Composable () -> Unit)? = null,
    public val enabled: Boolean = true,
    public val content: @Composable ColumnScope.() -> Unit,
)

/**
 * Accordion sections with the list dividers (`ListItemTheme.dividers`) between them. [expanded] is the set of
 * open keys (controlled). With [singleExpand], opening one section closes the others (an FAQ).
 */
@Composable
public fun <K> CamoAccordionGroup(
    expanded: Set<K>,
    onExpandedChange: (Set<K>) -> Unit,
    items: List<CamoAccordionItem<K>>,
    modifier: Modifier = Modifier,
    singleExpand: Boolean = false,
) {
    val skin = requireSkin("CamoAccordionGroup")
    val theme = requireTheme("CamoAccordionGroup")
    val list = remember(skin, theme) { skin.listItem.defaults(theme).merge(theme.components.listItem) }
    val style = remember(skin, theme) { skin.accordion.defaults(theme).merge(theme.components.accordion) }
    // An `Inset` divider starts where the header's text does (headers have no leading slot to clear).
    val inset = style.headerPadding.calculateStartPadding(LocalLayoutDirection.current)
    DividedColumn(list.dividers, inset, modifier) {
        for (item in items) {
            CamoAccordion(
                title = item.title,
                expanded = item.key in expanded,
                onExpandedChange = { open ->
                    onExpandedChange(
                        when {
                            !open -> expanded - item.key
                            singleExpand -> setOf(item.key)
                            else -> expanded + item.key
                        },
                    )
                },
                subtitle = item.subtitle,
                leading = item.leading,
                enabled = item.enabled,
                content = item.content,
            )
        }
    }
}
