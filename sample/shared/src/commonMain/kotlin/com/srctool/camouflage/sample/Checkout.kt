package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.divider.CamoDivider
import com.srctool.camouflage.core.divider.DividerInset
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.SurfaceLevel
import kotlinx.coroutines.delay

private const val PAYING_MILLIS = 2000L
private val BrandSteps = listOf(100, 300, 500, 700, 900)

/**
 * The M4 demo screen: a checkout card built only from Camouflage components. Nothing here names a brand or a
 * skin; switching either at the provider restyles it (Provider and Scoping §3.3).
 */
@Composable
fun Checkout() {
    var favorite by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }
    LaunchedEffect(paying) {
        if (paying) {
            delay(PAYING_MILLIS)
            paying = false
        }
    }
    val type = Camouflage.typography
    val spacing = Camouflage.spacing
    CamoSurface(
        Modifier.fillMaxWidth(),
        level = SurfaceLevel.Lowest,
        shape = ShapeRole.Large,
        elevation = ElevationLevel.Level2,
        border = true,
    ) {
        Column(Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CamoIconButton({ AppIcon.Back() }, "Back", {}, tooltip = "Back to the cart")
                CamoText("Checkout", Modifier.weight(1f).padding(horizontal = spacing.s), style = type.titleLarge)
                CamoIconButton(
                    { if (favorite) AppIcon.Star() else AppIcon.StarOutline() },
                    "Favorite",
                    { favorite = !favorite },
                    checked = favorite,
                    tooltip = "Save this order",
                )
            }
            CamoDivider()
            Line("Desk lamp", "€ 49.00")
            Line("Linen shade", "€ 23.00")
            CamoDivider(inset = DividerInset.Start)
            Row {
                CamoText("Total", Modifier.weight(1f), style = type.titleMedium)
                CamoText("€ 72.00", style = type.titleMedium)
            }
            BrandStrip()
            CamoText(
                listOf(CamoSpan("By paying you agree to the "), CamoSpan("Terms", link = {}), CamoSpan(".")),
                style = type.bodySmall,
                color = Camouflage.colors.onSurfaceVariant,
            )
            CamoButton(
                "Pay € 72.00",
                { paying = true },
                Modifier.fillMaxWidth(),
                loading = paying,
                prefixIcon = { AppIcon.Star() },
            )
            CamoButton("Save for later", {}, Modifier.fillMaxWidth(), appearance = Appearance.Ghost)
        }
    }
}

@Composable
private fun Line(label: String, price: String) {
    Row {
        CamoText(label, Modifier.weight(1f))
        CamoText(price, color = Camouflage.colors.onSurfaceVariant)
    }
}

// The brand's own tokens, read like any theme value; nothing shows under a theme without them.
@Composable
private fun BrandStrip() {
    val tokens = Camouflage.themeOrNull?.tokens<BrandTokens>() ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.xs)) {
        BrandSteps.forEach {
            Box(Modifier.size(24.dp).background(tokens.brand[it], RoundedCornerShape(tokens.controlRadius)))
        }
    }
}
