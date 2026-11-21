package com.srctool.camouflage.core.dock

import androidx.compose.runtime.Composable

/**
 * A tool window: [id] identifies it, [title] names it on its stripe button and header, [icon] is the stripe button's
 * glyph, [defaultEdge] is where it starts (the state may have moved it) and [content] is what it shows.
 */
public data class CamoToolWindow(
    val id: Any,
    val title: String,
    val icon: @Composable () -> Unit,
    val defaultEdge: DockEdge,
    val content: @Composable () -> Unit,
)
