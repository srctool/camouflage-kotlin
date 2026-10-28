package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable
import com.srctool.camouflage.core.accordion.AccordionTheme
import com.srctool.camouflage.core.avatar.AvatarTheme
import com.srctool.camouflage.core.badge.BadgeTheme
import com.srctool.camouflage.core.bottomsheet.BottomSheetTheme
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.card.CardTheme
import com.srctool.camouflage.core.checkbox.CheckboxTheme
import com.srctool.camouflage.core.chip.ChipTheme
import com.srctool.camouflage.core.datepicker.DatePickerTheme
import com.srctool.camouflage.core.dialog.DialogTheme
import com.srctool.camouflage.core.divider.DividerTheme
import com.srctool.camouflage.core.icon.IconTheme
import com.srctool.camouflage.core.iconbutton.IconButtonTheme
import com.srctool.camouflage.core.list.ListItemTheme
import com.srctool.camouflage.core.menu.MenuTheme
import com.srctool.camouflage.core.pagedlist.PagedListTheme
import com.srctool.camouflage.core.progress.ProgressTheme
import com.srctool.camouflage.core.radio.RadioTheme
import com.srctool.camouflage.core.search.SearchFieldTheme
import com.srctool.camouflage.core.segmented.SegmentedControlTheme
import com.srctool.camouflage.core.select.SelectTheme
import com.srctool.camouflage.core.skeleton.SkeletonTheme
import com.srctool.camouflage.core.slider.SliderTheme
import com.srctool.camouflage.core.stepper.StepperTheme
import com.srctool.camouflage.core.surface.SurfaceTheme
import com.srctool.camouflage.core.switch.SwitchTheme
import com.srctool.camouflage.core.textfield.TextFieldTheme
import com.srctool.camouflage.core.timepicker.TimePickerTheme
import com.srctool.camouflage.core.tooltip.TooltipTheme
import com.srctool.camouflage.core.topbar.TopBarTheme

/**
 * Per-component overrides: one nullable `XTheme` per component, `null` meaning the skin's default.
 * Each component adds its field here in its own milestone (ADR-39).
 */
@Immutable
public data class CamoComponentThemes(
    val icon: IconTheme? = null,
    val surface: SurfaceTheme? = null,
    val divider: DividerTheme? = null,
    val button: ButtonTheme? = null,
    val iconButton: IconButtonTheme? = null,
    val progress: ProgressTheme? = null,
    val tooltip: TooltipTheme? = null,
    val checkbox: CheckboxTheme? = null,
    val switch: SwitchTheme? = null,
    val radio: RadioTheme? = null,
    val segmentedControl: SegmentedControlTheme? = null,
    val slider: SliderTheme? = null,
    val stepper: StepperTheme? = null,
    val textField: TextFieldTheme? = null,
    val searchField: SearchFieldTheme? = null,
    val menu: MenuTheme? = null,
    val bottomSheet: BottomSheetTheme? = null,
    val datePicker: DatePickerTheme? = null,
    val timePicker: TimePickerTheme? = null,
    val card: CardTheme? = null,
    val listItem: ListItemTheme? = null,
    val accordion: AccordionTheme? = null,
    val skeleton: SkeletonTheme? = null,
    val topBar: TopBarTheme? = null,
    val pagedList: PagedListTheme? = null,
    val dialog: DialogTheme? = null,
    val select: SelectTheme? = null,
    val chip: ChipTheme? = null,
    val badge: BadgeTheme? = null,
    val avatar: AvatarTheme? = null,
)
