package com.srctool.camouflage.core.snackbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.banner.ofTone
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberSkinIndication
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.math.min
import kotlin.coroutines.resume

/**
 * Shows snackbars from anywhere below the root `CamouflageProvider`, which installs one messenger for the app:
 * one snackbar at a time, the rest queued in order. Get it with `Camouflage.messenger`.
 */
@Stable
public interface CamoMessenger {
    /**
     * Shows [message] (with an optional [action], "Undo") after the snackbars queued before it, and returns how it
     * left. Cancelling the caller (the page leaves) removes it. [withDismiss] adds a ✕; `null` follows
     * `SnackbarTheme.showDismiss`. With a screen reader on, the platform's accessibility timeout lengthens (or
     * removes) the time, so users can reach the action.
     */
    public suspend fun show(
        message: String,
        action: String? = null,
        tone: Tone = Tone.Default,
        duration: SnackbarDuration = SnackbarDuration.Auto,
        withDismiss: Boolean? = null,
    ): SnackbarResult

    /** Dismisses the snackbar on screen, if any (an outdated message); the next one in the queue shows. */
    public fun dismissCurrent()
}

// One shown snackbar and the caller waiting for its result.
internal class SnackbarData(
    val message: String,
    val action: String?,
    val tone: Tone,
    val duration: SnackbarDuration,
    val withDismiss: Boolean?,
    private val result: CancellableContinuation<SnackbarResult>,
) {
    fun performAction() {
        if (result.isActive) result.resume(SnackbarResult.ActionPerformed)
    }

    fun dismiss() {
        if (result.isActive) result.resume(SnackbarResult.Dismissed)
    }
}

/** Space a screen keeps clear of snackbars: a bottom bar or a FAB ([bottom]), a top bar ([top]). */
internal data class MessengerInsets(val bottom: Dp, val top: Dp)

@Stable
internal class MessengerState : CamoMessenger {
    private val queue = Mutex()
    var current by mutableStateOf<SnackbarData?>(null)
        private set
    val insets = mutableStateListOf<MessengerInsets>()

    // The host's exit animation; the next snackbar waits for it.
    var exitMillis = 0

    override suspend fun show(
        message: String,
        action: String?,
        tone: Tone,
        duration: SnackbarDuration,
        withDismiss: Boolean?,
    ): SnackbarResult = queue.withLock {
        val result = try {
            suspendCancellableCoroutine { continuation ->
                current = SnackbarData(message, action, tone, duration, withDismiss, continuation)
            }
        } finally {
            current = null
        }
        delay(exitMillis.toLong())
        result
    }

    override fun dismissCurrent() {
        current?.dismiss()
    }
}

internal val LocalCamoMessenger = staticCompositionLocalOf<MessengerState?> { null }

/** The app's messenger, installed by the root `CamouflageProvider`. */
public val Camouflage.messenger: CamoMessenger
    @Composable get() = checkNotNull(LocalCamoMessenger.current) {
        "Camouflage.messenger needs a CamouflageProvider above it."
    }

/**
 * Keeps snackbars clear of this screen's chrome while [content] is shown: [bottom] above a navigation bar or a
 * FAB (for `Bottom` snackbars), [top] below a top bar (for `Top` ones). The innermost (latest) one wins.
 */
@Composable
public fun CamoMessengerInsets(bottom: Dp = 0.dp, top: Dp = 0.dp, content: @Composable () -> Unit) {
    val messenger = LocalCamoMessenger.current
    if (messenger != null) {
        DisposableEffect(messenger, bottom, top) {
            val insets = MessengerInsets(bottom, top)
            messenger.insets += insets
            onDispose { messenger.insets -= insets }
        }
    }
    content()
}

// Installs the messenger at the root provider and shows its snackbars, in a popup created when one shows, so it
// sits above the dialogs and sheets open at the time.
@Composable
internal fun ProvideMessenger(content: @Composable () -> Unit) {
    if (LocalCamoMessenger.current != null) {
        content()
        return
    }
    val messenger = remember { MessengerState() }
    CompositionLocalProvider(LocalCamoMessenger provides messenger) {
        content()
        MessengerHost(messenger)
    }
}

