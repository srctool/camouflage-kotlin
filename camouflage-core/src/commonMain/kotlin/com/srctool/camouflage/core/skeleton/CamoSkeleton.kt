package com.srctool.camouflage.core.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.of
import kotlin.math.abs

/** How placeholders move: a highlight sweeping across, a slow fade in and out, or not at all. */
public enum class SkeletonAnimation { Shimmer, Pulse, None }

/** The leading placeholder of [CamoSkeletonListItem]: an avatar circle, a square image, or nothing. */
public enum class SkeletonLeading { Circle, Square, None }

/** Overrides for skeletons. `null` keeps the skin's default. */
@Immutable
public data class SkeletonTheme(
    val animation: SkeletonAnimation? = null,
    val baseColor: Color? = null,
    val highlightColor: Color? = null,
    /** One shimmer or pulse cycle, in milliseconds. */
    val cycleDuration: Int? = null,
    val textLineGap: Dp? = null,
)

/** The resolved skeleton style: every field set. */
@Immutable
public data class SkeletonStyle(
    val animation: SkeletonAnimation,
    val baseColor: Color,
    val highlightColor: Color,
    val cycleDuration: Int,
    val textLineGap: Dp,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SkeletonTheme?): SkeletonStyle = if (overrides == null) {
        this
    } else {
        SkeletonStyle(
            animation = overrides.animation ?: animation,
            baseColor = overrides.baseColor ?: baseColor,
            highlightColor = overrides.highlightColor ?: highlightColor,
            cycleDuration = overrides.cycleDuration ?: cycleDuration,
            textLineGap = overrides.textLineGap ?: textLineGap,
        )
    }
}

/**
 * What a skeleton renderer draws: one block in [shape]. [animation] is already `None` under reduced motion.
 * [phase] runs from 0 to 1 once per cycle and is shared by every block in a [CamoSkeletonGroup]; read it while
 * drawing (not in composition), so the animation doesn't recompose.
 */
@Stable
public class SkeletonState(
    public val shape: Shape,
    public val animation: SkeletonAnimation,
    public val phase: () -> Float,
)

/** Draws skeleton blocks. Core sizes them and hides them from screen readers. */
@Stable
public interface SkeletonRenderer {
    /** The complete skeleton style for [theme]. */
    public fun defaults(theme: CamoTheme): SkeletonStyle

    /** Draws one block filling `modifier`'s size. */
    @Composable
    public fun Render(state: SkeletonState, theme: CamoTheme, style: SkeletonStyle, modifier: Modifier)
}

private val LocalSkeletonPhase = compositionLocalOf<State<Float>?> { null }
private val StillPhase: State<Float> = mutableFloatStateOf(0f)
private val Avatar = 40.dp
private const val HEADLINE_FRACTION = 0.5f
private const val SUPPORTING_FRACTION = 0.8f

/** Placeholders inside share one animation clock, so a screen of skeletons moves together. */
@Composable
public fun CamoSkeletonGroup(content: @Composable () -> Unit) {
    val style = skeletonStyle()
    val phase = rememberPhase(style.cycleDuration, animated(style))
    CompositionLocalProvider(LocalSkeletonPhase provides phase, content = content)
}

/**
 * Like [CamoSkeletonGroup], but the clock runs only while [active], so a list that shows skeletons only now and
 * then doesn't animate all the time. The content's structure is the same either way.
 */
@Composable
internal fun SkeletonPhaseScope(active: Boolean, content: @Composable () -> Unit) {
    val style = skeletonStyle()
    val phase = if (active) rememberPhase(style.cycleDuration, animated(style)) else StillPhase
    CompositionLocalProvider(LocalSkeletonPhase provides phase, content = content)
}

/**
 * A placeholder block for content that hasn't loaded (an image, a button). A `null` [width] fills the available
 * width. Hidden from screen readers: the screen that shows skeletons announces "Loading" once.
 */
@Composable
public fun CamoSkeleton(
    height: Dp,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    shape: ShapeRole = ShapeRole.Small,
) {
    val theme = requireTheme("CamoSkeleton")
    val size = if (width == null) Modifier.fillMaxWidth() else Modifier.width(width)
    SkeletonBlock(theme.shapes.of(shape), modifier.then(size).height(height))
}

/** A round placeholder (an avatar). */
@Composable
public fun CamoSkeletonCircle(size: Dp, modifier: Modifier = Modifier) {
    SkeletonBlock(CircleShape, modifier.size(size))
}

/**
 * [lines] placeholder lines as tall as text in [style] (so they match the real text at any font scale), the last
 * one [lastLineFraction] of the width.
 */
