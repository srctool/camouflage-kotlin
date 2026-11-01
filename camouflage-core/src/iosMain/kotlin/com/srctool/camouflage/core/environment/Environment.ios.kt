package com.srctool.camouflage.core.environment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIAccessibilityIsReduceTransparencyEnabled
import platform.UIKit.UIAccessibilityReduceTransparencyStatusDidChangeNotification

internal actual fun currentPlatform(): CamoPlatform = CamoPlatform.Ios

@Composable
internal actual fun rememberCapabilities(): CamoCapabilities {
    var reduce by remember { mutableStateOf(UIAccessibilityIsReduceTransparencyEnabled()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIAccessibilityReduceTransparencyStatusDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> reduce = UIAccessibilityIsReduceTransparencyEnabled() }
        onDispose { center.removeObserver(observer) }
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
