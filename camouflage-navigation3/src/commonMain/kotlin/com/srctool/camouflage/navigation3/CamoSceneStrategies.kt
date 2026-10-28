package com.srctool.camouflage.navigation3

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.srctool.camouflage.core.bottomsheet.CamoSheetFrame
import com.srctool.camouflage.core.dialog.CamoDialogFrame
import com.srctool.camouflage.core.overlay.CamoOverlayExit

internal data class DialogOptions(val dismissible: Boolean)

internal data class SheetOptions(val dismissible: Boolean, val dragToDismiss: Boolean)

/**
 * Shows an entry registered with [dialog] metadata as a Camouflage dialog over the entries below it: they stay
 * drawn behind the scrim, and a scrim tap, back or Esc removes the entry (through `NavDisplay`'s `onBack`). The
 * dialog fades out before it leaves, also when the app pops it. Combine it with the other strategies:
 * `CamoDialogSceneStrategy<NavKey>() then CamoBottomSheetSceneStrategy<NavKey>()`.
 */
public class CamoDialogSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull()
        val options = last?.metadata?.get(DialogKey) ?: return null
        return OverlayFrameScene(last.contentKey, last, entries.dropLast(1), onBack) { exit, back, content ->
            CamoDialogFrame(onDismissRequest = back, dismissible = options.dismissible, exit = exit, content = content)
        }
    }

    public companion object {
        internal object DialogKey : NavMetadataKey<DialogOptions>

        /** The metadata of an entry shown as a dialog. With [dismissible] `false`, only the app closes it. */
        public fun dialog(dismissible: Boolean = true): Map<String, Any> =
            metadata { put(DialogKey, DialogOptions(dismissible)) }
    }
}

/**
 * Shows an entry registered with [bottomSheet] metadata as a Camouflage bottom sheet over the entries below
 * it: they stay drawn behind the scrim, and a scrim tap, back, Esc or a drag down removes the entry (through
 * `NavDisplay`'s `onBack`). The sheet slides out before it leaves, also when the app pops it.
 */
public class CamoBottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull()
        val options = last?.metadata?.get(SheetKey) ?: return null
        return OverlayFrameScene(last.contentKey, last, entries.dropLast(1), onBack) { exit, back, content ->
            CamoSheetFrame(
                onDismissRequest = back,
                dismissible = options.dismissible,
                dragToDismiss = options.dragToDismiss,
                exit = exit,
                content = content,
            )
        }
    }

    public companion object {
        internal object SheetKey : NavMetadataKey<SheetOptions>

        /**
         * The metadata of an entry shown as a bottom sheet. With [dismissible] `false`, only the app closes it;
         * with [dragToDismiss] `false`, dragging doesn't.
         */
        public fun bottomSheet(dismissible: Boolean = true, dragToDismiss: Boolean = true): Map<String, Any> =
            metadata { put(SheetKey, SheetOptions(dismissible, dragToDismiss)) }
    }
}

// One overlay entry in a Camouflage frame; the entries below keep drawing (an overlay scene).
private class OverlayFrameScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onBack: () -> Unit,
    private val frame: @Composable (CamoOverlayExit, () -> Unit, @Composable () -> Unit) -> Unit,
) : OverlayScene<T> {
    private val exit = CamoOverlayExit()

    override val entries: List<NavEntry<T>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<T>> get() = previousEntries

    override val content: @Composable () -> Unit = { frame(exit, onBack) { entry.Content() } }

    // Popped by the app (or after a dismissal that already played it): play the exit before leaving.
    override suspend fun onRemove() {
        exit.play()
    }

    override fun equals(other: Any?): Boolean = other is OverlayFrameScene<*> &&
        key == other.key && entry == other.entry && previousEntries == other.previousEntries

    override fun hashCode(): Int = (key.hashCode() * HASH + entry.hashCode()) * HASH + previousEntries.hashCode()

    private companion object {
        const val HASH = 31
    }
}
