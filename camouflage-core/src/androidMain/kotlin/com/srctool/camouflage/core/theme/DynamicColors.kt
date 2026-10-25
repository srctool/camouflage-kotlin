package com.srctool.camouflage.core.theme

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.Color

/**
 * The system's dynamic colors (Android 12+, from the wallpaper) as [CamoColors], or `null` below Android 12.
 *
 * The system's accent and neutral key colors become the seeds of [CamoColors.fromSeed], so the roles keep
 * Camouflage's tone table; the status families come from the default seeds.
 */
public fun dynamicCamoColors(context: Context, dark: Boolean): CamoColors? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    fun color(id: Int) = Color(context.getColor(id))
    return CamoColors.fromSeed(
        primary = color(android.R.color.system_accent1_500),
        secondary = color(android.R.color.system_accent2_500),
        tertiary = color(android.R.color.system_accent3_500),
        neutral = color(android.R.color.system_neutral1_500),
        dark = dark,
    )
}
