package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.github.takahirom.roborazzi.RoborazziOptions
import com.srctool.camouflage.core.menu.CamoMenuPanel
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.skin.testing.ButtonGallery
import com.srctool.camouflage.skin.testing.DialogGallery
import com.srctool.camouflage.skin.testing.IconButtonGallery
import com.srctool.camouflage.skin.testing.InputGallery
import com.srctool.camouflage.skin.testing.MenuGallery
import com.srctool.camouflage.skin.testing.PickerGallery
import com.srctool.camouflage.skin.testing.PrimitiveGallery
import com.srctool.camouflage.skin.testing.StructureGallery
import com.srctool.camouflage.skin.testing.TextInputGallery
import com.srctool.camouflage.skin.testing.withGoldenFont
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

// Screenshot goldens: light and dark at Medium density, Small density in light, and one brand theme
// (Testing §3.2). The goldens are Linux renders, the OS CI verifies on: when one changes on purpose, take the
// `*_actual.png` from the PR's `roborazzi-outputs` artifact. `recordRoborazziJvm` works for a first local look.
// Skia antialiases edges slightly differently on macOS and Linux CI: up to 0.5% of the pixels may differ, while
// a real change (a color, a size) moves far more.
private val CrossPlatform = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
)

@OptIn(ExperimentalTestApi::class)
class FluentGoldenTest {
    private val themes = mapOf(
        "light" to fluentTheme(),
        "dark" to fluentTheme(dark = true),
        "small" to fluentTheme(density = FluentDensity.Small),
        "brand" to fluentTheme(brand = Color(0xFF8A3FFC)),
    )

    @Test fun buttons() = golden("button", width = 900, height = 300) { ButtonGallery() }

    @Test fun iconButtons() = golden("iconbutton", width = 420, height = 80) { IconButtonGallery() }

    @Test fun primitives() = golden("primitives", width = 420, height = 360) { PrimitiveGallery() }

    @Test fun inputs() = golden("inputs", width = 520, height = 720) { InputGallery() }

    @Test fun textInputs() = golden("textinputs", width = 360, height = 820) { TextInputGallery() }

    @Test fun pickers() = golden("pickers", width = 400, height = 1900) { PickerGallery() }

    @Test fun structure() = golden("structure", width = 400, height = 1300) { StructureGallery() }

    @Test fun dialogs() = golden("dialogs", width = 420, height = 1000) { DialogGallery() }

    @Test fun menus() = golden("menus", width = 360, height = 480) { MenuGallery() }

    // Acrylic: a panel at flyout depth in a popup over a striped backdrop, the way flyouts and dialogs open.
    @Test
    fun acrylic() = runDesktopComposeUiTest(360, 200) {
        setContent {
            CamouflagePreview(fluentSkin(FluentSurface.Acrylic), fluentTheme().withGoldenFont()) {
                FluentBackdrop(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        repeat(STRIPES) {
                            val color = if (it % 2 == 0) Color(0xFF0F6CBD) else Color(0xFFF7630C)
                            Box(Modifier.fillMaxWidth().weight(1f).background(color))
                        }
                    }
                    Popup(alignment = Alignment.Center) {
                        CamoMenuPanel { CamoText("Acrylic", Modifier.padding(24.dp)) }
                    }
                }
            }
        }
        // The popup is a second root; the first one is the window, popup included.
        onAllNodes(isRoot()).onFirst()
            .captureRoboImage(roborazziOptions = CrossPlatform, filePath = "src/jvmTest/snapshots/acrylic_fluent.png")
    }

    // Every theme is captured before failing, so a CI run uploads all of them (Linux renders, for re-recording).
    private fun golden(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val failures = themes.mapNotNull { (key, theme) ->
            runCatching {
                runDesktopComposeUiTest(width, height) {
                    setContent { CamouflagePreview(FluentSkin, theme.withGoldenFont(), content) }
                    // The first root is the window, popups included (the menu gallery opens one).
                    onAllNodes(isRoot()).onFirst().captureRoboImage(
                        roborazziOptions = CrossPlatform,
                        filePath = "src/jvmTest/snapshots/${name}_fluent_$key.png",
                    )
                }
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { throw it }
    }
}

private const val STRIPES = 8
