package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.bottomsheet.BottomSheetRenderer
import com.srctool.camouflage.core.bottomsheet.BottomSheetSlots
import com.srctool.camouflage.core.bottomsheet.BottomSheetState
import com.srctool.camouflage.core.bottomsheet.BottomSheetStyle
import com.srctool.camouflage.core.bottomsheet.WidePresentation
import com.srctool.camouflage.core.datepicker.AllowTyping
import com.srctool.camouflage.core.datepicker.CalendarCell
import com.srctool.camouflage.core.datepicker.CalendarSlots
import com.srctool.camouflage.core.datepicker.CalendarState
import com.srctool.camouflage.core.datepicker.DatePickerColors
import com.srctool.camouflage.core.datepicker.DatePickerForm
import com.srctool.camouflage.core.datepicker.DatePickerRenderer
import com.srctool.camouflage.core.datepicker.DatePickerStrings
import com.srctool.camouflage.core.datepicker.DatePickerStyle
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.menu.CheckPosition
import com.srctool.camouflage.core.menu.MenuColors
import com.srctool.camouflage.core.menu.MenuItemSlots
import com.srctool.camouflage.core.menu.MenuItemState
import com.srctool.camouflage.core.menu.MenuRenderer
import com.srctool.camouflage.core.menu.MenuStyle
import com.srctool.camouflage.core.overlay.StackedScrim
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.timepicker.DialState
import com.srctool.camouflage.core.timepicker.TimePickerColors
import com.srctool.camouflage.core.timepicker.TimePickerForm
import com.srctool.camouflage.core.timepicker.TimePickerRenderer
import com.srctool.camouflage.core.timepicker.TimePickerSlots
import com.srctool.camouflage.core.timepicker.TimePickerState
import com.srctool.camouflage.core.timepicker.TimePickerStrings
import com.srctool.camouflage.core.timepicker.TimePickerStyle

// The M5 overlays and pickers in the plain test skin: boxes and rows, no decoration.

private val Square = IconSource.Vector(
    ImageVector.Builder("square", 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black)) { moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); close() }
        .build(),
)

internal object PlainMenu : MenuRenderer {
    override fun defaults(theme: CamoTheme) = MenuStyle(
        RectangleShape, 112.dp, 280.dp, 48.dp, PaddingValues(8.dp), theme.typography.labelLarge, 24.dp,
        ElevationLevel.Level2, CheckPosition.Trailing, Square,
        theme.colors.let { MenuColors(it.surface, it.onSurface, it.onSurfaceVariant, it.onSurfaceVariant) },
    )

    @Composable
    override fun Panel(theme: CamoTheme, style: MenuStyle, modifier: Modifier, content: @Composable () -> Unit) =
        Box(modifier.background(style.colors.container).widthIn(min = style.minWidth)) { content() }

    @Composable
    override fun Item(
        state: MenuItemState,
        slots: MenuItemSlots,
        theme: CamoTheme,
        style: MenuStyle,
        modifier: Modifier,
    ) =
        Row(modifier.fillMaxWidth().heightIn(min = style.itemHeight)) {
            slots.leadingIcon?.invoke()
            BasicText(state.text)
            state.trailingText?.let { BasicText(it) }
        }
}

internal object PlainBottomSheet : BottomSheetRenderer {
    override fun defaults(theme: CamoTheme) = BottomSheetStyle(
        RectangleShape, 640.dp, PaddingValues(16.dp), true, DpSize(32.dp, 4.dp), theme.colors.outline,
        theme.typography.titleLarge, TitleAlign.Start, false, Square, theme.colors.surface, 0.32f,
        WidePresentation.Sheet, StackedScrim.Each,
    )

    @Composable
    override fun Render(
        state: BottomSheetState,
        slots: BottomSheetSlots,
        theme: CamoTheme,
        style: BottomSheetStyle,
        modifier: Modifier,
    ) = Column(modifier.background(style.containerColor)) {
        state.title?.let { BasicText(it, style = style.titleStyle) }
        slots.content()
    }
}

internal object PlainDatePicker : DatePickerRenderer {
    override fun defaults(theme: CamoTheme): DatePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return DatePickerStyle(
            DatePickerForm.Calendar, null, null, true, AllowTyping.Never, false, 40.dp, 600.dp, PaddingValues(8.dp),
            t.titleMedium, t.labelMedium, t.bodyMedium, t.bodyLarge, 40.dp, Square, Square, Square,
            DatePickerColors(c.primary, c.onPrimary, c.primaryContainer, c.onPrimaryContainer, c.outline, c.onSurface,
                c.onSurfaceVariant, c.outline),
            DatePickerStrings(),
        )
    }

    @Composable
    override fun Calendar(
        state: CalendarState,
        slots: CalendarSlots,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    ) = Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            slots.previous()
            slots.header()
            slots.next()
        }
        if (state.choosingYear) {
            Box(Modifier.size(280.dp)) { slots.years() }
        } else {
            Row { state.weekdayLabels.forEach { Box(Modifier.size(style.daySize)) { BasicText(it) } } }
            state.days.chunked(7).forEach { week -> Row { week.forEach { slots.day(it) } } }
        }
    }

    @Composable
    override fun Cell(cell: CalendarCell, theme: CamoTheme, style: DatePickerStyle, modifier: Modifier) =
        Box(cell.behavior.then(modifier).size(style.daySize), contentAlignment = Alignment.Center) {
            if (cell.inMonth || style.showOutsideDays) BasicText(if (cell.selected) "[${cell.label}]" else cell.label)
        }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    ) =
        Row(modifier) { wheels.forEach { Box(Modifier.widthIn(min = 64.dp)) { it() } } }
}

internal object PlainTimePicker : TimePickerRenderer {
    override fun defaults(theme: CamoTheme): TimePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return TimePickerStyle(
            TimePickerForm.Fields, true, t.displaySmall, t.bodyMedium, t.bodyLarge, 40.dp, 64.dp, 240.dp, 600.dp,
            PaddingValues(8.dp), Square,
            TimePickerColors(c.primary, c.primaryContainer, c.surfaceContainerHighest, c.onSurface, c.onSurfaceVariant),
            TimePickerStrings(),
        )
    }

    @Composable
    override fun Render(
        state: TimePickerState,
        slots: TimePickerSlots,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    ) = Column(modifier) {
        val wheels = slots.wheels
        if (wheels != null) {
            Row { wheels.forEach { it() } }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                slots.hour()
                BasicText(":")
                slots.minute()
                slots.period?.invoke()
            }
            slots.dial?.invoke()
        }
    }

    @Composable
    override fun Dial(state: DialState, theme: CamoTheme, style: TimePickerStyle, modifier: Modifier) =
        Box(modifier.background(style.colors.face))

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    ) =
        Row(modifier) { wheels.forEach { it() } }
}
