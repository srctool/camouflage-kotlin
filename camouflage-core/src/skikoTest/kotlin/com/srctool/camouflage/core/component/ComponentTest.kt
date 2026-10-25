package com.srctool.camouflage.core.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.ButtonStyle
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.divider.CamoDivider
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.SurfaceLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.tooltip.CamoTooltip
import com.srctool.camouflage.core.util.ProvideContentStyle
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Range = SemanticsProperties.ProgressBarRangeInfo

// The M3 components' tests from their Kotlin notes, under the plain test skin.
@OptIn(ExperimentalTestApi::class)
class ComponentTest {
    private val dot = ImageVector.Builder("dot", 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black)) { moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); close() }
        .build()

    private fun ComposeUiTest.camo(theme: CamoTheme = themeT, content: @Composable () -> Unit) =
        setContent { CamouflageProvider(skinA, theme, content) }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()

    // CamoText

    @Test
    fun textTakesTheContentColor() = runComposeUiTest {
        var root = Color.Unspecified
        var inside = Color.Unspecified
        camo {
            root = Camouflage.contentColor
            ProvideContentStyle(Color.Red, IconStyle(18.dp, Color.Red)) { inside = Camouflage.contentColor }
            CamoText("Hello")
        }
        waitForIdle()
        assertEquals(themeT.colors.onSurface, root)
        assertEquals(Color.Red, inside)
        onNodeWithText("Hello").assertExists()
    }

    @Test
    fun eachLinkIsItsOwnLink() = runComposeUiTest {
        val clicked = mutableListOf<String>()
        camo {
            CamoText(
                listOf(
                    CamoSpan("I agree to the "),
                    CamoSpan("Terms", link = { clicked += "terms" }),
                    CamoSpan(" and "),
                    CamoSpan("Privacy", link = { clicked += "privacy" }),
                ),
            )
        }
        waitForIdle()
        val text = onNodeWithText("I agree to the Terms and Privacy").fetchSemanticsNode()
        val layout = mutableListOf<TextLayoutResult>()
        text.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action!!.invoke(layout)
        val result = layout.single()
        val privacy = result.getBoundingBox("I agree to the Terms and P".length)
        onNodeWithText("I agree to the Terms and Privacy").performTouchInput { click(privacy.center) }
        waitForIdle()
        assertEquals(listOf("privacy"), clicked)
    }

    @Test
    fun maxLinesEllipsizes() = runComposeUiTest {
        var overflow = false
        camo {
            Box(Modifier.width(40.dp)) {
                CamoText("A very long sentence that cannot fit", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        waitForIdle()
        val node = onNodeWithText("A very long sentence that cannot fit").fetchSemanticsNode()
        val layout = mutableListOf<TextLayoutResult>()
        node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action!!.invoke(layout)
        overflow = layout.single().hasVisualOverflow
        assertTrue(overflow)
    }

    // CamoIcon

    @Test
    fun iconFollowsTheAmbientStyleAndParametersOverride() = runComposeUiTest {
        camo {
            ProvideContentStyle(Color.Red, IconStyle(32.dp, Color.Red)) {
                Column {
                    CamoIcon(IconSource.Vector(dot), "ambient", Modifier.testTag("a"))
                    CamoIcon(IconSource.Vector(dot), "param", Modifier.testTag("b"), size = 12.dp)
                }
            }
        }
        waitForIdle()
        assertEquals(px(32), onNodeWithTag("a").fetchSemanticsNode().size.width)
        assertEquals(px(12), onNodeWithTag("b").fetchSemanticsNode().size.width)
    }

    @Test
    fun aDecorativeIconHasNoSemantics() = runComposeUiTest {
        camo { CamoIcon(IconSource.Vector(dot), null, Modifier.testTag("deco")) }
        waitForIdle()
        val node = onNodeWithTag("deco", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(SemanticsProperties.ContentDescription !in node.config)
    }

    // CamoSurface and CamoDivider

    @Test
    fun surfaceContentIsOnSurface() = runComposeUiTest {
        var color = Color.Unspecified
        camo {
            ProvideContentStyle(Color.Red, IconStyle(18.dp, Color.Red)) {
                CamoSurface(level = SurfaceLevel.High) { color = Camouflage.contentColor }
            }
        }
        waitForIdle()
        assertEquals(themeT.colors.onSurface, color)
    }

    @Test
    fun aDividerIsHiddenAndTakesItsThickness() = runComposeUiTest {
        camo { CamoDivider(Modifier.testTag("line"), orientation = Orientation.Horizontal) }
        waitForIdle()
        val node = onNodeWithTag("line", useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(px(1), node.size.height)
        assertTrue(SemanticsProperties.ContentDescription !in node.config && SemanticsProperties.Text !in node.config)
        assertTrue(node.children.isEmpty())
    }

    // CamoButton

    @Test
    fun aButtonClicksOncePerTapAndNotWhenDisabledOrLoading() = runComposeUiTest {
        var clicks = 0
        camo {
            Column {
                CamoButton("Save", { clicks++ })
                CamoButton("Off", { clicks += 10 }, enabled = false)
                CamoButton("Busy", { clicks += 100 }, loading = true)
            }
        }
        onNodeWithText("Save").performClick().performClick()
        onNodeWithText("Off").assertIsNotEnabled().performClick()
        onNodeWithText("Busy").performClick()
        waitForIdle()
        assertEquals(2, clicks)
        onNodeWithText("Save").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        onNodeWithText("Busy").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Busy"))
    }

    @Test
    fun aSmallButtonStillHasA48Target() = runComposeUiTest {
        camo {
            val small = themeT.copy(components = CamoComponentThemes(button = ButtonTheme(height = 32.dp)))
            CamouflageProvider(theme = small) {
                CamoButton("OK", {})
            }
        }
        waitForIdle()
        assertTrue(onNodeWithText("OK").fetchSemanticsNode().size.height >= px(48))
    }

    @Test
    fun aPrefixIconTakesTheButtonsIconSize() = runComposeUiTest {
        camo { CamoButton("Save", {}, prefixIcon = { CamoIcon(IconSource.Vector(dot), null, Modifier.testTag("i")) }) }
        waitForIdle()
        val iconSize = skinA.button.defaults(themeT).iconSize
        val icon = onNodeWithTag("i", useUnmergedTree = true).fetchSemanticsNode()
        assertEquals(px(iconSize.value.toInt()), icon.size.width)
    }

    @Test
    fun aButtonGrowsWithFontScale() = runComposeUiTest {
        camo {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                CamoButton("Large text", {})
            }
        }
        waitForIdle()
        val height = skinA.button.defaults(themeT).height
        assertTrue(onNodeWithText("Large text").fetchSemanticsNode().size.height > px(height.value.toInt()))
    }

    @Test
    fun buttonStyleMergesAndPicksToneColors() {
        val style: ButtonStyle = skinA.button.defaults(themeT)
        assertEquals(56.dp, style.merge(ButtonTheme(height = 56.dp)).height)
        assertEquals(style.minWidth, style.merge(ButtonTheme(height = 56.dp)).minWidth)
        assertEquals(themeT.colors.error, style.colorsFor(Appearance.Outline, Tone.Error, themeT).border)
    }

    // CamoIconButton

    @Test
    fun anIconButtonIsOneLabelledButtonWithA48Target() = runComposeUiTest {
        camo { CamoIconButton({ CamoIcon(IconSource.Vector(dot), "inner label") }, "Back", {}) }
        waitForIdle()
        onAllNodesWithContentDescription("Back").assertCountEquals(1)
        onAllNodesWithContentDescription("inner label").assertCountEquals(0)
        val node = onNodeWithContentDescription("Back")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .fetchSemanticsNode()
        assertEquals(px(48), node.size.width)
    }

    @Test
    fun aCheckedIconButtonIsAToggle() = runComposeUiTest {
        camo { CamoIconButton({ CamoIcon(IconSource.Vector(dot), null) }, "Favorite", {}, checked = true) }
        onNodeWithContentDescription("Favorite")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
    }

    // CamoProgressCircle

    @Test
    fun progressIsClampedAndExposed() = runComposeUiTest {
        camo {
            Column {
                CamoProgressCircle(progress = 1.7f, label = "Upload")
                CamoProgressCircle(label = "Loading")
            }
        }
        onNodeWithContentDescription("Upload")
            .assert(SemanticsMatcher.expectValue(Range, ProgressBarRangeInfo(1f, 0f..1f)))
        onNodeWithContentDescription("Loading")
            .assert(SemanticsMatcher.expectValue(Range, ProgressBarRangeInfo.Indeterminate))
    }

    // CamoTooltip

    @Test
    fun aTooltipShowsAfterTheHoverDelay() = runComposeUiTest {
        mainClock.autoAdvance = false
        camo { CamoTooltip("Close the panel") { Box(Modifier.testTag("anchor").width(40.dp)) { CamoText("X") } } }
        mainClock.advanceTimeByFrame()
        onNodeWithTag("anchor").performMouseInput { enter(center) }
        mainClock.advanceTimeBy(100)
        onNodeWithText("Close the panel").assertDoesNotExist()
        mainClock.advanceTimeBy(600)
        onNodeWithText("Close the panel").assertExists()
    }

    @Test
    fun aTooltipEqualToTheLabelAddsNoAction() = runComposeUiTest {
        camo { CamoIconButton({ CamoIcon(IconSource.Vector(dot), null) }, "Close", {}, tooltip = "Close") }
        waitForIdle()
        val node = onNodeWithContentDescription("Close").fetchSemanticsNode()
        assertTrue(androidx.compose.ui.semantics.SemanticsActions.OnLongClick !in node.config)
        assertEquals(1, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }
}
