package com.srctool.camouflage.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.theme.CamoColors
import com.srctool.camouflage.core.theme.CamoTokenSet

/** What a chart looks like, fully resolved: every field set. Charts and [ChartRenderer]s read this. */
@Immutable
public data class ResolvedChartStyle(
    val gridLines: Boolean,
    val gridDashed: Boolean,
    val lineWidth: Dp,
    val markerSize: Dp,
    val areaOpacity: Float,
    val barRadius: Dp,
    val barGap: Dp,
    val groupGap: Float,
    val donutThickness: Float,
    val sliceGap: Dp,
    val selectedOffset: Dp,
    val labelThreshold: Float,
    val animate: Boolean,
    val palette: List<Color>,
) {
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    internal fun merge(overrides: ChartStyle?): ResolvedChartStyle = if (overrides == null) {
        this
    } else {
        ResolvedChartStyle(
            gridLines = overrides.gridLines ?: gridLines,
            gridDashed = overrides.gridDashed ?: gridDashed,
            lineWidth = overrides.lineWidth ?: lineWidth,
            markerSize = overrides.markerSize ?: markerSize,
            areaOpacity = overrides.areaOpacity ?: areaOpacity,
            barRadius = overrides.barRadius ?: barRadius,
            barGap = overrides.barGap ?: barGap,
            groupGap = overrides.groupGap ?: groupGap,
            donutThickness = overrides.donutThickness ?: donutThickness,
            sliceGap = overrides.sliceGap ?: sliceGap,
            selectedOffset = overrides.selectedOffset ?: selectedOffset,
            labelThreshold = overrides.labelThreshold ?: labelThreshold,
            animate = overrides.animate ?: animate,
            palette = overrides.palette?.takeIf { it.isNotEmpty() } ?: palette,
        )
    }

    /** The color of series [index], cycling when there are more series than colors. */
    public fun seriesColor(index: Int): Color = palette[index % palette.size]
}

/**
 * Chart style changes: a field that is `null` keeps the layer below (the theme's defaults, then the skin's preset,
 * then [ChartTokens], then the chart's own `style`).
 */
@Immutable
public data class ChartStyle(
    val gridLines: Boolean? = null,
    val gridDashed: Boolean? = null,
    val lineWidth: Dp? = null,
    val markerSize: Dp? = null,
    val areaOpacity: Float? = null,
    val barRadius: Dp? = null,
    val barGap: Dp? = null,
    val groupGap: Float? = null,
    val donutThickness: Float? = null,
    val sliceGap: Dp? = null,
    val selectedOffset: Dp? = null,
    val labelThreshold: Float? = null,
    val animate: Boolean? = null,
    val palette: List<Color>? = null,
)

/** A brand's chart tokens (ADR-32): a [style] that applies on top of the skin's preset, whichever skin is active. */
@Immutable
public data class ChartTokens(val style: ChartStyle) : CamoTokenSet<ChartTokens> {
    override fun lerp(to: ChartTokens, fraction: Float): ChartTokens = if (fraction < HALF) this else to
}

private const val HALF = 0.5f
private const val MINIMUM_CONTRAST = 3.0
private const val CONTRAST_PAD = 0.05
private const val MINIMUM_PALETTE = 3

/** The theme-derived defaults: a palette from the color families and the skin-neutral metrics. */
private fun themeStyle(colors: CamoColors): ResolvedChartStyle = ResolvedChartStyle(
    gridLines = true,
    gridDashed = false,
    lineWidth = 2.dp,
    markerSize = 6.dp,
    areaOpacity = 0.18f,
    barRadius = 4.dp,
    barGap = 4.dp,
    groupGap = 0.3f,
    donutThickness = 0.35f,
    sliceGap = 2.dp,
    selectedOffset = 6.dp,
    labelThreshold = 0.05f,
    animate = true,
    palette = paletteFor(colors),
)

private fun contrast(a: Color, b: Color): Double {
    val high = maxOf(a.luminance(), b.luminance()) + CONTRAST_PAD
    val low = minOf(a.luminance(), b.luminance()) + CONTRAST_PAD
    return (high / low).toDouble()
}

// The families in order; a color too close to the surface (a pale yellow on white) is left out of the cycle.
internal fun paletteFor(colors: CamoColors): List<Color> {
    val all = listOf(
        colors.primary, colors.info, colors.success, colors.warning, colors.error, colors.secondary, colors.tertiary,
    ).distinct()
    val readable = all.filter { contrast(it, colors.surface) >= MINIMUM_CONTRAST }
    return if (readable.size >= MINIMUM_PALETTE) readable else all
}

/** Chart presets per skin name, and the way to add one for a skin of your own. */
public object CamoCharts {
    private val presets = mutableMapOf(
        "minimal" to ChartStyle(lineWidth = 1.5.dp, areaOpacity = 0.12f, barRadius = 2.dp, markerSize = 5.dp),
        "fluent" to ChartStyle(
            lineWidth = 2.dp, areaOpacity = 0.2f, barRadius = 4.dp, gridDashed = true, sliceGap = 3.dp,
        ),
        "cupertino" to ChartStyle(
            lineWidth = 2.dp, markerSize = 7.dp, areaOpacity = 0.2f, barRadius = 4.dp, barGap = 2.dp,
            donutThickness = 0.4f, sliceGap = 2.dp,
        ),
        "carbon" to ChartStyle(
            lineWidth = 1.5.dp, markerSize = 6.dp, areaOpacity = 0.3f, barRadius = 0.dp, barGap = 1.dp,
            gridDashed = true, donutThickness = 0.35f, sliceGap = 1.dp,
        ),
        "material" to ChartStyle(
            lineWidth = 3.dp, markerSize = 8.dp, areaOpacity = 0.16f, barRadius = 8.dp, barGap = 2.dp,
            donutThickness = 0.3f, sliceGap = 4.dp,
        ),
    )

    /** Sets the preset charts use while the skin named [skinName] is active. */
    public fun registerPreset(skinName: String, style: ChartStyle) {
        presets[skinName] = style
    }

    internal fun presetFor(skinName: String): ChartStyle? = presets[skinName]
}

/**
 * The style a chart draws with: the theme's defaults, then the preset for the skin's name, then the theme's
 * [ChartTokens] if it has them, then [overrides].
 */
@Composable
public fun rememberChartStyle(overrides: ChartStyle? = null): ResolvedChartStyle {
    val skin = Camouflage.skin
    val theme = Camouflage.theme
    return remember(skin, theme, overrides) {
        themeStyle(theme.colors)
            .merge(CamoCharts.presetFor(skin.name))
            .merge(theme.tokens<ChartTokens>()?.style)
            .merge(overrides)
    }
}
