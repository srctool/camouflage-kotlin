package com.srctool.camouflage.core.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.srctool.camouflage.core.scrollbar.rememberCamoScrollAdapter
import com.srctool.camouflage.core.scrollbar.EdgeScrollbar
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.round
import com.srctool.camouflage.core.divider.CamoDivider
import com.srctool.camouflage.core.overlay.CamoAnchoredPopup
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.speedsearch.CamoSpeedSearchState
import com.srctool.camouflage.core.speedsearch.CamoSpeedSearchOverlay
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.collectInteractionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/** One entry in a menu: a [CamoMenuItem], a [CamoMenuDivider], a [CamoMenuGroup] or a [CamoMenuSubmenu]. */
public sealed interface CamoMenuEntry

/**
 * A menu action. Selecting it closes the menu, then runs [onClick] in the next frame (so a dialog it opens
 * isn't stacked over a closing menu). [trailingText] is for a shortcut ("⌘C"); [tone] `Error` marks a
 * destructive item. A non-null [checked] makes it checkable: the app flips it in [onClick].
 */
@Immutable
public data class CamoMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val trailingText: String? = null,
    val tone: Tone = Tone.Default,
    val enabled: Boolean = true,
    val checked: Boolean? = null,
) : CamoMenuEntry

/** A line between entries. */
public data object CamoMenuDivider : CamoMenuEntry

/** Items that belong together; [exclusive] announces the checked one as a radio item ("Sort by: Newest"). */
@Immutable
public data class CamoMenuGroup(val items: List<CamoMenuItem>, val exclusive: Boolean = false) : CamoMenuEntry

/**
 * A row that opens a nested menu of [items] beside it (Reset ▸ Soft, Mixed, Hard). Hovering opens it after a
 * short delay; a click, Enter and the arrow toward the child (Right, or Left in right-to-left) open it at once.
 * The arrow back and Esc close only the submenu; selecting a leaf closes every level. A submenu may hold
 * submenus; more than two levels is a design smell.
 */
@Immutable
public data class CamoMenuSubmenu(
    val text: String,
    val items: List<CamoMenuEntry>,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true,
) : CamoMenuEntry

/** Opens and closes a [CamoMenuAnchor] from outside it. */
@Stable
public class CamoMenuController {
    /** Whether the menu is showing. */
    public var isOpen: Boolean by mutableStateOf(false)
        private set

    /** Shows the menu. */
    public fun open() {
        isOpen = true
    }

    /** Hides the menu. */
    public fun close() {
        isOpen = false
    }
}

/**
 * A menu attached to [anchor] (an overflow button, a split button's arrow): [anchor] gets `open` for its click.
 * The menu shows [items], or [panel]'s custom content in a [CamoMenuPanel] (ADR-30), below the anchor (above
 * it near the bottom edge). Opened from the keyboard, the first item takes focus; Up and Down move, Home and
 * End jump, typing jumps to the next item starting with the typed letters, Enter selects, and Esc or an outside
 * tap closes it, returning focus to the anchor. With [speedSearch] typing instead filters the items by what was
 * typed (prefix, substring or camel-hump), highlights the matches, and Esc clears the text before it closes the
 * menu; a menu with submenus keeps the plain type-ahead.
 */
@Composable
public fun CamoMenuAnchor(
    modifier: Modifier = Modifier,
    items: List<CamoMenuEntry> = emptyList(),
    panel: (@Composable () -> Unit)? = null,
    controller: CamoMenuController? = null,
    speedSearch: Boolean = false,
    anchor: @Composable (open: () -> Unit) -> Unit,
) {
    val state = controller ?: remember { CamoMenuController() }
    val scope = rememberCoroutineScope()
    val opener = remember { FocusRequester() }
    val keyboard = LocalInputModeManager.current
    val close = {
        state.close()
        if (keyboard.inputMode == InputMode.Keyboard) opener.requestFocus()
    }
    Box(modifier.focusRequester(opener)) {
        anchor(state::open)
        if (state.isOpen) MenuPopup(items, panel, scope, onClose = close, speedSearch = speedSearch)
    }
}

/**
 * A menu on right-click (desktop, web) or long press (touch) anywhere in [content], opening at the pointer.
 * A long press that opens the menu doesn't also click [content]. Screen readers reach it as the long-press
 * action.
 */
