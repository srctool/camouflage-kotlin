package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable
import com.srctool.camouflage.core.environment.CamoBreakpoints
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.ThemeAdaptation

/**
 * Everything a skin draws with: colors, typography, component overrides, shapes, spacing, elevation, motion
 * and the brand's own token sets. Light and dark are two instances; there's no `isDark` flag.
 *
 * [breakpoints] classify the window; [adaptation] holds the theme's adaptive rules (set them with `adapt`). The
 * provider applies the rules and sets [environment] on the theme components read (ADR-41), so a skin's
 * `defaults(theme)` can follow the platform's conventions; a theme you build has no environment.
 */
@Immutable
public data class CamoTheme(
    val colors: CamoColors,
    val typography: CamoTypography,
    val components: CamoComponentThemes = CamoComponentThemes(),
    val shapes: CamoShapes = CamoShapes(),
    val spacing: CamoSpacing = CamoSpacing(),
    val elevation: CamoElevation = CamoElevation(),
    val motion: CamoMotion = CamoMotion(),
    val tokenSets: List<CamoTokenSet<*>> = emptyList(),
    val breakpoints: CamoBreakpoints = CamoBreakpoints(),
    val adaptation: ThemeAdaptation? = null,
    val environment: CamoEnvironment? = null,
) {
    /** The token set of type [T], or `null` if this theme doesn't include it. */
    public inline fun <reified T : CamoTokenSet<T>> tokens(): T? = tokenSets.firstOrNull { it is T } as T?

    /** Reports contrast and value problems. It never throws and never corrects a brand's colors. */
    public fun validate(): List<ThemeIssue> = ThemeValidator.validate(this)
}
