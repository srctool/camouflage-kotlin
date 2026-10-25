package com.srctool.camouflage.skin.minimal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.skin.testing.ButtonGallery
import com.srctool.camouflage.skin.testing.IconButtonGallery
import com.srctool.camouflage.skin.testing.PrimitiveGallery
import com.srctool.camouflage.skin.testing.withGoldenFont
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

// Screenshot goldens: light and dark for every M3 component, plus one brand theme (an accent), light only
// (Testing §3.2). Record with `./gradlew recordRoborazziJvm`, verify with `verifyRoborazziJvm`.
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

    private fun golden(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        for ((key, theme) in themes) {
            runDesktopComposeUiTest(width, height) {
                setContent { CamouflagePreview(MinimalSkin, theme.withGoldenFont(), content) }
                onRoot().captureRoboImage("src/jvmTest/snapshots/${name}_minimal_$key.png")
            }
        }
    }
}

