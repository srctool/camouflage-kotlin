package com.srctool.camouflage.skin.cupertino

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RenderEffect

/**
 * The effect Liquid Glass applies to the sampled backdrop: the edge refraction (a rounded-rect distance field bends
 * the backdrop inward within `depthPx` of the edge, harder at the rim) then a blur. One per material node: it keeps
 * the compiled shader and the last effect, and rebuilds only when the size, corner, depth or blur change, so an idle
 * glass material allocates nothing per frame. A shader that can't compile or run means `null` forever, so the caller
 * falls back to a plain blur.
 */
internal expect class GlassEffects() {
    /** The effect for these values, or `null` when [refract] is false or the platform can't run the shader. */
    fun effect(size: Size, cornerPx: Float, depthPx: Float, blurPx: Float, refract: Boolean): RenderEffect?
}

// One shader for both runtimes: AGSL (Android 13+) and SkSL (Skia) read the same program.
internal const val GLASS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float depth;

half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - float2(radius));
    float2 s = float2(p.x < c.x ? -1.0 : 1.0, p.y < c.y ? -1.0 : 1.0);
    float outside = length(max(q, float2(0.0)));
    float d = outside + min(max(q.x, q.y), 0.0) - radius;
    float2 n = outside > 0.0 ? normalize(max(q, float2(0.0)) * s)
        : (q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y));
    float t = clamp(-d / depth, 0.0, 1.0);
    float bend = (1.0 - t) * (1.0 - t) * depth * 0.6;
    return content.eval(p - n * bend);
}
"""
