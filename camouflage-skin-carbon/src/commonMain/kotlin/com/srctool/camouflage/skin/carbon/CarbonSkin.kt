package com.srctool.camouflage.skin.carbon

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.skin.carbon.renderer.CarbonAccordionRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonAvatarRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonBadgeRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonBannerRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonBottomSheetRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonBreadcrumbRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonButtonGroupRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonButtonRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonCardRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonCarouselRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonCheckboxRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonChipRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonDataTableRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonDatePickerRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonDialogRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonDividerRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonFabRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonFileUploaderRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonIconButtonRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonIconRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonImageRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonListItemRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonMenuRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonNavigationBarRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonNavigationDrawerRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonNavigationRailRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonPagedListRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonPaginationRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonPopoverRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonProgressBarRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonProgressCircleRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonProgressStepsRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonRadioRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonRatingRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSearchFieldRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSegmentedControlRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSideSheetRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSkeletonRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSliderRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSnackbarRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSplitButtonRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonStepperRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSurfaceRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonSwitchRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTabsRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTextFieldRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTextRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTimePickerRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonToolbarRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTooltipRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTopBarRenderer
import com.srctool.camouflage.skin.carbon.renderer.CarbonTreeRenderer

/**
 * IBM Carbon's dialect: square corners, opaque layered surfaces with no shadows (but for overlays), hover and
 * active fills, an inset focus border and a bold blue interactive color. [labelPlacement] is the default for
 * text fields and the fields built on their frame (`Above`, or `Floating` inside the field).
 */
public fun carbonSkin(labelPlacement: LabelPlacement = LabelPlacement.Above): CamoSkin = CamoSkin(
    name = "carbon",
    surfaceTreatment = Layered,
    feedback = CarbonStates,
    text = CarbonTextRenderer,
    icon = CarbonIconRenderer,
    surface = CarbonSurfaceRenderer,
    divider = CarbonDividerRenderer,
    button = CarbonButtonRenderer,
    iconButton = CarbonIconButtonRenderer,
    progressCircle = CarbonProgressCircleRenderer,
    progressBar = CarbonProgressBarRenderer,
    tooltip = CarbonTooltipRenderer,
    checkbox = CarbonCheckboxRenderer,
    switch = CarbonSwitchRenderer,
    radio = CarbonRadioRenderer,
    segmentedControl = CarbonSegmentedControlRenderer,
    slider = CarbonSliderRenderer,
    stepper = CarbonStepperRenderer,
    textField = CarbonTextFieldRenderer(labelPlacement),
    searchField = CarbonSearchFieldRenderer,
    menu = CarbonMenuRenderer,
    bottomSheet = CarbonBottomSheetRenderer,
    datePicker = CarbonDatePickerRenderer,
    timePicker = CarbonTimePickerRenderer,
    card = CarbonCardRenderer,
    listItem = CarbonListItemRenderer,
    accordion = CarbonAccordionRenderer,
    skeleton = CarbonSkeletonRenderer,
    topBar = CarbonTopBarRenderer,
    pagedList = CarbonPagedListRenderer,
    dialog = CarbonDialogRenderer,
    chip = CarbonChipRenderer,
    badge = CarbonBadgeRenderer,
    avatar = CarbonAvatarRenderer,
    snackbar = CarbonSnackbarRenderer,
    banner = CarbonBannerRenderer,
    tabs = CarbonTabsRenderer,
    fab = CarbonFabRenderer,
    navigationBar = CarbonNavigationBarRenderer,
    navigationRail = CarbonNavigationRailRenderer,
    navigationDrawer = CarbonNavigationDrawerRenderer,
    popover = CarbonPopoverRenderer,
    sideSheet = CarbonSideSheetRenderer,
    buttonGroup = CarbonButtonGroupRenderer,
    splitButton = CarbonSplitButtonRenderer,
    toolbar = CarbonToolbarRenderer,
    image = CarbonImageRenderer,
    rating = CarbonRatingRenderer,
    pagination = CarbonPaginationRenderer,
    breadcrumb = CarbonBreadcrumbRenderer,
    progressSteps = CarbonProgressStepsRenderer,
    dataTable = CarbonDataTableRenderer,
    tree = CarbonTreeRenderer,
    carousel = CarbonCarouselRenderer,
    fileUploader = CarbonFileUploaderRenderer,
)

/** The Carbon skin. */
public val CarbonSkin: CamoSkin = carbonSkin()

/** A provider for `@Preview`s with [CarbonSkin] and [carbonLight] or [carbonDark]. */
@Composable
public fun CarbonPreview(dark: Boolean = false, content: @Composable () -> Unit) {
    CamouflagePreview(CarbonSkin, if (dark) carbonDark else carbonLight, content)
}
