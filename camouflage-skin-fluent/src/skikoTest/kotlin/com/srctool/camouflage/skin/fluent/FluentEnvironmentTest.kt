package com.srctool.camouflage.skin.fluent

import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.environment.forEnvironment
import com.srctool.camouflage.core.snackbar.SnackbarAlignment
import com.srctool.camouflage.core.theme.ElevationLevel
import kotlin.test.Test
import kotlin.test.assertEquals

// Fluent's environment-driven defaults (ADR-41): density follows the input, the toast's place the window.
class FluentEnvironmentTest {
    private val caps =
        CamoCapabilities(backdropBlur = true, runtimeShaders = true, windowMaterial = false, reduceTransparency = false)
    private val desktop = CamoEnvironment(WindowSizeClass.Expanded, InputMode.Pointer, CamoPlatform.Windows, caps)
    private val phone = CamoEnvironment(WindowSizeClass.Compact, InputMode.Touch, CamoPlatform.Android, caps)

    @Test
    fun theDefaultDensityFollowsTheInput() {
        assertEquals(32.dp, fluentTheme().forEnvironment(desktop).components.button?.height)
        assertEquals(40.dp, fluentTheme().forEnvironment(phone).components.button?.height)
        assertEquals(40.dp, fluentTheme().forEnvironment(phone).components.iconButton?.containerSize)
    }

    @Test
    fun theTouchDensityKeepsABrandsButtonOverrides() {
        val brand = fluentTheme().let {
            val button = it.components.button?.copy(raisedElevation = ElevationLevel.Level3)
            it.copy(components = it.components.copy(button = button))
        }
        val touch = brand.forEnvironment(phone).components.button
        assertEquals(40.dp, touch?.height)
        assertEquals(ElevationLevel.Level3, touch?.raisedElevation)
    }

    @Test
    fun anExplicitDensityStays() {
        assertEquals(24.dp, fluentTheme(density = FluentDensity.Small).forEnvironment(phone).components.button?.height)
    }

    @Test
    fun theToastSitsAtTheEndExceptOnAPhone() {
        val theme = fluentTheme()
        assertEquals(SnackbarAlignment.End, FluentSkin.snackbar.defaults(theme.forEnvironment(desktop)).alignment)
        assertEquals(SnackbarAlignment.Center, FluentSkin.snackbar.defaults(theme.forEnvironment(phone)).alignment)
    }
}
