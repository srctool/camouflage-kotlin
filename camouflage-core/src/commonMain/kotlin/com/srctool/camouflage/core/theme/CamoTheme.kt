package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable

/**
 * Everything a skin draws with: colors, typography, component overrides, shapes, spacing, elevation, motion
 * and the brand's own token sets. Light and dark are two instances; there's no `isDark` flag.
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
) {
    /** The token set of type [T], or `null` if this theme doesn't include it. */
    public inline fun <reified T : CamoTokenSet<T>> tokens(): T? = tokenSets.firstOrNull { it is T } as T?

    /** Reports contrast and value problems. It never throws and never corrects a brand's colors. */
    public fun validate(): List<ThemeIssue> = ThemeValidator.validate(this)
}
