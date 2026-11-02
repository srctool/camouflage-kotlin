package com.srctool.camouflage.core.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.provider.requireTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val DISMISS_FRACTION = 0.4f
private const val DISMISS_VELOCITY = 1200f
private const val MAX_WIDTH_FRACTION = 0.8f

/**
 * The modal frame shared by the modal drawer and the modal side sheet (ADR-30): a scrim, a panel of [width] (at
 * most 80% of the window) sliding in from [edge], drag toward the edge to dismiss, back and Esc, and focus trapped
 * inside. Its visibility is state ([open]); the popup stays until the slide-out has finished. Every dismissal is
 * [onDismissRequest], and the app decides whether to close it by setting [open] to `false`.
 */
@Suppress("LongMethod") // the scrim, slide, drag and focus share one offset
@Composable
internal fun SideFrame(
    edge: SideEdge,
    open: Boolean,
    onDismissRequest: () -> Unit,
    width: Dp,
    scrimAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val theme = requireTheme("SideFrame")
    val dismiss by rememberUpdatedState(onDismissRequest)
    val scope = rememberCoroutineScope()
    // 0 = fully in, 1 = fully out; the popup stays until it has slid out.
    val progress = remember { Animatable(1f) }
    var present by remember { mutableStateOf(open) }
    val focus = remember { FocusRequester() }
    val slide = tween<Float>(theme.motion.durationMedium, easing = theme.motion.easingEmphasized)
    LaunchedEffect(open) {
        if (open) {
            present = true
            progress.animateTo(0f, slide)
        } else {
            progress.animateTo(1f, slide)
            present = false
        }
    }
    if (!present) return
    // Which way is "out": toward the edge the panel is attached to (left is -1, right is +1).
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outward = if ((edge == SideEdge.Start) != rtl) -1f else 1f
    Popup(
        popupPositionProvider = FullWindow,
        onDismissRequest = { dismiss() },
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(theme.colors.scrim.copy(alpha = scrimAlpha * (1f - progress.value)))
                .pointerInput(Unit) { detectTapGestures { dismiss() } }
                .onPreviewKeyEvent {
                    val esc = it.type == KeyEventType.KeyDown && it.key == Key.Escape
                    if (esc) dismiss()
                    esc
                },
        ) {
            val panel = minOf(width, maxWidth * MAX_WIDTH_FRACTION)
            val panelPx = with(LocalDensity.current) { panel.toPx() }
            val drag = rememberDraggableState { delta ->
                scope.launch { progress.snapTo((progress.value + delta * outward / panelPx).coerceIn(0f, 1f)) }
            }
            Box(
                Modifier
                    .align(if (edge == SideEdge.Start) Alignment.CenterStart else Alignment.CenterEnd)
                    .width(panel)
                    .fillMaxHeight()
                    .offset { IntOffset((progress.value * panelPx * outward).roundToInt(), 0) }
                    .draggable(
                        drag,
                        Orientation.Horizontal,
                        onDragStopped = { velocity ->
                            if (progress.value > DISMISS_FRACTION || velocity * outward > DISMISS_VELOCITY) {
                                dismiss()
                            } else {
                                progress.animateTo(0f, slide)
                            }
                        },
                    )
                    .pointerInput(Unit) { detectTapGestures {} }
                    .focusRequester(focus)
                    .focusable()
                    .then(modifier),
            ) { content() }
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
    }
}
