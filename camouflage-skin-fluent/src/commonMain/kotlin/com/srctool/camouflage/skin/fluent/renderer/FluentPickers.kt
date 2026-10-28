package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
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
import com.srctool.camouflage.core.datepicker.RangePosition
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.menu.CheckPosition
import com.srctool.camouflage.core.menu.MenuColors
import com.srctool.camouflage.core.menu.MenuRenderer
import com.srctool.camouflage.core.menu.MenuStyle
import com.srctool.camouflage.core.overlay.StackedScrim
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.timepicker.DialState
import com.srctool.camouflage.core.timepicker.TimePart
import com.srctool.camouflage.core.timepicker.TimePickerColors
import com.srctool.camouflage.core.timepicker.TimePickerForm
import com.srctool.camouflage.core.timepicker.TimePickerRenderer
import com.srctool.camouflage.core.timepicker.TimePickerSlots
import com.srctool.camouflage.core.timepicker.TimePickerState
import com.srctool.camouflage.core.timepicker.TimePickerStrings
import com.srctool.camouflage.core.timepicker.TimePickerStyle
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.rememberSkinIndication
import com.srctool.camouflage.skin.fluent.FluentTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Fluent's flyout, bottom drawer, CalendarView and TimePicker (base Fluent Skin §3.2), on the Solid surface
// until Acrylic arrives in M6: 4 px corners, a neutral stroke, brand for the selection.

private const val WEEK = 7
private const val DISABLED = 0.38f
private const val WHEEL_ROWS = 5
private const val OUTER_LABELS = 0.8f
private const val INNER_LABELS = 0.45f
private const val ICON_STROKE = 1.5f
private const val ICON_GRID = 24f
private const val RADIANS_PER_DEGREE = PI / 180
private val IconSize = 24.dp

internal object FluentMenuRenderer : MenuRenderer {
    override fun defaults(theme: CamoTheme): MenuStyle {
        val c = theme.colors
        return MenuStyle(
            shape = theme.shapes.small,
            minWidth = 128.dp,
            maxWidth = 300.dp,
            itemHeight = 32.dp,
            contentPadding = PaddingValues(theme.spacing.xs),
            textStyle = theme.typography.bodyMedium,
            iconSize = 20.dp,
            // shadow16: Fluent's flyout depth, where Acrylic applies.
            elevation = ElevationLevel.Level4,
            checkPosition = CheckPosition.Leading,
            colors = MenuColors(c.surface, c.onSurface, c.onSurfaceVariant, c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Panel(theme: CamoTheme, style: MenuStyle, modifier: Modifier, content: @Composable () -> Unit) {
        val stroke = theme.tokens<FluentTokens>()?.neutralStroke2 ?: theme.colors.outlineVariant
        val treatment = Camouflage.skin.surfaceTreatment.modifier(
            style.colors.container,
            style.shape,
            theme.elevation.of(style.elevation),
            theme,
        )
        Box(
            modifier
                .then(treatment)
                .border(1.dp, stroke, style.shape)
                .clip(style.shape)
                .widthIn(min = style.minWidth)
                .padding(style.contentPadding),
        ) {
            ProvideContentStyle(style.colors.text, Camouflage.iconStyle.copy(color = style.colors.icon), content)
        }
    }
}

internal object FluentBottomSheetRenderer : BottomSheetRenderer {
    override fun defaults(theme: CamoTheme): BottomSheetStyle {
        val c = theme.colors
        return BottomSheetStyle(
            shape = RectangleShape,
            maxWidth = 640.dp,
            contentPadding = PaddingValues(start = theme.spacing.xl, end = theme.spacing.xl, bottom = theme.spacing.xl),
            showHandle = false,
            handleSize = DpSize(32.dp, 4.dp),
            handleColor = c.outline,
            titleStyle = theme.typography.titleLarge,
            titleAlign = TitleAlign.Start,
            showCloseButton = true,
            closeIcon = IconSource.Vector(DismissIcon),
            containerColor = c.surface,
            scrimAlpha = fluentSmoke(theme),
            widePresentation = WidePresentation.Sheet,
            stackedScrim = StackedScrim.Each,
        )
    }

    @Composable
    override fun Render(
        state: BottomSheetState,
        slots: BottomSheetSlots,
        theme: CamoTheme,
        style: BottomSheetStyle,
        modifier: Modifier,
    ) {
        val shape = if (state.centered) theme.shapes.large else style.shape
        val treatment = Camouflage.skin.surfaceTreatment.modifier(
            style.containerColor,
            shape,
            theme.elevation.level5,
            theme,
        )
        Column(modifier.then(treatment).clip(shape).fillMaxWidth()) {
            if (style.showHandle && !state.centered) {
                Box(Modifier.fillMaxWidth().padding(vertical = theme.spacing.m), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(style.handleSize).background(style.handleColor, theme.shapes.full))
                }
            }
            val close = style.showCloseButton && state.dismissible
            if (state.title != null || close) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = theme.spacing.xl, end = theme.spacing.m, top = theme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val align = if (style.titleAlign == TitleAlign.Center) Alignment.Center else Alignment.CenterStart
                    Box(Modifier.weight(1f), contentAlignment = align) {
                        state.title?.let { CamoText(it, style = style.titleStyle, color = theme.colors.onSurface) }
                    }
                    if (close) {
                        CamoIconButton(
                            icon = { CamoIcon(style.closeIcon, null) },
                            contentDescription = "Close",
                            onClick = state.onDismissRequest,
                            appearance = Appearance.Ghost,
                        )
                    }
                }
            }
            Box(Modifier.padding(style.contentPadding).padding(top = theme.spacing.m)) { slots.content() }
        }
    }
}

