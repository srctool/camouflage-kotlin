package com.srctool.camouflage.skin.carbon.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.carousel.CarouselButtonState
import com.srctool.camouflage.core.carousel.CarouselColors
import com.srctool.camouflage.core.carousel.CarouselDirection
import com.srctool.camouflage.core.carousel.CarouselRenderer
import com.srctool.camouflage.core.carousel.CarouselState
import com.srctool.camouflage.core.carousel.CarouselStyle
import com.srctool.camouflage.core.fileuploader.FileUploaderRenderer
import com.srctool.camouflage.core.fileuploader.UploadRowSlots
import com.srctool.camouflage.core.fileuploader.UploadRowState
import com.srctool.camouflage.core.fileuploader.UploadStatus
import com.srctool.camouflage.core.fileuploader.UploaderColors
import com.srctool.camouflage.core.fileuploader.UploaderStyle
import com.srctool.camouflage.core.fileuploader.UploaderZoneSlots
import com.srctool.camouflage.core.fileuploader.UploaderZoneState
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.progress.CamoProgressBar
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication

// Carbon's carousel and file uploader: a hairline dashed zone, floating round buttons.

private const val DISABLED_CONTENT = 0.38f
private const val BUTTON_ALPHA = 0.92f
private const val DASH = 6f
private val ZoneIcon = 28.dp
private val ButtonIcon = 24.dp

internal object CarbonCarouselRenderer : CarouselRenderer {
    override fun defaults(theme: CamoTheme): CarouselStyle {
        val c = theme.colors
        return CarouselStyle(
            itemShape = theme.shapes.none,
            itemSpacing = 1.dp,
            peek = 32.dp,
            buttonSize = 40.dp,
            previousIcon = IconSource.Vector(ChevronLeftGlyph),
            nextIcon = IconSource.Vector(ChevronRightGlyph),
            colors = CarouselColors(c.surface.copy(alpha = BUTTON_ALPHA), c.onSurface),
        )
    }

    @Composable
    override fun Container(state: CarouselState, theme: CamoTheme, style: CarouselStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        Box(modifier) { content() }
    }

    @Composable
    override fun Item(theme: CamoTheme, style: CarouselStyle, modifier: Modifier, content: @Composable () -> Unit) {
        Box(modifier.fillMaxWidth().clip(style.itemShape).background(theme.colors.surfaceContainerHigh)) { content() }
    }

    @Composable
    override fun Button(state: CarouselButtonState, theme: CamoTheme, style: CarouselStyle, modifier: Modifier) {
        val c = style.colors
        val content = if (state.enabled) c.buttonContent else c.buttonContent.disabled(DISABLED_CONTENT)
        Box(
            modifier
                .padding(8.dp)
                .size(style.buttonSize)
                .clip(RectangleShape)
                .background(c.buttonContainer)
                .indication(state.interactionSource, rememberSkinIndication(content, RectangleShape)),
            contentAlignment = Alignment.Center,
        ) {
            val icon = if (state.direction == CarouselDirection.Previous) style.previousIcon else style.nextIcon
            CamoIcon(icon, contentDescription = null, size = ButtonIcon, tint = content)
        }
    }
}

internal object CarbonFileUploaderRenderer : FileUploaderRenderer {
    override fun defaults(theme: CamoTheme): UploaderStyle {
        val c = theme.colors
        return UploaderStyle(
            zoneShape = theme.shapes.none,
            dashed = true,
            zoneHeight = 120.dp,
            icon = IconSource.Vector(UploadGlyph),
            removeIcon = IconSource.Vector(CloseGlyph),
            retryIcon = IconSource.Vector(RetryGlyph),
            titleStyle = theme.typography.labelLarge,
            hintStyle = theme.typography.bodySmall,
            colors = UploaderColors(c.surfaceContainerLow, c.outline, c.primary, c.error, c.onSurface,
                c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Zone(state: UploaderZoneState, slots: UploaderZoneSlots, theme: CamoTheme, style: UploaderStyle,
        modifier: Modifier) {
        val c = style.colors
        val edge = if (state.dragOver) c.dragBorder else c.border
        val shape = style.zoneShape
        val content = if (state.enabled) c.text else c.text.disabled(DISABLED_CONTENT)
        Column(
            modifier
                .fillMaxWidth()
                .heightIn(min = style.zoneHeight)
                .clip(shape)
                .background(if (state.dragOver) c.dragBorder.copy(alpha = DRAG_FILL) else c.zone)
                .indication(state.interactionSource, rememberSkinIndication(content, shape))
                .drawBehind {
                    val stroke = Stroke(
                        width = 1.dp.toPx() * (if (state.dragOver) 2 else 1),
                        pathEffect = if (style.dashed) PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(),
                            DASH.dp.toPx())) else null,
                    )
                    drawRoundRect(edge, cornerRadius = CornerRadius(8.dp.toPx()), style = stroke)
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CamoIcon(style.icon, contentDescription = null, size = ZoneIcon, tint = c.mutedText)
            BasicText(slots.title, style = style.titleStyle.copy(color = content, textAlign = TextAlign.Center))
            slots.hint?.let { BasicText(it, style = style.hintStyle.copy(color = c.mutedText,
                textAlign = TextAlign.Center)) }
        }
    }

    @Composable
    override fun Row(state: UploadRowState, slots: UploadRowSlots, theme: CamoTheme, style: UploaderStyle,
        modifier: Modifier) {
        val c = style.colors
        val failed = state.status is UploadStatus.Failed
        Row(modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(slots.name, style = style.titleStyle.copy(color = c.text, fontWeight = FontWeight.Normal),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(slots.detail, style = style.hintStyle.copy(color = if (failed) c.error else c.mutedText))
                if (state.status == UploadStatus.Uploading) {
                    CamoProgressBar(Modifier.fillMaxWidth(), progress = state.progress,
                        label = "Uploading ${slots.name}")
                }
                if (failed) CamoProgressBar(Modifier.fillMaxWidth(), progress = 1f, tone = Tone.Error)
            }
            slots.retry?.invoke()
            slots.remove()
        }
    }
}

private const val DRAG_FILL = 0.08f

internal val UploadGlyph: ImageVector = ImageVector.Builder("upload", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 15f)
        lineTo(12f, 4f)
        moveTo(7f, 9f)
        lineTo(12f, 4f)
        lineTo(17f, 9f)
        moveTo(4f, 15f)
        lineTo(4f, 19f)
        lineTo(20f, 19f)
        lineTo(20f, 15f)
    }
    .build()

internal val CloseGlyph: ImageVector = ImageVector.Builder("close", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(6f, 6f)
        lineTo(18f, 18f)
        moveTo(18f, 6f)
        lineTo(6f, 18f)
    }
    .build()

internal val RetryGlyph: ImageVector = ImageVector.Builder("retry", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(17.75f, 7.18f)
        lineTo(19.39f, 10.70f)
        lineTo(19.05f, 14.57f)
        lineTo(16.82f, 17.75f)
        lineTo(13.30f, 19.39f)
        lineTo(9.43f, 19.05f)
        lineTo(6.25f, 16.82f)
        lineTo(4.61f, 13.30f)
        lineTo(4.95f, 9.43f)
        lineTo(7.18f, 6.25f)
        lineTo(10.70f, 4.61f)
        moveTo(17.75f, 3.68f)
        lineTo(17.75f, 7.18f)
        lineTo(21.75f, 7.68f)
    }
    .build()
