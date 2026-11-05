package com.srctool.camouflage.skin.cupertino

import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoAccordionRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoAvatarRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoBadgeRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoBannerRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoBottomSheetRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoBreadcrumbRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoButtonGroupRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoButtonRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoCardRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoCarouselRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoCheckboxRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoChipRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoDataTableRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoDatePickerRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoDialogRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoDividerRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoFabRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoFileUploaderRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoIconButtonRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoIconRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoImageRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoListItemRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoMenuRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoNavigationBarRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoNavigationDrawerRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoNavigationRailRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoPagedListRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoPaginationRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoPopoverRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoProgressBarRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoProgressCircleRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoProgressStepsRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoRadioRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoRatingRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSearchFieldRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSegmentedControlRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSideSheetRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSkeletonRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSliderRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSnackbarRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSplitButtonRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoStepperRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSurfaceRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoSwitchRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTabsRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTextFieldRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTextRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTimePickerRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoToolbarRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTooltipRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTopBarRenderer
import com.srctool.camouflage.skin.cupertino.renderer.CupertinoTreeRenderer

/**
 * Apple's HIG dialect: opaque grouped surfaces, blur materials (or Liquid Glass, with [surface]) for bars, sheets
 * and menus, [Opacity] for feedback, and Apple's anatomy (stacked alerts, wheel pickers, inset grouped lists,
 * segmented tabs, a collapsing large title).
 */
public fun cupertinoSkin(
    surface: CupertinoSurface = CupertinoSurface.Classic,
    labelPlacement: LabelPlacement = LabelPlacement.Above,
): CamoSkin {
    val glass = surface == CupertinoSurface.LiquidGlass
    return CamoSkin(
        name = "cupertino",
        surfaceTreatment = if (glass) LiquidGlass else Classic,
        feedback = Opacity,
        text = CupertinoTextRenderer,
        icon = CupertinoIconRenderer,
        surface = CupertinoSurfaceRenderer,
        divider = CupertinoDividerRenderer,
        button = CupertinoButtonRenderer(glass),
        iconButton = CupertinoIconButtonRenderer(glass),
        progressCircle = CupertinoProgressCircleRenderer,
        progressBar = CupertinoProgressBarRenderer,
        tooltip = CupertinoTooltipRenderer,
        checkbox = CupertinoCheckboxRenderer,
        switch = CupertinoSwitchRenderer,
        radio = CupertinoRadioRenderer,
        segmentedControl = CupertinoSegmentedControlRenderer,
        slider = CupertinoSliderRenderer,
        stepper = CupertinoStepperRenderer,
        textField = CupertinoTextFieldRenderer(labelPlacement),
        searchField = CupertinoSearchFieldRenderer,
        menu = CupertinoMenuRenderer,
        bottomSheet = CupertinoBottomSheetRenderer,
        datePicker = CupertinoDatePickerRenderer,
        timePicker = CupertinoTimePickerRenderer,
        card = CupertinoCardRenderer,
        listItem = CupertinoListItemRenderer,
        accordion = CupertinoAccordionRenderer,
        skeleton = CupertinoSkeletonRenderer,
        topBar = CupertinoTopBarRenderer,
        pagedList = CupertinoPagedListRenderer,
        dialog = CupertinoDialogRenderer,
        chip = CupertinoChipRenderer,
        badge = CupertinoBadgeRenderer,
        avatar = CupertinoAvatarRenderer,
        snackbar = CupertinoSnackbarRenderer,
        banner = CupertinoBannerRenderer,
        tabs = CupertinoTabsRenderer,
        fab = CupertinoFabRenderer,
        navigationBar = CupertinoNavigationBarRenderer,
        navigationRail = CupertinoNavigationRailRenderer,
        navigationDrawer = CupertinoNavigationDrawerRenderer,
        popover = CupertinoPopoverRenderer,
        sideSheet = CupertinoSideSheetRenderer,
        buttonGroup = CupertinoButtonGroupRenderer,
        splitButton = CupertinoSplitButtonRenderer,
        toolbar = CupertinoToolbarRenderer,
        image = CupertinoImageRenderer,
        rating = CupertinoRatingRenderer,
        pagination = CupertinoPaginationRenderer,
        breadcrumb = CupertinoBreadcrumbRenderer,
        progressSteps = CupertinoProgressStepsRenderer,
        dataTable = CupertinoDataTableRenderer,
        tree = CupertinoTreeRenderer,
        carousel = CupertinoCarouselRenderer,
        fileUploader = CupertinoFileUploaderRenderer,
    )
}

/** The Cupertino skin. */
public val CupertinoSkin: CamoSkin = cupertinoSkin()

/**
 * A provider for `@Preview`s with [CupertinoSkin] (or the skin for [surface]) and [cupertinoLight] or
 * [cupertinoDark], inside a [CupertinoBackdrop] so the materials have something to blur.
 */
@Composable
public fun CupertinoPreview(
    dark: Boolean = false,
    surface: CupertinoSurface = CupertinoSurface.Classic,
    content: @Composable () -> Unit,
) {
    val skin = if (surface == CupertinoSurface.Classic) CupertinoSkin else cupertinoSkin(surface)
    CamouflagePreview(skin, if (dark) cupertinoDark else cupertinoLight) { CupertinoBackdrop { content() } }
}
