package com.srctool.camouflage.core.theme

import androidx.compose.runtime.Immutable
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.checkbox.CheckboxTheme
import com.srctool.camouflage.core.divider.DividerTheme
import com.srctool.camouflage.core.icon.IconTheme
import com.srctool.camouflage.core.iconbutton.IconButtonTheme
import com.srctool.camouflage.core.progress.ProgressTheme
import com.srctool.camouflage.core.search.SearchFieldTheme
import com.srctool.camouflage.core.radio.RadioTheme
import com.srctool.camouflage.core.segmented.SegmentedControlTheme
import com.srctool.camouflage.core.slider.SliderTheme
import com.srctool.camouflage.core.stepper.StepperTheme
import com.srctool.camouflage.core.surface.SurfaceTheme
import com.srctool.camouflage.core.switch.SwitchTheme
import com.srctool.camouflage.core.textfield.TextFieldTheme
import com.srctool.camouflage.core.tooltip.TooltipTheme

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
)
