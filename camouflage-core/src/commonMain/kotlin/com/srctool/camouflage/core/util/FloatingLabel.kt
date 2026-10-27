package com.srctool.camouflage.core.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.theme.CamoTheme
import kotlin.math.roundToInt

/**
 * A field frame with a `Floating` label, for text field renderers: [label] rests inside the field at [startInset]
 * (centered on the field's height, in [restingStyle]) and moves to sit on the top edge in [floatedStyle] when
 * [floated] (focused or not empty), on a [cutout] background that hides the border behind it. [field] is the
 * framed row; the label hides its semantics (core names the input).
 */
@Composable
public fun FloatingLabelBox(
    label: String,
    floated: Boolean,
    restingStyle: TextStyle,
    floatedStyle: TextStyle,
    color: Color,
    cutout: Color,
    startInset: Dp,
    theme: CamoTheme,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
    val t by animateFloatAsState(
        if (floated) 1f else 0f,
        tween(theme.motion.durationShort, easing = theme.motion.easingStandard),
    )
    Layout(
        content = {
            field()
            BasicText(
                label,
                Modifier
                    .clearAndSetSemantics {}
                    .background(if (t > 0f) cutout.copy(alpha = cutout.alpha * t) else Color.Transparent)
                    .padding(horizontal = (CutoutPadding.value * t).dp),
                style = lerp(restingStyle, floatedStyle, t).copy(color = color),
                maxLines = 1,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val frame = measurables[0].measure(constraints)
        val text = measurables[1].measure(Constraints(maxWidth = frame.width))
        layout(frame.width, frame.height) {
            frame.placeRelative(0, 0)
            val rest = (frame.height - text.height) / 2f
            val top = -text.height / 2f
            val x = startInset.roundToPx() - (CutoutPadding.roundToPx() * t).roundToInt()
            text.placeRelative(x, (rest + (top - rest) * t).roundToInt())
        }
    }
}

private val CutoutPadding: Dp = 4.dp

