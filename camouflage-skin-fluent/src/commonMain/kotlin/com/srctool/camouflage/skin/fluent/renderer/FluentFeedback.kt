package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.banner.BannerRenderer
import com.srctool.camouflage.core.banner.BannerSlots
import com.srctool.camouflage.core.banner.BannerState
import com.srctool.camouflage.core.banner.BannerStyle
import com.srctool.camouflage.core.banner.ToneIcons
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.progress.ProgressBarRenderer
import com.srctool.camouflage.core.progress.ProgressState
import com.srctool.camouflage.core.progress.ProgressStyle
import com.srctool.camouflage.core.provider.Camouflage
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
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.util.ProvideContentStyle
import kotlin.math.max
import kotlin.math.min

// Fluent's feedback components (base Fluent Skin §3): ProgressBar, a Toast card at the bottom end with its intent
// icon, MessageBar banners with an intent border.

private val SnackbarIconSize = 20.dp
private const val SEGMENT = 0.4f
private const val SWEEP_CYCLES = 4
private const val PULSE_MILLIS = 1200
private const val PULSE_LOW = 0.4f
private val ToneIconSize = 24.dp
private const val ICON_GRID = 24f
private const val CENTER = 12f
private const val RING = 9f
private const val INTENT_BORDER = 0.35f

internal object FluentProgressBarRenderer : ProgressBarRenderer {
    override fun defaults(theme: CamoTheme): ProgressStyle = FluentProgressCircleRenderer.defaults(theme)

    @Composable
    override fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) =
        PaintProgressBar(state, theme, style, modifier)
}

/**
 * A bar: the active part from the start (the right in RTL), the track after a gap, a stop dot on determinate
 * bars when the style asks; indeterminate bars sweep a segment, or pulse under reduced motion.
 */
@Composable
internal fun PaintProgressBar(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier) {
    val colors = style.colorsFor(state.tone, theme)
    val reduced = theme.motion.durationLong == 0
    val transition = rememberInfiniteTransition()
    val sweep by transition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween((theme.motion.durationLong * SWEEP_CYCLES).coerceAtLeast(1), easing = LinearEasing)),
    )
    val pulse by transition.animateFloat(PULSE_LOW, 1f, infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse))
    val value by animateFloatAsState(state.progress ?: 0f, tween(theme.motion.durationMedium))
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val indeterminate = state.progress == null
    Canvas(
        modifier
            .fillMaxWidth()
            .height(style.barHeight)
            .alpha(if (reduced && indeterminate) pulse else 1f),
    ) {
        val w = size.width
        val gap = style.trackGap.toPx()
        when {
            indeterminate && reduced -> segment(0f, w, colors.active, rtl)
            indeterminate -> {
                val length = w * SEGMENT
                val start = -length + (w + length) * sweep
                segment(0f, w, colors.track, rtl)
                segment(max(0f, start), min(w, start + length), colors.active, rtl)
            }
            else -> {
                val end = w * value
                segment(0f, end, colors.active, rtl)
                segment(min(w, if (end > 0f) end + gap else 0f), w, colors.track, rtl)
                if (style.showStopIndicator) {
                    val r = size.height / 2
                    drawCircle(colors.active, r, Offset(if (rtl) r else w - r, r))
                }
            }
        }
    }
}

// A rounded segment from `from` to `to`, measured from the start edge.
private fun DrawScope.segment(from: Float, to: Float, color: Color, rtl: Boolean) {
    if (to <= from) return
    val x = if (rtl) size.width - to else from
    val r = size.height / 2
    drawRoundRect(color, Offset(x, 0f), Size(to - from, size.height), CornerRadius(r, r))
}

internal object FluentSnackbarRenderer : SnackbarRenderer {
    override fun defaults(theme: CamoTheme): SnackbarStyle {
        val c = theme.colors
        val type = theme.typography
        // Fluent's Toast: a neutral card (shadow16) at the bottom end; the intent shows in its icon.
        return SnackbarStyle(
            shape = theme.shapes.small,
            maxWidth = 400.dp,
            margin = theme.spacing.l,
            padding = PaddingValues(start = theme.spacing.m, end = theme.spacing.xs),
            minHeight = 44.dp,
            textStyle = type.bodyMedium,
            actionStyle = type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            elevation = ElevationLevel.Level4,
            position = SnackbarPosition.Bottom,
            alignment = SnackbarAlignment.End,
            showDismiss = false,
            showToneIcon = true,
            durationShortMillis = 4000,
            durationLongMillis = 10000,
            colors = SnackbarColors(c.surface, c.onSurface, c.primary),
            dismissIcon = IconSource.Vector(DismissIcon),
        )
    }

    @Composable
    override fun Render(
        state: SnackbarState,
        slots: SnackbarSlots,
        theme: CamoTheme,
        style: SnackbarStyle,
        modifier: Modifier,
    ) = PaintSnackbar(
        state,
        slots,
        theme,
        style,
        style.colors,
        modifier,
        iconColor = style.colorsFor(state.tone, theme).container,
    )
}

