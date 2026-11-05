package com.srctool.camouflage.skin.cupertino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.levelOf

/** How Cupertino draws its materials (bars, sheets, menus, popovers, glass controls): see [cupertinoSkin]. */
public enum class CupertinoSurface { Classic, LiquidGlass }

/**
 * Cupertino's Classic surfaces: opaque fills below `Level2`; from `Level2` a system blur material, the app behind
 * blurred (20 dp) under the surface color at 80%, with a soft shadow from `Level3`. Without a [CupertinoBackdrop]
 * around the app, where the platform can't blur (`CamoCapabilities.backdropBlur`), or with Reduce Transparency
 * the material is the plain surface color, the end of the chain (ADR-42).
 */
public object Classic : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        material(color, shape, elevation, theme, glass = false)
}

/**
 * Cupertino's Liquid Glass (iOS 26): the [Classic] material with the backdrop refracted at the edges, lighter
 * tint and a specular edge highlight. Where custom shaders aren't available (`CamoCapabilities.runtimeShaders`)
 * the refraction is the part dropped; the rest of the chain is [Classic]'s.
 */
public object LiquidGlass : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme): Modifier =
        material(color, shape, elevation, theme, glass = true)
}

private fun material(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme, glass: Boolean): Modifier {
    val level = theme.elevation.levelOf(elevation)
    val capabilities = theme.environment?.capabilities
    val opaque = level < ElevationLevel.Level2 || capabilities?.reduceTransparency == true
    if (opaque) return Modifier.background(color, shape)
    val shadow = when {
        // Glass is always lifted: over a light page its tint matches the page, so the shadow and the rim show it.
        glass && level < ElevationLevel.Level3 ->
            Modifier.dropShadow(shape, Shadow(radius = 5.dp, color = Black16, offset = DpOffset(0.dp, 1.dp)))
        level >= ElevationLevel.Level4 ->
            Modifier.dropShadow(shape, Shadow(radius = 16.dp, color = Black20, offset = DpOffset(0.dp, 4.dp)))
        level >= ElevationLevel.Level3 ->
            Modifier.dropShadow(shape, Shadow(radius = 6.dp, color = Black16, offset = DpOffset(0.dp, 2.dp)))
        else -> Modifier
    }
    return shadow.then(
        MaterialElement(
            color = color,
            shape = shape,
            blur = capabilities?.backdropBlur ?: true,
            refract = glass && (capabilities?.runtimeShaders ?: true),
            glass = glass,
        ),
    )
}

private val Black16 = Color.Black.copy(alpha = 0.16f)
private val Black20 = Color.Black.copy(alpha = 0.20f)

// What CupertinoBackdrop shares with the materials: the recorded content, where it sits, and the root it is drawn in.
internal class Backdrop {
    var layer: GraphicsLayer? = null
    var origin: Offset = Offset.Zero

    // The root of the tree the backdrop is drawn in: a material in the same root is opaque, see [MaterialNode].
    var root: LayoutCoordinates? = null
}

internal val LocalCupertinoBackdrop = staticCompositionLocalOf<Backdrop?> { null }

// Whether the composable is part of the content a backdrop records (as opposed to its overlay).
internal val LocalInBackdropContent = staticCompositionLocalOf { false }

/**
 * The source Cupertino's blur materials and Liquid Glass sample. [content] is the app: it is recorded once, and drawn
 * from that recording. A material blurs the recording only when it is not part of it: the ones in [overlay] (the top
 * bar, the tab bar, a floating toolbar, drawn over the content, aligned with the box scope), and the ones in another
 * window or popup (a menu, a dialog, a sheet). A material inside [content] itself, and any material without a
 * backdrop, draws its opaque fill: it would otherwise sample a layer that contains it.
 */
@Composable
public fun CupertinoBackdrop(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember { Backdrop() }
    backdrop.layer = layer
    CompositionLocalProvider(LocalCupertinoBackdrop provides backdrop) {
        Box(modifier) {
            Box(
                Modifier
                    .onGloballyPositioned {
                        backdrop.origin = it.positionOnScreen()
                        backdrop.root = it.findRootCoordinates()
                    }
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    },
            ) {
                CompositionLocalProvider(LocalInBackdropContent provides true) { content() }
            }
            CompositionLocalProvider(LocalInBackdropContent provides false) { overlay() }
        }
    }
}

private val ClassicBlur = 20.dp
private val GlassBlur = 6.dp
private val GlassDepth = 14.dp
private const val CLASSIC_TINT = 0.8f
private const val GLASS_TINT = 0.45f
private const val SPECULAR_WIDTH = 1f
private const val RIM_ALPHA = 0.12f

