package com.srctool.camouflage.core.bottomsheet

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.overlay.CamoOverlayExit
import com.srctool.camouflage.core.overlay.DismissVetoes
import com.srctool.camouflage.core.overlay.FullWindow
import com.srctool.camouflage.core.overlay.LocalDismissVetoes
import com.srctool.camouflage.core.overlay.LocalOverlayDepth
import com.srctool.camouflage.core.overlay.StackedScrim
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.theme.CamoTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** How a sheet shows on a wide window: still a bottom sheet (capped at `maxWidth`), or a centered card. */
public enum class WidePresentation { Sheet, Centered }

/** Overrides for bottom sheets. `null` keeps the skin's default. */
@Immutable
public data class BottomSheetTheme(
    val shape: Shape? = null,
    val maxWidth: Dp? = null,
    val contentPadding: PaddingValues? = null,
    val showHandle: Boolean? = null,
    val handleSize: DpSize? = null,
    val handleColor: Color? = null,
    val titleStyle: TextStyle? = null,
    val titleAlign: TitleAlign? = null,
    val showCloseButton: Boolean? = null,
    val closeIcon: IconSource? = null,
    val containerColor: Color? = null,
    val scrimAlpha: Float? = null,
    val widePresentation: WidePresentation? = null,
    val stackedScrim: StackedScrim? = null,
)

/** The resolved bottom sheet style: every field set. */
@Immutable
public data class BottomSheetStyle(
    val shape: Shape,
    val maxWidth: Dp,
    val contentPadding: PaddingValues,
    val showHandle: Boolean,
    val handleSize: DpSize,
    val handleColor: Color,
    val titleStyle: TextStyle,
    val titleAlign: TitleAlign,
    val showCloseButton: Boolean,
    val closeIcon: IconSource,
    val containerColor: Color,
    val scrimAlpha: Float,
    val widePresentation: WidePresentation,
    val stackedScrim: StackedScrim,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: BottomSheetTheme?): BottomSheetStyle = if (overrides == null) {
        this
    } else {
        BottomSheetStyle(
            shape = overrides.shape ?: shape,
            maxWidth = overrides.maxWidth ?: maxWidth,
            contentPadding = overrides.contentPadding ?: contentPadding,
            showHandle = overrides.showHandle ?: showHandle,
            handleSize = overrides.handleSize ?: handleSize,
            handleColor = overrides.handleColor ?: handleColor,
            titleStyle = overrides.titleStyle ?: titleStyle,
            titleAlign = overrides.titleAlign ?: titleAlign,
            showCloseButton = overrides.showCloseButton ?: showCloseButton,
            closeIcon = overrides.closeIcon ?: closeIcon,
            containerColor = overrides.containerColor ?: containerColor,
            scrimAlpha = overrides.scrimAlpha ?: scrimAlpha,
            widePresentation = overrides.widePresentation ?: widePresentation,
            stackedScrim = overrides.stackedScrim ?: stackedScrim,
        )
    }
}

/**
 * What a bottom sheet renderer draws: the container, the header (handle, [title], a close button when
 * `showCloseButton` and [dismissible]) and the content. [dragProgress] runs from 0 (resting) to 1 (dragged
 * fully down). A close button calls [onDismissRequest], the same path as back and Esc.
 */
@Immutable
public data class BottomSheetState(
    val title: String?,
    val dragProgress: Float,
    val dismissible: Boolean,
    val onDismissRequest: () -> Unit,
    val centered: Boolean,
)

/** The bottom sheet's content. */
@Stable
public class BottomSheetSlots(public val content: @Composable () -> Unit)

/** Draws bottom sheets. Core places, drags and dismisses them; apply `modifier` (the behavior) outermost. */
@Stable
public interface BottomSheetRenderer {
    /** The complete bottom sheet style for [theme]. */
    public fun defaults(theme: CamoTheme): BottomSheetStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(
        state: BottomSheetState,
        slots: BottomSheetSlots,
        theme: CamoTheme,
        style: BottomSheetStyle,
        modifier: Modifier,
    )
}

// What the frame tells the sheet inside it.
@Immutable
private data class SheetFrameInfo(
    val dragProgress: Float,
    val dismissible: Boolean,
    val onDismissRequest: () -> Unit,
    val centered: Boolean,
)

private val LocalSheetFrame = compositionLocalOf<SheetFrameInfo?> { null }

// Outside a frame (a gallery, a preview): resting, not dismissible.
private val NoFrame = SheetFrameInfo(0f, dismissible = false, onDismissRequest = {}, centered = false)

private const val DISMISS_FRACTION = 0.4f
private const val DISMISS_VELOCITY = 1200f
private const val OFFSCREEN = 100_000f

