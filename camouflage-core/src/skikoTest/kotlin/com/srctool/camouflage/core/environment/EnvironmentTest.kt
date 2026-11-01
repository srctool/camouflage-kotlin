package com.srctool.camouflage.core.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.provider.ProvideCamouflage
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.theme.CamoSpacing
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.lerp
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

// The context layer (ADR-41): the environment, breakpoints and adaptive themes.
@OptIn(ExperimentalTestApi::class)
class EnvironmentTest {
    private val caps =
        CamoCapabilities(backdropBlur = true, runtimeShaders = true, windowMaterial = false, reduceTransparency = false)
    private val pointer = CamoEnvironment(WindowSizeClass.Expanded, InputMode.Pointer, CamoPlatform.Windows, caps)
    private val touch =
        pointer.copy(windowSize = WindowSizeClass.Compact, input = InputMode.Touch, platform = CamoPlatform.Android)

    // Spacing.s is 8 by default; the rule makes it 4 with a pointer.
    private val dense =
        themeT.adapt { env -> if (env.input == InputMode.Pointer) copy(spacing = CamoSpacing(s = 4.dp)) else this }

    @Composable
    private fun Window(widthPx: Int, content: @Composable () -> Unit) {
        val real = LocalWindowInfo.current
        val window = object : WindowInfo {
            override val isWindowFocused: Boolean get() = real.isWindowFocused
            override val keyboardModifiers: PointerKeyboardModifiers get() = real.keyboardModifiers
            override val containerSize: IntSize get() = IntSize(widthPx, 800)
        }
        CompositionLocalProvider(LocalWindowInfo provides window, LocalDensity provides Density(1f), content = content)
    }

    @Test
    fun breakpointsClassifyTheWidth() {
        val b = CamoBreakpoints()
        assertEquals(WindowSizeClass.Compact, b.classify(599.dp))
        assertEquals(WindowSizeClass.Medium, b.classify(600.dp))
        assertEquals(WindowSizeClass.Expanded, b.classify(840.dp))
        assertEquals(WindowSizeClass.Large, b.classify(1200.dp))
        assertEquals(WindowSizeClass.Medium, CamoBreakpoints(medium = 500.dp).classify(550.dp))
    }

    @Test
    fun theRootFollowsTheWindowWidthAndTheThemesBreakpoints() = runComposeUiTest {
        var width by mutableStateOf(400)
        var seen: WindowSizeClass? = null
        setContent {
            Window(width) {
                CamouflageProvider(skinA, themeT.copy(breakpoints = CamoBreakpoints(medium = 700.dp))) {
                    seen = Camouflage.environment.windowSize
                }
            }
        }
        assertEquals(WindowSizeClass.Compact, seen)
        width = 650
        waitForIdle()
        assertEquals(WindowSizeClass.Compact, seen)
        width = 900
        waitForIdle()
        assertEquals(WindowSizeClass.Expanded, seen)
    }

    @Test
    fun aRuleRunsForTheEnvironmentAndTheThemeCarriesIt() = runComposeUiTest {
        var theme: CamoTheme? = null
        var environment by mutableStateOf(touch)
        setContent { CamouflageProvider(skinA, dense, environment) { theme = Camouflage.theme } }
        assertEquals(8.dp, theme?.spacing?.s)
        assertEquals(touch, theme?.environment)
        environment = pointer
        waitForIdle()
        assertEquals(4.dp, theme?.spacing?.s)
        assertEquals(pointer, theme?.environment)
    }

    @Test
    fun aNestedProviderAdaptsTheWrittenThemeOnce() = runComposeUiTest {
        // A rule that halves spacing each time would show a second run.
        val halving = themeT.adapt { copy(spacing = CamoSpacing(s = spacing.s / 2)) }
        var outer: CamoTheme? = null
        var inner: CamoTheme? = null
        var override: CamoTheme? = null
        setContent {
            CamouflageProvider(skinA, halving, pointer) {
                outer = Camouflage.theme
                CamouflageProvider(skin = skinA) { inner = Camouflage.theme }
                CamouflageProvider(environment = touch) { override = Camouflage.theme }
            }
        }
        assertEquals(4.dp, outer?.spacing?.s)
        assertEquals(4.dp, inner?.spacing?.s)
        assertEquals(4.dp, override?.spacing?.s)
        assertEquals(touch, override?.environment)
    }

    @Test
    fun aThemeReadFromAProviderAndPassedOnIsNotAdaptedAgain() = runComposeUiTest {
        val halving = themeT.adapt { copy(spacing = CamoSpacing(s = spacing.s / 2)) }
        var inner: CamoTheme? = null
        setContent {
            CamouflageProvider(skinA, halving, pointer) {
                CamouflageProvider(theme = Camouflage.theme) { inner = Camouflage.theme }
            }
        }
        assertEquals(4.dp, inner?.spacing?.s)
    }

    @Test
    fun rulesComposeInOrder() {
        val theme = themeT
            .adapt { copy(spacing = CamoSpacing(s = 10.dp)) }
            .adapt { copy(spacing = CamoSpacing(s = spacing.s * 2)) }
        assertEquals(20.dp, theme.forEnvironment(pointer).spacing.s)
    }

    @Test
    fun aThemeYouBuildHasNoEnvironmentAndLerpKeepsTheRules() {
        assertNull(themeT.environment)
        val other = dense.copy(breakpoints = CamoBreakpoints(medium = 500.dp))
        val mid = lerp(themeT, other, 0.75f)
        assertSame(other.adaptation, mid.adaptation)
        assertEquals(other.breakpoints, mid.breakpoints)
    }

    @Test
    fun reducedMotionStillAppliesAfterTheRules() = runComposeUiTest {
        var theme: CamoTheme? = null
        setContent {
            ProvideCamouflage(skinA, dense, pointer, reduceMotion = true) {
                theme = Camouflage.theme
            }
        }
        assertEquals(0, theme?.motion?.durationMedium)
        assertEquals(4.dp, theme?.spacing?.s)
    }

    @Test
    fun theDefaultInputFollowsThePlatform() {
        assertEquals(InputMode.Touch, defaultInput(CamoPlatform.Ios))
        assertEquals(InputMode.Touch, defaultInput(CamoPlatform.Android))
        assertEquals(InputMode.Pointer, defaultInput(CamoPlatform.Web))
        assertEquals(InputMode.Pointer, defaultInput(CamoPlatform.MacOs))
    }
}
