package com.srctool.camouflage.skin.minimal.renderer

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
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
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.menu.CheckPosition
import com.srctool.camouflage.core.menu.MenuColors
import com.srctool.camouflage.core.menu.MenuItemSlots
import com.srctool.camouflage.core.menu.MenuItemState
import com.srctool.camouflage.core.menu.MenuRenderer
import com.srctool.camouflage.core.menu.MenuStyle
import com.srctool.camouflage.core.overlay.StackedScrim
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.timepicker.DIAL_INNER_RING
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
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Minimal's overlays and pickers (base Minimal Skin §3.3): flat surfaces with a hairline, ink for the selection.

private const val WEEK = 7
private const val DISABLED = 0.38f
private const val SCRIM = 0.4f
private const val WHEEL_ROWS = 5
private const val OUTER_LABELS = 0.8f
private const val INNER_LABELS = 0.45f
private const val RADIANS_PER_DEGREE = PI / 180

internal object MinimalMenuRenderer : MenuRenderer {
    override fun defaults(theme: CamoTheme): MenuStyle {
        val c = theme.colors
        return MenuStyle(
            shape = theme.shapes.medium,
            minWidth = 112.dp,
            maxWidth = 280.dp,
            itemHeight = 40.dp,
            contentPadding = PaddingValues(vertical = theme.spacing.xs),
            textStyle = theme.typography.bodyMedium,
            iconSize = 18.dp,
            elevation = ElevationLevel.Level0,
            checkPosition = CheckPosition.Trailing,
            checkIcon = IconSource.Vector(CheckIcon),
            colors = MenuColors(c.surface, c.onSurface, c.onSurfaceVariant, c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Item(
        state: MenuItemState,
        slots: MenuItemSlots,
        theme: CamoTheme,
        style: MenuStyle,
        modifier: Modifier,
    ) =
        PaintMenuItem(state, slots, theme, style, modifier, RectangleShape)

    @Composable
    override fun Panel(theme: CamoTheme, style: MenuStyle, modifier: Modifier, content: @Composable () -> Unit) {
        val treatment = Camouflage.skin.surfaceTreatment.modifier(
            style.colors.container,
            style.shape,
            theme.elevation.of(style.elevation),
            theme,
        )
        Box(
            modifier
                .then(treatment)
                .border(1.dp, theme.colors.outlineVariant, style.shape)
                .clip(style.shape)
                .widthIn(min = style.minWidth)
                .padding(style.contentPadding),
        ) {
            ProvideContentStyle(style.colors.text, Camouflage.iconStyle.copy(color = style.colors.icon), content)
        }
    }
}

internal object MinimalBottomSheetRenderer : BottomSheetRenderer {
    override fun defaults(theme: CamoTheme): BottomSheetStyle {
        val c = theme.colors
        val corner = 16.dp
        return BottomSheetStyle(
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
            maxWidth = 640.dp,
            contentPadding = PaddingValues(start = theme.spacing.l, end = theme.spacing.l, bottom = theme.spacing.xl),
            showHandle = true,
            handleSize = DpSize(36.dp, 4.dp),
            handleColor = c.outlineVariant,
            titleStyle = theme.typography.titleLarge,
            titleAlign = TitleAlign.Start,
            showCloseButton = false,
            closeIcon = IconSource.Vector(ClearIcon),
            containerColor = c.surface,
            scrimAlpha = SCRIM,
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
        val treatment = Camouflage.skin.surfaceTreatment.modifier(style.containerColor, shape, 0.dp, theme)
        Column(
            modifier
                .then(treatment)
                .border(1.dp, theme.colors.outlineVariant, shape)
                .clip(shape)
                .fillMaxWidth(),
        ) {
            SheetHeader(state, style, theme)
            Box(Modifier.padding(style.contentPadding)) { slots.content() }
        }
    }
}

// Handle, title and close button, in the combinations the sheet theme allows.
@Composable
private fun SheetHeader(state: BottomSheetState, style: BottomSheetStyle, theme: CamoTheme) {
    if (style.showHandle && !state.centered) {
        Box(Modifier.fillMaxWidth().padding(vertical = theme.spacing.m), contentAlignment = Alignment.Center) {
            Box(Modifier.size(style.handleSize).background(style.handleColor, CircleShape))
        }
    }
    val close = style.showCloseButton && state.dismissible
    if (state.title == null && !close) return
    Row(
        Modifier.fillMaxWidth().padding(start = theme.spacing.l, end = theme.spacing.s, top = theme.spacing.s),
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
            )
        }
    }
}

internal object MinimalDatePickerRenderer : DatePickerRenderer {
    override fun defaults(theme: CamoTheme): DatePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return DatePickerStyle(
            form = DatePickerForm.Calendar,
            firstDayOfWeek = null,
            displayFormat = null,
            confirmButton = true,
            allowTyping = AllowTyping.Auto,
            showOutsideDays = false,
            daySize = 36.dp,
            autoPopupMinWidth = 600.dp,
            contentPadding = PaddingValues(theme.spacing.m),
            headerStyle = t.titleSmall,
            weekdayStyle = t.labelSmall,
            dayStyle = t.bodyMedium,
            wheelTextStyle = t.bodyLarge,
            wheelItemHeight = 40.dp,
            calendarIcon = IconSource.Vector(CalendarIcon),
            previousIcon = IconSource.Vector(ChevronStart),
            nextIcon = IconSource.Vector(ChevronEnd),
            colors = DatePickerColors(
                selected = c.primary,
                onSelected = c.onPrimary,
                range = c.surfaceContainerHigh,
                onRange = c.onSurface,
                today = c.outline,
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
        CalendarCellBox(cell, style, modifier, CircleShape)
    }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: DatePickerStyle,
        modifier: Modifier,
    ) {
        WheelBand(wheels, style.wheelItemHeight, theme.colors.surfaceContainerHigh, theme, modifier)
    }
}

/**
 * A calendar cell: the range band behind (half of it on the range's ends), the selected fill or today's ring
 * in [shape], and the label; blank for another month's day unless `showOutsideDays`.
 */
@Composable
internal fun CalendarCellBox(cell: CalendarCell, style: DatePickerStyle, modifier: Modifier, shape: Shape) {
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
    val cellShape = if (year) RoundedCornerShape(50) else shape
    Box(
        cell.behavior
            .then(modifier)
            .then(size)
            .rangeBand(band, c.range, rtl),
        contentAlignment = Alignment.Center,
    ) {
        val mark = Modifier
            .then(if (year) Modifier.fillMaxWidth().padding(horizontal = 4.dp) else Modifier)
            .height(style.daySize)
            .width(if (year) style.daySize * 2 else style.daySize)
            .clip(cellShape)
            .then(if (cell.selected) Modifier.background(c.selected) else Modifier)
            .then(if (cell.isToday && !cell.selected) Modifier.border(1.dp, c.today, cellShape) else Modifier)
            .indication(cell.interactionSource, rememberSkinIndication(text, cellShape))
        Box(mark, contentAlignment = Alignment.Center) {
            CamoText(cell.label, style = style.dayStyle, color = text)
        }
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

// A cell's label color: on the selection, dimmed when disabled, on the band, quieter for another month.
private fun cellText(cell: CalendarCell, c: DatePickerColors): Color = when {
    cell.selected -> c.onSelected
    !cell.enabled -> c.disabled
    cell.rangePosition == RangePosition.Middle -> c.onRange
    !cell.inMonth -> c.weekday
    else -> c.day
}

/** Wheels side by side over a [band] behind the middle row. */
@Composable
internal fun WheelBand(
    wheels: List<@Composable () -> Unit>,
    itemHeight: androidx.compose.ui.unit.Dp,
    band: Color,
    theme: CamoTheme,
    modifier: Modifier,
) {
    Box(modifier.height(itemHeight * WHEEL_ROWS), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemHeight).background(band, theme.shapes.medium))
        Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.s)) {
            wheels.forEach { Box(Modifier.width(72.dp)) { it() } }
        }
    }
}

