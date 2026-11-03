package com.srctool.camouflage.core.carousel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.environment
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.pagination.CamoPagination
import com.srctool.camouflage.core.pagination.PaginationVariant
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget
import kotlinx.coroutines.launch

/** Every color a carousel uses: the previous and next buttons float over the items. */
@Immutable
public data class CarouselColors(val buttonContainer: Color, val buttonContent: Color)

/** Overrides for carousels. `null` keeps the skin's default. */
@Immutable
public data class CarouselTheme(
    val itemShape: Shape? = null,
    val itemSpacing: Dp? = null,
    val peek: Dp? = null,
    val buttonSize: Dp? = null,
    val previousIcon: IconSource? = null,
    val nextIcon: IconSource? = null,
    val colors: CarouselColors? = null,
)

/** The resolved carousels style: every field set. */
@Immutable
public data class CarouselStyle(
    val itemShape: Shape,
    val itemSpacing: Dp,
    val peek: Dp,
    val buttonSize: Dp,
    val previousIcon: IconSource,
    val nextIcon: IconSource,
    val colors: CarouselColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: CarouselTheme?): CarouselStyle = if (overrides == null) {
        this
    } else {
        CarouselStyle(
            itemShape = overrides.itemShape ?: itemShape,
            itemSpacing = overrides.itemSpacing ?: itemSpacing,
            peek = overrides.peek ?: peek,
            buttonSize = overrides.buttonSize ?: buttonSize,
            previousIcon = overrides.previousIcon ?: previousIcon,
            nextIcon = overrides.nextIcon ?: nextIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/** How the items sit: one at a time, or with the next one showing at the edge. */
public enum class CarouselLayout { Paged, Peek }

/** Which way a navigation button goes. */
public enum class CarouselDirection { Previous, Next }

/** What the container renderer draws: the frame around the items and the buttons. */
@Immutable
public data class CarouselState(val page: Int, val pageCount: Int, val layout: CarouselLayout)

/** What a navigation button draws; [enabled] is `false` at the first or last item. */
@Immutable
public data class CarouselButtonState(
    val direction: CarouselDirection,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** Draws carousels in parts: the container, each item's frame and the previous and next buttons. */
@Stable
public interface CarouselRenderer {
    /** The complete carousel style for [theme]. */
    public fun defaults(theme: CamoTheme): CarouselStyle

    /** The frame around [content], the pager of items. */
    @Composable
    public fun Container(state: CarouselState, theme: CamoTheme, style: CarouselStyle, modifier: Modifier,
        content: @Composable () -> Unit)

    /** One item's frame (its mask) around the app's [content]. */
    @Composable
    public fun Item(theme: CamoTheme, style: CarouselStyle, modifier: Modifier, content: @Composable () -> Unit)

    /** A previous or next button. Apply `modifier` (the behavior) to the outermost node. */
    @Composable
    public fun Button(state: CarouselButtonState, theme: CamoTheme, style: CarouselStyle, modifier: Modifier)
}

/**
 * A strip of items you swipe or step through. [state] is Compose's [PagerState] (its `currentPage`, and
 * `animateScrollToPage` to jump). [layout] `Peek` lets the next item show at the edge. Previous and next buttons
 * show on pointer devices by default ([showButtons]); Left and Right (reading direction), Home and End step
 * and jump; the page dots are [CamoPagination]'s. Each item announces "item 2 of 5"; [caption] names the
 * carousel. There is no auto-play.
 */
@Composable
public fun <T> CamoCarousel(
    items: List<T>,
    itemKey: (T) -> Any,
    caption: String,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState { items.size },
    layout: CarouselLayout = CarouselLayout.Paged,
    showButtons: Boolean = Camouflage.environment.input == InputMode.Pointer,
    showIndicator: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    val skin = requireSkin("CamoCarousel")
    val theme = requireTheme("CamoCarousel")
    val style = remember(skin, theme) { skin.carousel.defaults(theme).merge(theme.components.carousel) }
    val scope = rememberCoroutineScope()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val last = items.lastIndex
    val go: (Int) -> Unit = { page -> scope.launch { state.animateScrollToPage(page.coerceIn(0, last)) } }
    val behavior = modifier
        .semantics { contentDescription = caption }
        .focusable()
        .onKeyEvent {
            val target = carouselTarget(it, rtl, state.currentPage, last)
            target?.let(go)
            target != null
        }
    Column(behavior) {
        skin.carousel.Container(CarouselState(state.currentPage, items.size, layout), theme, style, Modifier) {
            Box {
                HorizontalPager(
                    state,
                    contentPadding = PaddingValues(end = if (layout == CarouselLayout.Peek) style.peek else 0.dp),
                    pageSpacing = style.itemSpacing,
                    key = { itemKey(items[it]) },
                ) { page ->
                    skin.carousel.Item(
                        theme,
                        style,
                        Modifier.semantics { contentDescription = "item ${page + 1} of ${items.size}" },
                    ) { content(items[page]) }
                }
                if (showButtons && items.size > 1) Buttons(skin.carousel, theme, style, state.currentPage, last, rtl,
                    go)
            }
        }
        if (showIndicator && items.size > 1) {
            CamoPagination(
                items.size, state.currentPage, { go(it) },
                Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                variant = PaginationVariant.Dots, label = "$caption pages",
            )
        }
    }
}

// The page a key moves to: Left and Right follow the reading direction; Home and End jump.
private fun carouselTarget(event: KeyEvent, rtl: Boolean, page: Int, last: Int): Int? {
    val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
    val backward = if (rtl) Key.DirectionRight else Key.DirectionLeft
    return when {
        event.type != KeyEventType.KeyDown -> null
        event.key == forward -> page + 1
        event.key == backward -> page - 1
        event.key == Key.MoveHome -> 0
        event.key == Key.MoveEnd -> last
        else -> null
    }
}

@Composable
private fun BoxScope.Buttons(
    renderer: CarouselRenderer,
    theme: CamoTheme,
    style: CarouselStyle,
    page: Int,
    last: Int,
    rtl: Boolean,
    go: (Int) -> Unit,
) {
    val start = if (rtl) Alignment.CenterEnd else Alignment.CenterStart
    val end = if (rtl) Alignment.CenterStart else Alignment.CenterEnd
    NavButton(renderer, theme, style, CarouselDirection.Previous, page > 0, rtl, Modifier.align(start)) { go(page - 1) }
    NavButton(renderer, theme, style, CarouselDirection.Next, page < last, rtl, Modifier.align(end)) { go(page + 1) }
}

@Composable
private fun NavButton(
    renderer: CarouselRenderer,
    theme: CamoTheme,
    style: CarouselStyle,
    direction: CarouselDirection,
    enabled: Boolean,
    rtl: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    // The previous button points to the start of the reading direction; the skin's icons are LTR.
    val shown = if (rtl) direction.flip() else direction
    val behavior = modifier
        .semantics {
            contentDescription = if (direction == CarouselDirection.Previous) "Previous item" else "Next item"
        }
        .clickable(source, null, enabled, role = Role.Button, onClick = onClick)
        .minimumTouchTarget()
    renderer.Button(CarouselButtonState(shown, enabled, interaction, source), theme, style, behavior)
}

private fun CarouselDirection.flip() =
    if (this == CarouselDirection.Previous) CarouselDirection.Next else CarouselDirection.Previous
