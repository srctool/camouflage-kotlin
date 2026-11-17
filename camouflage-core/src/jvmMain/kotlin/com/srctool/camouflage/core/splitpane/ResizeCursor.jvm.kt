package com.srctool.camouflage.core.splitpane

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

internal actual fun Modifier.resizeCursor(horizontal: Boolean): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(if (horizontal) Cursor.E_RESIZE_CURSOR else Cursor.N_RESIZE_CURSOR)))
