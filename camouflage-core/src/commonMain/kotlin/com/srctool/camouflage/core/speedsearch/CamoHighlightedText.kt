package com.srctool.camouflage.core.speedsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.surface.CamoSurface
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.SurfaceLevel

/** Overrides for speed search. `null` keeps the default. */
@Immutable
public data class SpeedSearchTheme(
    val highlightColor: Color? = null,
    val highlightTextColor: Color? = null,
    val matchCount: ((Int) -> String)? = null,
    val noMatches: String? = null,
)

/** What a match highlight looks like. */
@Immutable
internal data class HighlightStyle(val background: Color, val text: Color)

/** The highlight for the current theme: the tertiary container unless the theme says otherwise. */
@Composable
internal fun rememberHighlightStyle(): HighlightStyle {
    val theme = requireTheme("CamoHighlightedText")
    return remember(theme) {
        val o = theme.components.speedSearch
        HighlightStyle(
            o?.highlightColor ?: theme.colors.tertiaryContainer,
            o?.highlightTextColor ?: theme.colors.onTertiaryContainer,
        )
    }
}

/** [text] as a string with [ranges] (from [CamoSpeedSearchState.match]) drawn in the highlight style. */
internal fun highlighted(text: String, ranges: List<IntRange>, style: HighlightStyle): AnnotatedString =
    if (ranges.isEmpty()) {
        AnnotatedString(text)
    } else {
        buildAnnotatedString {
            append(text)
            for (r in ranges) {
                val start = r.first.coerceIn(0, text.length)
                val end = (r.last + 1).coerceIn(start, text.length)
                addStyle(SpanStyle(color = style.text, background = style.background), start, end)
            }
        }
    }

/**
 * [text] with the characters in [ranges] highlighted in the theme's speed search colors. Use it with
 * [CamoSpeedSearchState.match] in your own lists; the tree and menus do it themselves.
 */
@Composable
public fun CamoHighlightedText(
    text: String,
    ranges: List<IntRange>,
    modifier: Modifier = Modifier,
    style: TextStyle = Camouflage.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val look = rememberHighlightStyle()
    val content = remember(text, ranges, look) { highlighted(text, ranges, look) }
    BasicText(
        content,
        modifier,
        if (color == Color.Unspecified) style else style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * The typed query and the match count, in a small surface a collection puts at its start edge while a search
 * runs. A polite live region: a screen reader hears "3 matches" or "No matches" as the query changes.
 */
@Composable
public fun CamoSpeedSearchOverlay(state: CamoSpeedSearchState, modifier: Modifier = Modifier) {
    if (!state.active) return
    val theme = requireTheme("CamoSpeedSearchOverlay")
    val o = theme.components.speedSearch
    val count = state.matchCount
    val summary = if (count == 0) o?.noMatches ?: "No matches" else o?.matchCount?.invoke(count) ?: "$count matches"
    CamoSurface(
        modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "${state.query}, $summary"
        },
        level = SurfaceLevel.High,
        shape = ShapeRole.Small,
        border = true,
    ) {
        Row(
            Modifier.padding(horizontal = Camouflage.spacing.s, vertical = Camouflage.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Camouflage.spacing.s),
        ) {
            CamoText(state.query, style = Camouflage.typography.bodyMedium)
            CamoText(summary, style = Camouflage.typography.labelSmall, color = theme.colors.onSurfaceVariant)
        }
    }
}
