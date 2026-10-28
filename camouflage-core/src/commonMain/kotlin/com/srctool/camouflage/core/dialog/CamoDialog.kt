package com.srctool.camouflage.core.dialog

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.srctool.camouflage.core.theme.ElevationLevel
import kotlinx.coroutines.launch

/** How a dialog arranges its actions. `Stacked` puts `confirm` on top, full width. */
public enum class DialogActionsLayout { End, Center, FullWidthRow, Stacked }

/** Every color a dialog uses. */
@Immutable
public data class DialogColors(val container: Color, val title: Color, val message: Color)

/** Overrides for dialogs. `null` keeps the skin's default. */
@Immutable
public data class DialogTheme(
    val shape: Shape? = null,
    val minWidth: Dp? = null,
    val maxWidth: Dp? = null,
    val padding: PaddingValues? = null,
    val titleStyle: TextStyle? = null,
    val messageStyle: TextStyle? = null,
    val titleAlign: TitleAlign? = null,
    val actionsLayout: DialogActionsLayout? = null,
    /** Between the actions. */
    val actionGap: Dp? = null,
    /** Between the title, the message, the content and the actions. */
    val sectionGap: Dp? = null,
    val showCloseButton: Boolean? = null,
    val closeIcon: IconSource? = null,
    val elevation: ElevationLevel? = null,
    val scrimAlpha: Float? = null,
    val stackedScrim: StackedScrim? = null,
    val colors: DialogColors? = null,
)