@Composable
public fun CamoContextMenuArea(
    items: List<CamoMenuEntry>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var at by remember { mutableStateOf<IntOffset?>(null) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .pointerInput(Unit) {
                // A secondary press isn't a "down" for the gesture helpers, so it's read from the raw events.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            at = event.changes.first().position.round()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (down.type == PointerType.Touch) {
                        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation(PointerEventPass.Initial)
                        }
                        if (up == null) {
                            at = down.position.round()
                            // The rest of the gesture is the menu's: the content mustn't see a click.
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
            }
            .semantics {
                onLongClick {
                    at = IntOffset.Zero
                    true
                }
            },
    ) {
        content()
        at?.let { position ->
            Box(Modifier.offset { position }.size(0.dp)) {
                MenuPopup(items, panel = null, scope, onClose = { at = null })
            }
        }
    }
}

/**
 * A standard menu row for custom content in a [CamoMenuPanel] (an account switcher with a "Sign out" item):
 * [onSelected] runs on click, Enter or Space.
 */
@Composable
public fun CamoMenuItemRow(item: CamoMenuItem, onSelected: () -> Unit, modifier: Modifier = Modifier) {
    val kind = if (item.checked == null) RowKind.Plain else RowKind.Check
    MenuItemRow(item, kind, item.checked != null, item.leadingIcon != null, onSelected, modifier)
}

/**
 * The anchored popup with a menu in the skin's panel: [items], or [panel] when set. A selected item's action
 * runs in [scope] a frame after the menu closes, so [scope] must outlive the popup (the anchor's).
 */
@Composable
internal fun MenuPopup(
    items: List<CamoMenuEntry>,
    panel: (@Composable () -> Unit)?,
    scope: CoroutineScope,
    onClose: () -> Unit,
    minWidth: Dp = 0.dp,
    speedSearch: Boolean = false,
) {
    // A menu with submenus lays out all its levels in one window-sized layer (below); the rest is a plain popup.
    if (panel == null && items.any { it is CamoMenuSubmenu }) {
        NestedMenuPopup(items, scope, onClose)
        return
    }
    val search = if (speedSearch) remember { CamoSpeedSearchState() } else null
    CamoAnchoredPopup(onClose) {
        CamoMenuPanel {
            if (panel != null) panel() else MenuEntries(items, scope, onClose, minWidth, search = search)
        }
    }
}

internal enum class RowKind { Plain, Check, Radio, Submenu }

internal class MenuRow(val item: CamoMenuItem, val kind: RowKind, val submenu: CamoMenuSubmenu? = null)

private const val TYPE_AHEAD_MILLIS = 500

// Letters typed within half a second of each other make one prefix.
private class TypeAhead {
    private var prefix = ""
    private var last = TimeSource.Monotonic.markNow()

    fun add(char: Char): String {
        prefix = if (last.elapsedNow().inWholeMilliseconds > TYPE_AHEAD_MILLIS) "$char" else prefix + char
        last = TimeSource.Monotonic.markNow()
        return prefix
    }
}

