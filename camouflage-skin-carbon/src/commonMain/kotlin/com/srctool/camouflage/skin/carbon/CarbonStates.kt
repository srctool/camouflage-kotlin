package com.srctool.camouflage.skin.carbon

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.InteractionFeedback
import com.srctool.camouflage.core.theme.CamoTheme
import kotlinx.coroutines.launch

/**
 * Carbon's feedback: the fill moves to its hover and active step, drawn as a content-color overlay (8% hovered,
 * 20% pressed; no ripple, no scale), and keyboard focus is a 2 dp `primary` border *inside* the bounds with a
 * 1 dp `surface` line inside it (Carbon's focus never changes the layout and reads on a filled button).
 */
public object CarbonStates : InteractionFeedback {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme): CarbonStatesIndication =
        CarbonStatesIndication(
            contentColor, shape, theme.colors.primary, theme.colors.surface, theme.motion.durationShort,
        )
}

/** [CarbonStates]'s indication for one content color and shape. */
public data class CarbonStatesIndication(
    val contentColor: Color,
    val shape: Shape,
    val ringColor: Color,
    val innerColor: Color,
    val durationMillis: Int,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        CarbonStatesNode(interactionSource, this)
}

private const val HOVER_ALPHA = 0.08f
private const val PRESSED_ALPHA = 0.2f
private val RingWidth = 2.dp
private val InnerWidth = 1.dp

private class CarbonStatesNode(
    private val source: InteractionSource,
    private val spec: CarbonStatesIndication,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val overlay = Animatable(0f)
    private var hovered = false
    private var pressed = false
    private var keyboardFocus = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                update(interaction)
                val target = when {
                    pressed -> PRESSED_ALPHA
                    hovered -> HOVER_ALPHA
                    else -> 0f
                }
                launch { overlay.animateTo(target, tween(spec.durationMillis)) }
                invalidateDraw()
            }
        }
    }

    private fun update(interaction: Interaction) {
        when (interaction) {
            is HoverInteraction.Enter -> hovered = true
            is HoverInteraction.Exit -> hovered = false
            is PressInteraction.Press -> pressed = true
            is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
            is FocusInteraction.Focus ->
                keyboardFocus = currentValueOf(LocalInputModeManager).inputMode == InputMode.Keyboard
            is FocusInteraction.Unfocus -> keyboardFocus = false
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (overlay.value > 0f) {
            drawOutline(
                spec.shape.createOutline(size, layoutDirection, this),
                spec.contentColor.copy(alpha = spec.contentColor.alpha * overlay.value),
            )
        }
        if (keyboardFocus) {
            drawInset(RingWidth.toPx(), spec.ringColor)
            drawInset(InnerWidth.toPx(), spec.innerColor, RingWidth.toPx())
        }
    }

    // A border of [width] inside the bounds, [offset] in from the edge (a stroke is centered on its path).
    private fun ContentDrawScope.drawInset(width: Float, color: Color, offset: Float = 0f) {
        val inset = offset + width / 2
        val box = Size(size.width - inset * 2, size.height - inset * 2)
        if (box.width <= 0f || box.height <= 0f) return
        translate(inset, inset) {
            drawOutline(spec.shape.createOutline(box, layoutDirection, this), color, style = Stroke(width))
        }
    }
}
