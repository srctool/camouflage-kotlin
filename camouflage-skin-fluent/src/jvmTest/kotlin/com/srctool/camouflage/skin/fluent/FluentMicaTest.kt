package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import kotlin.test.Test
import kotlin.test.assertEquals

// Mica's fallback chain (ADR-42): the window material shows through the base layers, else the opaque surface.
@OptIn(ExperimentalTestApi::class)
class FluentMicaTest {
    private val window = Color(0xFFFF00FF)
    private val surface = Color(0xFF00AA00)

    private fun environment(windowMaterial: Boolean, reduceTransparency: Boolean = false) = CamoEnvironment(
        WindowSizeClass.Expanded,
        InputMode.Pointer,
        CamoPlatform.Windows,
        CamoCapabilities(true, true, windowMaterial, reduceTransparency),
    )

    // A base layer (elevation 0) of `surface` over a window-colored background; returns the middle pixel.
    private fun pixel(env: CamoEnvironment, elevation: Int = 0): Color {
        var result = Color.Unspecified
        runComposeUiTest {
            setContent {
                CamouflageProvider(fluentSkin(FluentSurface.Mica), fluentTheme(), env) {
                    Layer(elevation)
                }
            }
            val image = onNodeWithTag("window").captureToImage().toPixelMap()
            result = image[image.width / 2, image.height / 2]
        }
        return result
    }

    @Composable
    private fun Layer(elevation: Int) {
        val theme = Camouflage.theme
        val depth = if (elevation == 0) 0.dp else theme.elevation.level3
        Box(Modifier.testTag("window").size(40.dp).background(window)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(Camouflage.skin.surfaceTreatment.modifier(surface, RectangleShape, depth, theme)),
            )
        }
    }

    @Test
    fun theWindowMaterialShowsThroughABaseLayer() {
        assertEquals(window, pixel(environment(windowMaterial = true)))
    }

    @Test
    fun withoutAWindowMaterialTheBaseLayerIsSolid() {
        assertEquals(surface, pixel(environment(windowMaterial = false)))
    }

    @Test
    fun reduceTransparencyKeepsTheSolidSurface() {
        assertEquals(surface, pixel(environment(windowMaterial = true, reduceTransparency = true)))
    }

    @Test
    fun raisedSurfacesStaySolid() {
        assertEquals(surface, pixel(environment(windowMaterial = true), elevation = 3))
    }
}
