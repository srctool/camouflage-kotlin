package com.srctool.camouflage.skin.carbon

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.CamoTokenSet

/**
 * Carbon's control sizes (base Carbon Skin §3.5): [Xs] 24, [Sm] 32, [Md] 40 and [Lg] 48 dp. Desktop apps live at
 * [Sm] and [Md]; [Lg] is Carbon's large size and the touch default. `Xs` is a Camouflage extension (Carbon's own
 * buttons start at `Sm`). The sizes are minimums: larger text still grows a control, and a touch target stays 48.
 */
public enum class CarbonDensity(public val size: Dp) { Xs(24.dp), Sm(32.dp), Md(40.dp), Lg(48.dp) }

/**
 * The theme's [density], read by the renderers (`theme.tokens<CarbonDensityTokens>()`). One value for every
 * control, so a size is decided in one place; a component theme override still wins for one component.
 */
@Immutable
public data class CarbonDensityTokens(val density: CarbonDensity) : CamoTokenSet<CarbonDensityTokens> {
    override fun lerp(to: CarbonDensityTokens, fraction: Float): CarbonDensityTokens =
        if (fraction < HALF) this else to
}

private const val HALF = 0.5f
private const val XS_TEXT = 12

// The theme's density, `Lg` (today's sizes) without one.
internal val CamoTheme.density: CarbonDensity
    get() = tokens<CarbonDensityTokens>()?.density ?: CarbonDensity.Lg

// Carbon's body-compact-01 is 14 sp; Xs draws 12.
internal fun TextStyle.forDensity(density: CarbonDensity): TextStyle =
    if (density == CarbonDensity.Xs) copy(fontSize = XS_TEXT.sp) else this
