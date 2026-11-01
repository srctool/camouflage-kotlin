package com.srctool.camouflage.core.environment

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal actual fun currentPlatform(): CamoPlatform = CamoPlatform.Android

private const val BLUR_API = 31 // RenderEffect blur, Android 12
private const val SHADER_API = 33 // RuntimeShader, Android 13

// Android has no Reduce Transparency setting.
@Composable
internal actual fun rememberCapabilities(): CamoCapabilities = remember {
    CamoCapabilities(
        backdropBlur = Build.VERSION.SDK_INT >= BLUR_API,
        runtimeShaders = Build.VERSION.SDK_INT >= SHADER_API,
        windowMaterial = false,
        reduceTransparency = false,
    )
}
