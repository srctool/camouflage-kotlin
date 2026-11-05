package com.srctool.camouflage.skin.cupertino

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

// Compiled once for the process; `null` when this Skia can't compile the shader.
private val Compiled: RuntimeEffect? by lazy { runCatching { RuntimeEffect.makeForShader(GLASS_SHADER) }.getOrNull() }

internal actual class GlassEffects {
    private var key: List<Any>? = null
    private var cached: RenderEffect? = null

    actual fun effect(size: Size, cornerPx: Float, depthPx: Float, blurPx: Float, refract: Boolean): RenderEffect? {
        val compiled = Compiled?.takeIf { refract } ?: return null
        val now = listOf(size, cornerPx, depthPx, blurPx)
        if (now != key) {
            cached = runCatching { build(compiled, size, cornerPx, depthPx, blurPx) }.getOrNull()
            key = now
        }
        return cached
    }

    private fun build(
        compiled: RuntimeEffect,
        size: Size,
        cornerPx: Float,
        depthPx: Float,
        blurPx: Float,
    ): RenderEffect {
        val builder = RuntimeShaderBuilder(compiled)
        builder.uniform("size", size.width, size.height)
        builder.uniform("radius", minOf(cornerPx, size.minDimension / 2))
        builder.uniform("depth", depthPx)
        val refracted = ImageFilter.makeRuntimeShader(builder, "content", null)
        return ImageFilter.makeBlur(blurPx, blurPx, FilterTileMode.CLAMP, refracted).asComposeRenderEffect()
    }
}
