package com.srctool.camouflage.core.util

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import com.srctool.camouflage.core.skin.InteractionState

/** The [InteractionState] of this source: pressed, focused, hovered and dragged, as state. */
@Composable
public fun InteractionSource.collectInteractionState(): InteractionState = InteractionState(
    pressed = collectIsPressedAsState().value,
    focused = collectIsFocusedAsState().value,
    hovered = collectIsHoveredAsState().value,
    dragged = collectIsDraggedAsState().value,
)
