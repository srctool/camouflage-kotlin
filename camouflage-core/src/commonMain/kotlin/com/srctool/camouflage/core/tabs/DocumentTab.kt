package com.srctool.camouflage.core.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.style.TextOverflow
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.menu.CamoContextMenuArea
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.tooltip.CamoTooltip

// The tab being dragged and how far it has moved from where its place in the row would draw it.
@Stable
internal class DocumentDrag {
    var id: Any? by mutableStateOf(null)
    var dx: Float by mutableFloatStateOf(0f)

    fun begin(id: Any) {
        this.id = id
        dx = 0f
    }

    fun end() {
        id = null
        dx = 0f
    }
}

internal class DocumentTabSpec(val doc: CamoDocument, val position: ItemPosition, val selected: Boolean)

internal class DocumentTabActions(
    val onSelect: (Any) -> Unit,
    val onClose: (Any) -> Unit,
    val onContextMenu: ((CamoDocument) -> List<CamoMenuEntry>)?,
)

private val TitleMaxWidth = 180.dp
private val TrailingSize = 18.dp
private val PinStroke = 2.dp
private val DotSize = 8.dp
private val CloseIconSize = 12.dp

@Composable
internal fun CamoTabsScope.DocumentTab(
    spec: DocumentTabSpec,
    actions: DocumentTabActions,
    state: CamoDocumentTabsState,
    drag: DocumentDrag,
) {
    val doc = spec.doc
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pinned = state.isPinned(doc.id)
    val dragging = drag.id == doc.id
    val tab = CamoTab(value = doc.id, label = doc.title, contentDescription = doc.title)
    val labels = remember(doc.modified, pinned) {
        listOfNotNull("modified".takeIf { doc.modified }, "pinned".takeIf { pinned }).joinToString(", ")
    }
    // A moved tab is detached and re-attached by the row, which drops its focus: give it back once it has landed.
    var refocus by remember { mutableStateOf(false) }
    val index = spec.position.index
    LaunchedEffect(refocus, index) {
        if (refocus) {
            withFrameNanos { }
            focus.focusItem(index)
            refocus = false
        }
    }
    val behavior = Modifier
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer { translationX = if (dragging) drag.dx else 0f }
        // Before the tab list's own arrow keys, which would take Alt+arrow as a plain arrow.
        .onPreviewKeyEvent { event ->
            val left = event.key == Key.DirectionLeft
            val moves = event.type == KeyEventType.KeyDown && event.isAltPressed &&
                (left || event.key == Key.DirectionRight)
            if (moves) {
                refocus = true
                state.moveBy(doc.id, if (left != rtl) -1 else 1)
            }
            moves
        }
        .middleClickCloses(doc) { actions.onClose(doc.id) }
        .dragsToReorder(doc.id, rtl, tabs, state, drag)
        .semantics {
            if (labels.isNotEmpty()) stateDescription = labels
            customActions = documentTabActions(doc, pinned, state, actions.onClose)
        }
    val item: @Composable () -> Unit = {
        CamoTabItem(
            tab = tab,
            selected = spec.selected,
            onClick = { actions.onSelect(doc.id) },
            position = spec.position,
            modifier = behavior,
            content = { DocumentTabContent(spec, pinned, actions.onClose) },
        )
    }
    val menu = actions.onContextMenu
    if (menu == null) item() else CamoContextMenuArea(menu(doc)) { item() }
}

private fun documentTabActions(
    doc: CamoDocument,
    pinned: Boolean,
    state: CamoDocumentTabsState,
    onClose: (Any) -> Unit,
): List<CustomAccessibilityAction> {
    val at = state.order.indexOf(doc.id)
    val group = if (pinned) state.pinned.size else state.order.size
    val first = if (pinned) 0 else state.pinned.size
    return buildList {
        if (doc.closable) add(CustomAccessibilityAction("Close") { true.also { onClose(doc.id) } })
        if (at > first) add(CustomAccessibilityAction("Move earlier") { true.also { state.moveBy(doc.id, -1) } })
        if (at in first until group - 1) {
            add(CustomAccessibilityAction("Move later") { true.also { state.moveBy(doc.id, 1) } })
        }
        add(
            if (pinned) {
                CustomAccessibilityAction("Unpin") { true.also { state.unpin(doc.id) } }
            } else {
                CustomAccessibilityAction("Pin") { true.also { state.pin(doc.id) } }
            },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.middleClickCloses(doc: CamoDocument, close: () -> Unit): Modifier =
    pointerInput(doc.id, doc.closable) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (doc.closable && event.type == PointerEventType.Press && event.buttons.isTertiaryPressed) {
                    event.changes.forEach { it.consume() }
                    close()
                }
            }
        }
    }

