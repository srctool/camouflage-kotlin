package com.srctool.camouflage.skin.cupertino.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.breadcrumb.BreadcrumbColors
import com.srctool.camouflage.core.breadcrumb.BreadcrumbItemState
import com.srctool.camouflage.core.breadcrumb.BreadcrumbRenderer
import com.srctool.camouflage.core.breadcrumb.BreadcrumbSlots
import com.srctool.camouflage.core.breadcrumb.BreadcrumbState
import com.srctool.camouflage.core.breadcrumb.BreadcrumbStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.image.ImagePhase
import com.srctool.camouflage.core.image.ImageColors
import com.srctool.camouflage.core.image.ImageRenderer
import com.srctool.camouflage.core.image.ImageState
import com.srctool.camouflage.core.image.ImageStyle
import com.srctool.camouflage.core.pagination.PageArrowState
import com.srctool.camouflage.core.pagination.PageButtonState
import com.srctool.camouflage.core.pagination.PageDirection
import com.srctool.camouflage.core.pagination.PageDotState
import com.srctool.camouflage.core.pagination.PaginationColors
import com.srctool.camouflage.core.pagination.PaginationRenderer
import com.srctool.camouflage.core.pagination.PaginationState
import com.srctool.camouflage.core.pagination.PaginationStyle
import com.srctool.camouflage.core.progresssteps.ProgressStepsRenderer
import com.srctool.camouflage.core.progresssteps.StepItemState
import com.srctool.camouflage.core.progresssteps.StepSlots
import com.srctool.camouflage.core.progresssteps.StepStatus
import com.srctool.camouflage.core.progresssteps.StepsColors
import com.srctool.camouflage.core.progresssteps.StepsOrientation
import com.srctool.camouflage.core.progresssteps.StepsStyle
import com.srctool.camouflage.core.rating.RatingColors
import com.srctool.camouflage.core.rating.RatingRenderer
import com.srctool.camouflage.core.rating.RatingState
import com.srctool.camouflage.core.rating.RatingStyle
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication

// Cupertino's image, rating, pagination, breadcrumb and progress steps (base Cupertino Skin §3.2): 12-radius images on
// a gray well, accent stars, a page control of dots, a path control with chevrons, numbered circles for steps.

private val Hairline = 0.5.dp
private const val DISABLED_CONTENT = 0.38f
private const val FOCUS_WIDTH = 2
private val IconSize = 24.dp

internal object CupertinoImageRenderer : ImageRenderer {
    override fun defaults(theme: CamoTheme) = ImageStyle(
        placeholderIcon = IconSource.Vector(ImageGlyph),
        errorIcon = IconSource.Vector(BrokenImageGlyph),
        fadeMillis = 150,
        borderWidth = 0.dp,
        colors = ImageColors(theme.colors.surfaceContainerHigh, theme.colors.onSurfaceVariant,
            theme.colors.outlineVariant),
    )

    @Composable
    override fun Frame(
        state: ImageState,
        theme: CamoTheme,
        style: ImageStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) {
        val c = style.colors
        val framed = if (style.borderWidth > 0.dp) Modifier.border(style.borderWidth, c.border,
            state.shape) else Modifier
        Box(modifier.clip(state.shape).background(c.placeholder).then(framed), contentAlignment = Alignment.Center) {
            content()
            if (state.phase != ImagePhase.Loaded) {
                val icon = if (state.phase == ImagePhase.Failed) style.errorIcon else style.placeholderIcon
                CamoIcon(icon, contentDescription = null, size = IconSize, tint = c.icon)
            }
        }
    }
}

internal object CupertinoRatingRenderer : RatingRenderer {
    override fun defaults(theme: CamoTheme): RatingStyle {
        val c = theme.colors
        return RatingStyle(
            icon = IconSource.Vector(StarGlyph),
            smallSize = 16.dp,
            mediumSize = 20.dp,
            largeSize = 28.dp,
            gap = 4.dp,
            colors = RatingColors(c.primary, c.outlineVariant, c.primary.copy(alpha = HOVER_ALPHA),
                c.onSurface.disabled(DISABLED_CONTENT)),
        )
    }

