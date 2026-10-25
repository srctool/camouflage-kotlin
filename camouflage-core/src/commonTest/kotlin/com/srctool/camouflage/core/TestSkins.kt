package com.srctool.camouflage.core

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.skin.InteractionFeedback
import com.srctool.camouflage.core.skin.SurfaceTreatment
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTokenSet
import com.srctool.camouflage.core.theme.CamoTypography

// Skins and themes shared by the core tests.

internal object Fill : SurfaceTreatment {
    override fun modifier(color: Color, shape: Shape, elevation: Dp, theme: CamoTheme) =
        Modifier.background(color, shape)
}

internal object NoFeedback : InteractionFeedback, IndicationNodeFactory {
    override fun indication(contentColor: Color, theme: CamoTheme): Indication = this
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 0
}

internal data class Brand(val accent: Color) : CamoTokenSet<Brand> {
    override fun lerp(to: Brand, fraction: Float): Brand = this
}

internal val skinA = CamoSkin("a", Fill, NoFeedback)
internal val skinB = CamoSkin("b", Fill, NoFeedback)
internal val themeT = CamoTheme(
    colors = CamoColors.fromSeed(primary = Color(0xFF0B57D0)),
    typography = CamoTypography.system(),
)
internal val themeU = themeT.copy(colors = CamoColors.fromSeed(primary = Color(0xFFB3261E)))