@Composable
private fun MessengerHost(messenger: MessengerState) {
    val data = messenger.current
    val visible = remember { MutableTransitionState(false) }
    // The last snackbar shown, kept while it animates out.
    val shown = remember { arrayOfNulls<SnackbarData>(1) }
    if (data != null) shown[0] = data
    visible.targetState = data != null
    val last = shown[0] ?: return
    if (!visible.currentState && !visible.targetState && visible.isIdle) return
    val skin = requireSkin("CamoMessenger")
    val theme = requireTheme("CamoMessenger")
    val style = remember(skin, theme) { skin.snackbar.defaults(theme).merge(theme.components.snackbar) }
    messenger.exitMillis = theme.motion.durationMedium
    val top = style.position == SnackbarPosition.Top
    val insets = messenger.insets.lastOrNull() ?: MessengerInsets(0.dp, 0.dp)
    val edge = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + if (top) WindowInsetsSides.Top else WindowInsetsSides.Bottom,
    )
    // Read outside the popup, which has its own platform locals.
    val accessibility = LocalAccessibilityManager.current
    Popup(popupPositionProvider = WindowEdge(top, style.alignment), properties = PopupProperties(focusable = false)) {
        Box(
            Modifier
                .windowInsetsPadding(edge)
                .padding(style.margin)
                .padding(top = if (top) insets.top else 0.dp, bottom = if (top) 0.dp else insets.bottom),
        ) {
            val tween = tween<IntOffset>(theme.motion.durationMedium)
            val fade = tween<Float>(theme.motion.durationMedium)
            AnimatedVisibility(
                visible,
                enter = slideInVertically(tween) { if (top) -it else it } + fadeIn(fade),
                exit = slideOutVertically(tween) { if (top) -it else it } + fadeOut(fade),
            ) {
                Snackbar(last, style, top, accessibility, Modifier.widthIn(max = style.maxWidth).fillMaxWidth())
            }
        }
    }
}

// Places the snackbar popup at the window's bottom or top edge, aligned along it.
private class WindowEdge(private val top: Boolean, private val alignment: SnackbarAlignment) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val free = windowSize.width - popupContentSize.width
        val start = if (layoutDirection == LayoutDirection.Ltr) 0 else free
        val x = when (alignment) {
            SnackbarAlignment.Start -> start
            SnackbarAlignment.Center -> free / 2
            SnackbarAlignment.End -> free - start
        }
        return IntOffset(x, if (top) 0 else windowSize.height - popupContentSize.height)
    }
}

private const val SWIPE_FRACTION = 0.35f
private const val PAUSE_STEP_MILLIS = 100L

