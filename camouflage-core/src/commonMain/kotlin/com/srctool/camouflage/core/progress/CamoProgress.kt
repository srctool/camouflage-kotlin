package com.srctool.camouflage.core.progress

import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family

/** A progress circle's size. */
public enum class ProgressSize { Small, Medium, Large }

/** The active part and the track of an indicator. */
@Immutable
public data class ProgressColors(val active: Color, val track: Color)

/** Overrides for progress indicators, circles and bars. `null` keeps the skin's default. */
@Immutable
public data class ProgressTheme(
    val circleStroke: Dp? = null,
    val circleSizeSmall: Dp? = null,
    val circleSizeMedium: Dp? = null,
    val circleSizeLarge: Dp? = null,
    val colors: ProgressColors? = null,
    val barHeight: Dp? = null,
    /** The gap between a bar's active part and its track (0: they touch). */
    val trackGap: Dp? = null,
    /** An end dot on determinate bars. */
    val showStopIndicator: Boolean? = null,
)

/** The resolved progress style: every field set. [colors] is for `Tone.Default`; status tones use their family. */
@Immutable
public data class ProgressStyle(
    val circleStroke: Dp,
    val circleSizeSmall: Dp,
    val circleSizeMedium: Dp,
    val circleSizeLarge: Dp,
    val colors: ProgressColors,
    val barHeight: Dp,
    val trackGap: Dp,
    val showStopIndicator: Boolean,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: ProgressTheme?): ProgressStyle = if (overrides == null) {
        this
    } else {
        ProgressStyle(
            circleStroke = overrides.circleStroke ?: circleStroke,
            circleSizeSmall = overrides.circleSizeSmall ?: circleSizeSmall,
            circleSizeMedium = overrides.circleSizeMedium ?: circleSizeMedium,
            circleSizeLarge = overrides.circleSizeLarge ?: circleSizeLarge,
            colors = overrides.colors ?: colors,
            barHeight = overrides.barHeight ?: barHeight,
            trackGap = overrides.trackGap ?: trackGap,
            showStopIndicator = overrides.showStopIndicator ?: showStopIndicator,
        )
    }

    /** The diameter for [size]. */
    public fun circleSizeOf(size: ProgressSize): Dp = when (size) {
        ProgressSize.Small -> circleSizeSmall
        ProgressSize.Medium -> circleSizeMedium
        ProgressSize.Large -> circleSizeLarge
    }

    /** The active and track colors for [tone]: this style's for `Tone.Default`, the tone's strong color otherwise. */
    public fun colorsFor(tone: Tone, theme: CamoTheme): ProgressColors =
        if (tone == Tone.Default) colors else ProgressColors(theme.colors.family(tone).strong, colors.track)
}

/**
 * What a progress renderer draws. `progress == null` is indeterminate; otherwise it's in `0f..1f`. [size] is a
 * circle's (bars ignore it).
 */
@Immutable
public data class ProgressState(val progress: Float?, val tone: Tone, val size: ProgressSize)

/** Draws progress circles. */
@Stable
public interface ProgressCircleRenderer {
    /** The complete progress style for [theme]. */
    public fun defaults(theme: CamoTheme): ProgressStyle

    /**
     * Draws [state]. Indeterminate circles animate with `theme.motion`; when its durations are zero
     * (reduced motion), they pulse slowly instead of moving.
     */
    @Composable
    public fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier)
}

/** A circular progress indicator. `progress == null` spins; a value is clamped to `0f..1f`. */
@Composable
public fun CamoProgressCircle(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    tone: Tone = Tone.Default,
    size: ProgressSize = ProgressSize.Medium,
    label: String? = null,
) {
    val skin = requireSkin("CamoProgressCircle")
    val theme = requireTheme("CamoProgressCircle")
    val style = remember(skin, theme) { skin.progressCircle.defaults(theme).merge(theme.components.progress) }
    val value = progress?.coerceIn(0f, 1f)
    val semantics = (if (value == null) Modifier.progressSemantics() else Modifier.progressSemantics(value))
        .then(if (label == null) Modifier else Modifier.semantics { contentDescription = label })
    skin.progressCircle.Render(ProgressState(value, tone, size), theme, style, modifier.then(semantics))
}

/** Draws progress bars. */
@Stable
public interface ProgressBarRenderer {
    /** The complete progress style for [theme] (the same style as the circle's). */
    public fun defaults(theme: CamoTheme): ProgressStyle

    /**
     * Draws [state] across the width it's given, at the style's bar height, filling from the start (the right in
     * right-to-left layouts). Indeterminate bars animate with `theme.motion`; when its durations are zero (reduced
     * motion), they pulse slowly instead of moving. A change of value animates over `durationMedium`.
     */
    @Composable
    public fun Render(state: ProgressState, theme: CamoTheme, style: ProgressStyle, modifier: Modifier)
}

/**
 * A linear progress indicator, as wide as its parent allows. `progress == null` is indeterminate (loading); a
 * value is clamped to `0f..1f` (an upload at 40%). [tone] colors a failing upload; [label] names it ("Uploading
 * photo"). Value changes aren't announced as they happen: announce the milestones ("Upload complete").
 */
@Composable
public fun CamoProgressBar(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    tone: Tone = Tone.Default,
    label: String? = null,
) {
    val skin = requireSkin("CamoProgressBar")
    val theme = requireTheme("CamoProgressBar")
    val style = remember(skin, theme) { skin.progressBar.defaults(theme).merge(theme.components.progress) }
    val value = progress?.coerceIn(0f, 1f)
    val semantics = (if (value == null) Modifier.progressSemantics() else Modifier.progressSemantics(value))
        .then(if (label == null) Modifier else Modifier.semantics { contentDescription = label })
    skin.progressBar.Render(ProgressState(value, tone, ProgressSize.Medium), theme, style, modifier.then(semantics))
}
