package com.srctool.camouflage.skin.fluent

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.skin.fluent.renderer.FluentAccordionRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentAvatarRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentBadgeRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentBannerRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentBottomSheetRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentBreadcrumbRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentButtonGroupRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentButtonRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentCardRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentCarouselRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentCheckboxRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentChipRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentDataTableRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentDatePickerRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentDialogRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentDividerRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentFabRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentFileUploaderRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentIconButtonRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentIconRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentImageRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentListItemRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentMenuRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentNavigationBarRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentNavigationDrawerRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentNavigationRailRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentPagedListRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentPaginationRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentPopoverRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentProgressBarRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentProgressCircleRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentProgressStepsRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentRadioRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentRatingRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSearchFieldRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSegmentedControlRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSideSheetRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSkeletonRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSliderRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSnackbarRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSplitButtonRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentStepperRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSurfaceRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentSwitchRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTabsRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTextFieldRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTextRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTimePickerRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentToolbarRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTooltipRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTopBarRenderer
import com.srctool.camouflage.skin.fluent.renderer.FluentTreeRenderer

/**
 * Microsoft's Fluent 2, drawn on Compose foundation: [FluentSolid] surfaces, [Subtle] feedback with the
 * double-stroke focus, and Fluent's proportions. Pair it with [fluentTheme]. Each component's renderer joins in
 * its milestone.
 */
@Suppress("LongMethod") // one renderer per component
public fun fluentSkin(surface: FluentSurface = FluentSurface.Solid): CamoSkin = CamoSkin(
    name = "fluent",
    surfaceTreatment = when (surface) {
        FluentSurface.Solid -> FluentSolid
        FluentSurface.Acrylic -> FluentAcrylic
        FluentSurface.Mica -> FluentMica
    },
    feedback = Subtle,
    text = FluentTextRenderer,
    icon = FluentIconRenderer,
    surface = FluentSurfaceRenderer,
    divider = FluentDividerRenderer,
    button = FluentButtonRenderer,
    iconButton = FluentIconButtonRenderer,
    progressCircle = FluentProgressCircleRenderer,
    progressBar = FluentProgressBarRenderer,
    tooltip = FluentTooltipRenderer,
    checkbox = FluentCheckboxRenderer,
    switch = FluentSwitchRenderer,
    radio = FluentRadioRenderer,
    segmentedControl = FluentSegmentedControlRenderer,
    slider = FluentSliderRenderer,
    stepper = FluentStepperRenderer,
    textField = FluentTextFieldRenderer,
    searchField = FluentSearchFieldRenderer,
    menu = FluentMenuRenderer,
    bottomSheet = FluentBottomSheetRenderer,
    datePicker = FluentDatePickerRenderer,
    timePicker = FluentTimePickerRenderer,
    card = FluentCardRenderer,
    listItem = FluentListItemRenderer,
    accordion = FluentAccordionRenderer,
    skeleton = FluentSkeletonRenderer,
    topBar = FluentTopBarRenderer,
    pagedList = FluentPagedListRenderer,
    dialog = FluentDialogRenderer,
    chip = FluentChipRenderer,
    badge = FluentBadgeRenderer,
    avatar = FluentAvatarRenderer,
    snackbar = FluentSnackbarRenderer,
    banner = FluentBannerRenderer,
    tabs = FluentTabsRenderer,
    fab = FluentFabRenderer,
    navigationBar = FluentNavigationBarRenderer,
    navigationRail = FluentNavigationRailRenderer,
    navigationDrawer = FluentNavigationDrawerRenderer,
    popover = FluentPopoverRenderer,
    sideSheet = FluentSideSheetRenderer,
    buttonGroup = FluentButtonGroupRenderer,
    splitButton = FluentSplitButtonRenderer,
    toolbar = FluentToolbarRenderer,
    image = FluentImageRenderer,
    rating = FluentRatingRenderer,
    pagination = FluentPaginationRenderer,
    breadcrumb = FluentBreadcrumbRenderer,
    progressSteps = FluentProgressStepsRenderer,
    dataTable = FluentDataTableRenderer,
    tree = FluentTreeRenderer,
    carousel = FluentCarouselRenderer,
    fileUploader = FluentFileUploaderRenderer,
)

/** The Fluent skin with the `Solid` surface. */
public val FluentSkin: CamoSkin = fluentSkin()

/**
 * A provider for `@Preview`s with [FluentSkin] (or the skin for [surface]) and [fluentTheme], inside a
 * [FluentBackdrop] so Acrylic surfaces have something to blur.
 */
@Composable
public fun FluentPreview(
    dark: Boolean = false,
    density: FluentDensity = FluentDensity.Medium,
    surface: FluentSurface = FluentSurface.Solid,
    content: @Composable () -> Unit,
) {
    val skin = if (surface == FluentSurface.Solid) FluentSkin else fluentSkin(surface)
    CamouflagePreview(skin, fluentTheme(dark = dark, density = density)) { FluentBackdrop { content() } }
}
