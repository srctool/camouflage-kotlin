package com.srctool.camouflage.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.banner.BannerRenderer
import com.srctool.camouflage.core.banner.BannerSlots
import com.srctool.camouflage.core.banner.BannerState
import com.srctool.camouflage.core.banner.BannerStyle
import com.srctool.camouflage.core.banner.ToneIcons
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.progress.ProgressBarRenderer
import com.srctool.camouflage.core.progress.ProgressColors
import com.srctool.camouflage.core.progress.ProgressState
import com.srctool.camouflage.core.progress.ProgressStyle
import com.srctool.camouflage.core.snackbar.SnackbarAlignment
import com.srctool.camouflage.core.snackbar.SnackbarColors
import com.srctool.camouflage.core.snackbar.SnackbarPosition
import com.srctool.camouflage.core.snackbar.SnackbarRenderer
import com.srctool.camouflage.core.snackbar.SnackbarSlots
import com.srctool.camouflage.core.snackbar.SnackbarState
import com.srctool.camouflage.core.snackbar.SnackbarStyle
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.util.ProvideContentStyle

// The M7 feedback components in the plain test skin: boxes, rows and text, no decoration.

private val TestIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())

internal fun plainProgressStyle(theme: CamoTheme) = ProgressStyle(
    4.dp, 24.dp, 48.dp, 64.dp, ProgressColors(theme.colors.primary, theme.colors.outlineVariant), 4.dp, 0.dp, false,
)

internal object PlainProgressBar : ProgressBarRenderer {
    override fun defaults(theme: CamoTheme) = plainProgressStyle(theme)

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.tone, theme)
        Canvas(modifier.fillMaxWidth().height(style.barHeight)) {
            drawRect(colors.track)
            drawRect(colors.active, size = size.copy(width = size.width * (state.progress ?: 1f)))
        }
    }
}

internal object PlainSnackbar : SnackbarRenderer {
    override fun defaults(theme: CamoTheme): SnackbarStyle {
        val c = theme.colors
        return SnackbarStyle(
            RectangleShape, 560.dp, 16.dp, PaddingValues(horizontal = 16.dp), 48.dp, theme.typography.bodyMedium,
            theme.typography.labelLarge, ElevationLevel.Level3, SnackbarPosition.Bottom, SnackbarAlignment.Center,
            false, false, 4000, 10000, SnackbarColors(c.inverseSurface, c.inverseOnSurface, c.inversePrimary),
            TestIcon,
        )
    }

    @Composable
    override fun Render(
        state: SnackbarState,
        slots: SnackbarSlots,
        theme: CamoTheme,
        style: SnackbarStyle,
        modifier: Modifier,
    ) {
        val colors = style.colorsFor(state.tone, theme)
        Row(
            modifier.background(colors.container).heightIn(min = style.minHeight).padding(style.padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideContentStyle(colors.text, IconStyle(24.dp, colors.text)) {
                slots.icon?.invoke()
                BasicText(state.message, Modifier.weight(1f))
            }
            ProvideContentStyle(colors.action, IconStyle(24.dp, colors.action)) { slots.action?.invoke() }
            ProvideContentStyle(colors.text, IconStyle(24.dp, colors.text)) { slots.dismiss?.invoke() }
        }
    }
}

internal object PlainBanner : BannerRenderer {
    override fun defaults(theme: CamoTheme) = BannerStyle(
        Appearance.Soft, RectangleShape, PaddingValues(16.dp), theme.typography.titleSmall,
        theme.typography.bodyMedium, 24.dp, false, 4.dp, 1.dp, ToneIcons(TestIcon, TestIcon, TestIcon, TestIcon),
        TestIcon,
    )

    @Composable
    override fun Render(
        state: BannerState,
        slots: BannerSlots,
        theme: CamoTheme,
        style: BannerStyle,
        modifier: Modifier,
    ) {
        val colors = style.colorsFor(state.appearance, state.tone, theme)
        Box(modifier.background(colors.container).padding(style.padding)) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                Row {
                    slots.icon?.invoke()
                    Column(Modifier.weight(1f)) {
                        state.title?.let { BasicText(it) }
                        BasicText(state.message)
                        slots.actions?.let { Row(content = it) }
                    }
                    slots.dismiss?.invoke()
                }
            }
        }
    }
}
