package com.srctool.camouflage.skin.material

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.InteractionFeedback
import com.srctool.camouflage.core.theme.CamoTheme
import kotlinx.coroutines.launch

/**
 * Material 3's feedback: a bounded ripple from the touch point, a state layer (8% hovered, 10% focused or
 * pressed) over the bounds, and a 3 dp `primary` focus ring outside the shape for keyboard focus only.
 */
public object Ripple : InteractionFeedback {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme): RippleIndication =
        RippleIndication(contentColor, shape, theme.colors.primary, theme.motion.durationShort)
}

/** [Ripple]'s indication for one content color and shape. */
public data class RippleIndication(
    val contentColor: Color,
    val shape: Shape,
    val ringColor: Color,
    val durationMillis: Int,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        RippleNode(interactionSource, this)
}

private const val HOVER_ALPHA = 0.08f
private const val FOCUS_ALPHA = 0.10f
private const val PRESSED_ALPHA = 0.10f
private const val DRAGGED_ALPHA = 0.16f
private val RingWidth = 3.dp
private val RingOffset = 0.dp

// The touch ripple is the material-ripple node; the state layer and the focus ring are drawn here.
private class RippleNode(
    private val source: InteractionSource,
    private val spec: RippleIndication,
) : DelegatingNode(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val layer = Animatable(0f)
    private var hovered = false
    private var focused = false
    private var pressed = false
    private var keyboardFocus = false

    init {
        delegate(
            createRippleModifierNode(
                interactionSource = source,
                bounded = true,
                radius = Dp.Unspecified,
                color = { spec.contentColor },
                rippleAlpha = {
                    RippleAlpha(
                        draggedAlpha = DRAGGED_ALPHA,
                        focusedAlpha = 0f,
                        hoveredAlpha = 0f,
                        pressedAlpha = PRESSED_ALPHA,
                    )
                },
            ),
        )
    }

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                update(interaction)
                val target = when {
                    focused && keyboardFocus -> FOCUS_ALPHA
                    hovered -> HOVER_ALPHA
                    else -> 0f
                }
                launch { layer.animateTo(target, tween(spec.durationMillis)) }
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
            is FocusInteraction.Focus -> {
                focused = true
                keyboardFocus = currentValueOf(LocalInputModeManager).inputMode == InputMode.Keyboard
            }
            is FocusInteraction.Unfocus -> {
                focused = false
                keyboardFocus = false
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (layer.value > 0f) {
            drawOutline(
                spec.shape.createOutline(size, layoutDirection, this),
                spec.contentColor.copy(alpha = spec.contentColor.alpha * layer.value),
            )
        }
        if (focused && keyboardFocus) {
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
