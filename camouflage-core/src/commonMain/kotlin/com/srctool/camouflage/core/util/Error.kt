package com.srctool.camouflage.core.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * Marks the node invalid with [message], announced politely when it appears. `null` leaves the node as it is.
 * Inputs put it on their behavior modifier; the visible error text then hides its own semantics.
 */
public fun Modifier.camoError(message: String?): Modifier = if (message == null) {
    this
} else {
    semantics {
        error(message)
        liveRegion = LiveRegionMode.Polite
    }
}
