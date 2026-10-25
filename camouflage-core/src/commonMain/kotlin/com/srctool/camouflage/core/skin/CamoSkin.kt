package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Immutable

/**
 * A design language: how each component is built, how it reacts to touch and what its surfaces are made of.
 *
 * Every field is required, so a skin that misses one doesn't compile. Each component adds its renderer
 * field here in its own milestone (M3 to M8, ADR-39). `copy(feedback = …)` replaces part of a skin and
 * keeps the rest.
 */
@Immutable
public data class CamoSkin(
    /** For debugging and tests, e.g. `"minimal"`. */
    val name: String,
    val surfaceTreatment: SurfaceTreatment,
    val feedback: InteractionFeedback,
)
