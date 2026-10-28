package com.srctool.camouflage.core.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.SurfaceLevel
import com.srctool.camouflage.core.util.LocalRowToggle
import com.srctool.camouflage.core.util.RowToggle
import com.srctool.camouflage.core.util.collectInteractionState

/** The lines a list section draws between its rows: none, edge to edge, or starting after the leading slot. */
public enum class ListDividers { None, Full, Inset }

/** Every color a list row and section use. */
@Immutable
public data class ListItemColors(
    val headline: Color,
    val supporting: Color,
    val overline: Color,
    val icon: Color,
    val selectedContainer: Color,
    val sectionTitle: Color,
    val sectionFooter: Color,
)

/** Overrides for list rows and sections. `null` keeps the skin's default. */
@Immutable
public data class ListItemTheme(
    val minHeightOneLine: Dp? = null,
    val minHeightTwoLines: Dp? = null,
    val minHeightThreeLines: Dp? = null,
    val contentPadding: PaddingValues? = null,
    val leadingGap: Dp? = null,
    val trailingGap: Dp? = null,
    val headlineStyle: TextStyle? = null,
    val supportingStyle: TextStyle? = null,
    val overlineStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val sectionShape: Shape? = null,
    val sectionTitleStyle: TextStyle? = null,
    val sectionFooterStyle: TextStyle? = null,
    val dividers: ListDividers? = null,
    /** Where an `Inset` divider starts: after the row's padding, a leading icon and the gap. */
    val dividerInsetStart: Dp? = null,
    val colors: ListItemColors? = null,
)

/** The resolved list style: every field set. */
@Immutable
public data class ListItemStyle(
    val minHeightOneLine: Dp,
    val minHeightTwoLines: Dp,
    val minHeightThreeLines: Dp,
    val contentPadding: PaddingValues,
    val leadingGap: Dp,
    val trailingGap: Dp,
    val headlineStyle: TextStyle,
    val supportingStyle: TextStyle,
    val overlineStyle: TextStyle,
    val iconSize: Dp,
    val sectionShape: Shape,
    val sectionTitleStyle: TextStyle,
    val sectionFooterStyle: TextStyle,
    val dividers: ListDividers,
    val dividerInsetStart: Dp,
    val colors: ListItemColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: ListItemTheme?): ListItemStyle = if (overrides == null) {
        this
    } else {
        ListItemStyle(
            minHeightOneLine = overrides.minHeightOneLine ?: minHeightOneLine,
            minHeightTwoLines = overrides.minHeightTwoLines ?: minHeightTwoLines,
            minHeightThreeLines = overrides.minHeightThreeLines ?: minHeightThreeLines,
            contentPadding = overrides.contentPadding ?: contentPadding,
            leadingGap = overrides.leadingGap ?: leadingGap,
            trailingGap = overrides.trailingGap ?: trailingGap,
            headlineStyle = overrides.headlineStyle ?: headlineStyle,
            supportingStyle = overrides.supportingStyle ?: supportingStyle,
            overlineStyle = overrides.overlineStyle ?: overlineStyle,
            iconSize = overrides.iconSize ?: iconSize,
            sectionShape = overrides.sectionShape ?: sectionShape,
            sectionTitleStyle = overrides.sectionTitleStyle ?: sectionTitleStyle,
            sectionFooterStyle = overrides.sectionFooterStyle ?: sectionFooterStyle,
            dividers = overrides.dividers ?: dividers,
            dividerInsetStart = overrides.dividerInsetStart ?: dividerInsetStart,
            colors = overrides.colors ?: colors,
        )
    }

    /** The minimum height of a row with [lines] lines of text. */
    public fun minHeight(lines: Int): Dp = when {
        lines <= 1 -> minHeightOneLine
        lines == 2 -> minHeightTwoLines
        else -> minHeightThreeLines
    }
}

/**
 * What a list row renderer draws: [headline] with [overline] above and [supporting] below, in a row at least
 * `style.minHeight(lines)` tall. Center the slots for one or two lines; for three, align them to the top of the
 * first line.
 */
