package com.srctool.camouflage.core.scrollbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.LocalCamoEnvironment
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import kotlinx.coroutines.delay

/**
 * When a scrollbar shows: [Auto] is the skin's choice, [OnScroll] while scrolling and a moment after, [Always]
 * always.
 */
public enum class ScrollbarVisibility { Auto, OnScroll, Always }

/** Every color a scrollbar uses. */
@Immutable
public data class ScrollbarColors(val thumb: Color, val thumbHover: Color, val thumbDrag: Color, val track: Color)

/** Overrides for scrollbars. `null` keeps the skin's default. */
@Immutable
public data class ScrollbarTheme(
    /** The thumb's width at rest. */
    val thickness: Dp? = null,
    /** The thumb's width under the pointer or while dragged. */
    val hoverThickness: Dp? = null,
    /** The shortest thumb. */
    val minThumb: Dp? = null,
    /** What `ScrollbarVisibility.Auto` means for this skin. */
    val visibility: ScrollbarVisibility? = null,
    /** How long the bar stays after scrolling stops (`OnScroll`), in milliseconds. */
    val hideDelayMillis: Long? = null,
    val colors: ScrollbarColors? = null,
)

/** The resolved scrollbar style: every field set; [visibility] is never `Auto`. */
@Immutable
public data class ScrollbarStyle(
    val thickness: Dp,
    val hoverThickness: Dp,
    val minThumb: Dp,
    val visibility: ScrollbarVisibility,
    val hideDelayMillis: Long,
    val colors: ScrollbarColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: ScrollbarTheme?): ScrollbarStyle = if (overrides == null) {
        this
    } else {
        ScrollbarStyle(
            thickness = overrides.thickness ?: thickness,
            hoverThickness = overrides.hoverThickness ?: hoverThickness,
            minThumb = overrides.minThumb ?: minThumb,
            visibility = overrides.visibility?.takeIf { it != ScrollbarVisibility.Auto } ?: visibility,
            hideDelayMillis = overrides.hideDelayMillis ?: hideDelayMillis,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a scrollbar renderer draws inside its lane (as wide as the thickest thumb, as long as the track): the
 * optional track, and the thumb at [thumbStart] for [thumbLength] pixels along [orientation], thin at rest and
 * `hoverThickness` while [hovered] or [dragging], against the end edge (the right in left-to-right).
 */
@Immutable
public data class ScrollbarState(
    val orientation: Orientation,
    val thumbStart: Float,
    val thumbLength: Float,
    val hovered: Boolean,
    val dragging: Boolean,
)

/** Draws the scrollbar; core owns the geometry, the drag, the track click and the fade. */
@Stable
public interface ScrollbarRenderer {
    /** The complete scrollbar style for [theme]. */
    public fun defaults(theme: CamoTheme): ScrollbarStyle

    /** Draws [state] in `modifier`'s bounds (the lane). */
    @Composable
    public fun Render(state: ScrollbarState, theme: CamoTheme, style: ScrollbarStyle, modifier: Modifier)
}

// Touch platforms keep their native indicators; with a pointer on desktop or the web, Camouflage draws the bar.
@Composable
internal fun showsScrollbars(): Boolean {
    val environment = LocalCamoEnvironment.current ?: return false
    return environment.input == InputMode.Pointer &&
        environment.platform != CamoPlatform.Android &&
        environment.platform != CamoPlatform.Ios
}

/**
 * A scrollbar for [adapter]'s content, in the skin's look: a thumb that grows under the pointer, dragged to scroll
 * and a click on the track pages by a viewport. It shows only with pointer input on desktop and the web (nothing on
 * touch platforms), takes no focus and is hidden from screen readers, which scroll the content themselves; wheel
 * events go to the content. A bar with nothing to scroll draws nothing. Place it at the edge of the scrolling
 * content, or use [CamoScrollArea].
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // the geometry, the fade and the lane in one composable
@Composable
public fun CamoScrollbar(
    adapter: CamoScrollAdapter,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    visibility: ScrollbarVisibility = ScrollbarVisibility.Auto,
) {
    if (!showsScrollbars()) return
    val skin = requireSkin("CamoScrollbar")
    val theme = requireTheme("CamoScrollbar")
    val style = remember(skin, theme) { skin.scrollbar.defaults(theme).merge(theme.components.scrollbar) }
    val vertical = orientation == Orientation.Vertical
    val density = LocalDensity.current
    var track by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val viewport = adapter.viewport
    val content = adapter.content
    val offset = adapter.offset
    val thumb = thumbLength(viewport, content, track, with(density) { style.minThumb.toPx() })
    val start = thumbStart(offset, viewport, content, track, thumb)
    val mode = if (visibility == ScrollbarVisibility.Auto) style.visibility else visibility
    var recent by remember { mutableStateOf(false) }
    var seen by remember { mutableFloatStateOf(offset) }
    LaunchedEffect(offset) {
        // Only a scroll shows the bar, not the first composition.
        if (mode == ScrollbarVisibility.OnScroll && offset != seen) {
            seen = offset
            recent = true
            delay(style.hideDelayMillis)
            recent = false
        }
    }
    val shown = thumb > 0f && (mode == ScrollbarVisibility.Always || hovered || dragging || recent)
    val alpha by animateFloatAsState(
        if (shown) 1f else 0f,
        tween(if (shown) theme.motion.durationShort else theme.motion.durationLong),
    )
    val lane = if (vertical) {
        Modifier.fillMaxHeight().width(style.hoverThickness)
    } else {
        Modifier.fillMaxWidth().height(style.hoverThickness)
    }
    Box(
        modifier
            .then(lane)
            .clearAndSetSemantics { }
            .onSizeChanged { track = (if (vertical) it.height else it.width).toFloat() }
            .hoverable(source)
            .thumbGestures(adapter, vertical, track, start, thumb, { dragging = it }, { alpha > 0f }),
    ) {
        if (thumb > 0f) {
            skin.scrollbar.Render(
                ScrollbarState(orientation, start, thumb, hovered, dragging),
                theme,
                style,
                Modifier.fillMaxSize().alpha(alpha),
            )
        }
    }
}

/**
 * [content] with scrollbars laid over it: [vertical] on the end edge (the right in left-to-right) and [horizontal]
 * along the bottom, each for the scrolling container inside [content]. Without pointer input on desktop or the web
 * the bars draw nothing and it is just a box around [content].
 */
@Composable
public fun CamoScrollArea(
    modifier: Modifier = Modifier,
    vertical: CamoScrollAdapter? = null,
    horizontal: CamoScrollAdapter? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
        vertical?.let { EdgeScrollbar(it, Orientation.Vertical) }
        horizontal?.let { EdgeScrollbar(it, Orientation.Horizontal) }
    }
}

/**
 * The bar laid over a box's content at its end edge (vertical) or bottom (horizontal) without changing the box's
 * size: its lane follows the box, it doesn't make the box.
 */
@Composable
internal fun BoxScope.EdgeScrollbar(adapter: CamoScrollAdapter, orientation: Orientation = Orientation.Vertical) {
    Box(Modifier.matchParentSize()) {
        val edge = if (orientation == Orientation.Vertical) Alignment.CenterEnd else Alignment.BottomCenter
        CamoScrollbar(adapter, Modifier.align(edge), orientation)
    }
}

// The thumb is dragged; a click on the track pages a viewport toward it. Wheel events are not consumed.
private fun Modifier.thumbGestures(
    adapter: CamoScrollAdapter,
    vertical: Boolean,
    track: Float,
    start: Float,
    thumb: Float,
    onDragging: (Boolean) -> Unit,
    visible: () -> Boolean,
): Modifier = pointerInput(vertical, track, start, thumb, adapter) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (thumb <= 0f || !visible()) return@awaitEachGesture
        val at = if (vertical) down.position.y else down.position.x
        val direction = pageDirection(at, start, thumb)
        if (direction == 0) {
            onDragging(true)
            drag(down.id) { change ->
                val moved = change.positionChange()
                val along = if (vertical) moved.y else moved.x
                adapter.scrollBy(scrollForThumbDrag(along, adapter.viewport, adapter.content, track, thumb))
                change.consume()
            }
            onDragging(false)
        } else if (waitForUpOrCancellation() != null) {
            adapter.scrollBy(direction * adapter.viewport)
        }
    }
}
