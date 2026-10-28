package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.levelOf
import kotlin.random.Random

/**
 * Fluent's Acrylic surface: frosted glass for flyouts, menus and dialogs (elevation `Level4` and up). It draws
 * the app behind it blurred (30 dp), a luminosity tint of the surface color and a light noise, inside Fluent's
 * shadow. The app behind is recorded by [FluentBackdrop]; without one, or where the platform can't blur
 * (Android below 12), it falls back to the tint and noise over a solid fill, which is Fluent's own fallback.
 * Lower containers (cards) stay [FluentSolid].
 */
public object FluentAcrylic : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        if (theme.elevation.levelOf(elevation) < ElevationLevel.Level4) {
            FluentSolid.modifier(color, shape, elevation, theme)
        } else {
            FluentSolid.shadows(shape, elevation, theme).then(AcrylicElement(color, shape))
        }
}

// What FluentBackdrop shares with the Acrylic surfaces: the recorded app, where it sits, and whether it's being
// recorded right now (an Acrylic surface inside the app then draws its fallback, instead of itself).
internal class Backdrop {
    var layer: GraphicsLayer? = null
    var origin: Offset = Offset.Zero
    var recording: Boolean = false

    // Bumped after each recording, so Acrylic surfaces (in popups) redraw when the app behind them changes.
    var version by mutableIntStateOf(0)
}

internal val LocalFluentBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The source Fluent's Acrylic surfaces blur: wrap the app's content in it (`FluentPreview` does it for
 * previews). Without it, Acrylic surfaces draw their solid fallback.
 */
@Composable
public fun FluentBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember { Backdrop() }
    backdrop.layer = layer
    CompositionLocalProvider(LocalFluentBackdrop provides backdrop) {
        Box(
            modifier
                .onGloballyPositioned { backdrop.origin = it.positionOnScreen() }
                .drawWithContent {
                    backdrop.recording = true
                    layer.record { this@drawWithContent.drawContent() }
                    backdrop.recording = false
                    drawLayer(layer)
                    // Unobserved: reading it here would redraw the backdrop on its own write, forever.
                    Snapshot.withoutReadObservation { backdrop.version++ }
                },
        ) { content() }
    }
}

private val BlurRadius = 30.dp
private const val TINT_ALPHA = 0.75f
private const val NOISE_ALPHA = 0.02f
private const val NOISE_SIZE = 64
private const val NOISE_SEED = 7

// A tile of grey speckles, generated once at runtime (nothing bundled); the same every run, for goldens.
private val Noise: ImageBitmap by lazy {
    val bitmap = ImageBitmap(NOISE_SIZE, NOISE_SIZE)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val random = Random(NOISE_SEED)
    for (x in 0 until NOISE_SIZE) {
        for (y in 0 until NOISE_SIZE) {
            paint.color = if (random.nextBoolean()) Color.White else Color.Black
            canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
        }
    }
    bitmap
}

private val NoiseBrush by lazy { ShaderBrush(ImageShader(Noise, TileMode.Repeated, TileMode.Repeated)) }

private data class AcrylicElement(val color: Color, val shape: Shape) : ModifierNodeElement<AcrylicNode>() {
    override fun create() = AcrylicNode(color, shape)

    override fun update(node: AcrylicNode) {
        node.color = color
        node.shape = shape
        node.invalidateDraw()
    }
}

private class AcrylicNode(var color: Color, var shape: Shape) :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {
    private var position = Offset.Zero
    private var blurred: GraphicsLayer? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        position = coordinates.positionOnScreen()
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val outline = shape.createOutline(size, layoutDirection, this)
        val backdrop = currentValueOf(LocalFluentBackdrop)
        val source = backdrop?.takeUnless { it.recording }?.layer
        val radius = BlurRadius.toPx()
        val blur = BlurEffect(radius, radius, TileMode.Clamp)
        if (backdrop != null && source != null && blur.isSupported()) {
            backdrop.version // read, so a new recording redraws this surface
            val layer = blurred ?: requireGraphicsContext().createGraphicsLayer().also { blurred = it }
            layer.renderEffect = blur
            layer.record(size.toIntSize()) {
                translate(backdrop.origin.x - position.x, backdrop.origin.y - position.y) { drawLayer(source) }
            }
            clipPath(Path().apply { addOutline(outline) }) {
                drawLayer(layer)
                drawRect(color.copy(alpha = color.alpha * TINT_ALPHA))
                drawRect(NoiseBrush, alpha = NOISE_ALPHA)
            }
        } else {
            drawOutline(outline, color)
            clipPath(Path().apply { addOutline(outline) }) { drawRect(NoiseBrush, alpha = NOISE_ALPHA) }
        }
        drawContent()
    }

    override fun onDetach() {
        blurred?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        blurred = null
    }
}