    @Composable
    override fun Render(state: RatingState, theme: CamoTheme, style: RatingStyle, modifier: Modifier) {
        val size = style.sizeOf(state.size)
        val shown = state.hoverValue ?: state.value
        val filled = when {
            !state.enabled -> style.colors.disabled
            state.hoverValue != null -> style.colors.hover
            else -> style.colors.filled
        }
        val ring = if (state.interaction.focused) Modifier.border(FOCUS_WIDTH.dp, style.colors.filled,
            RoundedCornerShape(4.dp)) else Modifier
        Row(modifier.then(ring), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until state.max) {
                val fill = (shown - i).coerceIn(0f, 1f)
                Box(Modifier.padding(horizontal = style.gap / 2).size(size)) {
                    CamoIcon(style.icon, contentDescription = null, size = size, tint = style.colors.empty)
                    if (fill > 0f) {
                        Box(Modifier.size(size).clip(fraction(fill))) {
                            CamoIcon(style.icon, contentDescription = null, size = size, tint = filled)
                        }
                    }
                }
            }
        }
    }
}

private const val HOVER_ALPHA = 0.6f

// The part of an icon that is filled: [fill] of its width from the start, mirrored in right-to-left layouts.
private fun fraction(fill: Float) = GenericShape { size, direction ->
    val width = size.width * fill
    addRect(if (direction == LayoutDirection.Rtl) Rect(size.width - width, 0f, size.width, size.height) else Rect(0f,
        0f, width, size.height))
}

internal object CupertinoPaginationRenderer : PaginationRenderer {
    override fun defaults(theme: CamoTheme): PaginationStyle {
        val c = theme.colors
        return PaginationStyle(
            buttonSize = 32.dp,
            shape = theme.shapes.full,
            dotSize = 8.dp,
            gap = 4.dp,
            textStyle = theme.typography.labelLarge,
            previousIcon = IconSource.Vector(ChevronLeftGlyph),
            nextIcon = IconSource.Vector(ChevronRightGlyph),
            colors = PaginationColors(c.primaryContainer, c.primary, c.onSurface, c.outline, c.onSurface),
        )
    }

    @Composable
    override fun Container(
        state: PaginationState,
        theme: CamoTheme,
        style: PaginationStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        Row(modifier, Arrangement.spacedBy(style.gap), Alignment.CenterVertically, content = content)
    }

    @Composable
    override fun Button(state: PageButtonState, label: String, theme: CamoTheme, style: PaginationStyle,
        modifier: Modifier) {
        val c = style.colors
        val content = when {
            !state.enabled -> c.content.disabled(DISABLED_CONTENT)
            state.current -> c.currentContent
            else -> c.content
        }
        Box(
            modifier
                .size(style.buttonSize)
                .clip(style.shape)
                .background(if (state.current) c.currentContainer else Color.Transparent)
                .indication(state.interactionSource, rememberSkinIndication(content, style.shape)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(label, style = style.textStyle.copy(color = content,
                fontWeight = if (state.current) FontWeight.SemiBold else null))
        }
    }

    @Composable
    override fun Arrow(state: PageArrowState, theme: CamoTheme, style: PaginationStyle, modifier: Modifier) {
        val content = if (state.enabled) style.colors.content else style.colors.content.disabled(DISABLED_CONTENT)
        Box(
            modifier
                .size(style.buttonSize)
                .clip(style.shape)
                .indication(state.interactionSource, rememberSkinIndication(content, style.shape)),
            contentAlignment = Alignment.Center,
        ) {
            val icon = if (state.direction == PageDirection.Previous) style.previousIcon else style.nextIcon
            CamoIcon(icon, contentDescription = null, size = IconSize, tint = content)
        }
    }

    @Composable
    override fun Gap(theme: CamoTheme, style: PaginationStyle) {
        Box(Modifier.size(style.buttonSize), contentAlignment = Alignment.Center) {
            BasicText("…", style = style.textStyle.copy(color = style.colors.content))
        }
    }

    @Composable
    override fun Dot(state: PageDotState, theme: CamoTheme, style: PaginationStyle, modifier: Modifier) {
        val color = if (state.active) style.colors.activeDot else style.colors.dot
        Box(modifier.size(style.buttonSize), contentAlignment = Alignment.Center) {
            val shown = if (state.enabled) color else color.disabled(DISABLED_CONTENT)
            Box(Modifier.size(style.dotSize).clip(CircleShape).background(shown))
        }
    }

    @Composable
    override fun Summary(text: String, theme: CamoTheme, style: PaginationStyle) {
        BasicText(text, Modifier.padding(horizontal = style.gap),
            style = style.textStyle.copy(color = style.colors.content))
    }
}

internal object CupertinoBreadcrumbRenderer : BreadcrumbRenderer {
    override fun defaults(theme: CamoTheme): BreadcrumbStyle {
        val c = theme.colors
        return BreadcrumbStyle(
            separator = IconSource.Vector(ChevronRightGlyph, autoMirror = true),
            itemPadding = 4.dp,
            overflowIcon = IconSource.Vector(MoreIcon),
            iconSize = 16.dp,
            textStyle = theme.typography.bodyMedium,
            colors = BreadcrumbColors(c.primary, c.onSurface, c.outline, c.onSurface),
        )
    }

