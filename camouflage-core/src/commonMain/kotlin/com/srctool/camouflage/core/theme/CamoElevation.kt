package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Semantic depth, not a shadow: each skin's surface treatment decides how depth looks. */
@Immutable
public data class CamoElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

/** The depth for an [ElevationLevel]. */
public fun CamoElevation.of(level: ElevationLevel): Dp = when (level) {
    ElevationLevel.Level0 -> level0
    ElevationLevel.Level1 -> level1
    ElevationLevel.Level2 -> level2
    ElevationLevel.Level3 -> level3
    ElevationLevel.Level4 -> level4
    ElevationLevel.Level5 -> level5
}
