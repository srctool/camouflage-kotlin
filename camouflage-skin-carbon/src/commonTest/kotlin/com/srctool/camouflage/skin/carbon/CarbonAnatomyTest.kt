package com.srctool.camouflage.skin.carbon

import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.dialog.DialogActionsLayout
import com.srctool.camouflage.core.theme.Appearance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// What makes Carbon Carbon, read from the renderers' defaults (base Carbon Skin §3.2).
class CarbonAnatomyTest {
    private val families = CarbonThemeFamily.entries.map { carbonTheme(it) }

    @Test
    fun buttonsAre48TallAndSquare() = families.forEach {
        val style = CarbonSkin.button.defaults(it)
        assertEquals(48.dp, style.height)
        assertEquals(it.shapes.none, style.shape)
        assertEquals(Appearance.Solid, style.defaultAppearance)
    }

    @Test
    fun theDialogActionsFillTheRowAndTheDialogIsSquare() = families.forEach {
        val style = CarbonSkin.dialog.defaults(it)
        assertEquals(DialogActionsLayout.FullWidthRow, style.actionsLayout)
        assertEquals(it.shapes.none, style.shape)
        assertTrue(style.showCloseButton)
    }

    @Test
    fun theHeaderIsGray100InEveryFamily() = families.forEach {
        val colors = CarbonSkin.topBar.defaults(it).colors
        assertEquals(Gray100, colors.container)
        assertEquals(Gray10, colors.title)
    }

    @Test
    fun textFieldsAre40TallOnTheFieldFill() = families.forEach {
        val style = CarbonSkin.textField.defaults(it)
        assertEquals(40.dp, style.height)
        assertEquals(it.colors.surfaceContainerLow, style.colors.container)
    }

    @Test
    fun tagsArePillsAndAvatarsAreSquare() = families.forEach {
        assertEquals(it.shapes.full, CarbonSkin.chip.defaults(it).shape)
        assertEquals(com.srctool.camouflage.core.avatar.AvatarShape.Square, CarbonSkin.avatar.defaults(it).shape)
    }

    @Test
    fun theToggleIsGreenWhenOn() = families.forEach {
        val style = CarbonSkin.switch.defaults(it)
        assertEquals(48.dp, style.trackWidth)
        assertEquals(it.colors.success, style.colors.trackOn)
    }
}
