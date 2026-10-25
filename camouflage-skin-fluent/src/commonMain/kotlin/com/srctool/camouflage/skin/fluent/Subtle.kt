package com.srctool.camouflage.skin.fluent

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
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
 * Fluent's feedback. Under a Fluent theme the renderers swap fills to [FluentTokens]' hover and pressed steps
 * themselves, so `Subtle` draws only the keyboard focus indicator: Fluent's double stroke, 2 dp `strokeFocus2`
 * outside and 1 dp `strokeFocus1` inside. Under another theme (a renderer borrowed into Minimal, a brand theme
 * not built with [fluentTheme]) it also draws a 4% / 8% content overlay, so hover and press still show.
 */
public object Subtle : InteractionFeedback {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme): SubtleIndication {
        val tokens = theme.tokens<FluentTokens>()
        return SubtleIndication(
            overlayColor = if (tokens == null) contentColor else Color.Unspecified,
            shape = shape,
            outer = tokens?.strokeFocus2 ?: theme.colors.onSurface,
            inner = tokens?.strokeFocus1 ?: theme.colors.surface,
            durationMillis = theme.motion.durationShort,
        )
    }
}

/** [Subtle]'s indication. [overlayColor] is `Unspecified` when the renderer draws the state steps. */
public data class SubtleIndication(
    val overlayColor: Color,
    val shape: Shape,
    val outer: Color,
    val inner: Color,
    val durationMillis: Int,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = SubtleNode(interactionSource, this)
}

private const val HOVER_ALPHA = 0.04f
private const val PRESSED_ALPHA = 0.08f
private val Outer = 2.dp
private val Inner = 1.dp

private class SubtleNode(
    private val source: InteractionSource,
    private val spec: SubtleIndication,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val overlay = Animatable(0f)
    private var hovered = false
    private var pressed = false
    private var keyboardFocus = false

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                update(interaction)
                if (spec.overlayColor != Color.Unspecified) {
                    val target = when {
                        pressed -> PRESSED_ALPHA
                        hovered -> HOVER_ALPHA
                        else -> 0f
                    }
                    launch { overlay.animateTo(target, tween(spec.durationMillis)) }
                }
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
                spec.overlayColor.copy(alpha = spec.overlayColor.alpha * overlay.value),
            )
        }
        if (keyboardFocus) {
            // Both strokes sit inside the bounds: the outer one along the edge, the inner one just within it.
            stroke(spec.outer, Outer, inset = 0.dp)
            stroke(spec.inner, Inner, inset = Outer)
        }
    }

    private fun DrawScope.stroke(color: Color, width: Dp, inset: Dp) {
        val offset = inset.toPx() + width.toPx() / 2
        val area = Size(size.width - offset * 2, size.height - offset * 2)
        translate(offset, offset) {
            drawOutline(spec.shape.createOutline(area, layoutDirection, this), color, style = Stroke(width.toPx()))
        }
    }
}
