package com.srctool.camouflage.skin.minimal

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.skin.minimal.renderer.MinimalAccordionRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalAvatarRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalBadgeRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalBannerRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalBottomSheetRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalButtonRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalCardRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalCheckboxRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalChipRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalDatePickerRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalDialogRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalDividerRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalIconButtonRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalIconRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalListItemRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalMenuRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalPagedListRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalProgressBarRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalProgressCircleRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalRadioRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSearchFieldRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSegmentedControlRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSkeletonRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSliderRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSnackbarRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalStepperRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSurfaceRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalSwitchRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTextFieldRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTextRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTimePickerRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTooltipRenderer
import com.srctool.camouflage.skin.minimal.renderer.MinimalTopBarRenderer

/**
 * Camouflage's own neutral dialect: flat surfaces with hairlines, a quiet fade for feedback, ink as the
 * primary, and a clear focus ring. [labelPlacement] is the default for text fields and the fields built on
 * their frame (`Above`, or `Floating` inside the field). Each component's renderer joins in its milestone.
 */
public fun minimalSkin(labelPlacement: LabelPlacement = LabelPlacement.Above): CamoSkin = CamoSkin(
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
    progressBar = MinimalProgressBarRenderer,
    tooltip = MinimalTooltipRenderer,
    checkbox = MinimalCheckboxRenderer,
    switch = MinimalSwitchRenderer,
    radio = MinimalRadioRenderer,
    segmentedControl = MinimalSegmentedControlRenderer,
    slider = MinimalSliderRenderer,
    stepper = MinimalStepperRenderer,
    textField = MinimalTextFieldRenderer(labelPlacement),
    searchField = MinimalSearchFieldRenderer,
    menu = MinimalMenuRenderer,
    bottomSheet = MinimalBottomSheetRenderer,
    datePicker = MinimalDatePickerRenderer,
    timePicker = MinimalTimePickerRenderer,
    card = MinimalCardRenderer,
    listItem = MinimalListItemRenderer,
    accordion = MinimalAccordionRenderer,
    skeleton = MinimalSkeletonRenderer,
    topBar = MinimalTopBarRenderer,
    pagedList = MinimalPagedListRenderer,
    dialog = MinimalDialogRenderer,
    chip = MinimalChipRenderer,
    badge = MinimalBadgeRenderer,
    avatar = MinimalAvatarRenderer,
    snackbar = MinimalSnackbarRenderer,
    banner = MinimalBannerRenderer,
)

/** The Minimal skin. */
public val MinimalSkin: CamoSkin = minimalSkin()

/** A provider for `@Preview`s with [MinimalSkin] and [minimalLight] or [minimalDark]. */
@Composable
public fun MinimalPreview(dark: Boolean = false, content: @Composable () -> Unit) {
    CamouflagePreview(MinimalSkin, if (dark) minimalDark else minimalLight, content)
}
