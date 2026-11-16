package com.srctool.camouflage.skin.cupertino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.SurfaceLevel
import com.srctool.camouflage.skin.testing.ActionsGallery
import com.srctool.camouflage.skin.testing.ButtonGallery
import com.srctool.camouflage.skin.testing.CollectionsGallery
import com.srctool.camouflage.skin.testing.DataDisplayGallery
import com.srctool.camouflage.skin.testing.DialogGallery
import com.srctool.camouflage.skin.testing.FeedbackGallery
import com.srctool.camouflage.skin.testing.IconButtonGallery
import com.srctool.camouflage.skin.testing.InputGallery
import com.srctool.camouflage.skin.testing.ListsGallery
import com.srctool.camouflage.skin.testing.MenuGallery
import com.srctool.camouflage.skin.testing.NavigationChromeGallery
import com.srctool.camouflage.skin.testing.NavigationGallery
import com.srctool.camouflage.skin.testing.PanelsGallery
import com.srctool.camouflage.skin.testing.PickerGallery
import com.srctool.camouflage.skin.testing.PrimitiveGallery
import com.srctool.camouflage.skin.testing.SmallDataGallery
import com.srctool.camouflage.skin.testing.StructureGallery
import com.srctool.camouflage.skin.testing.TextInputGallery
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Screenshot goldens on the Android host: Robolectric with native graphics, so the pixels are Android's own text and
// shapes (the JVM goldens are Skia desktop). Light, dark and brand for every gallery, like the JVM goldens. The
// goldens are Linux renders, the OS CI verifies on: when one changes on purpose, take the `*_actual.png` from the
// PR's `roborazzi-outputs` artifact.
private val CrossPlatform = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
)

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35]) // 36 needs Java 21; CI runs 17
class CupertinoAndroidGoldenTest {
    private val themes = mapOf(
        "light" to cupertinoLight,
        "dark" to cupertinoDark,
        "brand" to cupertinoTheme(accent = Color(0xFF0B57D0)),
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

    @Test fun dataDisplay() = golden("datadisplay", width = 560, height = 420) { DataDisplayGallery() }

    @Test fun feedback() = golden("feedback", width = 420, height = 720) { FeedbackGallery() }

    @Test fun navigation() = golden("navigation", width = 480, height = 420) { NavigationGallery() }

    @Test fun navigationChrome() = golden("navchrome", width = 480, height = 480) { NavigationChromeGallery() }

    @Test fun panels() = golden("panels", width = 640, height = 560) { PanelsGallery() }

    @Test fun actions() = golden("actions", width = 640, height = 480) { ActionsGallery() }

    @Test fun smalldata() = golden("smalldata", width = 640, height = 720) { SmallDataGallery() }

    @Test fun lists() = golden("lists", width = 640, height = 560) { ListsGallery() }

    @Test fun collections() = golden("collections", width = 640, height = 560) { CollectionsGallery() }

    // Every theme is captured before failing, so a CI run uploads all of them (Linux renders, for re-recording).
    private fun golden(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        RuntimeEnvironment.setQualifiers("w${width}dp-h${height}dp")
        val failures = themes.mapNotNull { (key, theme) ->
            runCatching {
                runComposeUiTest {
                    setContent { CamouflagePreview(CupertinoSkin, theme, content) }
                    onRoot().captureRoboImage(
                        roborazziOptions = CrossPlatform,
                        filePath = "src/androidHostTest/snapshots/${name}_cupertino_$key.png",
                    )
                }
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { throw it }
    }
}
