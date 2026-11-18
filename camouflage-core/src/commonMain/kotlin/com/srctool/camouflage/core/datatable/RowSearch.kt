package com.srctool.camouflage.core.datatable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.srctool.camouflage.core.speedsearch.CamoSpeedSearchState
import com.srctool.camouflage.core.speedsearch.SpeedSearchMode

// What a speed search leaves of a table's rows: the rows to show, and which of them match.
internal class RowSearch<T>(val rows: List<T>, val hitRows: List<Int>, val count: Int)

// A row matches when any column that gives its text (`CamoColumn.text`) matches.
internal fun <T> rowMatches(state: CamoSpeedSearchState, columns: List<CamoColumn<T>>, row: T): Boolean =
    columns.any { column -> column.text?.let { state.matches(it(row)) } == true }

@Composable
internal fun <T> rememberRowSearch(
    state: CamoSpeedSearchState?,
    columns: List<CamoColumn<T>>,
    rows: List<T>,
): RowSearch<T> {
    val query = state?.query.orEmpty()
    return remember(state, query, state?.kind, columns, rows) {
        if (state == null || query.isEmpty()) {
            RowSearch(rows, emptyList(), rows.size)
        } else {
            val hits = rows.filter { rowMatches(state, columns, it) }
            if (state.mode == SpeedSearchMode.Filter) {
                RowSearch(hits, hits.indices.toList(), hits.size)
            } else {
                RowSearch(rows, rows.indices.filter { rowMatches(state, columns, rows[it]) }, hits.size)
            }
        }
    }
}
