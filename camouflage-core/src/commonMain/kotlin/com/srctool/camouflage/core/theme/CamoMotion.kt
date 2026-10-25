package com.srctool.camouflage.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

/** Durations in milliseconds (they match `tween(durationMillis = …)`) and the two easings. */
@Immutable
public data class CamoMotion(
    val durationShort: Int = 120,
    val durationMedium: Int = 200,
    val durationLong: Int = 300,
    val easingStandard: Easing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f),
    val easingEmphasized: Easing = CubicBezierEasing(a = 0.05f, b = 0.7f, c = 0.1f, d = 1f),
) {
    /** The motion for "reduce motion": every duration is zero. */
    public fun reduced(): CamoMotion = copy(durationShort = 0, durationMedium = 0, durationLong = 0)
}
