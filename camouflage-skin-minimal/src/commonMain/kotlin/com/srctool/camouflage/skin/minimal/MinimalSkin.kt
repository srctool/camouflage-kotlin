package com.srctool.camouflage.skin.minimal

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.skin.minimal.renderer.MinimalButtonRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalCheckboxRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalDividerRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalIconButtonRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalIconRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalProgressCircleRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalRadioRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSegmentedControlRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSliderRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalStepperRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSurfaceRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSwitchRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTextRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTooltipRenderer

/**
 * Camouflage's own neutral dialect: flat surfaces with hairlines, a quiet fade for feedback, ink as the
 * primary, and a clear focus ring. Each component's renderer joins in its milestone.
 */
public fun minimalSkin(): CamoSkin = CamoSkin(
    name = "minimal",
    surfaceTreatment = Flat,
    feedback = Fade,
    text = MinimalTextRenderer,
    icon = MinimalIconRenderer,
    surface = MinimalSurfaceRenderer,
    divider = MinimalDividerRenderer,
    button = MinimalButtonRenderer,
    iconButton = MinimalIconButtonRenderer,
    progressCircle = MinimalProgressCircleRenderer,
    tooltip = MinimalTooltipRenderer,
    checkbox = MinimalCheckboxRenderer,
    switch = MinimalSwitchRenderer,
    radio = MinimalRadioRenderer,
    segmentedControl = MinimalSegmentedControlRenderer,
    slider = MinimalSliderRenderer,
    stepper = MinimalStepperRenderer,
)

/** The Minimal skin. */
public val MinimalSkin: CamoSkin = minimalSkin()

/** A provider for `@Preview`s with [MinimalSkin] and [minimalLight] or [minimalDark]. */
@Composable
public fun MinimalPreview(dark: Boolean = false, content: @Composable () -> Unit) {
    CamouflagePreview(MinimalSkin, if (dark) minimalDark else minimalLight, content)
}
