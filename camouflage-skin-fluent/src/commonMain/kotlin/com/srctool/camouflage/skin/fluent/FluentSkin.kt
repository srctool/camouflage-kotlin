package com.srctool.camouflage.skin.fluent

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.skin.fluent.renderer.FluentButtonRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentDividerRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentIconButtonRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentIconRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentProgressCircleRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSurfaceRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTextRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTooltipRenderer

/**
 * Microsoft's Fluent 2, drawn on Compose foundation: [FluentSolid] surfaces, [Subtle] feedback with the
 * double-stroke focus, and Fluent's proportions. Pair it with [fluentTheme]. Each component's renderer joins in
 * its milestone.
 */
public fun fluentSkin(surface: FluentSurface = FluentSurface.Solid): CamoSkin = CamoSkin(
    name = "fluent",
    surfaceTreatment = when (surface) {
        FluentSurface.Solid -> FluentSolid
    },
    feedback = Subtle,
    text = FluentTextRenderer,
    icon = FluentIconRenderer,
    surface = FluentSurfaceRenderer,
    divider = FluentDividerRenderer,
    button = FluentButtonRenderer,
    iconButton = FluentIconButtonRenderer,
    progressCircle = FluentProgressCircleRenderer,
    tooltip = FluentTooltipRenderer,
)

/** The Fluent skin with the `Solid` surface. */
public val FluentSkin: CamoSkin = fluentSkin()

/** A provider for `@Preview`s with [FluentSkin] and [fluentTheme]. */
@Composable
public fun FluentPreview(
    dark: Boolean = false,
    density: FluentDensity = FluentDensity.Medium,
    content: @Composable () -> Unit,
) {
    CamouflagePreview(FluentSkin, fluentTheme(dark = dark, density = density), content)
}
