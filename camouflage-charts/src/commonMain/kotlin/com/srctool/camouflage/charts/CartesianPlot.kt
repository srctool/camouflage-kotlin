package com.srctool.camouflage.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.theme.CamoColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.text.CamoText
import kotlin.math.roundToInt

private const val DASH = 4f
private const val TOOLTIP_GAP = 12f
private val PlotPadding = 8.dp
private val LabelGap = 6.dp

/** Where the plot sits inside a chart's [size]: the rectangle the marks are drawn in, leaving room for the labels. */
internal class PlotLayout(val size: Size, val plot: Rect)

/** Room around the plot: [start] pixels on the start side (the labels), [top] and [bottom], [pad] on the other side. */
internal class PlotInsets(val start: Float, val top: Float, val bottom: Float, val pad: Float)

/** The plot rectangle for a chart [size] with [insets]; the start side is the right in a right-to-left layout. */
internal fun plotLayout(size: Size, insets: PlotInsets, rtl: Boolean): PlotLayout {
    val left = if (rtl) insets.pad else insets.start
    val right = size.width - if (rtl) insets.start else insets.pad
    val top = insets.pad + insets.top
    val bottom = size.height - insets.bottom
    return PlotLayout(size, Rect(left, top, maxOf(left, right), maxOf(top, bottom)))
}

/** What measuring and placing axis labels needs: the [measurer], the label [style] and the layout direction. */
internal data class AxisText(val measurer: TextMeasurer, val style: TextStyle, val rtl: Boolean)

/** Which side of its anchor an axis label sits. */
internal enum class LabelSide { Before, After, Below, Above, Center }

/** An axis label: [text] next to the pixel position [anchor]. */
internal class AxisLabel(val text: String, val anchor: Offset, val side: LabelSide)

/** The measuring and drawing tools every cartesian chart needs, gathered once per composition. */
internal class PlotEnv(
    val renderer: ChartRenderer,
    val text: AxisText,
    val colors: CamoColors,
    val density: Density,
) {
    val rtl: Boolean get() = text.rtl
    val labelHeight: Float = text.measurer.measure("0", text.style).size.height.toFloat()
    val pad: Float = with(density) { PlotPadding.toPx() }
    val gap: Float = with(density) { LabelGap.toPx() }

    /** The insets for value labels [startLabels] on the start side and axis titles above and below. */
    fun insets(startLabels: List<String>, topTitle: Boolean, bottomTitle: Boolean): PlotInsets {
        val titleRoom = labelHeight + gap
        return PlotInsets(
            start = text.measurer.widest(startLabels, text.style) + gap * 2,
            top = if (topTitle) titleRoom else 0f,
            bottom = labelHeight + gap * 2 + if (bottomTitle) titleRoom else 0f,
            pad = pad,
        )
    }

    /** The titles: [bottom] centered below the labels, [top] at the start edge above the plot. */
    fun titles(layout: PlotLayout, top: String?, bottom: String?): List<AxisLabel> {
        val plot = layout.plot
        val labels = ArrayList<AxisLabel>()
        if (bottom != null) {
            val y = plot.bottom + gap * 2 + labelHeight
            labels += AxisLabel(bottom, Offset(plot.center.x, y), LabelSide.Below)
        }
        if (top != null) {
            val x = if (rtl) layout.size.width - pad else pad
            labels += AxisLabel(top, Offset(x, pad + labelHeight / 2), if (rtl) LabelSide.Before else LabelSide.After)
        }
        return labels
    }
}

@Composable
internal fun rememberPlotEnv(): PlotEnv {
    val colors = Camouflage.colors
    val style = Camouflage.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val renderer = LocalChartRenderer.current
    val density = LocalDensity.current
    return remember(renderer, measurer, style, colors, rtl, density) {
        PlotEnv(renderer, AxisText(measurer, style, rtl), colors, density)
    }
}

/** What the pointer and the keyboard point at in a chart: a [pointer] position and a [walk] of (category, series). */
@Stable
internal class PlotInput {
    var pointer by mutableStateOf<Offset?>(null)
    var walk by mutableStateOf<Pair<Int, Int>?>(null)
}

/**
 * The pointer, focus and keys of a chart over [categories] and [series]: Left and Right, Up and Down, Home and End walk
 * the points ([linear] makes all four arrows walk the categories, for a pie), Enter and Space call [onActivate], and
 * [announce] says the current point to a screen reader.
 */
internal fun Modifier.plotInput(
    input: PlotInput,
    categories: Int,
    series: Int,
    announce: String?,
    linear: Boolean = false,
    onActivate: (() -> Unit)? = null,
): Modifier = this
    .semantics {
        if (announce != null) {
            stateDescription = announce
            liveRegion = LiveRegionMode.Polite
        }
    }
    .onFocusChanged { input.walk = if (it.isFocused) input.walk ?: (0 to 0) else null }
    .focusable()
    .onKeyEvent { event ->
        val activate = event.key == Key.Enter || event.key == Key.Spacebar
        val next = if (event.type == KeyEventType.KeyDown) {
            walkTarget(event.key, input.walk ?: (0 to 0), categories, series, linear)
        } else {
            null
        }
        if (event.type == KeyEventType.KeyDown && activate && onActivate != null) onActivate()
        next?.let {
            input.walk = it
            input.pointer = null
        }
        next != null || (activate && onActivate != null)
    }
    .chartPointer { input.pointer = it }

