package com.srctool.camouflage.skin.material

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.skin.material.renderer.MaterialAccordionRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialAvatarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialBadgeRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialBannerRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialBottomSheetRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialBreadcrumbRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialButtonGroupRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialButtonRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialCardRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialCarouselRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialCheckboxRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialChipRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialDataTableRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialDatePickerRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialDialogRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialDividerRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialFabRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialFileUploaderRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialIconButtonRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialIconRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialImageRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialListItemRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialMenuRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialNavigationBarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialNavigationDrawerRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialNavigationRailRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialPagedListRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialPaginationRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialPopoverRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialProgressBarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialProgressCircleRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialProgressStepsRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialWavyProgressBarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialRadioRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialRatingRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSearchFieldRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSegmentedControlRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSideSheetRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSkeletonRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSliderRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSnackbarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSplitButtonRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialStepperRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSurfaceRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialSwitchRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTabsRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTextFieldRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTextRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTimePickerRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialToolbarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTooltipRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTopBarRenderer
import com.srctool.camouflage.skin.material.renderer.MaterialTreeRenderer

/**
 * Material Design 3, drawn on Compose foundation and material-ripple: [Paper] surfaces, [Ripple] feedback and
 * M3's proportions. Pair it with [materialTheme]. [expressiveProgress] draws the linear progress indicator as M3
 * Expressive's wave; anything a brand could want on any skin is a component-theme field instead.
 */
public fun materialSkin(expressiveProgress: Boolean = false): CamoSkin = CamoSkin(
    name = "material",
    surfaceTreatment = Paper,
    feedback = Ripple,
    text = MaterialTextRenderer,
    icon = MaterialIconRenderer,
    surface = MaterialSurfaceRenderer,
    divider = MaterialDividerRenderer,
    button = MaterialButtonRenderer,
    iconButton = MaterialIconButtonRenderer,
    progressCircle = MaterialProgressCircleRenderer,
    progressBar = if (expressiveProgress) MaterialWavyProgressBarRenderer else MaterialProgressBarRenderer,
    tooltip = MaterialTooltipRenderer,
    checkbox = MaterialCheckboxRenderer,
    switch = MaterialSwitchRenderer,
    radio = MaterialRadioRenderer,
    segmentedControl = MaterialSegmentedControlRenderer,
    slider = MaterialSliderRenderer,
    stepper = MaterialStepperRenderer,
    textField = MaterialTextFieldRenderer,
    searchField = MaterialSearchFieldRenderer,
    menu = MaterialMenuRenderer,
    bottomSheet = MaterialBottomSheetRenderer,
    datePicker = MaterialDatePickerRenderer,
    timePicker = MaterialTimePickerRenderer,
    card = MaterialCardRenderer,
    listItem = MaterialListItemRenderer,
    accordion = MaterialAccordionRenderer,
    skeleton = MaterialSkeletonRenderer,
    topBar = MaterialTopBarRenderer,
    pagedList = MaterialPagedListRenderer,
    dialog = MaterialDialogRenderer,
    chip = MaterialChipRenderer,
    badge = MaterialBadgeRenderer,
    avatar = MaterialAvatarRenderer,
    snackbar = MaterialSnackbarRenderer,
    banner = MaterialBannerRenderer,
    tabs = MaterialTabsRenderer,
    fab = MaterialFabRenderer,
    navigationBar = MaterialNavigationBarRenderer,
    navigationRail = MaterialNavigationRailRenderer,
    navigationDrawer = MaterialNavigationDrawerRenderer,
    popover = MaterialPopoverRenderer,
    sideSheet = MaterialSideSheetRenderer,
    buttonGroup = MaterialButtonGroupRenderer,
    splitButton = MaterialSplitButtonRenderer,
    toolbar = MaterialToolbarRenderer,
    image = MaterialImageRenderer,
    rating = MaterialRatingRenderer,
    pagination = MaterialPaginationRenderer,
    breadcrumb = MaterialBreadcrumbRenderer,
    progressSteps = MaterialProgressStepsRenderer,
    dataTable = MaterialDataTableRenderer,
    tree = MaterialTreeRenderer,
    carousel = MaterialCarouselRenderer,
    fileUploader = MaterialFileUploaderRenderer,
)

/** The Material skin. */
public val MaterialSkin: CamoSkin = materialSkin()

/** A provider for `@Preview`s with [MaterialSkin] and the baseline theme, light or dark. */
@Composable
public fun MaterialPreview(dark: Boolean = false, content: @Composable () -> Unit) {
    CamouflagePreview(MaterialSkin, if (dark) materialBaselineDark else materialBaselineLight, content)
}