@Immutable
public data class ListItemState(
    val headline: String,
    val supporting: String?,
    val overline: String?,
    val lines: Int,
    val clickable: Boolean,
    val selected: Boolean,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** The caller's leading and trailing content (an icon, an avatar, a display-only control, a chevron). */
@Stable
public class ListItemSlots(
    public val leading: (@Composable () -> Unit)?,
    public val trailing: (@Composable () -> Unit)?,
)

/** What a list section renderer draws: an optional [title] (a heading) above the rows and [footer] below. */
@Immutable
public data class ListSectionState(val title: String?, val footer: String?, val level: SurfaceLevel)

/** The section's rows, already laid out by core with the dividers between them. */
@Stable
public class ListSectionSlots(public val rows: @Composable () -> Unit)

/** Draws list rows and sections. */
@Stable
public interface ListItemRenderer {
    /** The complete list style for [theme]. */
    public fun defaults(theme: CamoTheme): ListItemStyle

    /**
     * Draws one row. Apply `modifier` (the component's behavior) to the outermost node, and keep the texts you
     * draw in the semantics tree: a clickable row merges them into one announcement.
     */
    @Composable
    public fun Render(
        state: ListItemState,
        slots: ListItemSlots,
        theme: CamoTheme,
        style: ListItemStyle,
        modifier: Modifier,
    )

    /** Draws a section around its rows. Mark the drawn title with `Modifier.semantics { heading() }`. */
    @Composable
    public fun Section(
        state: ListSectionState,
        slots: ListSectionSlots,
        theme: CamoTheme,
        style: ListItemStyle,
        modifier: Modifier,
    )
}

/**
 * One list row: up to three lines of text with a leading and a trailing slot. With [onClick] the whole row is one
 * target, announced as one element; without it the row is plain content, with no focus stop. For a settings row
 * with a switch, pass the switch display-only (`onCheckedChange = null`) as [trailing] and toggle in [onClick]:
 * the row is then announced as the switch ("Wi-Fi, switch, on").
 */
@Composable
public fun CamoListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    overline: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = requireSkin("CamoListItem")
    val theme = requireTheme("CamoListItem")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.listItem.defaults(theme).merge(theme.components.listItem) }
    val toggle = remember { RowToggle() }
    val behavior = if (onClick != null) {
        // Read during composition, so the row's semantics update when a control reports.
        val role = toggle.role
        val toggleState = toggle.state
        val toggleSelected = toggle.selected
        modifier
            .clickable(source, indication = null, enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.role = role ?: Role.Button
                if (toggleState != null) this.toggleableState = toggleState
                if (toggleSelected != null || selected) this.selected = toggleSelected ?: selected
            }
    } else if (selected) {
        modifier.semantics { this.selected = true }
    } else {
        modifier
    }
    val lines = 1 + (if (supporting != null) 1 else 0) + (if (overline != null) 1 else 0)
    CompositionLocalProvider(LocalRowToggle provides if (onClick != null) toggle else null) {
        skin.listItem.Render(
            ListItemState(headline, supporting, overline, lines, onClick != null, selected, enabled, interaction,
                source),
            ListItemSlots(leading, trailing),
            theme,
            style,
            behavior,
        )
    }
}

/**
 * Rows grouped under an optional [title] (a heading) with an optional [footer]. The section draws the dividers
 * between its rows from `ListItemTheme.dividers`, never after the last one. [level] is the section's fill;
 * whether it's inset or edge to edge is the skin's choice.
 */
@Composable
public fun CamoListSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    level: SurfaceLevel = SurfaceLevel.Surface,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoListSection")
    val theme = requireTheme("CamoListSection")
    val style = remember(skin, theme) { skin.listItem.defaults(theme).merge(theme.components.listItem) }
    skin.listItem.Section(
        ListSectionState(title, footer, level),
        ListSectionSlots { DividedColumn(style.dividers, style.dividerInsetStart, content = content) },
        theme,
        style,
        modifier,
    )
}
