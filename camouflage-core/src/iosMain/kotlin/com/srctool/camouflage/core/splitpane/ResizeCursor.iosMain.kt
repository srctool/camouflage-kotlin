package com.srctool.camouflage.core.splitpane

import androidx.compose.ui.Modifier

// No resize pointer here (touch, or the browser's own cursor): the default one.
internal actual fun Modifier.resizeCursor(horizontal: Boolean): Modifier = this