private data class MaterialElement(
    val color: Color,
    val shape: Shape,
    val blur: Boolean,
    val refract: Boolean,
    val glass: Boolean,
) : ModifierNodeElement<MaterialNode>() {
    override fun create() = MaterialNode(color, shape, blur, refract, glass)

    override fun update(node: MaterialNode) {
        node.color = color
        node.shape = shape
        node.blur = blur
        node.refract = refract
        node.glass = glass
        node.invalidateDraw()
    }
}

private class MaterialNode(
    var color: Color,
    var shape: Shape,
    var blur: Boolean,
    var refract: Boolean,
    var glass: Boolean,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {
    private var position = Offset.Zero
    private var root: LayoutCoordinates? = null
    private var sampled: GraphicsLayer? = null
    private val glassEffects = GlassEffects()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        position = coordinates.positionOnScreen()
        root = coordinates.findRootCoordinates()
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val outline = shape.createOutline(size, layoutDirection, this)
        val backdrop = currentValueOf(LocalCupertinoBackdrop)
        val source = sourceOf(backdrop)
        val effect = effectOrNull()
        if (backdrop != null && source != null && effect != null) {
            paintBlurred(backdrop, source, effect, outline)
        } else {
            drawOutline(outline, color)
        }
        if (glass) drawSpecular(outline)
        drawContent()
    }

    // The layer to blur, or `null` for the opaque fill: no blur capability, no backdrop, or a material in the
    // backdrop's content. That one would sample a layer that contains it: the layer's render nodes include the
    // material's ancestors, which draw the material, which draws the layer (a cycle that overflows the renderer's
    // stack, on every target once the app has layered ancestors). The overlay and other roots (a popup, a menu, a
    // dialog) are not in the recording, so they blur. Until the roots are known a material is taken to be inside.
    private fun CompositionLocalConsumerModifierNode.sourceOf(backdrop: Backdrop?): GraphicsLayer? {
        val inContent = currentValueOf(LocalInBackdropContent) &&
            (root == null || backdrop?.root == null || backdrop.root === root)
        return backdrop?.takeUnless { !blur || inContent }?.layer
    }

    // Glass: the refraction then a blur; Classic (or without shaders): the blur alone, if the platform has one.
    private fun ContentDrawScope.effectOrNull(): RenderEffect? {
        val blurPx = (if (glass) GlassBlur else ClassicBlur).toPx()
        return glassEffects.effect(
            size = size,
            cornerPx = cornerRadiusPx(),
            depthPx = GlassDepth.toPx(),
            blurPx = blurPx,
            refract = glass && refract,
        ) ?: BlurEffect(blurPx, blurPx, TileMode.Clamp).takeIf { it.isSupported() }
    }

    private fun ContentDrawScope.paintBlurred(
        backdrop: Backdrop,
        source: GraphicsLayer,
        effect: RenderEffect,
        outline: androidx.compose.ui.graphics.Outline,
    ) {
        val layer = sampled ?: requireGraphicsContext().createGraphicsLayer().also { sampled = it }
        if (layer.renderEffect != effect) layer.renderEffect = effect
        layer.record(size.toIntSize()) {
            translate(backdrop.origin.x - position.x, backdrop.origin.y - position.y) { drawLayer(source) }
        }
        clipPath(Path().apply { addOutline(outline) }) {
            drawLayer(layer)
            drawRect(color.copy(alpha = color.alpha * if (glass) GLASS_TINT else CLASSIC_TINT))
        }
    }

    // The edge of the glass: a faint dark rim all round (what shows it on a light page, where the white highlight
    // and the tint vanish), then the lit edge, bright where the light hits (top start), fading across, a faint rim
    // opposite.
    private fun ContentDrawScope.drawSpecular(outline: androidx.compose.ui.graphics.Outline) {
        drawOutline(outline, Color.Black.copy(alpha = RIM_ALPHA), style = Stroke(SPECULAR_WIDTH.dp.toPx()))
        val edge = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.7f),
            0.45f to Color.White.copy(alpha = 0.08f),
            0.55f to Color.White.copy(alpha = 0.08f),
            1f to Color.White.copy(alpha = 0.35f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        drawOutline(outline, edge, style = Stroke(SPECULAR_WIDTH.dp.toPx()))
    }

    // The corner radius of a rounded shape, to place the refraction; a pill's half height, else 0.
    private fun ContentDrawScope.cornerRadiusPx(): Float {
        val outline = shape.createOutline(size, layoutDirection, this)
        return when (outline) {
            is androidx.compose.ui.graphics.Outline.Rounded -> outline.roundRect.topLeftCornerRadius.x
            else -> 0f
        }
    }

    override fun onDetach() {
        sampled?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        sampled = null
    }
}
