package com.srctool.camouflage.core.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.lerp as lerpText
import androidx.compose.ui.unit.lerp as lerpDp
import kotlin.math.roundToInt

/**
 * The theme between [a] and [b] at [t] (0 = a, 1 = b): colors and text styles blend, sizes interpolate,
 * shapes, easings and component overrides switch at the midpoint, and token sets blend by type.
 */
public fun lerp(a: CamoTheme, b: CamoTheme, t: Float): CamoTheme = when {
    t <= 0f -> a
    t >= 1f -> b
    else -> blend(a, b, t)
}

private fun blend(a: CamoTheme, b: CamoTheme, t: Float): CamoTheme {
    fun <V> pick(x: V, y: V): V = if (t < MIDPOINT) x else y
    return CamoTheme(
        colors = lerp(a.colors, b.colors, t),
        typography = lerp(a.typography, b.typography, t),
        components = pick(a.components, b.components),
        shapes = pick(a.shapes, b.shapes),
        spacing = a.spacing.let { x ->
            val y = b.spacing
            CamoSpacing(
                none = lerpDp(x.none, y.none, t),
                xxs = lerpDp(x.xxs, y.xxs, t),
                xs = lerpDp(x.xs, y.xs, t),
                s = lerpDp(x.s, y.s, t),
                m = lerpDp(x.m, y.m, t),
                l = lerpDp(x.l, y.l, t),
                xl = lerpDp(x.xl, y.xl, t),
                xxl = lerpDp(x.xxl, y.xxl, t),
            )
        },
        elevation = a.elevation.let { x ->
            val y = b.elevation
            CamoElevation(
                level0 = lerpDp(x.level0, y.level0, t),
                level1 = lerpDp(x.level1, y.level1, t),
                level2 = lerpDp(x.level2, y.level2, t),
                level3 = lerpDp(x.level3, y.level3, t),
                level4 = lerpDp(x.level4, y.level4, t),
                level5 = lerpDp(x.level5, y.level5, t),
            )
        },
        motion = CamoMotion(
            durationShort = lerpInt(a.motion.durationShort, b.motion.durationShort, t),
            durationMedium = lerpInt(a.motion.durationMedium, b.motion.durationMedium, t),
            durationLong = lerpInt(a.motion.durationLong, b.motion.durationLong, t),
            easingStandard = pick(a.motion.easingStandard, b.motion.easingStandard),
            easingEmphasized = pick(a.motion.easingEmphasized, b.motion.easingEmphasized),
        ),
        tokenSets = lerpTokenSets(a.tokenSets, b.tokenSets, t),
    )
}

/** Animates to [target]: a theme change blends over [durationMillis], restarting from wherever it is. */
@Composable
public fun animateCamoTheme(target: CamoTheme, durationMillis: Int = 300): CamoTheme {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target != to) {
            from = lerp(from, to, progress.value)
            to = target
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis))
        }
    }
    return lerp(from, to, progress.value)
}

