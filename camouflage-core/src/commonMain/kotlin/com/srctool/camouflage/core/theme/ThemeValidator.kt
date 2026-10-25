package com.srctool.camouflage.core.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min

/** A problem [CamoTheme.validate] found. */
public sealed interface ThemeIssue {
    /** `pair` is `"onX/x"`; WCAG 2.2 AA asks for 4.5:1 for text and 3:1 for non-text. */
    public data class LowContrast(val pair: String, val ratio: Double, val required: Double) : ThemeIssue

    /** A negative radius, spacing, elevation or duration. */
    public data class InvalidValue(val token: String, val value: Any) : ThemeIssue

    /** Two token sets of the same type: the first one found shadows the other. */
    public data class ShadowedRole(val key: String) : ThemeIssue

    /** A component size below the 48 touch target where the component needs one (checked from M3). */
    public data class SmallTarget(val component: String, val size: Dp) : ThemeIssue
}

/** The WCAG contrast ratio of two colors, from 1 to 21. */
internal fun contrastRatio(a: Color, b: Color): Double {
    val l1 = a.luminance() + 0.05
    val l2 = b.luminance() + 0.05
    return max(l1, l2) / min(l1, l2)
}

internal object ThemeValidator {
    private const val TEXT = 4.5
    private const val NON_TEXT = 3.0

    fun validate(theme: CamoTheme): List<ThemeIssue> = buildList {
        val c = theme.colors
        fun check(pair: String, content: Color, background: Color, required: Double) {
            val ratio = contrastRatio(content, background)
            if (ratio < required) add(ThemeIssue.LowContrast(pair, ratio, required))
        }

        // Every onX on its x.
        listOf(
            Triple("onPrimary/primary", c.onPrimary, c.primary),
            Triple("onPrimaryContainer/primaryContainer", c.onPrimaryContainer, c.primaryContainer),
            Triple("onSecondary/secondary", c.onSecondary, c.secondary),
            Triple("onSecondaryContainer/secondaryContainer", c.onSecondaryContainer, c.secondaryContainer),
            Triple("onTertiary/tertiary", c.onTertiary, c.tertiary),
            Triple("onTertiaryContainer/tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer),
            Triple("onError/error", c.onError, c.error),
            Triple("onErrorContainer/errorContainer", c.onErrorContainer, c.errorContainer),
            Triple("onSuccess/success", c.onSuccess, c.success),
            Triple("onSuccessContainer/successContainer", c.onSuccessContainer, c.successContainer),
            Triple("onWarning/warning", c.onWarning, c.warning),
            Triple("onWarningContainer/warningContainer", c.onWarningContainer, c.warningContainer),
            Triple("onInfo/info", c.onInfo, c.info),
            Triple("onInfoContainer/infoContainer", c.onInfoContainer, c.infoContainer),
            Triple("inverseOnSurface/inverseSurface", c.inverseOnSurface, c.inverseSurface),
        ).forEach { (pair, content, background) -> check(pair, content, background, TEXT) }

        // Text on every surface level.
        val surfaces = listOf(
            "surface" to c.surface,
            "surfaceContainerLowest" to c.surfaceContainerLowest,
            "surfaceContainerLow" to c.surfaceContainerLow,
            "surfaceContainer" to c.surfaceContainer,
            "surfaceContainerHigh" to c.surfaceContainerHigh,
            "surfaceContainerHighest" to c.surfaceContainerHighest,
        )
        for ((name, background) in surfaces) {
            check("onSurface/$name", c.onSurface, background, TEXT)
            check("onSurfaceVariant/$name", c.onSurfaceVariant, background, TEXT)
        }
        check("outline/surface", c.outline, c.surface, NON_TEXT)

        // Values that can't be negative.
        val s = theme.spacing
        mapOf(
            "none" to s.none, "xxs" to s.xxs, "xs" to s.xs, "s" to s.s,
            "m" to s.m, "l" to s.l, "xl" to s.xl, "xxl" to s.xxl,
        ).forEach { (k, v) -> if (v.value < 0f) add(ThemeIssue.InvalidValue("spacing.$k", v)) }
        val e = theme.elevation
        mapOf(
            "level0" to e.level0, "level1" to e.level1, "level2" to e.level2,
            "level3" to e.level3, "level4" to e.level4, "level5" to e.level5,
        ).forEach { (k, v) -> if (v.value < 0f) add(ThemeIssue.InvalidValue("elevation.$k", v)) }
        val m = theme.motion
        mapOf(
            "durationShort" to m.durationShort, "durationMedium" to m.durationMedium, "durationLong" to m.durationLong,
        ).forEach { (k, v) -> if (v < 0) add(ThemeIssue.InvalidValue("motion.$k", v)) }
        val sh = theme.shapes
        mapOf(
            "none" to sh.none, "extraSmall" to sh.extraSmall, "small" to sh.small, "medium" to sh.medium,
            "large" to sh.large, "extraLarge" to sh.extraLarge, "full" to sh.full,
        ).forEach { (k, v) -> v.negativeCorner()?.let { add(ThemeIssue.InvalidValue("shapes.$k", it)) } }

        // Token sets: one per type.
        theme.tokenSets.groupBy { it::class }.filterValues { it.size > 1 }.keys
            .forEach { add(ThemeIssue.ShadowedRole("tokenSets.${it.simpleName}")) }
    }

    private val probe = Size(100f, 100f)
    private val unitDensity = Density(1f)

    private fun CornerBasedShape.negativeCorner(): Float? =
        listOf(topStart, topEnd, bottomEnd, bottomStart).map { it.toPx(probe, unitDensity) }.firstOrNull { it < 0f }
}
