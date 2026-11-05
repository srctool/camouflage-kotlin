package com.srctool.camouflage.skin.cupertino

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

internal actual class GlassEffects {
    // The compiled shader, kept per node; `failed` once it threw, so a broken shader degrades to the blur for good.
    private var shader: RuntimeShader? = null
    private var failed = false
    private var key: List<Any>? = null
    private var cached: RenderEffect? = null

    actual fun effect(size: Size, cornerPx: Float, depthPx: Float, blurPx: Float, refract: Boolean): RenderEffect? {
        if (!refract || failed || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val now = listOf(size, cornerPx, depthPx, blurPx)
        if (now != key) {
            cached = runCatching { build(size, cornerPx, depthPx, blurPx) }.getOrElse {
                failed = true
                null
            }
            key = now
        }
        return cached
    }

    private fun build(size: Size, cornerPx: Float, depthPx: Float, blurPx: Float): RenderEffect {
        val glass = shader ?: RuntimeShader(GLASS_SHADER).also { shader = it }
        glass.setFloatUniform("size", size.width, size.height)
        glass.setFloatUniform("radius", minOf(cornerPx, size.minDimension / 2))
        glass.setFloatUniform("depth", depthPx)
        val refracted = android.graphics.RenderEffect.createRuntimeShaderEffect(glass, "content")
        val blurred = android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, refracted, Shader.TileMode.CLAMP)
        return blurred.asComposeRenderEffect()
    }
}
