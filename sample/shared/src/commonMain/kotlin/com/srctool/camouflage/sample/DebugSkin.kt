package com.srctool.camouflage.sample

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skin.InteractionFeedback
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme
import kotlinx.coroutines.launch

/** A skin for proving the contract before any real skin exists: plain fills and no press feedback. */
val DebugSkin = CamoSkin(
    name = "debug",
    surfaceTreatment = NoTreatment,
    feedback = NoFeedback,
    text = DebugTextRenderer,
    icon = DebugIconRenderer,
    surface = DebugSurfaceRenderer,
    divider = DebugDividerRenderer,
    button = DebugButtonRenderer,
    iconButton = DebugIconButtonRenderer,
    progressCircle = DebugProgressCircleRenderer,
    tooltip = DebugTooltipRenderer,
)

/** A second debug skin, to show a skin switch: outlined surfaces and a tint while pressed. */
val DebugOutlineSkin = DebugSkin.copy(name = "debug-outline", surfaceTreatment = OutlineTreatment, feedback = PressTint)

/** Fills the container and nothing else: no depth, no border. */
object NoTreatment : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        Modifier.background(color, shape)
}

/** An outline in `outline` instead of depth. */
object OutlineTreatment : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        Modifier.background(color, shape).border(1.dp, theme.colors.outline, shape)
}

/** Draws no feedback. DebugSkin's placeholders draw their own focus border. */
object NoFeedback : InteractionFeedback {
    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme) = NoIndication

    object NoIndication : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}
        override fun equals(other: Any?) = other === this
        override fun hashCode() = 0
    }
}

/** A content-color tint while pressed. */
object PressTint : InteractionFeedback {
    private const val PRESSED_ALPHA = 0.12f

    override fun indication(contentColor: Color, shape: Shape, theme: CamoTheme) = Tint(contentColor)

    data class Tint(val color: Color) : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode = TintNode(interactionSource, color)
    }

    private class TintNode(private val source: InteractionSource, private val color: Color) :
        Modifier.Node(), DrawModifierNode {
        private var pressed = false

        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect {
                    pressed = when (it) {
                        is PressInteraction.Press -> true
                        is PressInteraction.Release, is PressInteraction.Cancel -> false
                        else -> pressed
                    }
                    invalidateDraw()
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (pressed) drawRect(color.copy(alpha = PRESSED_ALPHA))
        }
    }
}