// Dragging the tab moves it with the pointer; when it passes its neighbor's middle they swap places.
private fun Modifier.dragsToReorder(
    id: Any,
    rtl: Boolean,
    registry: TabsRegistry,
    state: CamoDocumentTabsState,
    drag: DocumentDrag,
): Modifier = pointerInput(id, rtl) {
    detectDragGestures(
        onDragStart = { drag.begin(id) },
        onDragEnd = { drag.end() },
        onDragCancel = { drag.end() },
    ) { change, amount ->
        change.consume()
        drag.dx += amount.x
        swapWithNeighbor(id, rtl, registry, state, drag)
    }
}

private fun swapWithNeighbor(
    id: Any,
    rtl: Boolean,
    registry: TabsRegistry,
    state: CamoDocumentTabsState,
    drag: DocumentDrag,
) {
    val at = state.order.indexOf(id)
    // Physically right is the next index in left-to-right and the previous one in right-to-left.
    val rightward = if (rtl) -1 else 1
    val movingRight = drag.dx > 0
    val step = if (movingRight) rightward else -rightward
    val own = registry.bounds[at]
    val neighbor = registry.bounds[at + step]
    if (own != null && neighbor != null) {
        val center = own.start + own.width / 2 + drag.dx
        val middle = neighbor.start + neighbor.width / 2
        if (if (movingRight) center > middle else center < middle) {
            state.move(id, at + step)
            // The tab's place moved by the neighbor's width; so does the offset that draws it under the pointer.
            if (state.order.indexOf(id) != at) drag.dx -= (if (movingRight) 1 else -1) * neighbor.width
        }
    }
}

@Composable
private fun DocumentTabContent(spec: DocumentTabSpec, pinned: Boolean, onClose: (Any) -> Unit) {
    val doc = spec.doc
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val showClose = doc.closable && !pinned && (hovered || (spec.selected && !doc.modified))
    CamoTooltip(doc.title) {
        Row(
            Modifier.hoverable(hover),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            doc.icon?.invoke()
            CamoText(doc.title, Modifier.widthIn(max = TitleMaxWidth), maxLines = 1, overflow = TextOverflow.Ellipsis)
            when {
                showClose -> CloseButton { onClose(doc.id) }
                doc.modified -> Dot()
                pinned -> PinGlyph()
                else -> Spacer(Modifier.size(TrailingSize))
            }
        }
    }
}

// The small close button: reached through the tab's "Close" action, never a Tab stop of its own.
@Composable
private fun CloseButton(onClick: () -> Unit) {
    val skin = requireSkin("CamoDocumentTabs")
    val theme = requireTheme("CamoDocumentTabs")
    val icon = remember(skin, theme) { skin.chip.defaults(theme).merge(theme.components.chip).removeIcon }
    Box(
        Modifier
            .size(TrailingSize)
            .clip(CircleShape)
            .focusProperties { canFocus = false }
            .clickable(onClick = onClick)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) { CamoIcon(icon, null, size = CloseIconSize) }
}

// The modified dot, in the tab's own text color.
@Composable
private fun Dot() {
    val color = Camouflage.iconStyle.color
    Box(Modifier.size(TrailingSize).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(Modifier.size(DotSize).clip(CircleShape).background(color))
    }
}

// A hollow dot marks a pinned tab.
@Composable
private fun PinGlyph() {
    val color = Camouflage.iconStyle.color
    Box(Modifier.size(TrailingSize).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(Modifier.size(DotSize).clip(CircleShape).border(PinStroke, color, CircleShape))
    }
}
