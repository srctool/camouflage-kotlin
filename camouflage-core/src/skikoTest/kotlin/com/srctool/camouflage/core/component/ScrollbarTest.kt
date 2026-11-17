package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.PlainScrollbar
import com.srctool.camouflage.core.environment.CamoCapabilities
import com.srctool.camouflage.core.environment.CamoEnvironment
import com.srctool.camouflage.core.environment.CamoPlatform
import com.srctool.camouflage.core.environment.InputMode
import com.srctool.camouflage.core.environment.WindowSizeClass
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.scrollbar.CamoScrollArea
import com.srctool.camouflage.core.scrollbar.rememberCamoScrollAdapter
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// CamoScrollbar and CamoScrollArea (base Scrollbar, M16): thumb geometry, drag, track click, touch, no size change.
@OptIn(ExperimentalTestApi::class)
class ScrollbarTest {
    private val caps = CamoCapabilities(
        backdropBlur = false,
        runtimeShaders = false,
        windowMaterial = false,
        reduceTransparency = false,
    )
    private val desktop = CamoEnvironment(WindowSizeClass.Expanded, InputMode.Pointer, CamoPlatform.Windows, caps)
    private val phone = CamoEnvironment(WindowSizeClass.Compact, InputMode.Touch, CamoPlatform.Android, caps)

    private fun ComposeUiTest.camo(environment: CamoEnvironment = desktop, content: @Composable () -> Unit) {
        PlainScrollbar.last = null
        setContent { CamouflageProvider(skinA, themeT, environment, content) }
    }

    // 100 dp of viewport over 200 dp of content.
    @Composable
    private fun Scrolling(state: androidx.compose.foundation.ScrollState, content: Int = 200) {
        Box(Modifier.size(200.dp, 100.dp)) {
            CamoScrollArea(vertical = rememberCamoScrollAdapter(state)) {
                Column(Modifier.verticalScroll(state)) { Box(Modifier.fillMaxWidth().height(content.dp)) }
            }
        }
    }

    @Test
    fun theThumbIsProportionalAndFollowsTheScroll() = runComposeUiTest {
        val state = androidx.compose.foundation.ScrollState(0)
        camo { Scrolling(state) }
        waitForIdle()
        val first = assertNotNull(PlainScrollbar.last)
        assertEquals(50f * density.density, first.thumbLength, 1f)
        assertEquals(0f, first.thumbStart, 1f)
        state.dispatchRawDelta(50f * density.density)
        waitForIdle()
        assertEquals(25f * density.density, PlainScrollbar.last!!.thumbStart, 2f)
    }

    @Test
    fun nothingIsDrawnWhenTheContentFits() = runComposeUiTest {
        val state = androidx.compose.foundation.ScrollState(0)
        camo { Scrolling(state, content = 80) }
        waitForIdle()
        assertNull(PlainScrollbar.last)
    }

    @Test
    fun touchInputDrawsNothing() = runComposeUiTest {
        val state = androidx.compose.foundation.ScrollState(0)
        camo(phone) { Scrolling(state) }
        waitForIdle()
        assertNull(PlainScrollbar.last)
    }

    @Test
    fun draggingTheThumbScrollsTheContent() = runComposeUiTest {
        val state = androidx.compose.foundation.ScrollState(0)
        camo { Scrolling(state) }
        waitForIdle()
        val scale = density.density
        val x = 200f * scale - 2f * scale // inside the lane at the end edge
        onRoot().performMouseInput {
            moveTo(Offset(x, 10f * scale))
            press()
            moveBy(Offset(0f, 20f * scale))
            release()
        }
        waitForIdle()
        // 20 dp of thumb over 50 dp of room is 40% of the 100 dp that can scroll.
        assertTrue(state.value > 0, "the content scrolled")
        assertEquals(40f * scale, state.value.toFloat(), 8f * scale)
    }

    @Test
    fun aClickOnTheTrackPagesAViewport() = runComposeUiTest {
        val state = androidx.compose.foundation.ScrollState(0)
        camo { Scrolling(state) }
        waitForIdle()
        val scale = density.density
        onRoot().performMouseInput {
            moveTo(Offset(200f * scale - 2f * scale, 90f * scale))
            press()
            release()
        }
        waitForIdle()
        assertEquals(100f * scale, state.value.toFloat(), 2f)
    }

    @Test
    fun theBarDoesNotChangeItsContainersSize() = runComposeUiTest {
        val state = androidx.compose.foundation.ScrollState(0)
        camo { Box(Modifier.size(200.dp, 100.dp)) { Scrolling(state) } }
        waitForIdle()
        val size = onRoot().fetchSemanticsNode().size
        assertEquals((200f * density.density).toInt(), size.width)
    }
}
