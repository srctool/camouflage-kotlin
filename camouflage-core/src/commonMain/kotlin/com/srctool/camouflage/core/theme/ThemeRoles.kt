package com.srctool.camouflage.core.theme

/** The corner-radius roles of [CamoShapes]. */
public enum class ShapeRole { None, ExtraSmall, Small, Medium, Large, ExtraLarge, Full }

/** The semantic depth levels of [CamoElevation]. How depth looks is the skin's surface treatment. */
public enum class ElevationLevel { Level0, Level1, Level2, Level3, Level4, Level5 }

/** The status a tone-aware component expresses. Each tone reads one color family ([CamoColors.family]). */
public enum class Tone { Default, Error, Success, Warning, Info }

/** How separated a surface is from the screen background, from `surface` to `surfaceContainerHighest`. */
public enum class SurfaceLevel { Surface, Lowest, Low, Container, High, Highest }