private fun lerp(a: CamoColors, b: CamoColors, t: Float): CamoColors {
    fun c(x: Color, y: Color) = lerpColor(x, y, t)
    return CamoColors(
        primary = c(a.primary, b.primary),
        onPrimary = c(a.onPrimary, b.onPrimary),
        primaryContainer = c(a.primaryContainer, b.primaryContainer),
        onPrimaryContainer = c(a.onPrimaryContainer, b.onPrimaryContainer),
        secondary = c(a.secondary, b.secondary),
        onSecondary = c(a.onSecondary, b.onSecondary),
        secondaryContainer = c(a.secondaryContainer, b.secondaryContainer),
        onSecondaryContainer = c(a.onSecondaryContainer, b.onSecondaryContainer),
        tertiary = c(a.tertiary, b.tertiary),
        onTertiary = c(a.onTertiary, b.onTertiary),
        tertiaryContainer = c(a.tertiaryContainer, b.tertiaryContainer),
        onTertiaryContainer = c(a.onTertiaryContainer, b.onTertiaryContainer),
        surface = c(a.surface, b.surface),
        surfaceContainerLowest = c(a.surfaceContainerLowest, b.surfaceContainerLowest),
        surfaceContainerLow = c(a.surfaceContainerLow, b.surfaceContainerLow),
        surfaceContainer = c(a.surfaceContainer, b.surfaceContainer),
        surfaceContainerHigh = c(a.surfaceContainerHigh, b.surfaceContainerHigh),
        surfaceContainerHighest = c(a.surfaceContainerHighest, b.surfaceContainerHighest),
        onSurface = c(a.onSurface, b.onSurface),
        onSurfaceVariant = c(a.onSurfaceVariant, b.onSurfaceVariant),
        outline = c(a.outline, b.outline),
        outlineVariant = c(a.outlineVariant, b.outlineVariant),
        error = c(a.error, b.error),
        onError = c(a.onError, b.onError),
        errorContainer = c(a.errorContainer, b.errorContainer),
        onErrorContainer = c(a.onErrorContainer, b.onErrorContainer),
        success = c(a.success, b.success),
        onSuccess = c(a.onSuccess, b.onSuccess),
        successContainer = c(a.successContainer, b.successContainer),
        onSuccessContainer = c(a.onSuccessContainer, b.onSuccessContainer),
        warning = c(a.warning, b.warning),
        onWarning = c(a.onWarning, b.onWarning),
        warningContainer = c(a.warningContainer, b.warningContainer),
        onWarningContainer = c(a.onWarningContainer, b.onWarningContainer),
        info = c(a.info, b.info),
        onInfo = c(a.onInfo, b.onInfo),
        infoContainer = c(a.infoContainer, b.infoContainer),
        onInfoContainer = c(a.onInfoContainer, b.onInfoContainer),
        inverseSurface = c(a.inverseSurface, b.inverseSurface),
        inverseOnSurface = c(a.inverseOnSurface, b.inverseOnSurface),
        inversePrimary = c(a.inversePrimary, b.inversePrimary),
        scrim = c(a.scrim, b.scrim),
    )
}

private fun lerp(a: CamoTypography, b: CamoTypography, t: Float): CamoTypography = CamoTypography(
    displayLarge = lerpText(a.displayLarge, b.displayLarge, t),
    displayMedium = lerpText(a.displayMedium, b.displayMedium, t),
    displaySmall = lerpText(a.displaySmall, b.displaySmall, t),
    headlineLarge = lerpText(a.headlineLarge, b.headlineLarge, t),
    headlineMedium = lerpText(a.headlineMedium, b.headlineMedium, t),
    headlineSmall = lerpText(a.headlineSmall, b.headlineSmall, t),
    titleLarge = lerpText(a.titleLarge, b.titleLarge, t),
    titleMedium = lerpText(a.titleMedium, b.titleMedium, t),
    titleSmall = lerpText(a.titleSmall, b.titleSmall, t),
    bodyLarge = lerpText(a.bodyLarge, b.bodyLarge, t),
    bodyMedium = lerpText(a.bodyMedium, b.bodyMedium, t),
    bodySmall = lerpText(a.bodySmall, b.bodySmall, t),
    labelLarge = lerpText(a.labelLarge, b.labelLarge, t),
    labelMedium = lerpText(a.labelMedium, b.labelMedium, t),
    labelSmall = lerpText(a.labelSmall, b.labelSmall, t),
)

private fun lerpInt(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()

// Sets of the same type blend; a set only one theme has switches at the midpoint.
private fun lerpTokenSets(a: List<CamoTokenSet<*>>, b: List<CamoTokenSet<*>>, t: Float): List<CamoTokenSet<*>> {
    val blended = b.mapNotNull { to ->
        val from = a.firstOrNull { it::class == to::class }
        when {
            from != null -> lerpSameType(from, to, t)
            t >= MIDPOINT -> to
            else -> null
        }
    }
    val onlyInA = if (t < MIDPOINT) a.filter { from -> b.none { it::class == from::class } } else emptyList()
    return blended + onlyInA
}

// `from` and `to` are the same class (checked above), so `to` is a valid argument for `from.lerp`.
// T is erased at runtime; the cast only satisfies the compiler.
@Suppress("UNCHECKED_CAST")
private fun lerpSameType(from: CamoTokenSet<*>, to: CamoTokenSet<*>, t: Float): CamoTokenSet<*> =
    lerpTyped(from as CamoTokenSet<Nothing>, to, t)

@Suppress("UNCHECKED_CAST")
private fun <T : CamoTokenSet<T>> lerpTyped(from: CamoTokenSet<T>, to: CamoTokenSet<*>, t: Float): CamoTokenSet<*> =
    from.lerp(to as T, t)
