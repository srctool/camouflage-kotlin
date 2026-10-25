package com.srctool.camouflage.skin.minimal

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
 * Minimal's feedback: a content-color overlay (4% hovered, 8% pressed), no ripple and no scale, and a 2 dp
 * focus ring in `primary`, 2 dp outside the shape, for keyboard focus only.
 */
public object Fade : InteractionFeedback {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme): FadeIndication =
        FadeIndication(contentColor, shape, theme.colors.primary, theme.motion.durationShort)
}

/** [Fade]'s indication for one content color and shape. */
public data class FadeIndication(
    val contentColor: Color,
    val shape: Shape,
    val ringColor: Color,
    val durationMillis: Int,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = FadeNode(interactionSource, this)
}

private const val HOVER_ALPHA = 0.04f
private const val PRESSED_ALPHA = 0.08f
private val RingWidth = 2.dp
private val RingOffset = 2.dp

private class FadeNode(
    private val source: InteractionSource,
    private val spec: FadeIndication,
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
            val inset = RingOffset.toPx() + RingWidth.toPx() / 2
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

