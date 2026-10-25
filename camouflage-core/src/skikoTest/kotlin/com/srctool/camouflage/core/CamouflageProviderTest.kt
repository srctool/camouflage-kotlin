package com.srctool.camouflage.core

import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skin.IconStyle
import com.srctool.camouflage.core.skin.ProvideContentStyle
import com.srctool.camouflage.core.skin.rememberSkinIndication
import com.srctool.camouflage.core.theme.CamoTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

// Assertions stay inside runComposeUiTest: on wasmJs the test body runs asynchronously.
@OptIn(ExperimentalTestApi::class)
class CamouflageProviderTest {
    private class Seen {
        var skin: CamoSkin? = null
        var theme: CamoTheme? = null
    }

    // Base Provider and Scoping §3.1: what the outer and inner providers set, read inside the inner one.
    private fun ComposeUiTest.inside(outer: Pair<CamoSkin?, CamoTheme?>?, inner: Pair<CamoSkin?, CamoTheme?>): Seen {
        val seen = Seen()
        val read: @Composable () -> Unit = {
            seen.skin = Camouflage.skinOrNull
            seen.theme = Camouflage.themeOrNull
        }
        setContent {
            if (outer == null) {
                CamouflageProvider(inner.first, inner.second, read)
            } else {
                CamouflageProvider(outer.first, outer.second) { CamouflageProvider(inner.first, inner.second, read) }
            }
        }
        waitForIdle()
        return seen
    }

    private fun ComposeUiTest.assertMissing(component: String, missing: String, content: @Composable () -> Unit) {
        val error = assertFailsWith<CamouflageMissingProviderException> {
            setContent(content)
            waitForIdle()
        }
        assertEquals(CamouflageMissingProviderException(component, missing).message, error.message)
    }

    @Test
    fun innerProvidingNothingInheritsBoth() = runComposeUiTest {
        val seen = inside(skinA to themeT, null to null)
        assertSame(skinA, seen.skin)
        assertSame(themeT, seen.theme)
    }

    @Test
    fun innerThemeOverridesOnlyTheTheme() = runComposeUiTest {
        val seen = inside(skinA to themeT, null to themeU)
        assertSame(skinA, seen.skin)
        assertSame(themeU, seen.theme)
    }

    @Test
    fun innerSkinOverridesOnlyTheSkin() = runComposeUiTest {
        val seen = inside(skinA to themeT, skinB to null)
        assertSame(skinB, seen.skin)
        assertSame(themeT, seen.theme)
    }

    @Test
    fun innerSkinAndThemeOverrideBoth() = runComposeUiTest {
        val seen = inside(skinA to themeT, skinB to themeU)
        assertSame(skinB, seen.skin)
        assertSame(themeU, seen.theme)
    }

    @Test
    fun aLoneSkinHasNoTheme() = runComposeUiTest {
        val seen = inside(null, skinB to null)
        assertSame(skinB, seen.skin)
        assertNull(seen.theme)
    }

    @Test
    fun aLoneThemeHasNoSkin() = runComposeUiTest {
        val seen = inside(null, null to themeU)
        assertNull(seen.skin)
        assertSame(themeU, seen.theme)
    }

    @Test
    fun aLoneProviderWithBothWorks() = runComposeUiTest {
        val seen = inside(null, skinB to themeU)
        assertSame(skinB, seen.skin)
        assertSame(themeU, seen.theme)
    }

    @Test
    fun noProviderFailsFastNamingTheComponent() = runComposeUiTest {
        assertMissing("CamoButton", "skin") { requireSkin("CamoButton") }
    }

    @Test
    fun aThemeOnlyProviderFailsFastOnTheSkin() = runComposeUiTest {
        assertMissing("CamoButton", "skin") { CamouflageProvider(theme = themeU) { requireSkin("CamoButton") } }
    }

    @Test
    fun accessorsFailFastToo() = runComposeUiTest {
        assertMissing("Camouflage.colors", "theme") { CamouflageProvider(skin = skinA) { Camouflage.colors } }
    }

