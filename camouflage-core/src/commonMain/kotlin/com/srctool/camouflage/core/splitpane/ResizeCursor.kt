package com.srctool.camouflage.core.splitpane

import androidx.compose.ui.Modifier

/** The resize pointer over a divider: `col-resize` for a `horizontal` split's vertical line, `row-resize` otherwise. */
internal expect fun Modifier.resizeCursor(horizontal: Boolean): Modifier
