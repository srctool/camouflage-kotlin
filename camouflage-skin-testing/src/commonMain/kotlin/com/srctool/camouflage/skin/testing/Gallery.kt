package com.srctool.camouflage.skin.testing

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.divider.CamoDivider
import com.srctool.camouflage.core.divider.DividerInset
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.radio.CamoRadioGroup
import com.srctool.camouflage.core.search.CamoSearchField
import com.srctool.camouflage.core.radio.RadioOption
import com.srctool.camouflage.core.segmented.CamoSegmentedControl
import com.srctool.camouflage.core.segmented.Segment
import com.srctool.camouflage.core.slider.CamoSlider
import com.srctool.camouflage.core.stepper.CamoStepper
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.textfield.CamoFieldFrame
import com.srctool.camouflage.core.textfield.CamoTextField
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.core.textfield.TextFieldTheme
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.SurfaceLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.tooltip.TooltipState

// The components in their states, for screenshot goldens of any skin. Static: no animation, no input.

/** A small star-like test icon. */
public val GalleryIcon: ImageVector = ImageVector.Builder("gallery-icon", 24.dp, 24.dp, 24f, 24f)
    .path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f); lineTo(15f, 9f); lineTo(22f, 9f); lineTo(16.5f, 14f)
        lineTo(18.5f, 21f); lineTo(12f, 17f); lineTo(5.5f, 21f); lineTo(7.5f, 14f)
        lineTo(2f, 9f); lineTo(9f, 9f); close()
    }
    .build()

@Composable
private fun Icon() = CamoIcon(IconSource.Vector(GalleryIcon), contentDescription = null)

/** Every button appearance, enabled and disabled, in the default and the error tone, plus icons and loading. */
@Composable
public fun ButtonGallery() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (tone in listOf(Tone.Default, Tone.Error)) {
            for (enabled in listOf(true, false)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Appearance.entries.forEach {
                        CamoButton(it.name, {}, appearance = it, tone = tone, enabled = enabled)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CamoButton("Save", {}, prefixIcon = { Icon() })
            CamoButton("Next", {}, appearance = Appearance.Outline, suffixIcon = { Icon() })
            CamoButton("Saving", {}, loading = true)
            CamoButton("Done", {}, tone = Tone.Success, appearance = Appearance.Soft)
        }
    }
}

/** Every icon button appearance, a checked toggle, a disabled one and a tone. */
@Composable
public fun IconButtonGallery() {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Appearance.Solid, Appearance.Soft, Appearance.Outline, Appearance.Ghost).forEach {
            CamoIconButton({ Icon() }, it.name, {}, appearance = it)
        }
        CamoIconButton({ Icon() }, "Checked", {}, checked = true)
        CamoIconButton({ Icon() }, "Disabled", {}, enabled = false)
        CamoIconButton({ Icon() }, "Delete", {}, tone = Tone.Error)
    }
}

/** Text styles and links, icons, the six surface levels, dividers, progress circles and a tooltip bubble. */
@Composable
public fun PrimitiveGallery() {
    val theme = Camouflage.theme
    val type = theme.typography
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CamoText("Headline", style = type.headlineSmall)
        CamoText("Title", style = type.titleMedium)
        CamoText(listOf(CamoSpan("Body with a "), CamoSpan("link", link = {}), CamoSpan(".")))
        CamoText("Success", color = theme.colors.success)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceLevel.entries.forEach {
                CamoSurface(Modifier.size(40.dp), level = it, shape = ShapeRole.Medium, border = true) {}
            }
            CamoSurface(Modifier.size(40.dp), shape = ShapeRole.Medium, elevation = ElevationLevel.Level3) {}
        }
        CamoDivider()
        CamoDivider(inset = DividerInset.Middle)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon()
            CamoDivider(orientation = Orientation.Vertical)
            CamoProgressCircle(progress = 0.3f, size = ProgressSize.Small)
            CamoProgressCircle(progress = 0.6f)
            CamoProgressCircle(progress = 0.9f, tone = Tone.Error, size = ProgressSize.Large)
        }
        Camouflage.skin.tooltip.let { renderer ->
            val style = renderer.defaults(theme).merge(theme.components.tooltip)
            renderer.Render(TooltipState("Tooltip", visible = true), theme, style, Modifier.width(IntrinsicSize.Max))
        }
    }
}

/**
 * The M5 selection controls, slider and stepper in their states: checked, unchecked, disabled, with a
 * description and with an error.
 */
@Composable
public fun InputGallery() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CamoCheckbox(false, {})
            CamoCheckbox(true, {})
            CamoCheckbox(true, {}, enabled = false)
            CamoCheckbox(true, {}, label = "Remember me")
        }
        CamoCheckbox(false, {}, label = "I agree", description = "Required to continue", error = "Please accept")
        CamoSwitch(true, {}, Modifier.width(280.dp), label = "Wi-Fi", description = "Uses more battery")
        CamoSwitch(false, {}, Modifier.width(280.dp), label = "Bluetooth")
        CamoSwitch(true, {}, Modifier.width(280.dp), label = "Airplane mode", enabled = false)
        CamoRadioGroup(
            "standard",
            {},
            listOf(
                RadioOption("standard", "Standard", "3 to 5 days"),
                RadioOption("express", "Express"),
                RadioOption("pickup", "Pick up", enabled = false),
            ),
            label = "Shipping",
        )
        CamoSegmentedControl(listOf(Segment("d", "Day"), Segment("w", "Week"), Segment("m", "Month")), "w", {})
        CamoSlider(VOLUME, {}, Modifier.width(280.dp), label = "Volume")
        CamoSlider(RATING, {}, Modifier.width(280.dp), steps = RATING_STEPS, label = "Rating")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CamoStepper(2, {}, range = 0..GALLERY_MAX, label = "Quantity")
            CamoStepper(0, {}, range = 0..GALLERY_MAX, label = "Guests", error = "Add a guest")
        }
    }
}

private const val GALLERY_MAX = 9
private const val VOLUME = 0.3f
private const val RATING = 0.5f
private const val RATING_STEPS = 3

/**
 * The M5 text inputs in their states: empty with a placeholder, filled with an icon and prefix text, with
 * supporting text, an error, disabled, a floating label, a custom input in [CamoFieldFrame], and search fields.
 */
@Composable
public fun TextInputGallery() {
    Column(Modifier.padding(16.dp).width(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CamoTextField("", {}, label = "Email", placeholder = "you@example.com", supportingText = "We never share it")
        CamoTextField("0812345", {}, label = "Phone", prefixIcon = { Icon() }, prefixText = "+62")
        CamoTextField("ab", {}, label = "Password", secure = true, error = "Use at least 8 characters",
            suffixIcon = { CamoFieldAction({ Icon() }, "Show password", {}) })
        CamoTextField("Locked", {}, label = "Company", enabled = false)
        val floating = TextFieldTheme(labelPlacement = LabelPlacement.Floating)
        ProvideComponentThemes(Camouflage.theme.components.copy(textField = floating)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CamoTextField("", {}, label = "Floating, resting")
                CamoTextField("Ann", {}, label = "Floating, floated", suffixText = "kg")
            }
        }
        CamoFieldFrame(focused = false, isEmpty = false, label = "Code") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(GALLERY_CODE) { CamoText("•") } }
        }
        CamoSearchField("", {}, {})
        CamoSearchField("tea", {}, {}, placeholder = "Search drinks")
    }
}

private const val GALLERY_CODE = 4