// One menu level: its rows, keys and submenu state in one place.
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
internal fun MenuEntries(
    entries: List<CamoMenuEntry>,
    scope: CoroutineScope,
    onClose: () -> Unit,
    minWidth: Dp,
    onBack: (() -> Unit)? = null,
    open: Int = -1,
    childHovered: Boolean = false,
    onOpen: (index: Int, submenu: CamoMenuSubmenu, bounds: IntRect) -> Unit = { _, _, _ -> },
    onCloseChild: () -> Unit = {},
    search: CamoSpeedSearchState? = null,
) {
    val skin = requireSkin("CamoMenuAnchor")
    val theme = requireTheme("CamoMenuAnchor")
    val style = remember(skin, theme) { skin.menu.defaults(theme).merge(theme.components.menu) }
    val query = search?.query.orEmpty()
    val shownEntries = remember(entries, query) {
        if (search == null || query.isEmpty()) entries else filterEntries(entries, search)
    }
    val rows = remember(shownEntries) { flatten(shownEntries) }
    val requesters = remember(rows.size) { List(rows.size) { FocusRequester() } }
    val reserveCheck = rows.any { it.item.checked != null }
    val reserveIcon = rows.any { it.item.leadingIcon != null }
    var focused by remember { mutableIntStateOf(-1) }
    var hovered by remember { mutableIntStateOf(-1) }
    val bounds = remember(rows.size) { arrayOfNulls<IntRect>(rows.size) } // each submenu row, in the layer
    fun openRow(at: Int) {
        val submenu = rows[at].submenu
        val rect = bounds[at]
        if (submenu != null && rect != null) onOpen(at, submenu, rect)
    }
    val typeAhead = remember { TypeAhead() }
    val keyboard = LocalInputModeManager.current
    val toward = if (LocalLayoutDirection.current == LayoutDirection.Ltr) Key.DirectionRight else Key.DirectionLeft
    val away = if (toward == Key.DirectionRight) Key.DirectionLeft else Key.DirectionRight
    val enabled = rows.indices.filter { rows[it].item.enabled }
    LaunchedEffect(Unit) {
        // After the popup has taken focus itself; a touch opening shows no focus ring.
        withFrameNanos { }
        if (keyboard.inputMode == InputMode.Keyboard) enabled.firstOrNull()?.let { requesters[it].requestFocus() }
    }
    if (search != null) {
        SideEffect { search.matchCount = rows.size }
        // The rows changed with the query: focus the first match (the focused row may be gone).
        LaunchedEffect(query) {
            withFrameNanos { }
            if (query.isNotEmpty()) enabled.firstOrNull()?.let { requesters[it].requestFocus() }
        }
    }
    SubmenuTimers(rows, requesters, hovered, open, childHovered, ::openRow, onCloseChild)

    fun keys(event: KeyEvent): Boolean {
        // Speed search takes the typed keys first (and Esc, while it has text).
        val down = event.type == KeyEventType.KeyDown
        if (!down || search?.onKey(event) == true) return down
        val canOpen = rows.getOrNull(focused)?.let { it.submenu != null && it.item.enabled } == true
        return when (submenuKey(event.key, toward, away, canOpen, onBack != null)) {
            SubmenuKey.Open -> {
                openRow(focused)
                true
            }
            SubmenuKey.Back -> {
                onBack?.invoke()
                true
            }
            null -> keyTarget(event, rows, enabled, focused, typeAhead)?.also { requesters[it].requestFocus() } != null
        }
    }

    val scroll = rememberScrollState()
    Box {
    Column(
        Modifier
            .widthIn(min = max(style.minWidth, minWidth), max = max(style.maxWidth, minWidth))
            .width(IntrinsicSize.Max)
            .verticalScroll(scroll)
            .onKeyEvent(::keys),
    ) {
        var index = 0

        @Composable
        fun Row(row: MenuRow) {
            val at = index++
            val modifier = Modifier.focusRequester(requesters[at]).onFocusChanged { if (it.isFocused) focused = at }
            val submenu = row.submenu
            if (submenu == null) {
                MenuItemRow(
                    row.item,
                    row.kind,
                    reserveCheck,
                    reserveIcon,
                    onSelected = {
                        onClose()
                        scope.launch {
                            withFrameNanos { }
                            row.item.onClick()
                        }
                    },
                    modifier,
                    highlight = search?.match(row.item.text).orEmpty(),
                )
            } else {
                MenuItemRow(
                    row.item,
                    row.kind,
                    reserveCheck,
                    reserveIcon,
                    onSelected = { openRow(at) },
                    modifier.onGloballyPositioned { bounds[at] = it.boundsInRoot().roundToIntRect() },
                    submenuOpen = open == at,
                    onCollapse = onCloseChild,
                    onHoverChange = { if (it) hovered = at else if (hovered == at) hovered = -1 },
                )
            }
        }

        shownEntries.forEach { entry ->
            when (entry) {
                is CamoMenuItem, is CamoMenuSubmenu -> Row(rows[index])
                CamoMenuDivider -> CamoDivider(Modifier.padding(vertical = Camouflage.spacing.xs))
                is CamoMenuGroup -> Column(if (entry.exclusive) Modifier.selectableGroup() else Modifier) {
                    entry.items.forEach { _ -> Row(rows[index]) }
                }
            }
        }
    }
        EdgeScrollbar(rememberCamoScrollAdapter(scroll))
        if (search != null) {
            CamoSpeedSearchOverlay(search, Modifier.align(Alignment.BottomStart).padding(Camouflage.spacing.xs))
        }
    }
}

// The entries that match the query: items and submenu rows by their text, groups by their matching items; dividers
// go while a search runs.
private fun filterEntries(entries: List<CamoMenuEntry>, search: CamoSpeedSearchState): List<CamoMenuEntry> =
    entries.mapNotNull { entry ->
        when (entry) {
            is CamoMenuItem -> entry.takeIf { search.matches(it.text) }
            is CamoMenuSubmenu -> entry.takeIf { search.matches(it.text) }
            CamoMenuDivider -> null
            is CamoMenuGroup -> entry.items.filter { search.matches(it.text) }
                .takeIf { it.isNotEmpty() }?.let { CamoMenuGroup(it, entry.exclusive) }
        }
    }