/**
 * The modal frame of a bottom sheet (base Overlays §3.1): the scrim, the sheet anchored to the bottom (centered
 * and capped at `maxWidth`), drag to dismiss, the keyboard and system-bar insets, and focus trapped in it. A
 * scrim tap, back, Esc, a drag down and the screen-reader dismiss action are all [onDismissRequest]; with
 * [dismissible] `false` they do nothing, and `OnDismissRequest` inside can veto them. A navigator passes [exit]
 * to play the slide-out before it removes the frame. It's visible while it's in the composition, so close it by
 * removing it:
 *
 * ```
 * if (picking) CamoSheetFrame(onDismissRequest = { picking = false }) { CamoBottomSheet(title = "Country") { … } }
 * ```
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // the scrim, drag and focus share the sheet's offset
@Composable
public fun CamoSheetFrame(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    dragToDismiss: Boolean = true,
    exit: CamoOverlayExit? = null,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoSheetFrame")
    val theme = requireTheme("CamoSheetFrame")
    val style = remember(skin, theme) { skin.bottomSheet.defaults(theme).merge(theme.components.bottomSheet) }
    val dismiss by rememberUpdatedState(onDismissRequest)
    val depth = LocalOverlayDepth.current
    val scope = rememberCoroutineScope()
    var offset by remember { mutableFloatStateOf(OFFSCREEN) }
    var height by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }
    val slide = tween<Float>(theme.motion.durationMedium, easing = theme.motion.easingEmphasized)
    val vetoes = remember { DismissVetoes() }
    suspend fun slideOut() {
        if (height > 0 && offset < height) {
            animate(offset, height.toFloat(), animationSpec = tween(theme.motion.durationShort)) { v, _ -> offset = v }
        }
    }
    if (exit != null) exit.animation = ::slideOut
    fun requestDismiss() {
        if (!dismissible) return
        scope.launch {
            if (!vetoes.allow()) {
                animate(offset, 0f, animationSpec = slide) { v, _ -> offset = v }
                return@launch
            }
            slideOut()
            dismiss()
            // Still here: the app kept it, so the sheet comes back.
            animate(offset, 0f, animationSpec = slide) { v, _ -> offset = v }
        }
    }
    LaunchedEffect(height > 0) {
        if (height > 0) {
            offset = height.toFloat()
            animate(offset, 0f, animationSpec = slide) { v, _ -> offset = v }
        }
    }
    val progress = if (height > 0) (offset / height).coerceIn(0f, 1f) else 1f
    val scrimAlpha = if (style.stackedScrim == StackedScrim.TopOnly && depth > 0) 0f else style.scrimAlpha
    Popup(
        popupPositionProvider = FullWindow,
        onDismissRequest = ::requestDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(theme.colors.scrim.copy(alpha = scrimAlpha * (1f - progress)))
                .pointerInputTap { requestDismiss() }
                .onPreviewKeyEvent {
                    val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                    if (esc) requestDismiss()
                    esc
                },
        ) {
            val centered = style.widePresentation == WidePresentation.Centered &&
                maxWidth > style.maxWidth + theme.spacing.xxl * 2
            val drag = rememberDraggableState { delta -> offset = (offset + delta).coerceAtLeast(0f) }
            Box(
                Modifier
                    .align(if (centered) Alignment.Center else Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .imePadding()
                    .widthIn(max = style.maxWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * MAX_HEIGHT_FRACTION)
                    .onSizeChanged { height = it.height }
                    .offset { IntOffset(0, if (centered) 0 else offset.roundToInt()) }
                    .draggable(
                        drag,
                        Orientation.Vertical,
                        enabled = dragToDismiss && dismissible && !centered,
                        onDragStopped = { velocity ->
                            if (offset > height * DISMISS_FRACTION || velocity > DISMISS_VELOCITY) {
                                requestDismiss()
                            } else {
                                animate(offset, 0f, animationSpec = slide) { v, _ -> offset = v }
                            }
                        },
                    )
                    .pointerInputTap {}
                    .focusRequester(focus)
                    .focusable()
                    .then(modifier),
            ) {
                CompositionLocalProvider(
                    LocalSheetFrame provides SheetFrameInfo(progress, dismissible, ::requestDismiss, centered),
                    LocalOverlayDepth provides depth + 1,
                    LocalDismissVetoes provides vetoes,
                    content = content,
                )
            }
            // Here, not above: BoxWithConstraints composes this content while measuring.
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
    }
}

private const val MAX_HEIGHT_FRACTION = 0.9f

// Taps here go to [onTap] and stop; a tap a child takes (a button) never reaches it.
private fun Modifier.pointerInputTap(onTap: () -> Unit): Modifier = pointerInput(Unit) { detectTapGestures { onTap() } }

/**
 * A bottom sheet's content: the skin's container, handle, optional [title] and close button around [content].
 * Put it in a `CamoSheetFrame` (or, from M6, a sheet route); it scrolls nothing itself, so give long content
 * its own scroll. Screen readers hear it as a pane named [title], with a dismiss action when it's dismissible.
 */
@Composable
public fun CamoBottomSheet(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoBottomSheet")
    val theme = requireTheme("CamoBottomSheet")
    val style = remember(skin, theme) { skin.bottomSheet.defaults(theme).merge(theme.components.bottomSheet) }
    val frame = LocalSheetFrame.current ?: NoFrame
    skin.bottomSheet.Render(
        BottomSheetState(title, frame.dragProgress, frame.dismissible, frame.onDismissRequest, frame.centered),
        BottomSheetSlots {
            Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) { content() }
        },
        theme,
        style,
        modifier.semantics {
            if (title != null) paneTitle = title
            if (frame.dismissible) {
                dismiss {
                    frame.onDismissRequest()
                    true
                }
            }
        },
    )
}
