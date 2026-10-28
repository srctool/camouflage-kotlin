package com.srctool.camouflage.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.bottomsheet.BottomSheetRenderer
import com.srctool.camouflage.core.bottomsheet.BottomSheetSlots
import com.srctool.camouflage.core.bottomsheet.BottomSheetState
import com.srctool.camouflage.core.bottomsheet.BottomSheetStyle
import com.srctool.camouflage.core.bottomsheet.WidePresentation
import com.srctool.camouflage.core.datepicker.AllowTyping
import com.srctool.camouflage.core.datepicker.CalendarCell
import com.srctool.camouflage.core.datepicker.CalendarCellKind
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
import com.srctool.camouflage.core.menu.MenuRenderer
import com.srctool.camouflage.core.menu.MenuStyle
import com.srctool.camouflage.core.overlay.StackedScrim
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.timepicker.DialState
import com.srctool.camouflage.core.timepicker.TimePart
import com.srctool.camouflage.core.timepicker.TimePickerColors
import com.srctool.camouflage.core.timepicker.TimePickerForm
import com.srctool.camouflage.core.timepicker.TimePickerRenderer
import com.srctool.camouflage.core.timepicker.TimePickerSlots
import com.srctool.camouflage.core.timepicker.TimePickerState
import com.srctool.camouflage.core.timepicker.TimePickerStrings
import com.srctool.camouflage.core.timepicker.TimePickerStyle
import com.srctool.camouflage.sample.resources.Res
import com.srctool.camouflage.sample.resources.ic_calendar
import com.srctool.camouflage.sample.resources.ic_chevron
import com.srctool.camouflage.sample.resources.ic_chevron_start
import com.srctool.camouflage.sample.resources.ic_clock
import com.srctool.camouflage.sample.resources.ic_delete
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// The M5 overlays and pickers in the debug skin: outlined boxes, the calendar as a table of numbers.

private const val WEEK = 7
private const val SCRIM = 0.32f

object DebugMenuRenderer : MenuRenderer {
    override fun defaults(theme: CamoTheme) = MenuStyle(
        RectangleShape, 112.dp, 280.dp, 40.dp, PaddingValues(4.dp), theme.typography.bodyMedium, 20.dp,
        ElevationLevel.Level0, CheckPosition.Trailing,
        theme.colors.let { MenuColors(it.surface, it.onSurface, it.onSurfaceVariant, it.onSurfaceVariant) },
    )

    @Composable
    override fun Panel(theme: CamoTheme, style: MenuStyle, modifier: Modifier, content: @Composable () -> Unit) =
        Box(
            modifier
                .background(style.colors.container)
                .border(1.dp, theme.colors.outline)
                .widthIn(min = style.minWidth)
                .padding(style.contentPadding),
        ) { content() }
}

object DebugBottomSheetRenderer : BottomSheetRenderer {
    override fun defaults(theme: CamoTheme) = BottomSheetStyle(
        RectangleShape, 640.dp, PaddingValues(16.dp), true, DpSize(32.dp, 4.dp), theme.colors.outline,
        theme.typography.titleMedium, TitleAlign.Start, false, IconSource.Resource(Res.drawable.ic_delete),
        theme.colors.surface, SCRIM, WidePresentation.Sheet, StackedScrim.Each,
    )

    @Composable
    override fun Render(
        state: BottomSheetState,
        slots: BottomSheetSlots,
        theme: CamoTheme,
        style: BottomSheetStyle,
        modifier: Modifier,
    ) = Column(modifier.background(style.containerColor).border(1.dp, theme.colors.outline).fillMaxWidth()) {
        if (style.showHandle) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(style.handleSize).background(style.handleColor))
            }
        }
        state.title?.let { BasicText(it, Modifier.padding(horizontal = 16.dp), style.titleStyle) }
        Box(Modifier.padding(style.contentPadding)) { slots.content() }
    }
}

