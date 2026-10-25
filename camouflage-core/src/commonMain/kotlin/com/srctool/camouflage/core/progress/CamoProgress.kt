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

/** A progress circle's size. */
public enum class ProgressSize { Small, Medium, Large }

/** The active part and the track of an indicator. */
@Immutable
public data class ProgressColors(val active: Color, val track: Color)

/**
 * Overrides for progress indicators. `null` keeps the skin's default. The bar's fields join with
 * `CamoProgressBar` (M7, ADR-39).
 */
@Immutable
public data class ProgressTheme(
    val circleStroke: Dp? = null,
    val circleSizeSmall: Dp? = null,
    val circleSizeMedium: Dp? = null,
    val circleSizeLarge: Dp? = null,
    val colors: ProgressColors? = null,
)

/** The resolved progress style: every field set. [colors] is for `Tone.Default`; status tones use their family. */
@Immutable
public data class ProgressStyle(
    val circleStroke: Dp,
    val circleSizeSmall: Dp,
    val circleSizeMedium: Dp,
    val circleSizeLarge: Dp,
    val colors: ProgressColors,
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
        )
    }

    /** The diameter for [size]. */
    public fun circleSizeOf(size: ProgressSize): Dp = when (size) {
        ProgressSize.Small -> circleSizeSmall
        ProgressSize.Medium -> circleSizeMedium
        ProgressSize.Large -> circleSizeLarge
    }
}

/** What a progress renderer draws. `progress == null` is indeterminate; otherwise it's in `0f..1f`. */
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
