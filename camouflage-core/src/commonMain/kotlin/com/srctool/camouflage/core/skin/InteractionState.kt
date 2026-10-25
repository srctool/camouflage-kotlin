package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Immutable

/**
 * What the user is doing to a component right now. Components derive it from their `InteractionSource`;
 * renderers branch on it. `dragged` is for a slider handle or a switch thumb.
 */
@Immutable
public data class InteractionState(
    val pressed: Boolean = false,
    val focused: Boolean = false,
    val hovered: Boolean = false,
    val dragged: Boolean = false,
) {
    public companion object {
        /** Nothing is happening. */
        public val None: InteractionState = InteractionState()
    }
}
