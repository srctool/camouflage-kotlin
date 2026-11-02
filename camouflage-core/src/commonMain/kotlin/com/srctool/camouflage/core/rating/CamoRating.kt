package com.srctool.camouflage.core.rating

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Every color a rating uses: [hover] is the preview under the pointer. */
@Immutable
public data class RatingColors(val filled: Color, val empty: Color, val hover: Color, val disabled: Color)

/** Overrides for ratings. `null` keeps the skin's default. */
@Immutable
public data class RatingTheme(
    val icon: IconSource? = null,
    val smallSize: Dp? = null,
    val mediumSize: Dp? = null,
    val largeSize: Dp? = null,
    val gap: Dp? = null,
    val colors: RatingColors? = null,
)

/** The resolved ratings style: every field set. */
@Immutable
public data class RatingStyle(
    val icon: IconSource,
    val smallSize: Dp,
    val mediumSize: Dp,
    val largeSize: Dp,
    val gap: Dp,
    val colors: RatingColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: RatingTheme?): RatingStyle = if (overrides == null) {
        this
    } else {
        RatingStyle(
            icon = overrides.icon ?: icon,
            smallSize = overrides.smallSize ?: smallSize,
            mediumSize = overrides.mediumSize ?: mediumSize,
            largeSize = overrides.largeSize ?: largeSize,
            gap = overrides.gap ?: gap,
            colors = overrides.colors ?: colors,
        )
    }


    /** The icon size for [size]. */
    public fun sizeOf(size: RatingSize): Dp = when (size) {
        RatingSize.Small -> smallSize
        RatingSize.Medium -> mediumSize
        RatingSize.Large -> largeSize
    }
}

/** How finely a rating can be set. */
public enum class RatingStep { Whole, Half }

/** The size of a rating's icons. */
public enum class RatingSize { Small, Medium, Large }

/**
 * What the rating renderer draws: [max] icons, each filled up to [value]; while the pointer is over an
 * interactive rating, draw [hoverValue] in the hover color instead. The icons must share the width evenly
 * (each icon plus its gap), because core maps the pointer position to a value from the rating's width.
 */
@Immutable
public data class RatingState(
    val value: Float,
    val hoverValue: Float?,
    val max: Int,
    val size: RatingSize,
    val interactive: Boolean,
    val enabled: Boolean,
    val interaction: InteractionState,
)

/** Draws ratings. Apply `modifier` (the behavior) to the outermost node. */
@Stable
public interface RatingRenderer {
    /** The complete rating style for [theme]. */
    public fun defaults(theme: CamoTheme): RatingStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: RatingState, theme: CamoTheme, style: RatingStyle, modifier: Modifier)
}

/** A rating of [value] out of [max], read-only when [onValueChange] is `null`. [label] names it ("Quality"). */
@Composable
public fun CamoRating(
    value: Float,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: ((Float) -> Unit)? = null,
    max: Int = DEFAULT_MAX,
    step: RatingStep = RatingStep.Whole,
    size: RatingSize = RatingSize.Medium,
    enabled: Boolean = true,
) {
    val skin = requireSkin("CamoRating")
    val theme = requireTheme("CamoRating")
    val style = remember(skin, theme) { skin.rating.defaults(theme).merge(theme.components.rating) }
    val interactive = onValueChange != null && enabled
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    var hover by remember { mutableStateOf<Float?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val unit = if (step == RatingStep.Half) HALF else 1f
    val set: (Float) -> Unit = { v -> onValueChange?.invoke(v.coerceIn(0f, max.toFloat())) }
    var behavior = modifier.semantics(mergeDescendants = true) {
        contentDescription = label
        stateDescription = "${formatRating(value)} of $max stars"
        progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..max.toFloat(), (max / unit).toInt() - 1)
        if (interactive) {
            setProgress { target ->
                set(snap(target, unit, max))
                true
            }
        }
    }
    if (interactive) {
        behavior = behavior
            .focusable(interactionSource = source)
            .onKeyEvent { ratingKey(it, value, unit, max, rtl, set) }
            .pointerInput(max, unit, rtl) {
                detectTapGestures { set(valueAt(it.x, this.size.width, max, unit, rtl)) }
            }
            .pointerInput(max, unit, rtl) {
                detectHorizontalDragGestures(
                    onDragStart = { set(valueAt(it.x, this.size.width, max, unit, rtl)) },
                    onHorizontalDrag = { change, _ -> set(valueAt(change.position.x, this.size.width, max, unit,
                        rtl)) },
                    onDragEnd = {},
                )
            }
            .pointerInput(max, unit, rtl) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        hover = when {
                            change.type != PointerType.Mouse || event.type == PointerEventType.Exit -> null
                            else -> valueAt(change.position.x, this.size.width, max, unit, rtl)
                        }
                    }
                }
            }
    }
    skin.rating.Render(
        RatingState(value, hover, max, size, interactive, enabled, interaction),
        theme,
        style,
        behavior,
    )
}

internal const val DEFAULT_MAX = 5
private const val HALF = 0.5f

// The step the pointer position at [x] of [width] lands on: the icon it is in, or its half.
internal fun valueAt(x: Float, width: Int, max: Int, unit: Float, rtl: Boolean): Float {
    if (width <= 0) return 0f
    val fraction = (x / width).coerceIn(0f, 1f).let { if (rtl) 1f - it else it }
    return snap(fraction * max, unit, max)
}

internal fun snap(value: Float, unit: Float, max: Int): Float = (ceil(value / unit) * unit).coerceIn(unit,
    max.toFloat())

internal fun formatRating(value: Float): String =
    if (value == value.toInt().toFloat()) {
        value.toInt().toString()
    } else {
        (value * TENTH).roundToInt().div(TENTH).toString()
    }

private const val TENTH = 10f

private fun ratingKey(
    event: androidx.compose.ui.input.key.KeyEvent,
    value: Float,
    unit: Float,
    max: Int,
    rtl: Boolean,
    set: (Float) -> Unit,
): Boolean {
    val target = if (event.type == KeyEventType.KeyDown) ratingTarget(event.key, value, unit, max, rtl) else null
    target?.let(set)
    return target != null
}

// The value a key moves to, or `null` for a key the rating ignores.
private fun ratingTarget(key: Key, value: Float, unit: Float, max: Int, rtl: Boolean): Float? {
    val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
    val backward = if (rtl) Key.DirectionRight else Key.DirectionLeft
    return when (key) {
        forward, Key.DirectionUp -> value + unit
        backward, Key.DirectionDown -> value - unit
        Key.MoveHome -> 0f
        Key.MoveEnd -> max.toFloat()
        else -> null
    }
}
