@file:Suppress("MagicNumber") // the wave's geometry

package com.srctool.camouflage.skin.material.renderer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.progress.ProgressBarRenderer
import com.srctool.camouflage.core.progress.ProgressState
import com.srctool.camouflage.core.progress.ProgressStyle
import com.srctool.camouflage.core.theme.CamoTheme
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val Amplitude = 3.dp
private val WaveLength = 40.dp
private val WaveHeight = 12.dp
private const val STEP = 2f
private const val SEGMENT = 0.4f
private const val SWEEP_MILLIS = 1600
private const val DRIFT_MILLIS = 1500
private const val PULSE_MILLIS = 1200
private const val PULSE_LOW = 0.4f

/**
 * M3 Expressive's linear indicator: the active part is a drifting wave on a straight track, the stop dot stays.
 * Under reduced motion the wave holds still (and indeterminate bars pulse instead of sweeping).
 */
internal object MaterialWavyProgressBarRenderer : ProgressBarRenderer {
    override fun defaults(theme: CamoTheme): ProgressStyle = MaterialProgressBarRenderer.defaults(theme)

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.tone, theme)
        val reduced = theme.motion.durationLong == 0
        val transition = rememberInfiniteTransition()
        val drift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(DRIFT_MILLIS, easing = LinearEasing)))
        val sweep by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(SWEEP_MILLIS, easing = LinearEasing)))
        val pulse by transition.animateFloat(PULSE_LOW, 1f, infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse))
        val value by animateFloatAsState(state.progress ?: 0f, tween(theme.motion.durationMedium))
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val indeterminate = state.progress == null
        Canvas(
            modifier
                .fillMaxWidth()
                .height(WaveHeight)
                .alpha(if (reduced && indeterminate) pulse else 1f),
        ) {
            val w = size.width
            val gap = style.trackGap.toPx()
            val phase = if (reduced) 0f else drift
            val line = Stroke(style.barHeight.toPx(), cap = StrokeCap.Round)
            fun at(x: Float) = if (rtl) w - x else x
            when {
                indeterminate && reduced -> wave(0f, w, phase, colors.active, line, rtl)
                indeterminate -> {
                    val length = w * SEGMENT
                    val start = -length + (w + length) * sweep
                    straight(0f, w, colors.track, line, rtl)
                    wave(max(0f, start), min(w, start + length), phase, colors.active, line, rtl)
                }
                else -> {
                    val end = w * value
                    wave(0f, end, phase, colors.active, line, rtl)
                    straight(min(w, if (end > 0f) end + gap else 0f), w, colors.track, line, rtl)
                    if (style.showStopIndicator) {
                        val r = style.barHeight.toPx() / 2
                        drawCircle(colors.active, r, Offset(at(w - r), size.height / 2))
                    }
                }
            }
        }
    }
}

private fun DrawScope.straight(from: Float, to: Float, color: Color, line: Stroke, rtl: Boolean) {
    if (to <= from) return
    val y = size.height / 2
    val a = if (rtl) size.width - from else from
    val b = if (rtl) size.width - to else to
    drawLine(color, Offset(a, y), Offset(b, y), line.width, StrokeCap.Round)
}

private fun DrawScope.wave(
    from: Float,
    to: Float,
    phase: Float,
    color: Color,
    line: Stroke,
    rtl: Boolean,
) {
    if (to <= from) return
    val length = WaveLength.toPx()
    val amplitude = Amplitude.toPx()
    val mid = size.height / 2
    val path = Path()
    var x = from
    var first = true
    while (x <= to) {
        val px = if (rtl) size.width - x else x
        val py = mid + amplitude * sin(2 * PI * ((x / length) - phase)).toFloat()
        if (first) path.moveTo(px, py) else path.lineTo(px, py)
        first = false
        x += STEP
    }
    val px = if (rtl) size.width - to else to
    path.lineTo(px, mid + amplitude * sin(2 * PI * ((to / length) - phase)).toFloat())
    drawPath(path, color, style = line)
}
