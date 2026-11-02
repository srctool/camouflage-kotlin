package com.srctool.camouflage.core.pagination

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color pagination uses: the current page's [currentContainer] and [currentContent], the dots. */
@Immutable
public data class PaginationColors(
    val currentContainer: Color,
    val currentContent: Color,
    val content: Color,
    val dot: Color,
    val activeDot: Color,
)

/** Overrides for pagination. `null` keeps the skin's default. */
@Immutable
public data class PaginationTheme(
    val buttonSize: Dp? = null,
    val shape: Shape? = null,
    val dotSize: Dp? = null,
    val gap: Dp? = null,
    val textStyle: TextStyle? = null,
    val previousIcon: IconSource? = null,
    val nextIcon: IconSource? = null,
    val colors: PaginationColors? = null,
)

/** The resolved pagination style: every field set. */
@Immutable
public data class PaginationStyle(
    val buttonSize: Dp,
    val shape: Shape,
    val dotSize: Dp,
    val gap: Dp,
    val textStyle: TextStyle,
    val previousIcon: IconSource,
    val nextIcon: IconSource,
    val colors: PaginationColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: PaginationTheme?): PaginationStyle = if (overrides == null) {
        this
    } else {
        PaginationStyle(
            buttonSize = overrides.buttonSize ?: buttonSize,
            shape = overrides.shape ?: shape,
            dotSize = overrides.dotSize ?: dotSize,
            gap = overrides.gap ?: gap,
            textStyle = overrides.textStyle ?: textStyle,
            previousIcon = overrides.previousIcon ?: previousIcon,
            nextIcon = overrides.nextIcon ?: nextIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/** The form of pagination: numbered pages, a compact "Page 3 of 9", or the dots of a page control. */
public enum class PaginationVariant { Numbered, Compact, Dots }

/** Which way an arrow goes. */
public enum class PageDirection { Previous, Next }

/** What the container renderer draws: the row around the parts. */
@Immutable
public data class PaginationState(val pageCount: Int, val page: Int, val variant: PaginationVariant)

/** What a page button draws: page [index] (zero-based; show `index + 1`), the [current] one highlighted. */
@Immutable
public data class PageButtonState(
    val index: Int,
    val current: Boolean,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** What an arrow draws; [enabled] is `false` at the first or last page. */
@Immutable
public data class PageArrowState(
    val direction: PageDirection,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** What a dot of the page control draws. */
@Immutable
public data class PageDotState(
    val active: Boolean,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** Draws pagination in parts. Apply `modifier` (the behavior) to the outermost node of each interactive part. */
@Stable
public interface PaginationRenderer {
    /** The complete pagination style for [theme]. */
    public fun defaults(theme: CamoTheme): PaginationStyle

    /** The row around [content]. */
    @Composable
    public fun Container(
        state: PaginationState,
        theme: CamoTheme,
        style: PaginationStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    )

    /** A page button showing [label] (the page number). */
    @Composable
    public fun Button(
        state: PageButtonState,
        label: String,
        theme: CamoTheme,
        style: PaginationStyle,
        modifier: Modifier,
    )

    /** A previous or next arrow. */
    @Composable
    public fun Arrow(state: PageArrowState, theme: CamoTheme, style: PaginationStyle, modifier: Modifier)

    /** The gap ("…") between non-adjacent pages. */
    @Composable
    public fun Gap(theme: CamoTheme, style: PaginationStyle)

    /** A dot of the page control. */
    @Composable
    public fun Dot(state: PageDotState, theme: CamoTheme, style: PaginationStyle, modifier: Modifier)

    /** The compact form's text ("Page 3 of 9"). */
    @Composable
    public fun Summary(text: String, theme: CamoTheme, style: PaginationStyle)
}

/** One item of the numbered form: a page (zero-based) or a gap. */
internal sealed interface PageItem {
    data class Page(val index: Int) : PageItem

    data object Gap : PageItem
}

// The numbered form's fixed slots (first, last, current and two gaps) and where its middle pages start (1-based).
private const val FIXED_SLOTS = 5
private const val FIRST_MIDDLE = 3

/**
 * The items of the numbered form: always the first and last page, [siblings] pages around [page], and a gap
 * where pages are left out (a gap that would hide a single page shows that page instead), so the length is
 * fixed at `2 * siblings + 5` once there are more pages than that.
 */
internal fun pageItems(pageCount: Int, page: Int, siblings: Int): List<PageItem> {
    val total = pageCount
    if (total <= siblings * 2 + FIXED_SLOTS) return List(total) { PageItem.Page(it) }
    val current = page + 1
    val startSiblings = maxOf(minOf(current - siblings, total - siblings * 2 - 2), FIRST_MIDDLE)
    val endSiblings = minOf(maxOf(current + siblings, siblings * 2 + FIRST_MIDDLE), total - 2)
    val items = mutableListOf<PageItem>(PageItem.Page(0))
    items += if (startSiblings > FIRST_MIDDLE) PageItem.Gap else PageItem.Page(1)
    for (n in startSiblings..endSiblings) items += PageItem.Page(n - 1)
    items += if (endSiblings < total - 2) PageItem.Gap else PageItem.Page(total - 2)
    items += PageItem.Page(total - 1)
    return items
}

/**
 * Moves through pages. [page] is zero-based. One Tab stop: the arrow keys move between the parts and Space or
 * Enter picks one. [label] names the navigation; a page announces "Page 3" and the current one "current page".
 * With one page or fewer it draws nothing.
 */
@Composable
public fun CamoPagination(
    pageCount: Int,
    page: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    variant: PaginationVariant = PaginationVariant.Numbered,
    siblings: Int = 1,
    enabled: Boolean = true,
    label: String = "Pagination",
) {
    if (pageCount <= 1) return
    val skin = requireSkin("CamoPagination")
    val theme = requireTheme("CamoPagination")
    val style = remember(skin, theme) { skin.pagination.defaults(theme).merge(theme.components.pagination) }
    val current = page.coerceIn(0, pageCount - 1)
    val focus = rememberRovingFocus()
    val behavior = modifier.semantics {
        contentDescription = label
        isTraversalGroup = true
    }
    skin.pagination.Container(PaginationState(pageCount, current, variant), theme, style, behavior) {
        val parts = Parts(skin.pagination, theme, style, focus, enabled, onPageChange)
        parts.Arrow(PageDirection.Previous, 0, current - 1, current > 0)
        when (variant) {
            PaginationVariant.Numbered -> {
                pageItems(pageCount, current, siblings).forEachIndexed { i, item ->
                    when (item) {
                        is PageItem.Page -> parts.Page(item.index, i + 1, item.index == current)
                        PageItem.Gap -> skin.pagination.Gap(theme, style)
                    }
                }
            }
            PaginationVariant.Compact -> skin.pagination.Summary("Page ${current + 1} of $pageCount", theme, style)
            PaginationVariant.Dots -> for (i in 0 until pageCount) parts.Dot(i, i + 1, i == current)
        }
        parts.Arrow(PageDirection.Next, pageCount + 2, current + 1, current < pageCount - 1)
    }
}

// The behavior of each part: roving focus (arrows move, Enter picks), semantics and the page change.
private class Parts(
    val renderer: PaginationRenderer,
    val theme: CamoTheme,
    val style: PaginationStyle,
    val focus: RovingFocus,
    val enabled: Boolean,
    val onPageChange: (Int) -> Unit,
) {
    @Composable
    fun Page(index: Int, slot: Int, current: Boolean) {
        val source = remember { MutableInteractionSource() }
        val interaction = source.collectInteractionState()
        val behavior = Modifier
            .rovingFocusItem(focus, slot, enabled, selected = current, onSelect = {})
            .semantics {
                contentDescription = "Page ${index + 1}"
                selected = current
                if (current) stateDescription = "current page"
            }
            .clickable(source, null, enabled, role = Role.Button) { onPageChange(index) }
            .minimumTouchTarget()
        renderer.Button(PageButtonState(index, current, enabled, interaction, source), "${index + 1}", theme, style,
            behavior)
    }

    @Composable
    fun Dot(index: Int, slot: Int, active: Boolean) {
        val source = remember { MutableInteractionSource() }
        val interaction = source.collectInteractionState()
        val behavior = Modifier
            .rovingFocusItem(focus, slot, enabled, selected = active, onSelect = {})
            .semantics {
                contentDescription = "Page ${index + 1}"
                selected = active
            }
            .clickable(source, null, enabled, role = Role.Button) { onPageChange(index) }
            .minimumTouchTarget()
        renderer.Dot(PageDotState(active, enabled, interaction, source), theme, style, behavior)
    }

    @Composable
    fun Arrow(direction: PageDirection, slot: Int, target: Int, available: Boolean) {
        val source = remember { MutableInteractionSource() }
        val interaction = source.collectInteractionState()
        val on = enabled && available
        val behavior = Modifier
            .rovingFocusItem(focus, slot, on, selected = false, onSelect = {})
            .semantics {
                contentDescription = if (direction == PageDirection.Previous) "Previous page" else "Next page"
            }
            .clickable(source, null, on, role = Role.Button) { onPageChange(target) }
            .minimumTouchTarget()
        renderer.Arrow(PageArrowState(direction, on, interaction, source), theme, style, behavior)
    }
}
