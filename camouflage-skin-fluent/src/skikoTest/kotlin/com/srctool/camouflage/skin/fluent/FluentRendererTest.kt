package com.srctool.camouflage.skin.fluent

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.ButtonTheme
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.divider.CamoDivider
import com.srctool.camouflage.core.divider.DividerTheme
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.iconbutton.IconButtonTheme
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.progress.ProgressTheme
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.skin.testing.ProvideComponentThemes
import com.srctool.camouflage.skin.testing.assertNoLiterals
import com.srctool.camouflage.skin.testing.assertRendererAppliesBehavior
import kotlin.math.roundToInt
import kotlin.test.Test

// Fluent's renderers against the contract at both desktop densities (base Fluent Skin §4, done-when for M3).
@OptIn(ExperimentalTestApi::class)
class FluentRendererTest {
    private val small = fluentTheme(density = FluentDensity.Small)
    private val medium = fluentTheme()
    private val dot = ImageVector.Builder("dot", 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black)) { moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); close() }
        .build()

    @Test
    fun buttonAppliesBehaviorAtBothDensities() {
        for (theme in listOf(small, medium)) runComposeUiTest {
            assertRendererAppliesBehavior(FluentSkin, theme) { onClick -> CamoButton("Save", onClick) }
        }
    }

    @Test
    fun iconButtonAppliesBehaviorAtBothDensities() {
        for (theme in listOf(small, medium)) runComposeUiTest {
            assertRendererAppliesBehavior(FluentSkin, theme) { onClick ->
                CamoIconButton({ CamoIcon(IconSource.Vector(dot), null) }, "Close", onClick)
            }
        }
    }

    @Test
    fun densityMinWidthsReachTheButton() {
        for ((theme, width) in listOf(small to 64, medium to 96)) runComposeUiTest {
            assertNoLiterals(FluentSkin, theme, Unit, render = { CamoButton("Go", {}) }) {
                it.size.width == px(width)
            }
        }
    }

    @Test
    fun buttonHonorsHeightAndMinWidth() {
        for (theme in listOf(small, medium)) runComposeUiTest {
            assertNoLiterals(FluentSkin, theme, ButtonTheme(height = 77.dp, minWidth = 222.dp), render = {
                ProvideComponentThemes(CamoComponentThemes(button = it)) { CamoButton("Go", {}) }
            }, measure = { it.size.height == px(77) && it.size.width == px(222) })
        }
    }

    @Test
    fun iconButtonHonorsContainerSize() {
        for (theme in listOf(small, medium)) runComposeUiTest {
            assertNoLiterals(FluentSkin, theme, IconButtonTheme(containerSize = 66.dp), render = {
                ProvideComponentThemes(CamoComponentThemes(iconButton = it)) {
                    CamoIconButton({ CamoIcon(IconSource.Vector(dot), null) }, "Close", {})
                }
            }, measure = { it.size.width == px(66) })
        }
    }

    @Test
    fun dividerHonorsThickness() = runComposeUiTest {
        assertNoLiterals(FluentSkin, medium, DividerTheme(thickness = 7.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(divider = it)) { CamoDivider(Modifier.width(100.dp)) }
        }, measure = { it.size.height == px(7) })
    }

    @Test
    fun progressCircleHonorsSize() = runComposeUiTest {
        assertNoLiterals(FluentSkin, medium, ProgressTheme(circleSizeLarge = 70.dp), render = {
            ProvideComponentThemes(CamoComponentThemes(progress = it)) {
                CamoProgressCircle(progress = 0.5f, size = ProgressSize.Large)
            }
        }, measure = { it.size.width == px(70) })
    }

    private fun ComposeUiTest.px(dp: Int) = (dp * density.density).roundToInt()
}