internal object FluentDatePickerRenderer : DatePickerRenderer {
    override fun defaults(theme: CamoTheme): DatePickerStyle {
        val c = theme.colors
        val t = theme.typography
        val fluent = theme.tokens<FluentTokens>()
        return DatePickerStyle(
            form = DatePickerForm.Calendar,
            firstDayOfWeek = null,
            displayFormat = null,
            confirmButton = true,
            allowTyping = AllowTyping.Auto,
            showOutsideDays = true,
            daySize = 32.dp,
            autoPopupMinWidth = 600.dp,
            contentPadding = PaddingValues(theme.spacing.m),
            headerStyle = t.titleSmall,
            weekdayStyle = t.labelSmall,
            dayStyle = t.bodySmall,
            wheelTextStyle = t.bodyMedium,
            wheelItemHeight = 32.dp,
            calendarIcon = IconSource.Vector(FluentCalendarIcon),
            previousIcon = IconSource.Vector(FluentChevronStart),
            nextIcon = IconSource.Vector(FluentChevronEnd),
            colors = DatePickerColors(
                selected = fluent?.brandBackground ?: c.primary,
                onSelected = fluent?.neutralForegroundOnBrand ?: c.onPrimary,
                range = c.surfaceContainerHigh,
                onRange = c.onSurface,
                today = c.primary,
                day = c.onSurface,
                weekday = c.onSurfaceVariant,
                disabled = c.onSurface.copy(alpha = DISABLED),
            ),
            strings = DatePickerStrings(),
        )
    }

