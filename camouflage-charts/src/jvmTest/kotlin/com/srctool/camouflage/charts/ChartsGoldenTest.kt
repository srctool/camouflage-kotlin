package com.srctool.camouflage.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.github.takahirom.roborazzi.RoborazziOptions
import com.srctool.camouflage.core.provider.CamouflagePreview
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.skin.carbon.CarbonSkin
import com.srctool.camouflage.skin.carbon.carbonDark
import com.srctool.camouflage.skin.carbon.carbonLight
import com.srctool.camouflage.skin.cupertino.CupertinoSkin
import com.srctool.camouflage.skin.cupertino.cupertinoDark
import com.srctool.camouflage.skin.cupertino.cupertinoLight
import com.srctool.camouflage.skin.fluent.FluentSkin
import com.srctool.camouflage.skin.fluent.fluentTheme
import com.srctool.camouflage.skin.material.MaterialSkin
import com.srctool.camouflage.skin.material.materialBaselineDark
import com.srctool.camouflage.skin.material.materialBaselineLight
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.minimal.minimalDark
import com.srctool.camouflage.skin.minimal.minimalLight
import com.srctool.camouflage.skin.testing.withGoldenFont
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

// Screenshot goldens of the charts in both skins, light and dark. They are Linux renders, the OS CI verifies on:
// when one changes on purpose, take the `*_actual.png` from the PR's `roborazzi-outputs` artifact.
private val CrossPlatform = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
)

@OptIn(ExperimentalTestApi::class)
class ChartsGoldenTest {
    private val scenes = listOf<Triple<String, CamoSkin, CamoTheme>>(
        Triple("minimal_light", MinimalSkin, minimalLight),
        Triple("minimal_dark", MinimalSkin, minimalDark),
        Triple("fluent_light", FluentSkin, fluentTheme()),
        Triple("fluent_dark", FluentSkin, fluentTheme(dark = true)),
        Triple("material_light", MaterialSkin, materialBaselineLight),
        Triple("material_dark", MaterialSkin, materialBaselineDark),
        Triple("cupertino_light", CupertinoSkin, cupertinoLight),
        Triple("cupertino_dark", CupertinoSkin, cupertinoDark),
        Triple("carbon_light", CarbonSkin, carbonLight),
        Triple("carbon_dark", CarbonSkin, carbonDark),
    )

    @Test fun charts() = golden("charts", width = 480, height = 560) { ChartsGallery() }

    @Test fun cartesian() = golden("cartesian", width = 480, height = 1560) { CartesianGallery() }

    // Every scene is captured before failing, so a CI run uploads all of them (Linux renders, for re-recording).
    private fun golden(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val failures = scenes.mapNotNull { (key, skin, theme) ->
            runCatching {
                runDesktopComposeUiTest(width, height) {
                    // No draw-in: the screenshot shows the finished chart.
                    val still = theme.withGoldenFont().let { it.copy(motion = it.motion.reduced()) }
                    setContent { CamouflagePreview(skin, still, content) }
                    onAllNodes(isRoot()).onFirst().captureRoboImage(
                        roborazziOptions = CrossPlatform,
                        filePath = "src/jvmTest/snapshots/${name}_$key.png",
                    )
                }
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { throw it }
    }
}