// One snackbar: its timer (paused while hovered, focused or dragged), swipe to dismiss, and announcement.
@Composable
private fun Snackbar(
    data: SnackbarData,
    style: SnackbarStyle,
    top: Boolean,
    accessibility: AccessibilityManager?,
    modifier: Modifier,
) {
    val skin = requireSkin("CamoSnackbar")
    val theme = requireTheme("CamoSnackbar")
    val source = remember(data) { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    var focused by remember(data) { mutableStateOf(false) }
    var dragging by remember(data) { mutableStateOf(false) }
    val withDismiss = data.withDismiss ?: style.showDismiss
    val toneIcon = if (style.showToneIcon) {
        skin.banner.defaults(theme).merge(theme.components.banner).toneIcons.ofTone(data.tone)
    } else {
        null
    }
    DismissTimer(data, durationMillis(data, style), accessibility, toneIcon != null || withDismiss, withDismiss) {
        interaction.hovered || focused || dragging
    }
    val behavior = modifier
        .swipeToDismiss(data, top) { dragging = it }
        .hoverable(source)
        .onFocusChanged { focused = it.hasFocus }
        .semantics(mergeDescendants = false) {
            liveRegion = if (data.tone == Tone.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
            dismiss {
                data.dismiss()
                true
            }
        }
    skin.snackbar.Render(
        SnackbarState(data.message, data.action, data.tone, withDismiss, toneIcon != null, style.position, interaction),
        snackbarSlots(data, style, toneIcon, withDismiss),
        theme,
        style,
        behavior,
    )
}

// How long [data] stays before the accessibility adjustment, or null for indefinite.
private fun durationMillis(data: SnackbarData, style: SnackbarStyle): Int? = when (data.duration) {
    SnackbarDuration.Short -> style.durationShortMillis
    SnackbarDuration.Long -> style.durationLongMillis
    SnackbarDuration.Auto -> if (data.action != null) style.durationLongMillis else style.durationShortMillis
    SnackbarDuration.Indefinite -> null
}

// Dismisses [data] after [millis] (lengthened, or removed, by the platform's accessibility timeout), counting
// only while [paused] is false.
@Composable
private fun DismissTimer(
    data: SnackbarData,
    millis: Int?,
    accessibility: AccessibilityManager?,
    containsIcons: Boolean,
    withDismiss: Boolean,
    paused: () -> Boolean,
) {
    LaunchedEffect(data) {
        if (millis == null) return@LaunchedEffect
        var remaining = accessibility?.calculateRecommendedTimeoutMillis(
            millis.toLong(),
            containsIcons = containsIcons,
            containsText = true,
            containsControls = data.action != null || withDismiss,
        ) ?: millis.toLong()
        if (remaining == Long.MAX_VALUE) return@LaunchedEffect
        while (remaining > 0) {
            snapshotFlow { paused() }.first { !it }
            val step = min(remaining, PAUSE_STEP_MILLIS)
            delay(step)
            remaining -= step
        }
        data.dismiss()
    }
}

// Drags the snackbar sideways (and up at the top edge); far enough dismisses it, otherwise it springs back.
@Composable
private fun Modifier.swipeToDismiss(data: SnackbarData, top: Boolean, onDragging: (Boolean) -> Unit): Modifier {
    val drag = remember(data) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    return this
        .graphicsLayer {
            translationX = drag.value.x
            translationY = drag.value.y
            alpha = 1f - min(1f, abs(drag.value.x) / size.width.coerceAtLeast(1f))
        }
        .pointerInput(data) {
            detectDragGestures(
                onDragStart = { onDragging(true) },
                onDragEnd = {
                    onDragging(false)
                    val away = abs(drag.value.x) > size.width * SWIPE_FRACTION ||
                        (top && -drag.value.y > size.height * SWIPE_FRACTION)
                    if (away) data.dismiss() else scope.launch { drag.animateTo(Offset.Zero) }
                },
                onDragCancel = {
                    onDragging(false)
                    scope.launch { drag.animateTo(Offset.Zero) }
                },
            ) { change, amount ->
                change.consume()
                val y = if (top) min(0f, drag.value.y + amount.y) else 0f
                scope.launch { drag.snapTo(Offset(drag.value.x + amount.x, y)) }
            }
        }
}

// The parts core builds: the tone icon, the action button and the ✕.
private fun snackbarSlots(
    data: SnackbarData,
    style: SnackbarStyle,
    toneIcon: IconSource?,
    withDismiss: Boolean,
) = SnackbarSlots(
    icon = toneIcon?.let { { CamoIcon(it, contentDescription = null) } },
    action = data.action?.let { text -> { SnackbarAction(text, style.actionStyle, data::performAction) } },
    dismiss = if (withDismiss) {
        { CamoFieldAction({ CamoIcon(style.dismissIcon, null) }, style.strings.dismiss, data::dismiss) }
    } else {
        null
    },
)

// The snackbar's action: text in the content color the renderer provides (its action color), its own button.
@Composable
private fun SnackbarAction(text: String, style: TextStyle, onClick: () -> Unit) {
    val color = Camouflage.contentColor
    val source = remember { MutableInteractionSource() }
    val shape = Camouflage.theme.shapes.small
    val spacing = Camouflage.spacing
    Box(
        Modifier
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .indication(source, rememberSkinIndication(color, shape))
            .padding(horizontal = spacing.s, vertical = spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = style.copy(color = color), maxLines = 1)
    }
}
