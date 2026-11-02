package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.popover.PopoverColors
import com.srctool.camouflage.core.popover.PopoverRenderer
import com.srctool.camouflage.core.popover.PopoverSlots
import com.srctool.camouflage.core.popover.PopoverState
import com.srctool.camouflage.core.popover.PopoverStyle
import com.srctool.camouflage.core.sidesheet.SideSheetColors
import com.srctool.camouflage.core.sidesheet.SideSheetRenderer
import com.srctool.camouflage.core.sidesheet.SideSheetSlots
import com.srctool.camouflage.core.sidesheet.SideSheetState
import com.srctool.camouflage.core.sidesheet.SideSheetStyle
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel

// The M9 popover and side sheet in the plain test skin: boxes, rows and text, no decoration.

private val TestIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())
private const val SCRIM = 0.32f

/** The state the plain popover last drew, for tests. */
internal var lastPopover: PopoverState? = null

internal object PlainPopover : PopoverRenderer {
    override fun defaults(theme: CamoTheme) = PopoverStyle(
        RectangleShape, 320.dp, 16.dp, false, DpSize(16.dp, 8.dp), ElevationLevel.Level2, theme.typography.titleSmall,
        4.dp, PopoverColors(theme.colors.surface, theme.colors.onSurface, theme.colors.onSurfaceVariant),
    )

    @Composable
    override fun Render(
        state: PopoverState,
        slots: PopoverSlots,
        theme: CamoTheme,
        style: PopoverStyle,
        modifier: Modifier,
    ) {
        lastPopover = state
        Column(modifier.background(style.colors.container)) {
            slots.title?.let { BasicText(it) }
            slots.content(this)
            slots.actions?.let { Row { it() } }
        }
    }
}

internal object PlainSideSheet : SideSheetRenderer {
    override fun defaults(theme: CamoTheme): SideSheetStyle {
        val c = theme.colors
        return SideSheetStyle(
            320.dp, RectangleShape, 16.dp, theme.typography.titleLarge, true, true, TestIcon, SCRIM,
            SideSheetColors(c.surface, c.surfaceContainerLow, c.onSurface, c.outlineVariant),
        )
    }

    @Composable
    override fun Render(
        state: SideSheetState,
        slots: SideSheetSlots,
        theme: CamoTheme,
        style: SideSheetStyle,
        modifier: Modifier,
    ) {
        val frame = if (state.modal) Modifier.fillMaxSize() else Modifier.fillMaxHeight().width(style.width)
        Column(modifier.then(frame).background(style.colors.container)) {
            state.title?.let { BasicText(it) }
            state.onClose?.let { close -> BasicText("close", Modifier.clickable { close() }) }
            slots.content(this)
            slots.actions?.let { Row { it() } }
        }
    }
}