@Composable
public fun CamoSkeletonText(
    modifier: Modifier = Modifier,
    lines: Int = 1,
    style: TextStyle? = null,
    lastLineFraction: Float = 0.6f,
) {
    val theme = requireTheme("CamoSkeletonText")
    val text = style ?: theme.typography.bodyMedium
    val gap = skeletonStyle().textLineGap
    val lineHeight = with(LocalDensity.current) {
        (if (text.lineHeight.isSpecified) text.lineHeight else text.fontSize * LINE_HEIGHT_FALLBACK).toDp()
    }
    val shape = theme.shapes.of(ShapeRole.ExtraSmall)
    Column(modifier) {
        repeat(lines) { i ->
            val fraction = if (i == lines - 1 && lines > 1) lastLineFraction else 1f
            Box(Modifier.height(lineHeight).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                SkeletonBlock(shape, Modifier.fillMaxWidth(fraction).height((lineHeight - gap).coerceAtLeast(gap)))
            }
        }
    }
}

/** A placeholder list row with the proportions of a `CamoListItem` with [lines] lines of text. */
@Composable
public fun CamoSkeletonListItem(
    modifier: Modifier = Modifier,
    lines: Int = 2,
    leading: SkeletonLeading = SkeletonLeading.Circle,
) {
    val skin = requireSkin("CamoSkeletonListItem")
    val theme = requireTheme("CamoSkeletonListItem")
    val list = remember(skin, theme) { skin.listItem.defaults(theme).merge(theme.components.listItem) }
    Row(
        modifier.fillMaxWidth().heightIn(min = list.minHeight(lines)).padding(list.contentPadding),
        horizontalArrangement = Arrangement.spacedBy(list.leadingGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (leading) {
            SkeletonLeading.Circle -> CamoSkeletonCircle(Avatar)
            SkeletonLeading.Square -> CamoSkeleton(Avatar, width = Avatar)
            SkeletonLeading.None -> Unit
        }
        Column(Modifier.weight(1f)) {
            CamoSkeletonText(Modifier.fillMaxWidth(HEADLINE_FRACTION), style = list.headlineStyle)
            repeat(lines - 1) {
                CamoSkeletonText(Modifier.fillMaxWidth(SUPPORTING_FRACTION), style = list.supportingStyle)
            }
        }
    }
}

private const val LINE_HEIGHT_FALLBACK = 1.2f

@Composable
private fun SkeletonBlock(shape: Shape, modifier: Modifier) {
    val skin = requireSkin("CamoSkeleton")
    val theme = requireTheme("CamoSkeleton")
    val style = skeletonStyle()
    val animated = animated(style)
    val shared = LocalSkeletonPhase.current
    val phase = if (shared != null) shared else rememberPhase(style.cycleDuration, animated)
    skin.skeleton.Render(
        SkeletonState(shape, if (animated) style.animation else SkeletonAnimation.None) { phase.value },
        theme,
        style,
        modifier.clearAndSetSemantics {},
    )
}

@Composable
private fun skeletonStyle(): SkeletonStyle {
    val skin = requireSkin("CamoSkeleton")
    val theme = requireTheme("CamoSkeleton")
    return remember(skin, theme) { skin.skeleton.defaults(theme).merge(theme.components.skeleton) }
}

// Reduced motion (zero durations) makes skeletons static.
@Composable
private fun animated(style: SkeletonStyle): Boolean =
    style.animation != SkeletonAnimation.None && requireTheme("CamoSkeleton").motion.durationLong > 0

@Composable
private fun rememberPhase(cycle: Int, animated: Boolean): State<Float> {
    if (!animated) return StillPhase
    return rememberInfiniteTransition(label = "skeleton").animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(cycle, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
}

private const val PULSE_LOW = 0.45f

/**
 * Paints a skeleton block for a renderer: the base color in the state's shape, with the highlight swept across it
 * (`Shimmer`, start to end in the reading direction) or the block fading in and out (`Pulse`). The phase is read
 * while drawing, so the animation doesn't recompose.
 */
public fun Modifier.drawSkeleton(state: SkeletonState, style: SkeletonStyle, rtl: Boolean): Modifier =
    clip(state.shape).drawWithCache {
        val band = size.width.coerceAtLeast(1f)
        onDrawBehind {
            val phase = state.phase()
            when (state.animation) {
                SkeletonAnimation.None -> drawRect(style.baseColor)
                SkeletonAnimation.Pulse -> {
                    // 1 → PULSE_LOW → 1 over a cycle.
                    val wave = 1f - (1f - PULSE_LOW) * (1f - abs(2f * phase - 1f))
                    drawRect(style.baseColor, alpha = wave)
                }
                SkeletonAnimation.Shimmer -> {
                    drawRect(style.baseColor)
                    // The highlight band travels from fully before the block to fully past it.
                    val x = (phase * 3f - 1f) * band
                    val from = if (rtl) size.width - x else x
                    val to = if (rtl) from - band else from + band
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, style.highlightColor, Color.Transparent),
                            start = Offset(from, 0f),
                            end = Offset(to, 0f),
                        ),
                    )
                }
            }
        }
    }
