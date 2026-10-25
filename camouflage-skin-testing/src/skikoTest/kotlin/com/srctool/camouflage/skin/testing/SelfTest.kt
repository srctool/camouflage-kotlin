package com.srctool.camouflage.skin.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.ButtonRenderer
import com.srctool.camouflage.core.button.ButtonSlots
import com.srctool.camouflage.core.button.ButtonState
import com.srctool.camouflage.core.button.ButtonStyle
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.skin.minimal.MinimalSkin
import com.srctool.camouflage.skin.minimal.minimalLight
import kotlin.test.Test
import kotlin.test.assertFails

// The checks must catch the bugs they exist for (Testing §4, done when).
@OptIn(ExperimentalTestApi::class)
class SelfTest {
    // Applies the behavior modifier inside its padding, so the box's edge isn't clickable.
    private object BehaviorInside : ButtonRenderer by MinimalSkin.button {
        @Composable
        override fun Render(
            state: ButtonState,
            slots: ButtonSlots,
            theme: CamoTheme,
            style: ButtonStyle,
            modifier: Modifier,
        ) {
            Box(Modifier.background(style.solidColors.container).padding(24.dp)) {
                BasicText(state.text, modifier)
            }
        }
    }

    // Hard-codes its height instead of reading style.height.
    private object FixedHeight : ButtonRenderer by MinimalSkin.button {
        @Composable
        override fun Render(
            state: ButtonState,
            slots: ButtonSlots,
            theme: CamoTheme,
            style: ButtonStyle,
            modifier: Modifier,
        ) {
            Box(modifier.size(width = style.minWidth, height = 40.dp)) { BasicText(state.text) }
        }
    }

    @Test
    fun aRendererWithBehaviorInsideFails() = runComposeUiTest {
        assertFails {
            assertRendererAppliesBehavior(MinimalSkin.copy(button = BehaviorInside), minimalLight) { onClick ->
                CamoButton("Save", onClick)
            }
        }
    }

    @Test
    fun aHardCodedHeightFails() = runComposeUiTest {
        assertFails {
            val broken = MinimalSkin.copy(button = FixedHeight)
            assertNoLiterals(broken, minimalLight, ButtonTheme(height = 77.dp), render = {
                ProvideComponentThemes(CamoComponentThemes(button = it)) { CamoButton("Go", {}) }
            }, measure = { it.size.height > 100 })
        }
    }
}
