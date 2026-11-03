package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Immutable
import com.srctool.camouflage.core.accordion.AccordionRenderer
import com.srctool.camouflage.core.avatar.AvatarRenderer
import com.srctool.camouflage.core.badge.BadgeRenderer
import com.srctool.camouflage.core.banner.BannerRenderer
import com.srctool.camouflage.core.bottomsheet.BottomSheetRenderer
import com.srctool.camouflage.core.breadcrumb.BreadcrumbRenderer
import com.srctool.camouflage.core.button.ButtonRenderer
import com.srctool.camouflage.core.buttongroup.ButtonGroupRenderer
import com.srctool.camouflage.core.card.CardRenderer
import com.srctool.camouflage.core.checkbox.CheckboxRenderer
import com.srctool.camouflage.core.chip.ChipRenderer
import com.srctool.camouflage.core.datatable.DataTableRenderer
import com.srctool.camouflage.core.datepicker.DatePickerRenderer
import com.srctool.camouflage.core.dialog.DialogRenderer
import com.srctool.camouflage.core.divider.DividerRenderer
import com.srctool.camouflage.core.fab.FabRenderer
import com.srctool.camouflage.core.icon.IconRenderer
import com.srctool.camouflage.core.iconbutton.IconButtonRenderer
import com.srctool.camouflage.core.image.ImageRenderer
import com.srctool.camouflage.core.list.ListItemRenderer
import com.srctool.camouflage.core.menu.MenuRenderer
import com.srctool.camouflage.core.navigation.NavigationBarRenderer
import com.srctool.camouflage.core.navigation.NavigationDrawerRenderer
import com.srctool.camouflage.core.navigation.NavigationRailRenderer
import com.srctool.camouflage.core.pagedlist.PagedListRenderer
import com.srctool.camouflage.core.pagination.PaginationRenderer
import com.srctool.camouflage.core.popover.PopoverRenderer
import com.srctool.camouflage.core.progress.ProgressBarRenderer
import com.srctool.camouflage.core.progress.ProgressCircleRenderer
import com.srctool.camouflage.core.progresssteps.ProgressStepsRenderer
import com.srctool.camouflage.core.radio.RadioRenderer
import com.srctool.camouflage.core.rating.RatingRenderer
import com.srctool.camouflage.core.search.SearchFieldRenderer
import com.srctool.camouflage.core.segmented.SegmentedControlRenderer
import com.srctool.camouflage.core.sidesheet.SideSheetRenderer
import com.srctool.camouflage.core.skeleton.SkeletonRenderer
import com.srctool.camouflage.core.slider.SliderRenderer
import com.srctool.camouflage.core.snackbar.SnackbarRenderer
import com.srctool.camouflage.core.splitbutton.SplitButtonRenderer
import com.srctool.camouflage.core.stepper.StepperRenderer
import com.srctool.camouflage.core.surface.SurfaceRenderer
import com.srctool.camouflage.core.switch.SwitchRenderer
import com.srctool.camouflage.core.tabs.TabsRenderer
import com.srctool.camouflage.core.text.TextRenderer
import com.srctool.camouflage.core.textfield.TextFieldRenderer
import com.srctool.camouflage.core.timepicker.TimePickerRenderer
import com.srctool.camouflage.core.toolbar.ToolbarRenderer
import com.srctool.camouflage.core.tooltip.TooltipRenderer
import com.srctool.camouflage.core.topbar.TopBarRenderer
import com.srctool.camouflage.core.tree.TreeRenderer

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
    val progressBar: ProgressBarRenderer,
    val tooltip: TooltipRenderer,
    val checkbox: CheckboxRenderer,
    val switch: SwitchRenderer,
    val radio: RadioRenderer,
    val segmentedControl: SegmentedControlRenderer,
    val slider: SliderRenderer,
    val stepper: StepperRenderer,
    val textField: TextFieldRenderer,
    val searchField: SearchFieldRenderer,
    val menu: MenuRenderer,
    val bottomSheet: BottomSheetRenderer,
    val datePicker: DatePickerRenderer,
    val timePicker: TimePickerRenderer,
    val card: CardRenderer,
    val listItem: ListItemRenderer,
    val accordion: AccordionRenderer,
    val skeleton: SkeletonRenderer,
    val topBar: TopBarRenderer,
    val pagedList: PagedListRenderer,
    val dialog: DialogRenderer,
    val chip: ChipRenderer,
    val badge: BadgeRenderer,
    val avatar: AvatarRenderer,
    val snackbar: SnackbarRenderer,
    val banner: BannerRenderer,
    val tabs: TabsRenderer,
    val fab: FabRenderer,
    val navigationBar: NavigationBarRenderer,
    val navigationRail: NavigationRailRenderer,
    val navigationDrawer: NavigationDrawerRenderer,
    val popover: PopoverRenderer,
    val sideSheet: SideSheetRenderer,
    val buttonGroup: ButtonGroupRenderer,
    val splitButton: SplitButtonRenderer,
    val toolbar: ToolbarRenderer,
    val image: ImageRenderer,
    val rating: RatingRenderer,
    val pagination: PaginationRenderer,
    val breadcrumb: BreadcrumbRenderer,
    val progressSteps: ProgressStepsRenderer,
    val dataTable: DataTableRenderer,
    val tree: TreeRenderer,
)
