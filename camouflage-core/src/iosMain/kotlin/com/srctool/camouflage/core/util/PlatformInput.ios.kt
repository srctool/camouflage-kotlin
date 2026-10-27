package com.srctool.camouflage.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberSystem24Hour(): Boolean {
    val locale = currentLocaleTag()
    return remember(locale) { CamoLocaleData.uses24Hour(locale) }
}

internal actual val prefersTyping: Boolean = false