    @Composable
    override fun Container(
        state: BreadcrumbState,
        theme: CamoTheme,
        style: BreadcrumbStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, content = content)
    }

    @Composable
    override fun Item(
        state: BreadcrumbItemState,
        slots: BreadcrumbSlots,
        theme: CamoTheme,
        style: BreadcrumbStyle,
        modifier: Modifier,
    ) {
        val c = style.colors
        val color = when {
            state.current -> c.current
            state.interaction.hovered -> c.hover
            else -> c.link
        }
        val shape = theme.shapes.small
        Row(
            modifier.clip(shape).indication(state.interactionSource, rememberSkinIndication(color,
                shape)).padding(horizontal = style.itemPadding),
            Arrangement.spacedBy(style.itemPadding),
            Alignment.CenterVertically,
        ) {
            ProvideContentStyle(color, IconStyle(style.iconSize, color)) {
                slots.icon?.invoke()
                BasicText(
                    slots.label,
                    style = style.textStyle.copy(color = color,
                        fontWeight = if (state.current) FontWeight.SemiBold else null),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    override fun Separator(theme: CamoTheme, style: BreadcrumbStyle) {
        CamoIcon(style.separator, contentDescription = null, size = style.iconSize, tint = style.colors.separator)
    }
}

internal object CupertinoProgressStepsRenderer : ProgressStepsRenderer {
    override fun defaults(theme: CamoTheme): StepsStyle {
        val c = theme.colors
        return StepsStyle(
            markerSize = 24.dp,
            connectorWidth = 1.dp,
            showNumbers = true,
            checkIcon = IconSource.Vector(CheckGlyph),
            errorIcon = IconSource.Vector(AlertGlyph),
            labelStyle = theme.typography.labelLarge,
            descriptionStyle = theme.typography.bodySmall,
            colors = StepsColors(c.primary, c.onPrimary, c.primary, c.onPrimary, c.outlineVariant, c.error,
                c.outlineVariant, c.onSurface, c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Step(state: StepItemState, slots: StepSlots, theme: CamoTheme, style: StepsStyle, modifier: Modifier) {
        val c = style.colors
        val connector = if (state.status == StepStatus.Complete) c.complete else c.connector
        val shape = theme.shapes.small
        val textColor = if (state.status == StepStatus.Upcoming) c.mutedText else c.text
        val interactive = modifier.clip(shape).indication(state.interactionSource, rememberSkinIndication(textColor,
            shape))
        val labels: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    slots.label,
                    style = style.labelStyle.copy(color = textColor,
                        fontWeight = if (state.status == StepStatus.Current) FontWeight.SemiBold else null),
                )
                slots.description?.let { BasicText(it, style = style.descriptionStyle.copy(color = c.mutedText)) }
            }
        }
        if (state.orientation == StepsOrientation.Horizontal) {
            Column(interactive.padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Marker(state, style)
                    if (!state.last) {
                        Box(
                            Modifier.weight(1f).padding(horizontal = 6.dp).height(style.connectorWidth)
                                .background(connector),
                        )
                    }
                }
                labels()
            }
        } else {
            Row(interactive.padding(vertical = 2.dp), Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Marker(state, style)
                    if (!state.last) {
                        Box(
                            Modifier.padding(vertical = 4.dp).width(style.connectorWidth).heightIn(min = 24.dp)
                                .background(connector),
                        )
                    }
                }
                Box(Modifier.padding(bottom = 12.dp)) { labels() }
            }
        }
    }
}

@Composable
private fun Marker(state: StepItemState, style: StepsStyle) {
    val c = style.colors
    val (fill, content, border) = when (state.status) {
        StepStatus.Complete -> Triple(c.complete, c.onComplete, null)
        StepStatus.Current -> Triple(c.current, c.onCurrent, null)
        StepStatus.Error -> Triple(c.error, Color.White, null)
        StepStatus.Upcoming -> Triple(Color.Transparent, c.mutedText, c.upcoming)
    }
    Box(
        Modifier
            .size(style.markerSize)
            .clip(CircleShape)
            .background(fill)
            .then(if (border != null) Modifier.border(Hairline, border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.status == StepStatus.Complete -> CamoIcon(style.checkIcon, null,
                size = style.markerSize * ICON_SHARE, tint = content)
            state.status == StepStatus.Error -> CamoIcon(style.errorIcon, null, size = style.markerSize * ICON_SHARE,
                tint = content)
            style.showNumbers -> BasicText("${state.index + 1}", style = style.labelStyle.copy(color = content))
        }
    }
}

private const val ICON_SHARE = 0.7f

internal val ImageGlyph: ImageVector = ImageVector.Builder("image", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(4f, 5f)
        lineTo(20f, 5f)
        lineTo(20f, 19f)
        lineTo(4f, 19f)
        close()
        moveTo(4f, 16f)
        lineTo(9f, 11f)
        lineTo(14f, 16f)
        lineTo(16f, 14f)
        lineTo(20f, 18f)
    }
    .build()

internal val BrokenImageGlyph: ImageVector = ImageVector.Builder("broken-image", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(4f, 5f)
        lineTo(20f, 5f)
        lineTo(20f, 19f)
        lineTo(4f, 19f)
        close()
        moveTo(4f, 4f)
        lineTo(20f, 20f)
    }
    .build()

internal val StarGlyph: ImageVector = ImageVector.Builder("star", 24.dp, 24.dp, 24f, 24f)
    .path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        lineTo(15.09f, 8.26f)
        lineTo(22f, 9.27f)
        lineTo(17f, 14.14f)
        lineTo(18.18f, 21.02f)
        lineTo(12f, 17.77f)
        lineTo(5.82f, 21.02f)
        lineTo(7f, 14.14f)
        lineTo(2f, 9.27f)
        lineTo(8.91f, 8.26f)
        close()
    }
    .build()

internal val ChevronLeftGlyph: ImageVector = ImageVector.Builder("chevron-left", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(15f, 6f)
        lineTo(9f, 12f)
        lineTo(15f, 18f)
    }
    .build()

internal val ChevronRightGlyph: ImageVector = ImageVector.Builder("chevron-right", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(9f, 6f)
        lineTo(15f, 12f)
        lineTo(9f, 18f)
    }
    .build()

internal val SlashGlyph: ImageVector = ImageVector.Builder("slash", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(16f, 5f)
        lineTo(8f, 19f)
    }
    .build()

internal val CheckGlyph: ImageVector = ImageVector.Builder("check", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
        moveTo(5f, 12.5f)
        lineTo(10f, 17.5f)
        lineTo(19f, 7f)
    }
    .build()

internal val AlertGlyph: ImageVector = ImageVector.Builder("alert", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 5f)
        lineTo(12f, 14f)
        moveTo(12f, 19f)
        lineTo(12f, 19.01f)
    }
    .build()