/** A snackbar: one row of the tone icon, the message, the action and the ✕ on the surface treatment. */
@Composable
internal fun PaintSnackbar(
    state: SnackbarState,
    slots: SnackbarSlots,
    theme: CamoTheme,
    style: SnackbarStyle,
    colors: SnackbarColors,
    modifier: Modifier,
    iconColor: Color = colors.text,
) {
    Row(
        modifier
            .then(
                Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape,
                    theme.elevation.of(style.elevation), theme),
            )
            .clip(style.shape)
            .heightIn(min = style.minHeight)
            .padding(style.padding),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.icon?.let { ProvideContentStyle(iconColor, IconStyle(SnackbarIconSize, iconColor), it) }
        BasicText(
            state.message,
            Modifier.weight(1f).padding(vertical = theme.spacing.s),
            style.textStyle.copy(color = colors.text),
        )
        slots.action?.let { ProvideContentStyle(colors.action, IconStyle(SnackbarIconSize, colors.action), it) }
        slots.dismiss?.let { ProvideContentStyle(colors.text, IconStyle(SnackbarIconSize, colors.text), it) }
    }
}

internal object FluentBannerRenderer : BannerRenderer {
    // Fluent's MessageBar: the intent's background with its border, radius 4.
    override fun defaults(theme: CamoTheme) = BannerStyle(
        defaultAppearance = Appearance.Soft,
        shape = theme.shapes.small,
        padding = PaddingValues(theme.spacing.m),
        titleStyle = theme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        messageStyle = theme.typography.bodyMedium,
        iconSize = 20.dp,
        accentBorder = false,
        accentWidth = 4.dp,
        borderWidth = 1.dp,
        toneIcons = ToneIcons(
            IconSource.Vector(FluentInfoIcon),
            IconSource.Vector(FluentSuccessIcon),
            IconSource.Vector(FluentWarningIcon),
            IconSource.Vector(FluentErrorIcon),
        ),
        dismissIcon = IconSource.Vector(DismissIcon),
    )

    @Composable
    override fun Render(
        state: BannerState,
        slots: BannerSlots,
        theme: CamoTheme,
        style: BannerStyle,
        modifier: Modifier,
    ) = PaintBanner(
        state,
        slots,
        theme,
        style,
        modifier,
        border = style.colorsFor(state.appearance, state.tone, theme).accent.copy(alpha = INTENT_BORDER),
    )
}

/** A banner: the tone's surface (a border for Outline), the icon in the accent, texts, actions below, the ✕. */
@Composable
internal fun PaintBanner(
    state: BannerState,
    slots: BannerSlots,
    theme: CamoTheme,
    style: BannerStyle,
    modifier: Modifier,
    border: Color? = null,
) {
    val colors = style.colorsFor(state.appearance, state.tone, theme)
    val spacing = theme.spacing
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outline = colors.border ?: border
    val accent = if (style.accentBorder) {
        Modifier.drawWithContent {
            drawContent()
            val w = style.accentWidth.toPx()
            drawRect(colors.accent, Offset(if (rtl) size.width - w else 0f, 0f), Size(w, size.height))
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape, 0.dp, theme))
            .then(outline?.let { Modifier.border(style.borderWidth, it, style.shape) } ?: Modifier)
            .clip(style.shape)
            .then(accent),
    ) {
        Row(Modifier.padding(style.padding), horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            slots.icon?.let { ProvideContentStyle(colors.accent, IconStyle(style.iconSize, colors.accent), it) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                state.title?.let { BasicText(it, style = style.titleStyle.copy(color = colors.content)) }
                BasicText(state.message, style = style.messageStyle.copy(color = colors.content))
                slots.actions?.let { actions ->
                    ProvideContentStyle(colors.accent, IconStyle(style.iconSize, colors.accent)) {
                        FlowRow(
                            Modifier.padding(top = spacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(spacing.s),
                        ) { actions() }
                    }
                }
            }
            slots.dismiss?.let { ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content), it) }
        }
    }
}

private fun toneIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, ToneIconSize, ToneIconSize, ICON_GRID, ICON_GRID)
        .path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            pathBuilder = block,
        )
        .build()

// A ring of radius 9 around the 24 grid's center.
private fun PathBuilder.ring() {
    moveTo(CENTER + RING, CENTER)
    arcTo(RING, RING, 0f, true, true, CENTER - RING, CENTER)
    arcTo(RING, RING, 0f, true, true, CENTER + RING, CENTER)
}

internal val FluentInfoIcon: ImageVector = toneIcon("info") {
    ring()
    moveTo(12f, 11f); lineTo(12f, 16.5f); moveTo(12f, 7.5f); lineTo(12f, 7.6f)
}

internal val FluentSuccessIcon: ImageVector = toneIcon("success") {
    ring()
    moveTo(8f, 12.5f); lineTo(11f, 15.5f); lineTo(16f, 9f)
}

internal val FluentWarningIcon: ImageVector = toneIcon("warning") {
    moveTo(12f, 3.5f); lineTo(21.5f, 20f); lineTo(2.5f, 20f); close()
    moveTo(12f, 10f); lineTo(12f, 14f); moveTo(12f, 17f); lineTo(12f, 17.1f)
}

internal val FluentErrorIcon: ImageVector = toneIcon("error") {
    ring()
    moveTo(9f, 9f); lineTo(15f, 15f); moveTo(15f, 9f); lineTo(9f, 15f)
}