internal object MinimalTimePickerRenderer : TimePickerRenderer {
    override fun defaults(theme: CamoTheme): TimePickerStyle {
        val c = theme.colors
        val t = theme.typography
        return TimePickerStyle(
            form = TimePickerForm.Fields,
            confirmButton = true,
            digitStyle = t.displaySmall,
            dialTextStyle = t.bodyMedium,
            wheelTextStyle = t.bodyLarge,
            wheelItemHeight = 40.dp,
            fieldSize = 56.dp,
            dialSize = 240.dp,
            autoPopupMinWidth = 600.dp,
            contentPadding = PaddingValues(theme.spacing.l),
            clockIcon = IconSource.Vector(ClockIcon),
            colors = TimePickerColors(
                active = c.primary,
                activeContainer = c.surfaceContainerHigh,
                face = c.surfaceContainerHigh,
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
        Column(
            modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(theme.spacing.l),
        ) {
            TimeBoxes(state, slots, theme, style, theme.shapes.medium, ring = 2.dp)
            slots.dial?.invoke()
        }
    }

    @Composable
    override fun Dial(state: DialState, theme: CamoTheme, style: TimePickerStyle, modifier: Modifier) {
        DialFace(state, style, theme.colors.onPrimary, modifier)
    }

    @Composable
    override fun Wheels(
        wheels: List<@Composable () -> Unit>,
        theme: CamoTheme,
        style: TimePickerStyle,
        modifier: Modifier,
    ) {
        WheelBand(wheels, style.wheelItemHeight, theme.colors.surfaceContainerHigh, theme, modifier)
    }
}

/** The hour and minute boxes with a colon, then the period; the active part's box is ringed in `active`. */
@Composable
internal fun TimeBoxes(
    state: TimePickerState,
    slots: TimePickerSlots,
    theme: CamoTheme,
    style: TimePickerStyle,
    shape: Shape,
    ring: androidx.compose.ui.unit.Dp,
) {
    val c = style.colors
    @Composable
    fun Part(part: TimePart, content: @Composable () -> Unit) {
        val active = state.activePart == part
        Box(
            Modifier
                .size(style.fieldSize)
                .background(if (active) c.activeContainer else c.face, shape)
                .then(if (active) Modifier.border(ring, c.active, shape) else Modifier)
                .alpha(if (state.enabled) 1f else DISABLED),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.spacing.s)) {
        Part(TimePart.Hour, slots.hour)
        CamoText(":", style = style.digitStyle, color = c.digits)
        Part(TimePart.Minute, slots.minute)
        slots.period?.let { Box(Modifier.padding(start = theme.spacing.s)) { it() } }
    }
}

/** A clock face: the face circle, the hand with a dot under the selected label, and the labels on their rings. */
@Composable
internal fun DialFace(state: DialState, style: TimePickerStyle, onActive: Color, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val c = style.colors
    val textStyle: TextStyle = style.dialTextStyle
    Canvas(modifier) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(c.face, radius, center)
        fun at(angle: Float, r: Float) = center + Offset(
            (r * sin(angle * RADIANS_PER_DEGREE)).toFloat(),
            (-r * cos(angle * RADIANS_PER_DEGREE)).toFloat(),
        )
        val handRadius = radius * if (state.selectedInner) INNER_LABELS else OUTER_LABELS
        val knob = 18.dp.toPx()
        val tip = at(state.handAngle, handRadius)
        drawLine(c.active, center, tip, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(c.active, 3.dp.toPx(), center)
        drawCircle(c.active, knob, tip)
        fun ring(labels: List<String>, r: Float, inner: Boolean) {
            labels.forEachIndexed { index, label ->
                val selected = index == state.selectedIndex && inner == state.selectedInner
                val layout = measurer.measure(label, textStyle.copy(color = if (selected) onActive else c.digits))
                val p = at(index * 30f, r)
                drawText(layout, topLeft = p - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
        ring(state.labels, radius * OUTER_LABELS, inner = false)
        state.innerLabels?.let { ring(it, radius * INNER_LABELS, inner = true) }
        check(INNER_LABELS < DIAL_INNER_RING)
    }
}

internal val CalendarIcon: ImageVector = ImageVector.Builder("calendar", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(5f, 6f); lineTo(19f, 6f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(5f, 10f); lineTo(19f, 10f)
        moveTo(9f, 3.5f); lineTo(9f, 7f)
        moveTo(15f, 3.5f); lineTo(15f, 7f)
    }
    .build()

internal val ClockIcon: ImageVector = ImageVector.Builder("clock", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(20f, 12f)
        arcTo(8f, 8f, 0f, true, true, 4f, 12f)
        arcTo(8f, 8f, 0f, true, true, 20f, 12f)
        moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
    }
    .build()

// Chevrons drawn for left-to-right; core swaps previous and next for right to left.
internal val ChevronStart: ImageVector = ImageVector.Builder("chevron-start", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
    }
    .build()

internal val ChevronEnd: ImageVector = ImageVector.Builder("chevron-end", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
    }
    .build()

/**
 * A menu row: the check column (leading or trailing, per `style.checkPosition`), the icon column, the text and
 * the shortcut, in the item's tone, with the skin's feedback in [shape] for the highlight.
 */
@Composable
internal fun PaintMenuItem(
    state: MenuItemState,
    slots: MenuItemSlots,
    theme: CamoTheme,
    style: MenuStyle,
    modifier: Modifier,
    shape: Shape,
) {
    val alpha = if (state.enabled) 1f else DISABLED
    val toned = state.tone != Tone.Default
    val text = (if (toned) theme.colors.family(state.tone).strong else style.colors.text).disabled(alpha)
    val icon = (if (toned) text else style.colors.icon).disabled(alpha)
    val gap = theme.spacing.m
    @Composable
    fun Check(show: Boolean) {
        Box(Modifier.padding(end = gap).size(style.iconSize)) {
            if (show) CamoIcon(style.checkIcon, null, tint = text, size = style.iconSize)
        }
    }
    Row(
        modifier
            .indication(state.interactionSource, rememberSkinIndication(style.colors.text, shape))
            .fillMaxWidth()
            .heightIn(min = style.itemHeight)
            .padding(horizontal = gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideContentStyle(text, IconStyle(style.iconSize, icon)) {
            val leadingCheck = style.checkPosition == CheckPosition.Leading
            if (leadingCheck && state.reserveCheck) Check(state.checked == true)
            if (state.reserveIcon) {
                Box(Modifier.padding(end = gap).size(style.iconSize), contentAlignment = Alignment.Center) {
                    slots.leadingIcon?.invoke()
                }
            }
            BasicText(
                state.text,
                Modifier.weight(1f).padding(end = gap),
                style = style.textStyle.copy(color = text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state.trailingText?.let {
                BasicText(it, style = style.textStyle.copy(color = style.colors.trailingText.disabled(alpha)))
            }
            if (!leadingCheck && state.checked == true) {
                CamoIcon(style.checkIcon, null, Modifier.padding(start = gap), tint = text, size = style.iconSize)
            }
        }
    }
}

internal val CheckIcon: ImageVector = ImageVector.Builder("check", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(5f, 12.5f)
        lineTo(10f, 17.5f)
        lineTo(19f, 7f)
    }
    .build()
