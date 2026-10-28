package com.srctool.camouflage.core.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/** A sheet or dialog opened above another overlay: with its own scrim (darker), or sharing the one below. */
public enum class StackedScrim { Each, TopOnly }

/**
 * Lets a navigator play a frame's exit animation before it removes the frame: pass it to `CamoDialogFrame` or
 * `CamoSheetFrame`, and call [play] when the entry is popped (a Navigation 3 overlay scene's `onRemove`). A frame
 * that already animated out (a scrim tap, a drag) returns at once.
 */
@Stable
public class CamoOverlayExit {
    internal var animation: (suspend () -> Unit)? = null

    /** Plays the frame's exit animation and returns when it's done. */
    public suspend fun play() {
        animation?.invoke()
    }
}

// The dismissal vetoes registered by the content inside a frame, newest last.
@Stable
internal class DismissVetoes {
    private val vetoes = mutableListOf<() -> Boolean>()

    fun add(veto: () -> Boolean) {
        vetoes += veto
    }

    fun remove(veto: () -> Boolean) {
        vetoes -= veto
    }

    /** Whether every veto lets the overlay close. */
    fun allow(): Boolean = vetoes.all { it() }
}

internal val LocalDismissVetoes = compositionLocalOf<DismissVetoes?> { null }

// How many overlay frames are open around this point, for `StackedScrim.TopOnly`.
internal val LocalOverlayDepth = compositionLocalOf { 0 }

/**
 * Inside a dialog or sheet frame: decides whether a dismissal request (a scrim tap, back, Esc, a drag, the
 * close button, the screen-reader dismiss action) may close it. Return `false` to keep it open, for example
 * while saving or to ask "Discard changes?" first. Outside a frame it does nothing.
 */
@Composable
public fun OnDismissRequest(onRequest: () -> Boolean) {
    val vetoes = LocalDismissVetoes.current ?: return
    val current by rememberUpdatedState(onRequest)
    DisposableEffect(vetoes) {
        val veto = { current() }
        vetoes.add(veto)
        onDispose { vetoes.remove(veto) }
    }
}

// Places a popup over the whole window, for the frames' scrim.
internal object FullWindow : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}
