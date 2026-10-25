package com.srctool.camouflage.skin.minimal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.skin.testing.ButtonGallery
import com.srctool.camouflage.skin.testing.IconButtonGallery
import com.srctool.camouflage.skin.testing.PrimitiveGallery
import com.srctool.camouflage.skin.testing.withGoldenFont
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

// Screenshot goldens: light and dark for every M3 component, plus one brand theme (an accent), light only
// (Testing §3.2). The goldens are Linux renders, the OS CI verifies on: when one changes on purpose, take the
// `*_actual.png` from the PR's `roborazzi-outputs` artifact. `recordRoborazziJvm` works for a first local look.
// Skia antialiases edges slightly differently on macOS and Linux CI: up to 0.5% of the pixels may differ, while
// a real change (a color, a size) moves far more.
private val CrossPlatform = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
)

@OptIn(ExperimentalTestApi::class)
class MinimalGoldenTest {
    private val themes = mapOf(
        "light" to minimalLight,
        "dark" to minimalDark,
        "brand" to minimalTheme(accent = Color(0xFF0B57D0)),
    )

    @Test fun buttons() = golden("button", width = 900, height = 300) { ButtonGallery() }

    @Test fun iconButtons() = golden("iconbutton", width = 420, height = 80) { IconButtonGallery() }

    @Test fun primitives() = golden("primitives", width = 420, height = 360) { PrimitiveGallery() }

    // Every theme is captured before failing, so a CI run uploads all of them (Linux renders, for re-recording).
    private fun golden(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val failures = themes.mapNotNull { (key, theme) ->
            runCatching {
                runDesktopComposeUiTest(width, height) {
                    setContent { CamouflagePreview(MinimalSkin, theme.withGoldenFont(), content) }
                    onRoot().captureRoboImage(
                        roborazziOptions = CrossPlatform,
                        filePath = "src/jvmTest/snapshots/${name}_minimal_$key.png",
                    )
                }
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { throw it }
    }
}

