package com.srctool.camouflage.core.divider

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme

/** Where a divider leaves space: nowhere, at the start (after a leading icon), or on both sides. */
public enum class DividerInset { None, Start, Middle }

/** Overrides for dividers. `null` keeps the skin's default; `thickness = 0.dp` hides every divider. */
@Immutable
public data class DividerTheme(
    val thickness: Dp? = null,
    val color: Color? = null,
    val startInset: Dp? = null,
    val endInset: Dp? = null,
)

/** The resolved divider style: every field set. */
@Immutable
public data class DividerStyle(val thickness: Dp, val color: Color, val startInset: Dp, val endInset: Dp) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: DividerTheme?): DividerStyle = if (overrides == null) {
        this
    } else {
        DividerStyle(
            thickness = overrides.thickness ?: thickness,
            color = overrides.color ?: color,
            startInset = overrides.startInset ?: startInset,
            endInset = overrides.endInset ?: endInset,
        )
    }
}

/** What a divider renderer draws. */
@Immutable
public data class DividerState(val orientation: Orientation, val inset: DividerInset)

/** Draws dividers. */
@Stable
public interface DividerRenderer {
    /** The complete divider style for [theme]. */
    public fun defaults(theme: CamoTheme): DividerStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: DividerState, theme: CamoTheme, style: DividerStyle, modifier: Modifier)
}

/** A line between groups of content. Decorative: hidden from screen readers. */
@Composable
public fun CamoDivider(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    inset: DividerInset = DividerInset.None,
) {
    val skin = requireSkin("CamoDivider")
    val theme = requireTheme("CamoDivider")
    val style = remember(skin, theme) { skin.divider.defaults(theme).merge(theme.components.divider) }
    skin.divider.Render(DividerState(orientation, inset), theme, style, modifier)
}
