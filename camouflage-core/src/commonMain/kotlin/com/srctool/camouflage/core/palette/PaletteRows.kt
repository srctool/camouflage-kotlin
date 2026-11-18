package com.srctool.camouflage.core.palette

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.srctool.camouflage.core.speedsearch.MatchKind

// What the palette shows for one query: a flat list the lazy column draws, and the items among it.
internal sealed interface PaletteRow {
    data class Header(val title: String) : PaletteRow

    // [index] counts the items of every group; [position] and [size] are within the group.
    data class Entry(
        val item: CamoCommandItem,
        val group: String,
        val kind: MatchKind?,
        val index: Int,
        val position: Int,
        val size: Int,
    ) : PaletteRow

    data class Status(val text: String) : PaletteRow
}

// One source's latest answer; [items] are the previous query's while [loading] is true.
@Immutable
internal class SourceResult(val items: List<CamoCommandItem>, val loading: Boolean)

internal class PaletteLayout(val rows: List<PaletteRow>, val entries: List<PaletteRow.Entry>) {
    // The lazy column index of each entry, and the entry index each group starts at.
    val rowOfEntry: List<Int> = rows.mapIndexedNotNull { i, r -> i.takeIf { r is PaletteRow.Entry } }
    val groupStarts: List<Int> = entries.filter { it.position == 0 }.map { it.index }
}

internal fun layoutPalette(
    sources: List<CamoCommandSource>,
    results: List<SourceResult?>,
    recent: List<Any>,
    query: String,
    loadingText: String,
    noResultsText: String,
): PaletteLayout {
    val rows = ArrayList<PaletteRow>()
    val entries = ArrayList<PaletteRow.Entry>()
    var anyLoading = false
    sources.forEachIndexed { i, source ->
        val result = results.getOrNull(i) ?: SourceResult(emptyList(), true)
        // With nothing typed, what was run before comes first.
        val ordered = if (query.isEmpty()) result.items.sortedBy { recentRank(recent, it.id) } else result.items
        val items = ordered.take(source.limit)
        anyLoading = anyLoading || result.loading
        if (items.isNotEmpty() || result.loading) rows += PaletteRow.Header(source.title)
        items.forEachIndexed { p, item ->
            val entry = PaletteRow.Entry(item, source.title, source.matchKind, entries.size, p, items.size)
            entries += entry
            rows += entry
        }
        if (items.isEmpty() && result.loading) rows += PaletteRow.Status(loadingText)
    }
    if (entries.isEmpty() && !anyLoading) rows += PaletteRow.Status(noResultsText)
    return PaletteLayout(rows, entries)
}

private fun recentRank(recent: List<Any>, id: Any): Int = recent.indexOf(id).let { if (it < 0) Int.MAX_VALUE else it }

// Up and Down move the active row, Tab and Shift+Tab the group; Esc is the popup's, after the field's own clear.
internal fun paletteKey(
    event: KeyEvent,
    groupStarts: List<Int>,
    active: Int,
    count: Int,
    onActive: (Int) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown || count == 0) return false
    val target = when (event.key) {
        Key.DirectionDown -> (active + 1) % count
        Key.DirectionUp -> (active - 1 + count) % count
        Key.Tab -> groupTarget(groupStarts, active, if (event.isShiftPressed) -1 else 1)
        else -> null
    }
    target?.let(onActive)
    return target != null
}

// The group start a Tab (step 1) or Shift+Tab (step -1) from [active] lands on, wrapping around.
internal fun groupTarget(starts: List<Int>, active: Int, step: Int): Int? {
    if (starts.isEmpty()) return null
    val here = starts.indexOfLast { it <= active }.coerceAtLeast(0)
    return starts[(here + step).mod(starts.size)]
}

internal fun escapeCloses(event: KeyEvent, close: () -> Unit): Boolean {
    val esc = event.type == KeyEventType.KeyDown && event.key == Key.Escape
    if (esc) close()
    return esc
}
