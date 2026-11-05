package com.srctool.camouflage.skin.cupertino

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.theme.SurfaceLevel
import kotlin.test.assertEquals
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.ShapeRole
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.skin.testing.ButtonGallery
import com.srctool.camouflage.skin.testing.DataDisplayGallery
import com.srctool.camouflage.skin.testing.DialogGallery
import com.srctool.camouflage.skin.testing.FeedbackGallery
import com.srctool.camouflage.skin.testing.IconButtonGallery
import com.srctool.camouflage.skin.testing.InputGallery
import com.srctool.camouflage.skin.testing.MenuGallery
import com.srctool.camouflage.skin.testing.NavigationChromeGallery
import com.srctool.camouflage.skin.testing.NavigationGallery
import com.srctool.camouflage.skin.testing.ActionsGallery
import com.srctool.camouflage.skin.testing.CollectionsGallery
import com.srctool.camouflage.skin.testing.ListsGallery
import com.srctool.camouflage.skin.testing.PanelsGallery
import com.srctool.camouflage.skin.testing.SmallDataGallery
import com.srctool.camouflage.skin.testing.PickerGallery
import com.srctool.camouflage.skin.testing.PrimitiveGallery
import com.srctool.camouflage.skin.testing.StructureGallery
import com.srctool.camouflage.skin.testing.TextInputGallery
import com.srctool.camouflage.skin.testing.withGoldenFont
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

// Screenshot goldens: light and dark for every component so far, plus one brand theme (an accent), light only
// (Testing §3.2). The goldens are Linux renders, the OS CI verifies on: when one changes on purpose, take the
// `*_actual.png` from the PR's `roborazzi-outputs` artifact. `recordRoborazziJvm` works for a first local look.
// Skia antialiases edges slightly differently on macOS and Linux CI: up to 0.5% of the pixels may differ, while
// a real change (a color, a size) moves far more.
private val CrossPlatform = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
)

// Bars of color for the materials to blur (the content), and over it, in the backdrop's overlay, a panel, a capsule bar
// and two buttons: the materials of an app's bars.
@Composable
private fun MaterialsScene() {
    CupertinoBackdrop(
        Modifier.fillMaxSize(),
        overlay = {
            Column(
                Modifier.align(Alignment.Center).width(372.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CamoSurface(
                    Modifier.fillMaxWidth().height(96.dp),
                    shape = ShapeRole.Large,
                    elevation = ElevationLevel.Level3,
                ) {
                    Box(Modifier.padding(16.dp)) { CamoText("A material panel") }
                }
                CamoSurface(
                    Modifier.fillMaxWidth().height(56.dp),
                    shape = ShapeRole.Full,
                    elevation = ElevationLevel.Level2,
                ) {
                    Box(Modifier.padding(16.dp)) { CamoText("A capsule bar") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CamoButton("Solid", {})
                    CamoButton("Soft", {}, appearance = Appearance.Soft)
                }
            }
        },
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFE53935), Color(0xFFFFB300), Color(0xFF43A047), Color(0xFF1E88E5)),
                ),
            ),
        )
    }
}

private val Capabilities = CamoCapabilities(
    backdropBlur = true,
    runtimeShaders = true,
    windowMaterial = false,
    reduceTransparency = false,
)

@OptIn(ExperimentalTestApi::class)
class CupertinoGoldenTest {
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

    // The materials: Classic and Liquid Glass panels over a colorful app, so the blur and the refraction show.
    @Test fun materials() {
        val skins = mapOf("classic" to CupertinoSkin, "glass" to cupertinoSkin(CupertinoSurface.LiquidGlass))
        val failures = skins.flatMap { (skinKey, skin) ->
            mapOf("light" to cupertinoLight, "dark" to cupertinoDark).map { (key, theme) ->
                runCatching {
                    runDesktopComposeUiTest(420, 320) {
                        setContent {
                            CamouflagePreview(skin, theme.withGoldenFont()) { MaterialsScene() }
                        }
                        onAllNodes(isRoot()).onFirst().captureRoboImage(
                            roborazziOptions = CrossPlatform,
                            filePath = "src/jvmTest/snapshots/materials_cupertino_${skinKey}_$key.png",
                        )
                    }
                }.exceptionOrNull()
            }
        }
        failures.filterNotNull().firstOrNull()?.let { throw it }
    }

    // The materials chain (ADR-42), step by step, with the capability forced: glass without the shader (blur and
    // specular edge), no backdrop blur (opaque), Reduce Transparency (opaque), for Classic and Glass.
    @Test fun materialsWithoutShaders() =
        chain("noshaders", Capabilities.copy(runtimeShaders = false), glassOnly = true)

