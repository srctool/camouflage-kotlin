package com.srctool.camouflage.skin.cupertino

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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
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
 * Cupertino's feedback: pressed content fades to 35% (no ripple, no overlay on touch), a pointer hovering adds a
 * 4% content-color layer, and keyboard focus draws a 3 dp ring in `primary` just outside the shape.
 */
public object Opacity : InteractionFeedback {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme): OpacityIndication =
        OpacityIndication(contentColor, shape, theme.colors.primary, theme.motion.durationShort)
}

/** [Opacity]'s indication for one content color and shape. */
public data class OpacityIndication(
    val contentColor: Color,
    val shape: Shape,
    val ringColor: Color,
    val durationMillis: Int,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = OpacityNode(interactionSource, this)
}

private const val PRESSED_CONTENT_ALPHA = 0.35f
private const val HOVER_ALPHA = 0.04f
private val RingWidth = 3.dp

private class OpacityNode(
    private val source: InteractionSource,
    private val spec: OpacityIndication,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val faded = Animatable(0f)
    private val hover = Animatable(0f)
    private var hovered = false
    private var pressed = false
    private var keyboardFocus = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                update(interaction)
                launch { faded.animateTo(if (pressed) 1f else 0f, tween(spec.durationMillis)) }
                launch { hover.animateTo(if (hovered && !pressed) 1f else 0f, tween(spec.durationMillis)) }
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
        // Pressed: the content at 35%, drawn in a layer so the whole content fades together.
        val alpha = 1f - faded.value * (1f - PRESSED_CONTENT_ALPHA)
        if (alpha < 1f) {
            val paint = Paint().apply { this.alpha = alpha }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                this@draw.drawContent()
                canvas.restore()
            }
        } else {
            drawContent()
        }
        if (hover.value > 0f) {
            drawOutline(
                spec.shape.createOutline(size, layoutDirection, this),
                spec.contentColor.copy(alpha = HOVER_ALPHA * hover.value),
            )
        }
        if (keyboardFocus) {
            val inset = RingWidth.toPx() / 2
            val ring = Size(size.width + inset * 2, size.height + inset * 2)
            translate(-inset, -inset) {
                drawOutline(
                    spec.shape.createOutline(ring, layoutDirection, this),
                    spec.ringColor,
                    style = Stroke(RingWidth.toPx()),
                )
            }
        }
    }
}
