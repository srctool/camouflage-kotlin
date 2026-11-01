package com.srctool.camouflage.core.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.w3c.dom.events.Event

internal actual fun currentPlatform(): CamoPlatform = CamoPlatform.Web

@Composable
internal actual fun rememberCapabilities(): CamoCapabilities {
    val query = remember { window.matchMedia("(prefers-reduced-transparency: reduce)") }
    var reduce by remember { mutableStateOf(query.matches) }
    DisposableEffect(query) {
        val listener: (Event) -> Unit = { reduce = query.matches }
        query.addEventListener("change", listener)
        onDispose { query.removeEventListener("change", listener) }
    }
    return remember(reduce) {
        CamoCapabilities(
            backdropBlur = true,
            runtimeShaders = true,
            windowMaterial = false,
            reduceTransparency = reduce,
        )
    }
}
