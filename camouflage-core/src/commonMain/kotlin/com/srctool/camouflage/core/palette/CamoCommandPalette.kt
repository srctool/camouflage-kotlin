package com.srctool.camouflage.core.palette

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.menu.CamoMenuPanel
import com.srctool.camouflage.core.menu.MenuItemRow
import com.srctool.camouflage.core.menu.RowKind
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.search.CamoSearchField
import com.srctool.camouflage.core.speedsearch.CamoHighlightedText
import com.srctool.camouflage.core.speedsearch.CamoMatcher
import com.srctool.camouflage.core.text.CamoText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val DefaultMaxWidth = 640.dp
private val DefaultMaxHeight = 400.dp
private val EdgeMargin = 16.dp

/**
 * "Find action" and "Search everywhere": a popup at the top of the window with one field and results grouped by
 * source. Put it once near the root; show it with [CamoCommandPaletteState.show] from your own shortcut.
 *
 * Typing runs every source's search (a new keystroke cancels the old one, and a late answer for an old query
 * never shows); each group appears as it answers and a slow one shows a loading row. Titles are highlighted with
 * the source's match kind. With nothing typed each group lists what was run before first. Focus stays in the
 * field: Up and Down move the active row through every group, Tab and Shift+Tab jump between groups, Enter runs
 * the active row, Esc closes. The palette closes first, then the item's `onSelect` runs in the next frame.
 *
 * Built from the skin's popup panel, search field and menu rows: every skin draws it. Screen readers get the
 * field, a list with "n of m" and the group of every row, and hear the active row as it changes. (Compose has no
 * combobox role, so the field is announced as an edit field; check TalkBack and VoiceOver.)
 */
@Composable
public fun CamoCommandPalette(
    state: CamoCommandPaletteState,
    sources: List<CamoCommandSource>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    if (!state.open) return
    val theme = requireTheme("CamoCommandPalette")
    val o = theme.components.commandPalette
    val results = remember(sources) { mutableStateListOf<SourceResult?>().apply { repeat(sources.size) { add(null) } } }
    SourceRunner(state, sources, results)
    val loadingText = o?.loading ?: "Loading…"
    val noResultsText = o?.noResults ?: "No results"
    val layout by remember(sources, state) {
        derivedStateOf {
            layoutPalette(sources, results.toList(), state.recent, state.query, loadingText, noResultsText)
        }
    }
    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.query) { active = 0 }
    val current = active.coerceIn(0, maxOf(0, layout.entries.size - 1))
    val run = rememberUpdatedState<(PaletteRow.Entry) -> Unit> { entry ->
        state.remember(entry.item.id)
        state.hide()
        // After the palette is gone, so a dialog the item opens isn't stacked over it.
        scope.launch {
            withFrameNanos { }
            entry.item.onSelect()
        }
    }
    Popup(
        popupPositionProvider = remember { TopCenter },
        onDismissRequest = { state.hide() },
        properties = PopupProperties(focusable = true),
    ) {
        BoxWithConstraints {
            val width = minOf(o?.maxWidth ?: DefaultMaxWidth, maxWidth - EdgeMargin * 2)
            CamoMenuPanel(modifier.width(width)) {
                PaletteBody(
                    state, layout, current, o?.placeholder ?: "Search actions and files",
                    o?.maxHeight ?: DefaultMaxHeight,
                    onActive = { active = it },
                    onRun = { run.value(it) },
                )
            }
        }
    }
}

// Runs every source for the query: each answers on its own, and a new query (or the palette closing) cancels all.
@Composable
private fun SourceRunner(
    state: CamoCommandPaletteState,
    sources: List<CamoCommandSource>,
    results: MutableList<SourceResult?>,
) {
    LaunchedEffect(sources, state.query) {
        val query = state.query
        sources.indices.forEach { i ->
            // The previous answer stays on screen, marked loading, until the new one arrives.
            results[i] = SourceResult(results[i]?.items.orEmpty(), true)
        }
        coroutineScope {
            sources.forEachIndexed { i, source ->
                launch {
                    val items = try {
                        source.search(query)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                        emptyList()
                    }
                    results[i] = SourceResult(items, false)
                }
            }
        }
    }
}