object DebugDatePickerRenderer : DatePickerRenderer {
    override fun defaults(theme: CamoTheme): DatePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return DatePickerStyle(
            DatePickerForm.Calendar, null, null, true, AllowTyping.Auto, false, 40.dp, 600.dp, PaddingValues(8.dp),
            t.titleSmall, t.labelSmall, t.bodyMedium, t.bodyLarge, 40.dp,
            IconSource.Resource(Res.drawable.ic_calendar), IconSource.Resource(Res.drawable.ic_chevron_start),
            IconSource.Resource(Res.drawable.ic_chevron),
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
    ) = Column(modifier.width(style.daySize * WEEK)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { slots.header() }
            slots.previous()
            slots.next()
        }
        if (state.choosingYear) {
            Box(Modifier.height(style.daySize * WEEK)) { slots.years() }
        } else {
            Row {
                state.weekdayLabels.forEach {
                    Box(Modifier.size(style.daySize), contentAlignment = Alignment.Center) {
                        BasicText(it, style = style.weekdayStyle)
                    }
                }
            }
            state.days.chunked(WEEK).forEach { week -> Row { week.forEach { slots.day(it) } } }
        }
    }

    @Composable
    override fun Cell(cell: CalendarCell, theme: CamoTheme, style: DatePickerStyle, modifier: Modifier) {
        val c = style.colors
        val box = Modifier.height(style.daySize).then(
            if (cell.kind == CalendarCellKind.Year) Modifier.fillMaxWidth() else Modifier.width(style.daySize),
        )
        if (!cell.inMonth && !style.showOutsideDays) {
            Box(modifier.then(box))
            return
        }
        Box(
            cell.behavior
                .then(modifier)
                .then(box)
                .then(if (cell.selected) Modifier.background(c.selected) else Modifier)
                .then(if (cell.isToday) Modifier.border(1.dp, c.today) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                cell.label,
                style = style.dayStyle.copy(
                    color = when {
                        cell.selected -> c.onSelected
                        cell.enabled -> c.day
                        else -> c.disabled
                    },
                ),
            )
        }
    }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    ) =
        Row(modifier.border(1.dp, theme.colors.outline), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wheels.forEach { Box(Modifier.width(72.dp)) { it() } }
        }
}

object DebugTimePickerRenderer : TimePickerRenderer {
    override fun defaults(theme: CamoTheme): TimePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return TimePickerStyle(
            TimePickerForm.Fields, true, t.headlineMedium, t.bodySmall, t.bodyLarge, 40.dp, 56.dp, 224.dp, 600.dp,
            PaddingValues(12.dp), IconSource.Resource(Res.drawable.ic_clock),
            TimePickerColors(c.primary, c.primaryContainer, c.surfaceContainerHigh, c.onSurface, c.onSurfaceVariant),
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
    ) = Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val wheels = slots.wheels
        if (wheels != null) {
            Wheels(wheels, theme, style, Modifier)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TimePart.Hour to slots.hour, TimePart.Minute to slots.minute).forEach { (part, slot) ->
                    val active = state.activePart == part
                    Box(
                        Modifier
                            .size(style.fieldSize)
                            .border(
                                if (active) 2.dp else 1.dp,
                                if (active) style.colors.active else theme.colors.outline,
                            ),
                        contentAlignment = Alignment.Center,
                    ) { slot() }
                }
                slots.period?.invoke()
            }
            slots.dial?.invoke()
        }
    }

    @Composable
    override fun Dial(state: DialState, theme: CamoTheme, style: TimePickerStyle, modifier: Modifier) {
        val c = style.colors
        Box(modifier.border(1.dp, theme.colors.outline)) {
            Canvas(Modifier.size(style.dialSize)) {
                val r = size.minDimension / 2
                val center = Offset(r, r)
                val angle = state.handAngle * PI / 180
                val tip = center + Offset((r * 0.8f * sin(angle)).toFloat(), (-r * 0.8f * cos(angle)).toFloat())
                drawLine(c.active, center, tip, strokeWidth = 2f)
            }
            val selected = state.labels.getOrElse(state.selectedIndex) { "" }
            BasicText(selected, Modifier.align(Alignment.Center), TextStyle(color = c.digits))
        }
    }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    ) =
        Row(modifier.border(1.dp, theme.colors.outline), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wheels.forEach { Box(Modifier.width(64.dp)) { it() } }
        }
}