    @Composable
    override fun Calendar(
        state: CalendarState,
        slots: CalendarSlots,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    ) {
        Column(modifier.width(style.daySize * WEEK)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            CamoText(it, style = style.weekdayStyle, color = style.colors.weekday)
                        }
                    }
                }
                state.days.chunked(WEEK).forEach { week -> Row { week.forEach { slots.day(it) } } }
            }
        }
    }

    @Composable
    override fun Cell(cell: CalendarCell, theme: CamoTheme, style: DatePickerStyle, modifier: Modifier) {
        val c = style.colors
        val year = cell.kind == CalendarCellKind.Year
        val size = if (year) Modifier.fillMaxWidth().height(style.daySize) else Modifier.size(style.daySize)
        if (!cell.inMonth && !style.showOutsideDays) {
            Box(modifier.then(size))
            return
        }
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val band = cell.rangePosition
        val text = cellText(cell, c)
        val shape = theme.shapes.small
        Box(
            cell.behavior
                .then(modifier)
                .then(size)
                .rangeBand(band, c.range, rtl),
            contentAlignment = Alignment.Center,
        ) {
            val mark = Modifier
                .then(if (year) Modifier.padding(horizontal = 2.dp).fillMaxWidth() else Modifier.size(style.daySize))
                .height(style.daySize)
                .clip(shape)
                .then(if (cell.selected) Modifier.background(c.selected) else Modifier)
                .then(if (cell.isToday && !cell.selected) Modifier.border(1.dp, c.today, shape) else Modifier)
                .indication(cell.interactionSource, rememberSkinIndication(text, shape))
            Box(mark, contentAlignment = Alignment.Center) {
                CamoText(cell.label, style = style.dayStyle, color = text)
            }
        }
    }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    ) {
        FluentWheelBand(wheels, style.wheelItemHeight, theme, modifier)
    }
}

// The range band behind a day: all of a middle day, the inner half of the range's ends (mirrored right to left).
private fun Modifier.rangeBand(position: RangePosition, color: Color, rtl: Boolean): Modifier = drawBehind {
    if (position == RangePosition.None || position == RangePosition.Single) return@drawBehind
    val half = size.width / 2
    val (left, width) = when (position) {
        RangePosition.Middle -> 0f to size.width
        RangePosition.Start -> (if (rtl) 0f else half) to half
        else -> (if (rtl) half else 0f) to half
    }
    drawRect(color, Offset(left, 0f), Size(width, size.height))
}

// A cell's label color: on the selection, dimmed when disabled, quieter for another month, brand for today.
private fun cellText(cell: CalendarCell, c: DatePickerColors): Color = when {
    cell.selected -> c.onSelected
    !cell.enabled -> c.disabled
    !cell.inMonth -> c.weekday
    cell.isToday -> c.today
    else -> c.day
}

@Composable
private fun FluentWheelBand(
    wheels: List<@Composable () -> Unit>,
    itemHeight: Dp,
    theme: CamoTheme,
    modifier: Modifier,
) {
    val fluent = theme.tokens<FluentTokens>()
    Box(modifier.height(itemHeight * WHEEL_ROWS), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(fluent?.subtleBackgroundHover ?: theme.colors.surfaceContainerHigh, theme.shapes.small),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.xs)) {
            wheels.forEach { Box(Modifier.width(64.dp)) { it() } }
        }
    }
}

