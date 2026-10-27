package com.srctool.camouflage.core.util

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.text.CamoText
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.filter

/** How many rows a wheel shows: the selected one in the middle, two above and two below. */
internal const val WHEEL_ROWS = 5

/**
 * One wheel of a picker: [count] rows with snap scrolling, the row under the middle reported to [onSelect] once
 * scrolling stops. Screen readers get an adjustable control named [name] whose value is the selected row's text;
 * Up and Down move it by one when it has focus. The picker's renderer draws the band behind the middle row.
 */
@Composable
internal fun WheelColumn(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    label: (Int) -> String,
    name: String,
    itemHeight: Dp,
    textStyle: TextStyle,
    selectedColor: Color,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(selected.coerceIn(0, maxOf(0, count - 1)))
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(selected, count) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != selected) state.scrollToItem(selected)
    }
    val current by rememberUpdatedState(selected)
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.filter { !it }.collect {
            val index = middleRow(state) ?: return@collect
            if (index != current) select(index)
        }
    }
    val pad = itemHeight * ((WHEEL_ROWS - 1) / 2)
    Box(
        modifier
            .height(itemHeight * WHEEL_ROWS)
            .semantics(mergeDescendants = true) {
                contentDescription = name
                stateDescription = label(selected)
                progressBarRangeInfo = ProgressBarRangeInfo(selected.toFloat(), 0f..maxOf(0, count - 1).toFloat())
                setProgress { target ->
                    select(target.roundToInt().coerceIn(0, count - 1))
                    true
                }
            }
            .onKeyEvent {
                val step = when (it.key) {
                    Key.DirectionUp -> -1
                    Key.DirectionDown -> 1
                    else -> 0
                }
                val moves = step != 0 && it.type == KeyEventType.KeyDown && selected + step in 0 until count
                if (moves) select(selected + step)
                moves
            }
            .focusable(),
    ) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(state, SnapPosition.Center)),
            contentPadding = PaddingValues(vertical = pad),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clearAndSetSemantics {},
        ) {
            items(count) { index ->
                Box(Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CamoText(
                        label(index),
                        style = textStyle,
                        color = if (index == selected) selectedColor else color,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// The row whose middle is nearest the viewport's middle.
private fun middleRow(state: LazyListState): Int? {
    val info = state.layoutInfo
    val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
}
