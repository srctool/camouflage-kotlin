package com.srctool.camouflage.core.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal actual fun currentPlatform(): CamoPlatform {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        "win" in os -> CamoPlatform.Windows
        "mac" in os -> CamoPlatform.MacOs
        else -> CamoPlatform.Linux
    }
}

// Skia blurs and runs shaders everywhere; Mica is a Windows 11 window backdrop. Desktop OSes don't expose a
// transparency setting to the JVM.
@Composable
internal actual fun rememberCapabilities(): CamoCapabilities = remember {
    CamoCapabilities(
        backdropBlur = true,
        runtimeShaders = true,
        windowMaterial = "windows 11" in System.getProperty("os.name").orEmpty().lowercase(),
        reduceTransparency = false,
    )
}