internal object FluentTimePickerRenderer : TimePickerRenderer {
    override fun defaults(theme: CamoTheme): TimePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return TimePickerStyle(
            form = TimePickerForm.Fields,
            confirmButton = true,
            digitStyle = t.headlineSmall,
            dialTextStyle = t.bodySmall,
            wheelTextStyle = t.bodyMedium,
            wheelItemHeight = 32.dp,
            fieldSize = 48.dp,
            dialSize = 224.dp,
            autoPopupMinWidth = 600.dp,
            contentPadding = PaddingValues(theme.spacing.l),
            clockIcon = IconSource.Vector(FluentClockIcon),
            colors = TimePickerColors(
                active = c.primary,
                activeContainer = c.surface,
                face = c.surface,
                digits = c.onSurface,
                period = c.onSurfaceVariant,
            ),
            strings = TimePickerStrings(),
        )
    }

    @Composable
    override fun Render(
        state: TimePickerState,
        slots: TimePickerSlots,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    ) {
        val wheels = slots.wheels
        if (wheels != null) {
            Wheels(wheels, theme, style, modifier)
            return
        }
        val c = style.colors
        val stroke = theme.tokens<FluentTokens>()?.neutralStroke1 ?: theme.colors.outlineVariant
        val shape = theme.shapes.small
        @Composable
        fun Part(part: TimePart, content: @Composable () -> Unit) {
            val active = state.activePart == part
            Box(
                Modifier
                    .size(style.fieldSize)
                    .background(if (active) c.activeContainer else c.face, shape)
                    .border(1.dp, stroke, shape)
                    .drawBehind {
                        // Fluent's bottom accent: the stronger neutral stroke, brand and 2 dp on the active part.
                        val width = (if (active) 2.dp else 1.dp).toPx()
                        drawRect(
                            if (active) c.active else theme.colors.outline,
                            Offset(0f, size.height - width),
                            Size(size.width, width),
                        )
                    }
                    .alpha(if (state.enabled) 1f else DISABLED),
                contentAlignment = Alignment.Center,
            ) { content() }
        }
        Column(
            modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(theme.spacing.l),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(theme.spacing.s),
            ) {
                Part(TimePart.Hour, slots.hour)
                CamoText(":", style = style.digitStyle, color = c.digits)
                Part(TimePart.Minute, slots.minute)
                slots.period?.let { Box(Modifier.padding(start = theme.spacing.s)) { it() } }
            }
            slots.dial?.invoke()
        }
    }

    @Composable
    override fun Dial(state: DialState, theme: CamoTheme, style: TimePickerStyle, modifier: Modifier) {
        val measurer = rememberTextMeasurer()
        val c = style.colors
        val face = theme.colors.surfaceContainerHigh
        val onActive = theme.tokens<FluentTokens>()?.neutralForegroundOnBrand ?: theme.colors.onPrimary
        Canvas(modifier) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(face, radius, center)
            fun at(angle: Float, r: Float) = center + Offset(
                (r * sin(angle * RADIANS_PER_DEGREE)).toFloat(),
                (-r * cos(angle * RADIANS_PER_DEGREE)).toFloat(),
            )
            val tip = at(state.handAngle, radius * if (state.selectedInner) INNER_LABELS else OUTER_LABELS)
            drawLine(c.active, center, tip, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(c.active, 3.dp.toPx(), center)
            drawCircle(c.active, 16.dp.toPx(), tip)
            fun ring(labels: List<String>, r: Float, inner: Boolean) {
                labels.forEachIndexed { index, label ->
                    val selected = index == state.selectedIndex && inner == state.selectedInner
                    val color = if (selected) onActive else c.digits
                    val layout = measurer.measure(label, style.dialTextStyle.copy(color = color))
                    val p = at(index * 30f, r)
                    drawText(layout, topLeft = p - Offset(layout.size.width / 2f, layout.size.height / 2f))
                }
            }
            ring(state.labels, radius * OUTER_LABELS, inner = false)
            state.innerLabels?.let { ring(it, radius * INNER_LABELS, inner = true) }
        }
    }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    ) {
        FluentWheelBand(wheels, style.wheelItemHeight, theme, modifier)
    }
}

private fun icon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, IconSize, IconSize, ICON_GRID, ICON_GRID)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE,
            strokeLineCap = StrokeCap.Round,
            pathBuilder = block,
        )
        .build()

internal val FluentCalendarIcon: ImageVector = icon("calendar") {
    moveTo(4.5f, 5.5f); lineTo(19.5f, 5.5f); lineTo(19.5f, 19.5f); lineTo(4.5f, 19.5f); close()
    moveTo(4.5f, 9.5f); lineTo(19.5f, 9.5f)
}

internal val FluentClockIcon: ImageVector = icon("clock") {
    moveTo(20f, 12f)
    arcTo(8f, 8f, 0f, true, true, 4f, 12f)
    arcTo(8f, 8f, 0f, true, true, 20f, 12f)
    moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 12f)
}

internal val FluentChevronStart: ImageVector = icon("chevron-start") {
    moveTo(14f, 6f); lineTo(8f, 12f); lineTo(14f, 18f)
}

internal val FluentChevronEnd: ImageVector = icon("chevron-end") { moveTo(10f, 6f); lineTo(16f, 12f); lineTo(10f, 18f) }

internal val DismissIcon: ImageVector = icon("dismiss") {
    moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f)
}