@Composable
private fun PaletteBody(
    state: CamoCommandPaletteState,
    layout: PaletteLayout,
    active: Int,
    placeholder: String,
    maxHeight: Dp,
    onActive: (Int) -> Unit,
    onRun: (PaletteRow.Entry) -> Unit,
) {
    val listState = rememberLazyListState()
    val count = layout.entries.size
    LaunchedEffect(active, layout.rowOfEntry) {
        val row = layout.rowOfEntry.getOrNull(active) ?: return@LaunchedEffect
        // The first entry shows its group heading too.
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == row }
        if (!visible || active == 0) listState.scrollToItem(if (active == 0) 0 else row)
    }
    Column(
        Modifier
            .onPreviewKeyEvent { paletteKey(it, layout.groupStarts, active, count, onActive) }
            // After the field, which clears a typed query on its own Esc: the next one closes.
            .onKeyEvent { escapeCloses(it) { state.hide() } },
    ) {
        CamoSearchField(
            query = state.query,
            onQueryChange = { state.query = it },
            onSearch = { layout.entries.getOrNull(active)?.let(onRun) },
            placeholder = placeholder,
            modifier = Modifier.padding(Camouflage.spacing.xs),
            autoFocus = true,
        )
        PaletteList(layout, state.query, active, maxHeight, listState, onRun)
        ActiveAnnouncement(layout.entries.getOrNull(active), count)
    }
}

@Composable
private fun PaletteList(
    layout: PaletteLayout,
    query: String,
    active: Int,
    maxHeight: Dp,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRun: (PaletteRow.Entry) -> Unit,
) {
    val reserveIcon = layout.entries.any { it.item.icon != null }
    val onVariant = requireTheme("CamoCommandPalette").colors.onSurfaceVariant
    LazyColumn(
        Modifier
            .heightIn(max = maxHeight)
            .semantics { collectionInfo = CollectionInfo(layout.entries.size, 1) },
        state = listState,
    ) {
        itemsIndexed(layout.rows, key = { i, row -> rowKey(i, row) }) { _, row ->
            when (row) {
                is PaletteRow.Header -> CamoText(
                    row.title,
                    Modifier
                        .padding(horizontal = Camouflage.spacing.m, vertical = Camouflage.spacing.xs)
                        .semantics { heading() },
                    style = Camouflage.typography.labelSmall,
                    color = onVariant,
                )
                is PaletteRow.Status -> CamoText(
                    row.text,
                    Modifier.padding(horizontal = Camouflage.spacing.m, vertical = Camouflage.spacing.s),
                    style = Camouflage.typography.bodyMedium,
                    color = onVariant,
                )
                is PaletteRow.Entry -> EntryRow(row, query, row.index == active, reserveIcon, onRun)
            }
        }
    }
}

private fun rowKey(index: Int, row: PaletteRow): Any = when (row) {
    is PaletteRow.Header -> "h:${row.title}"
    is PaletteRow.Entry -> "e:${row.group}:${row.item.id}"
    is PaletteRow.Status -> "s:$index"
}

@Composable
private fun EntryRow(
    row: PaletteRow.Entry,
    query: String,
    active: Boolean,
    reserveIcon: Boolean,
    onRun: (PaletteRow.Entry) -> Unit,
) {
    val item = row.item
    val highlight = remember(query, item.title, row.kind) {
        row.kind?.let { CamoMatcher.match(query, item.title, it) }.orEmpty()
    }
    val description = listOfNotNull(row.group, "${row.position + 1} of ${row.size}", item.subtitle).joinToString(", ")
    MenuItemRow(
        item = CamoMenuItem(
            text = item.title,
            onClick = { onRun(row) },
            leadingIcon = item.icon,
            trailingText = item.shortcut ?: item.subtitle,
        ),
        kind = RowKind.Plain,
        reserveCheck = false,
        reserveIcon = reserveIcon,
        onSelected = { onRun(row) },
        // The field keeps focus: a row is never a Tab stop, the active one only looks focused.
        modifier = Modifier
            .focusProperties { canFocus = false }
            .semantics {
                selected = active
                stateDescription = description
                collectionItemInfo = CollectionItemInfo(row.index, 1, 0, 1)
            },
        highlight = highlight,
        active = active,
    )
}

// Says the active row as it changes, without moving focus out of the field.
@Composable
private fun ActiveAnnouncement(entry: PaletteRow.Entry?, count: Int) {
    val text = entry?.let { "${it.item.title}, ${it.group}, ${it.index + 1} of $count" }.orEmpty()
    CamoText(
        "",
        Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = text
        },
    )
}

private val TopCenter = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0),
        (windowSize.height / TOP_FRACTION).coerceAtMost(maxOf(0, windowSize.height - popupContentSize.height)),
    )
}

private const val TOP_FRACTION = 8