// The row a key moves focus to: Home and End jump; typed letters find the next item starting with them (a
// repeated first letter steps through the items starting with it).
private fun keyTarget(
    event: KeyEvent,
    rows: List<MenuRow>,
    enabled: List<Int>,
    focused: Int,
    typeAhead: TypeAhead,
): Int? =
    when (event.key) {
        Key.MoveHome -> enabled.firstOrNull()
        Key.MoveEnd -> enabled.lastOrNull()
        else -> typed(event)?.let { char ->
            val prefix = typeAhead.add(char)
            val from = if (prefix.length == 1) focused + 1 else maxOf(focused, 0)
            (enabled.filter { it >= from } + enabled.filter { it < from })
                .firstOrNull { rows[it].item.text.startsWith(prefix, ignoreCase = true) }
        }
    }

private fun flatten(entries: List<CamoMenuEntry>): List<MenuRow> = entries.flatMap { entry ->
    when (entry) {
        is CamoMenuItem -> listOf(MenuRow(entry, if (entry.checked == null) RowKind.Plain else RowKind.Check))
        is CamoMenuSubmenu -> listOf(
            MenuRow(
                CamoMenuItem(entry.text, onClick = {}, leadingIcon = entry.leadingIcon, enabled = entry.enabled),
                RowKind.Submenu,
                entry,
            ),
        )
        CamoMenuDivider -> emptyList()
        is CamoMenuGroup -> entry.items.map {
            MenuRow(
                it,
                when {
                    it.checked == null -> RowKind.Plain
                    entry.exclusive -> RowKind.Radio
                    else -> RowKind.Check
                },
            )
        }
    }
}

// The printable character [event] types, without a shortcut modifier; Space is the select key, not a letter.
private fun typed(event: KeyEvent): Char? {
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return null
    val code = event.utf16CodePoint
    return code.takeIf { it > ' '.code && it != DELETE }?.toChar()
}

private const val DELETE = 0x7F

@Composable
internal fun MenuItemRow(
    item: CamoMenuItem,
    kind: RowKind,
    reserveCheck: Boolean,
    reserveIcon: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier,
    submenuOpen: Boolean = false,
    onCollapse: () -> Unit = {},
    onHoverChange: ((Boolean) -> Unit)? = null,
    highlight: List<IntRange> = emptyList(),
    // Draws the row as focused without giving it focus (the command palette keeps focus in its field).
    active: Boolean = false,
) {
    val skin = requireSkin("CamoMenuItemRow")
    val theme = requireTheme("CamoMenuItemRow")
    val style = remember(skin, theme) { skin.menu.defaults(theme).merge(theme.components.menu) }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    if (active) {
        DisposableEffect(source) {
            val focus = FocusInteraction.Focus()
            source.tryEmit(focus)
            onDispose { source.tryEmit(FocusInteraction.Unfocus(focus)) }
        }
    }
    val checked = item.checked == true
    if (onHoverChange != null) {
        val hovered = interaction.hovered
        LaunchedEffect(hovered) { onHoverChange(hovered) }
    }
    val behavior = when (kind) {
        RowKind.Plain -> Modifier.clickable(source, null, item.enabled, role = Role.Button, onClick = onSelected)
        // A submenu row is a button with a popup: screen readers get its expanded or collapsed state and action.
        RowKind.Submenu -> Modifier
            .clickable(source, null, item.enabled, role = Role.Button, onClick = onSelected)
            .semantics {
                if (submenuOpen) {
                    collapse {
                        onCollapse()
                        true
                    }
                } else {
                    expand {
                        onSelected()
                        true
                    }
                }
            }
        RowKind.Check -> Modifier.toggleable(checked, source, null, item.enabled, Role.Checkbox) { onSelected() }
        RowKind.Radio -> Modifier.selectable(checked, source, null, item.enabled, Role.RadioButton, onSelected)
    }
    skin.menu.Item(
        MenuItemState(
            text = item.text,
            trailingText = item.trailingText,
            tone = item.tone,
            enabled = item.enabled,
            checked = item.checked,
            reserveCheck = reserveCheck,
            reserveIcon = reserveIcon,
            interaction = interaction,
            interactionSource = source,
            hasSubmenu = kind == RowKind.Submenu,
            submenuOpen = submenuOpen,
            highlight = highlight,
        ),
        MenuItemSlots(item.leadingIcon),
        theme,
        style,
        modifier.then(behavior),
    )
}