    @Test
    fun aMissingTokenSetIsNamed() = runComposeUiTest {
        assertMissing("Camouflage.tokens", "token set Brand in the theme") {
            CamouflageProvider(skinA, themeT) { Camouflage.tokens<Brand>() }
        }
    }

    @Test
    fun aSkinSwitchRecomposesAChild() = runComposeUiTest {
        var skin by mutableStateOf(skinA)
        val names = mutableListOf<String>()
        setContent { CamouflageProvider(skin, themeT) { names += Camouflage.skin.name } }
        waitForIdle()
        skin = skinB
        waitForIdle()
        assertEquals(listOf("a", "b"), names)
    }

    @Test
    fun reducedMotionZeroesTheDurations() = runComposeUiTest {
        var motion = themeT.motion
        setContent { ProvideCamouflage(skinA, themeT, reduceMotion = true) { motion = Camouflage.motion } }
        waitForIdle()
        assertEquals(listOf(0, 0, 0), listOf(motion.durationShort, motion.durationMedium, motion.durationLong))
    }

    @Test
    fun aProviderResetsTheContentStyle() = runComposeUiTest {
        var color = Color.Unspecified
        var icon: IconStyle? = null
        setContent {
            CamouflageProvider(skinA, themeT) {
                ProvideContentStyle(Color.Red, IconStyle(12.dp, Color.Red)) {
                    CamouflageProvider(theme = themeU) {
                        color = Camouflage.contentColor
                        icon = Camouflage.iconStyle
                    }
                }
            }
        }
        waitForIdle()
        assertEquals(themeU.colors.onSurface, color)
        assertEquals(IconStyle(20.dp, themeU.colors.onSurface), icon)
    }

    @Test
    fun contentStyleReachesSlots() = runComposeUiTest {
        var color = Color.Unspecified
        var icon: IconStyle? = null
        setContent {
            CamouflageProvider(skinA, themeT) {
                ProvideContentStyle(Color.Red, IconStyle(18.dp, Color.Blue)) {
                    color = Camouflage.contentColor
                    icon = Camouflage.iconStyle
                }
            }
        }
        waitForIdle()
        assertEquals(Color.Red, color)
        assertEquals(IconStyle(18.dp, Color.Blue), icon)
    }

    @Test
    fun theSkinIndicationComesFromTheCurrentSkin() = runComposeUiTest {
        var indication: Indication? = null
        setContent { CamouflageProvider(skinA, themeT) { indication = rememberSkinIndication(Color.Black) } }
        waitForIdle()
        assertSame(NoFeedback, indication)
    }

    // Skin Contract §2: the behavior modifier goes on the renderer's outermost node, before its padding.
    @Composable
    private fun TestRenderer(modifier: Modifier, behaviorFirst: Boolean) {
        val visual = Modifier.size(80.dp).background(Color.Gray)
        if (behaviorFirst) {
            Box(modifier.then(visual).padding(24.dp)) { Box(Modifier.size(32.dp)) }
        } else {
            Box(visual.padding(24.dp)) { Box(modifier.size(32.dp)) }
        }
    }

    private fun ComposeUiTest.cornerClicks(behaviorFirst: Boolean): Int {
        var clicks = 0
        setContent {
            Box(Modifier.testTag("box")) { TestRenderer(Modifier.clickable { clicks++ }, behaviorFirst) }
        }
        onNodeWithTag("box").performTouchInput { click(Offset(2f, 2f)) }
        waitForIdle()
        return clicks
    }

    @Test
    fun aRendererApplyingTheModifierFirstIsClickableAtItsCorner() = runComposeUiTest {
        assertEquals(1, cornerClicks(behaviorFirst = true))
    }

    @Test
    fun aRendererApplyingTheModifierInsideMissesTheCorner() = runComposeUiTest {
        assertEquals(0, cornerClicks(behaviorFirst = false))
    }
}
