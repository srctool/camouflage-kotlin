package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable

/**
 * Per-component overrides: one nullable `XTheme` per component, `null` meaning the skin's default.
 * Each component adds its field here, with its `XTheme` / `XStyle` pair, in its own milestone (M3 to M8).
 */
@Immutable
public class CamoComponentThemes {
    override fun equals(other: Any?): Boolean = other is CamoComponentThemes
    override fun hashCode(): Int = 0
    override fun toString(): String = "CamoComponentThemes()"
}