/** The resolved dialog style: every field set. */
@Immutable
public data class DialogStyle(
    val shape: Shape,
    val minWidth: Dp,
    val maxWidth: Dp,
    val padding: PaddingValues,
    val titleStyle: TextStyle,
    val messageStyle: TextStyle,
    val titleAlign: TitleAlign,
    val actionsLayout: DialogActionsLayout,
    val actionGap: Dp,
    val sectionGap: Dp,
    val showCloseButton: Boolean,
    val closeIcon: IconSource,
    val elevation: ElevationLevel,
    val scrimAlpha: Float,
    val stackedScrim: StackedScrim,
    val colors: DialogColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: DialogTheme?): DialogStyle = if (overrides == null) {
        this
    } else {
        DialogStyle(
            shape = overrides.shape ?: shape,
            minWidth = overrides.minWidth ?: minWidth,
            maxWidth = overrides.maxWidth ?: maxWidth,
            padding = overrides.padding ?: padding,
            titleStyle = overrides.titleStyle ?: titleStyle,
            messageStyle = overrides.messageStyle ?: messageStyle,
            titleAlign = overrides.titleAlign ?: titleAlign,
            actionsLayout = overrides.actionsLayout ?: actionsLayout,
            actionGap = overrides.actionGap ?: actionGap,
            sectionGap = overrides.sectionGap ?: sectionGap,
            showCloseButton = overrides.showCloseButton ?: showCloseButton,
            closeIcon = overrides.closeIcon ?: closeIcon,
            elevation = overrides.elevation ?: elevation,
            scrimAlpha = overrides.scrimAlpha ?: scrimAlpha,
            stackedScrim = overrides.stackedScrim ?: stackedScrim,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a dialog renderer draws: the container, the [title] (aligned by `titleAlign`), the [message], the
 * content and the actions arranged by `actionsLayout`. With `showCloseButton` and [dismissible], a close button
 * at the top end calls [onDismissRequest], the same path as Esc and back. Keep the title and actions fixed and
 * let the message and content scroll when they don't fit.
 */
@Immutable
public data class DialogState(
    val title: String?,
    val message: String,
    val dismissible: Boolean,
    val onDismissRequest: () -> Unit,
)

/** The caller's actions and optional content (a radio list, a small form). */
@Stable
public class DialogSlots(
    public val confirm: @Composable () -> Unit,
    public val dismiss: (@Composable () -> Unit)?,
    public val content: (@Composable ColumnScope.() -> Unit)?,
)

/** Draws dialogs. Core frames, centers and dismisses them; apply `modifier` (the semantics) outermost. */
@Stable
public interface DialogRenderer {
    /** The complete dialog style for [theme]. */
    public fun defaults(theme: CamoTheme): DialogStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(state: DialogState, slots: DialogSlots, theme: CamoTheme, style: DialogStyle, modifier: Modifier)
}

// What the frame tells the dialog inside it.
@Immutable
private data class DialogFrameInfo(val dismissible: Boolean, val onDismissRequest: () -> Unit)

private val LocalDialogFrame = compositionLocalOf<DialogFrameInfo?> { null }

// Outside a frame (a gallery, a preview): not dismissible.
private val NoFrame = DialogFrameInfo(dismissible = false, onDismissRequest = {})

private const val ENTER_SCALE = 0.9f
private const val MAX_HEIGHT_FRACTION = 0.9f

/**
 * The modal frame of a dialog (base Overlays §3.1): the scrim, the dialog centered at most `maxWidth` wide (and
 * 48 dp narrower than the window) and 90% of the window tall, focus trapped in it, and a fade and scale in. A
 * scrim tap, back and Esc are [onDismissRequest]; with [dismissible] `false` they do nothing, and
 * `OnDismissRequest` inside can veto them. A navigator passes [exit] to play the fade-out before it removes the
 * frame. It's visible while it's in the composition:
 *
 * ```
 * if (deleting) CamoDialogFrame(onDismissRequest = { deleting = false }) { CamoDialog(…) }
 * ```
 */
@Suppress("LongMethod") // the scrim, the enter and exit animation and focus share the frame's state
@Composable
public fun CamoDialogFrame(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissible: Boolean = true,
    exit: CamoOverlayExit? = null,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoDialogFrame")
    val theme = requireTheme("CamoDialogFrame")
    val style = remember(skin, theme) { skin.dialog.defaults(theme).merge(theme.components.dialog) }
    val dismiss by rememberUpdatedState(onDismissRequest)
    val depth = LocalOverlayDepth.current
    val scope = rememberCoroutineScope()
    val vetoes = remember { DismissVetoes() }
    val focus = remember { FocusRequester() }
    var shown by remember { mutableFloatStateOf(0f) }
    suspend fun fadeOut() {
        if (shown > 0f) animate(shown, 0f, animationSpec = tween(theme.motion.durationShort)) { v, _ -> shown = v }
    }
    if (exit != null) exit.animation = ::fadeOut
    fun requestDismiss() {
        if (!dismissible || !vetoes.allow()) return
        scope.launch {
            fadeOut()
            dismiss()
            // Still here: the app kept it, so the dialog comes back.
            animate(shown, 1f, animationSpec = tween(theme.motion.durationShort)) { v, _ -> shown = v }
        }
    }
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(theme.motion.durationMedium, easing = theme.motion.easingEmphasized)) {
                v, _ ->
            shown = v
        }
    }
    val scrimAlpha = if (style.stackedScrim == StackedScrim.TopOnly && depth > 0) 0f else style.scrimAlpha
    Popup(
        popupPositionProvider = FullWindow,
        onDismissRequest = ::requestDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(theme.colors.scrim.copy(alpha = scrimAlpha * shown))
                .pointerInput(Unit) { detectTapGestures { requestDismiss() } }
                .onPreviewKeyEvent {
                    val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                    if (esc) requestDismiss()
                    esc
                }
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            // A window narrower than the margins gets a zero-width dialog, never a negative one.
            val width = (maxWidth - theme.spacing.xl * 2).coerceIn(0.dp, style.maxWidth)
            Box(
                Modifier
                    .widthIn(min = style.minWidth.coerceAtMost(width), max = width)
                    .heightIn(max = maxHeight * MAX_HEIGHT_FRACTION)
                    .graphicsLayer {
                        alpha = shown
                        val scale = ENTER_SCALE + (1f - ENTER_SCALE) * shown
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) { detectTapGestures {} }
                    .focusRequester(focus)
                    .focusable()
                    .then(modifier),
            ) {
                CompositionLocalProvider(
                    LocalDialogFrame provides DialogFrameInfo(dismissible, ::requestDismiss),
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

/**
 * A dialog's content: an optional [title] (its accessible name), the [message], optional [content] (a radio
 * list, a small form) and the actions. [confirm] and [dismiss] are usually `CamoButton`s that close it, with a
 * result or without one. Put it in a `CamoDialogFrame`, or register it as a dialog route.
 */
@Composable
public fun CamoDialog(
    message: String,
    confirm: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismiss: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val skin = requireSkin("CamoDialog")
    val theme = requireTheme("CamoDialog")
    val style = remember(skin, theme) { skin.dialog.defaults(theme).merge(theme.components.dialog) }
    val frame = LocalDialogFrame.current ?: NoFrame
    skin.dialog.Render(
        DialogState(title, message, frame.dismissible, frame.onDismissRequest),
        DialogSlots(confirm, dismiss, content),
        theme,
        style,
        modifier.semantics {
            paneTitle = title ?: message
            if (frame.dismissible) {
                dismiss {
                    frame.onDismissRequest()
                    true
                }
            }
        },
    )
}
