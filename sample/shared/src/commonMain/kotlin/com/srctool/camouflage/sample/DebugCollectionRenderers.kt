package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.carousel.CarouselButtonState
import com.srctool.camouflage.core.carousel.CarouselColors
import com.srctool.camouflage.core.carousel.CarouselRenderer
import com.srctool.camouflage.core.carousel.CarouselState
import com.srctool.camouflage.core.carousel.CarouselStyle
import com.srctool.camouflage.core.fileuploader.FileUploaderRenderer
import com.srctool.camouflage.core.fileuploader.UploadRowSlots
import com.srctool.camouflage.core.fileuploader.UploadRowState
import com.srctool.camouflage.core.fileuploader.UploaderColors
import com.srctool.camouflage.core.fileuploader.UploaderStyle
import com.srctool.camouflage.core.fileuploader.UploaderZoneSlots
import com.srctool.camouflage.core.fileuploader.UploaderZoneState
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.theme.CamoTheme

// The M10 carousel and file uploader in the debug skin: boxes, rows and text.

private val DebugCollectionIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())

object DebugCarousel : CarouselRenderer {
    override fun defaults(theme: CamoTheme) = CarouselStyle(
        RectangleShape, 8.dp, 16.dp, 32.dp, DebugCollectionIcon, DebugCollectionIcon, CarouselColors(Color.White,
            Color.Black),
    )

    @Composable
    override fun Container(state: CarouselState, theme: CamoTheme, style: CarouselStyle, modifier: Modifier,
        content: @Composable () -> Unit) {
        Box(modifier) { content() }
    }

    @Composable
    override fun Item(theme: CamoTheme, style: CarouselStyle, modifier: Modifier, content: @Composable () -> Unit) {
        Box(modifier.fillMaxWidth().background(Color.LightGray)) { content() }
    }

    @Composable
    override fun Button(state: CarouselButtonState, theme: CamoTheme, style: CarouselStyle, modifier: Modifier) {
        Box(modifier.size(32.dp)) { BasicText(state.direction.name) }
    }
}

object DebugFileUploader : FileUploaderRenderer {
    override fun defaults(theme: CamoTheme) = UploaderStyle(
        RectangleShape, false, 80.dp, DebugCollectionIcon, DebugCollectionIcon, DebugCollectionIcon, TextStyle.Default,
            TextStyle.Default,
        UploaderColors(Color.White, Color.Gray, Color.Blue, Color.Red, Color.Black, Color.DarkGray),
    )

    @Composable
    override fun Zone(state: UploaderZoneState, slots: UploaderZoneSlots, theme: CamoTheme, style: UploaderStyle,
        modifier: Modifier) {
        Column(modifier.fillMaxWidth().padding(8.dp)) {
            BasicText(slots.title)
            slots.hint?.let { BasicText(it) }
        }
    }

    @Composable
    override fun Row(state: UploadRowState, slots: UploadRowSlots, theme: CamoTheme, style: UploaderStyle,
        modifier: Modifier) {
        Row(modifier.fillMaxWidth().padding(4.dp)) {
            Column(Modifier.weight(1f)) {
                BasicText(slots.name)
                BasicText(slots.detail)
            }
            slots.retry?.invoke()
            slots.remove()
        }
    }
}