    @Test fun materialsWithoutBlur() = chain("noblur", Capabilities.copy(backdropBlur = false))

    @Test fun materialsWithReducedTransparency() = chain("reduced", Capabilities.copy(reduceTransparency = true))

    private fun chain(name: String, capabilities: CamoCapabilities, glassOnly: Boolean = false) {
        val environment = CamoEnvironment(WindowSizeClass.Medium, InputMode.Touch, CamoPlatform.Ios, capabilities)
        val skins = mapOf("classic" to CupertinoSkin, "glass" to cupertinoSkin(CupertinoSurface.LiquidGlass))
            .filterKeys { !glassOnly || it == "glass" }
        val failures = skins.flatMap { (skinKey, skin) ->
            mapOf("light" to cupertinoLight, "dark" to cupertinoDark).map { (key, theme) ->
                runCatching {
                    runDesktopComposeUiTest(420, 320) {
                        setContent {
                            CamouflagePreview(skin, theme.withGoldenFont()) {
                                CamouflageProvider(environment = environment) { MaterialsScene() }
                            }
                        }
                        onAllNodes(isRoot()).onFirst().captureRoboImage(
                            roborazziOptions = CrossPlatform,
                            filePath = "src/jvmTest/snapshots/materials_${name}_cupertino_${skinKey}_$key.png",
                        )
                    }
                }.exceptionOrNull()
            }
        }
        failures.filterNotNull().firstOrNull()?.let { throw it }
    }

    // A material that has no backdrop to blur, or sits below Level2, is its plain surface color (opaque).
    @Test fun aMaterialWithoutABackdropIsOpaque() = runDesktopComposeUiTest(120, 60) {
        setContent {
            CamouflagePreview(cupertinoSkin(CupertinoSurface.LiquidGlass), cupertinoLight.withGoldenFont()) {
                Box(Modifier.fillMaxSize().background(Color.Red)) {
                    CamoSurface(
                        Modifier.fillMaxSize(),
                        level = SurfaceLevel.Low,
                        elevation = ElevationLevel.Level3,
                    ) {}
                }
            }
        }
        assertEquals(cupertinoLight.colors.surfaceContainerLow, onRoot().captureToImage().toPixelMap()[60, 30])
    }

    // A material in the backdrop's own tree would sample a layer that contains it (a render-node cycle): opaque.
    @Test fun aMaterialInTheBackdropsOwnTreeIsOpaque() = runDesktopComposeUiTest(120, 60) {
        setContent {
            CamouflagePreview(cupertinoSkin(CupertinoSurface.LiquidGlass), cupertinoLight.withGoldenFont()) {
                CupertinoBackdrop {
                    Box(Modifier.fillMaxSize().background(Color.Red)) {
                        CamoSurface(
                            Modifier.fillMaxSize(),
                            level = SurfaceLevel.Low,
                            elevation = ElevationLevel.Level3,
                        ) {}
                    }
                }
            }
        }
        assertEquals(cupertinoLight.colors.surfaceContainerLow, onRoot().captureToImage().toPixelMap()[60, 30])
    }

    @Test fun aMaterialBelowLevel2IsOpaque() = runDesktopComposeUiTest(120, 60) {
        setContent {
            CamouflagePreview(cupertinoSkin(CupertinoSurface.LiquidGlass), cupertinoLight.withGoldenFont()) {
                CupertinoBackdrop {
                    Box(Modifier.fillMaxSize().background(Color.Red)) {
                        CamoSurface(
                            Modifier.fillMaxSize(),
                            level = SurfaceLevel.Low,
                            elevation = ElevationLevel.Level1,
                        ) {}
                    }
                }
            }
        }
        assertEquals(cupertinoLight.colors.surfaceContainerLow, onRoot().captureToImage().toPixelMap()[60, 30])
    }

    // Every theme is captured before failing, so a CI run uploads all of them (Linux renders, for re-recording).
    private fun golden(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val failures = themes.mapNotNull { (key, theme) ->
            runCatching {
                runDesktopComposeUiTest(width, height) {
                    setContent { CamouflagePreview(CupertinoSkin, theme.withGoldenFont(), content) }
                    // The first root is the window, popups included (the menu gallery opens one).
                    onAllNodes(isRoot()).onFirst().captureRoboImage(
                        roborazziOptions = CrossPlatform,
                        filePath = "src/jvmTest/snapshots/${name}_cupertino_$key.png",
                    )
                }
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { throw it }
    }
}

