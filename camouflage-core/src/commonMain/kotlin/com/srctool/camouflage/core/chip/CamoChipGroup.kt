package com.srctool.camouflage.core.chip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** One option of a [CamoChipGroup]. */
@Immutable
public data class ChipOption<T>(
    val value: T,
    val label: String,
    val leading: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true,
)

/** How a [CamoChipGroup] lays out its chips. */
public enum class ChipLayout {
    /** Rows that wrap onto the next line. */
    Wrap,

    /** One horizontally scrolling row (under a search bar, say). */
    Scroll,
}

/**
 * The selection a tap on [value] makes: multi-select toggles membership; single choice replaces the set, and
 * deselects [value] only when the group isn't [required].
 */
public fun <T> chipGroupSelection(selected: Set<T>, value: T, multiSelect: Boolean, required: Boolean): Set<T> =
    when {
        multiSelect -> if (value in selected) selected - value else selected + value
        value in selected -> if (required) selected else emptySet()
        else -> setOf(value)
    }

/**
 * Filter chips for [options], [ChipStyle.groupSpacing] apart: several choices with [multiSelect] (a group of
 * toggles, the multi-select replacement for a segmented control), or one (a radio group, "Open now, 2 of 5").
 * A [required] single choice keeps its last chip selected. The group is one Tab stop; the arrow keys move
 * between chips, and select in a single-choice group. In [ChipLayout.Scroll], a focused chip scrolls into view.
 * Name the group with a heading above it or a semantics label on [modifier].
 */
@Composable
public fun <T> CamoChipGroup(
    options: List<ChipOption<T>>,
    selected: Set<T>,
    onSelectionChange: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = true,
    layout: ChipLayout = ChipLayout.Wrap,
    required: Boolean = false,
) {
    val skin = requireSkin("CamoChipGroup")
    val theme = requireTheme("CamoChipGroup")
    val style = remember(skin, theme) { skin.chip.defaults(theme).merge(theme.components.chip) }
    val focus = rememberRovingFocus()
    val group = modifier
        .then(if (multiSelect) Modifier else Modifier.selectableGroup())
        .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = options.size) }
    val chips: @Composable () -> Unit = {
        options.forEachIndexed { index, option ->
            val isSelected = option.value in selected
            val toggle = { onSelectionChange(chipGroupSelection(selected, option.value, multiSelect, required)) }
            // Arrow keys select in a single-choice group, and only move focus in a multi-select one.
            val onArrow = if (multiSelect) ({}) else ({ if (!isSelected) toggle() })
            ChipImpl(
                label = option.label,
                modifier = Modifier
                    .rovingFocusItem(focus, index, option.enabled, isSelected, onArrow)
                    .semantics { collectionItemInfo = CollectionItemInfo(0, 1, index, 1) },
                onClick = toggle,
                selected = isSelected,
                onRemove = null,
                leading = option.leading,
                appearance = null,
                tone = Tone.Default,
                enabled = option.enabled,
                interactionSource = null,
                groupRole = if (multiSelect) null else Role.RadioButton,
            )
        }
    }
    val spacing = Arrangement.spacedBy(style.groupSpacing)
    when (layout) {
        ChipLayout.Wrap -> FlowRow(group, horizontalArrangement = spacing, verticalArrangement = spacing) { chips() }
        // A scrolling Row, not a lazy one, so the arrow keys reach every chip; focus brings a chip into view.
        ChipLayout.Scroll -> Row(
            group.horizontalScroll(rememberScrollState()),
            horizontalArrangement = spacing,
            verticalAlignment = Alignment.CenterVertically,
        ) { chips() }
    }
}
