package com.srctool.camouflage.core.skin

import androidx.compose.runtime.Immutable

/** Which side of a control its label sits on, in reading order: `Start` before it, `End` after it. */
public enum class LabelPosition { Start, End }

/** An item's place in a row of [count] items (a segment, a tab), for its shape and its "2 of 3" announcement. */
@Immutable
public data class ItemPosition(val index: Int, val count: Int) {
    /** The first item. */
    val isFirst: Boolean get() = index == 0

    /** The last item. */
    val isLast: Boolean get() = index == count - 1
}
