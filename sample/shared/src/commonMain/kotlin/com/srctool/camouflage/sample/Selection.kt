package com.srctool.camouflage.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.radio.CamoRadioGroup
import com.srctool.camouflage.core.radio.RadioOption
import com.srctool.camouflage.core.segmented.CamoSegmentedControl
import com.srctool.camouflage.core.segmented.Segment
import com.srctool.camouflage.core.slider.CamoSlider
import com.srctool.camouflage.core.stepper.CamoDecimal
import com.srctool.camouflage.core.stepper.CamoStepper
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import kotlin.math.roundToInt

private enum class Period(val label: String) { Day("Day"), Week("Week"), Month("Month") }

/** The M5 selection controls: a settings list, a shipping choice, a period switch, sliders and steppers. */
@Composable
internal fun SelectionControls() {
    val spacing = Arrangement.spacedBy(Camouflage.spacing.s)
    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(false) }
    Column(verticalArrangement = spacing) {
        CamoSwitch(wifi, { wifi = it }, Modifier.fillMaxWidth(), label = "Wi-Fi", description = "Uses more battery")
        CamoSwitch(bluetooth, { bluetooth = it }, Modifier.fillMaxWidth(), label = "Bluetooth")
        CamoSwitch(true, {}, Modifier.fillMaxWidth(), label = "Location (managed by your organization)",
            enabled = false)
    }

    var rememberMe by remember { mutableStateOf(true) }
    var terms by remember { mutableStateOf(false) }
    var termsOpened by remember { mutableIntStateOf(0) }
    CamoCheckbox(rememberMe, { rememberMe = it }, label = "Remember me")
    CamoCheckbox(
        terms,
        { terms = it },
        labelSpans = listOf(CamoSpan("I agree to the "), CamoSpan("Terms", link = { termsOpened++ })),
        error = if (terms) null else "Please accept the terms",
    )
    if (termsOpened > 0) CamoText("Terms opened $termsOpened×", color = Camouflage.colors.onSurfaceVariant)

    var speed by remember { mutableStateOf<String?>("standard") }
    CamoRadioGroup(
        speed,
        { speed = it },
        listOf(
            RadioOption("standard", "Standard", "3 to 5 days, free"),
            RadioOption("express", "Express", "Tomorrow, € 9"),
            RadioOption("pickup", "Pick up in store", "Not available near you", enabled = false),
        ),
        label = "Shipping speed",
    )

    var period by remember { mutableStateOf(Period.Week) }
    CamoSegmentedControl(Period.entries.map { Segment(it, it.label) }, period, { period = it })

    var volume by remember { mutableFloatStateOf(0.4f) }
    var rating by remember { mutableFloatStateOf(3f) }
    CamoSlider(volume, { volume = it }, Modifier.fillMaxWidth(), label = "Volume",
        valueLabel = { "${(it * PERCENT).roundToInt()}%" })
    CamoSlider(rating, { rating = it }, Modifier.fillMaxWidth(), range = 1f..5f, steps = 3, label = "Rating",
        valueLabel = { "${it.roundToInt()} stars" })

    var quantity by remember { mutableIntStateOf(1) }
    var nights by remember { mutableIntStateOf(2) }
    var weight by remember { mutableStateOf(CamoDecimal("1.5")) }
    FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) {
        CamoStepper(quantity, { quantity = it }, range = 1..10, label = "Quantity")
        CamoStepper(nights, { nights = it }, range = 1..14, label = "Nights", allowTyping = false,
            valueText = { if (it == 1) "1 night" else "$it nights" })
        CamoStepper(weight, { weight = it }, CamoDecimal("0.5")..CamoDecimal("5"), CamoDecimal("0.25"),
            decimals = 2, label = "Weight in kg")
    }
}

private const val PERCENT = 100