// The (category, series) a key moves to: Left and Right walk categories, Up and Down series, Home and End jump.
internal fun walkTarget(
    key: Key,
    at: Pair<Int, Int>,
    categories: Int,
    series: Int,
    linear: Boolean = false,
): Pair<Int, Int>? {
    if (categories == 0 || series == 0) return null
    val (category, current) = at
    return when (if (linear) linearKey(key) else key) {
        Key.DirectionRight -> (category + 1).coerceAtMost(categories - 1) to current
        Key.DirectionLeft -> (category - 1).coerceAtLeast(0) to current
        Key.DirectionDown -> category to (current + 1).coerceAtMost(series - 1)
        Key.DirectionUp -> category to (current - 1).coerceAtLeast(0)
        Key.MoveHome -> 0 to current
        Key.MoveEnd -> categories - 1 to current
        else -> null
    }
}

internal fun TextMeasurer.widest(texts: List<String>, style: TextStyle): Float =
    texts.maxOfOrNull { measure(it, style).size.width.toFloat() } ?: 0f

internal fun DrawScope.drawAxisLabels(measurer: TextMeasurer, labels: List<AxisLabel>, style: TextStyle) {
    labels.forEach { label ->
        val text = measurer.measure(label.text, style)
        val w = text.size.width.toFloat()
        val h = text.size.height.toFloat()
        val topLeft = when (label.side) {
            LabelSide.Before -> Offset(label.anchor.x - w, label.anchor.y - h / 2)
            LabelSide.After -> Offset(label.anchor.x, label.anchor.y - h / 2)
            LabelSide.Below -> Offset(label.anchor.x - w / 2, label.anchor.y)
            LabelSide.Above -> Offset(label.anchor.x - w / 2, label.anchor.y - h)
            LabelSide.Center -> Offset(label.anchor.x - w / 2, label.anchor.y - h / 2)
        }
        drawText(text, topLeft = topLeft)
    }
}

/** The grid: a line at each of [positions] across the plot (vertical lines when [vertical]), and the axis lines. */
internal fun DrawScope.drawGrid(
    plot: Rect,
    positions: List<Float>,
    vertical: Boolean,
    style: ResolvedChartStyle,
    grid: Color,
    axis: Color,
) {
    val effect = if (style.gridDashed) PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(), DASH.dp.toPx())) else null
    if (style.gridLines) {
        positions.forEach { position ->
            val from = if (vertical) Offset(position, plot.top) else Offset(plot.left, position)
            val to = if (vertical) Offset(position, plot.bottom) else Offset(plot.right, position)
            drawLine(grid, from, to, strokeWidth = 1f, pathEffect = effect)
        }
    }
    drawLine(axis, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), strokeWidth = 1f)
}

/** Reports the pointer's position in the modifier's box while a mouse hovers or a finger touches; `null` on leaving. */
internal fun Modifier.chartPointer(onPointer: (Offset?) -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onPointer)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                when (event.type) {
                    PointerEventType.Exit -> latest(null)
                    PointerEventType.Release -> if (change.type == PointerType.Touch) latest(null)
                    else -> latest(change.position)
                }
            }
        }
    }
}

/** The draw-in's progress, `0` to `1`, restarted when [key] changes; at once when the style or motion say so. */
@Composable
internal fun rememberDrawIn(key: Any?, style: ResolvedChartStyle): Float {
    val motion = Camouflage.motion
    val progress = remember { Animatable(0f) }
    val animate = style.animate && motion.durationLong > 0
    LaunchedEffect(key, animate) {
        if (animate) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(motion.durationLong, easing = motion.easingStandard))
        } else {
            progress.snapTo(1f)
        }
    }
    return progress.value
}

/** One tooltip row: a [color] key, then [text]. */
internal class TooltipRow(val color: Color?, val text: String)

/** A tooltip: [title] and its [rows], beside the pixel position [x] (it flips to the other side near the edge). */
@Composable
internal fun ChartTooltip(title: String, rows: List<TooltipRow>, x: Float, modifier: Modifier = Modifier) {
    val colors = Camouflage.colors
    val spacing = Camouflage.spacing
    val gap = TOOLTIP_GAP
    Column(
        modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(constraints.maxWidth, placeable.height) {
                    val after = x + gap
                    val px = if (after + placeable.width > constraints.maxWidth) x - gap - placeable.width else after
                    placeable.place(px.roundToInt().coerceAtLeast(0), 0)
                }
            }
            .background(colors.inverseSurface, Camouflage.shapes.small)
            .padding(spacing.s),
        Arrangement.spacedBy(spacing.xxs),
    ) {
        CamoText(title, style = Camouflage.typography.labelSmall, color = colors.inverseOnSurface)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                if (row.color != null) Box(Modifier.size(8.dp).background(row.color, CircleShape))
                CamoText(row.text, style = Camouflage.typography.labelMedium, color = colors.inverseOnSurface)
            }
        }
    }
}

// A pie walks one way: Down is Right and Up is Left.
private fun linearKey(key: Key): Key = when (key) {
    Key.DirectionDown -> Key.DirectionRight
    Key.DirectionUp -> Key.DirectionLeft
    else -> key
}
