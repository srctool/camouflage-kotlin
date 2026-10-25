package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Immutable
import com.srctool.camouflage.core.button.ButtonRenderer
import com.srctool.camouflage.core.divider.DividerRenderer
import com.srctool.camouflage.core.icon.IconRenderer
import com.srctool.camouflage.core.iconbutton.IconButtonRenderer
import com.srctool.camouflage.core.progress.ProgressCircleRenderer
import com.srctool.camouflage.core.surface.SurfaceRenderer
import com.srctool.camouflage.core.text.TextRenderer
import com.srctool.camouflage.core.tooltip.TooltipRenderer

/**
 * A design language: how each component is built, how it reacts to touch and what its surfaces are made of.
 *
 * Every field is required, so a skin that misses one doesn't compile. Each component adds its renderer
 * field here in its own milestone (ADR-39). `copy(button = …)` replaces part of a skin and keeps the rest.
 * Renderers read `Camouflage.skin.surfaceTreatment` and `.feedback`, so a borrowed renderer takes the host
 * skin's.
 */
@Immutable
public data class CamoSkin(
    /** For debugging and tests, e.g. `"minimal"`. */
    val name: String,
    val surfaceTreatment: SurfaceTreatment,
    val feedback: InteractionFeedback,
    val text: TextRenderer,
    val icon: IconRenderer,
    val surface: SurfaceRenderer,
    val divider: DividerRenderer,
    val button: ButtonRenderer,
    val iconButton: IconButtonRenderer,
    val progressCircle: ProgressCircleRenderer,
    val tooltip: TooltipRenderer,
)
