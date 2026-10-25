package com.srctool.camouflage.core.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme

/**
 * One run of rich text. Each span inherits the base style and color and overrides only what it sets.
 * A span with [link] is its own accessible link, styled by the skin.
 */
@Immutable
public data class CamoSpan(
    val text: String,
    /** Replaces the base style for this span: one of the 15 roles, optionally `copy`-adjusted. */
    val style: TextStyle? = null,
    val fontWeight: FontWeight? = null,
    val italic: Boolean? = null,
    val color: Color = Color.Unspecified,
    val underline: Boolean? = null,
    val link: (() -> Unit)? = null,
)

/** What a text renderer draws. A plain string arrives as an [AnnotatedString] too, so renderers handle one type. */
@Immutable
public data class TextState(
    val content: AnnotatedString,
    val style: TextStyle,
    val color: Color,
    val maxLines: Int,
    val overflow: TextOverflow,
    val textAlign: TextAlign,
)

/** Draws text. It has no component theme: styles come from the call, colors from the content color. */
@Stable
public interface TextRenderer {
    /** How link spans look in this skin. */
    public fun linkStyles(theme: CamoTheme): TextLinkStyles

    /** Draws [state]. */
    @Composable
    public fun Render(state: TextState, theme: CamoTheme, modifier: Modifier)
}

/** Text in one of the theme's styles, in the surrounding container's content color unless [color] is set. */
@Composable
public fun CamoText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Camouflage.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign = TextAlign.Start,
) {
    val content = remember(text) { AnnotatedString(text) }
    RenderText(content, modifier, style, color, maxLines, overflow, textAlign)
}

/** Rich text: [spans] with their own weights, colors and links. The overload by type is Kotlin's `CamoText.rich`. */
@Composable
public fun CamoText(
    spans: List<CamoSpan>,
    modifier: Modifier = Modifier,
    style: TextStyle = Camouflage.typography.bodyMedium,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign = TextAlign.Start,
) {
    val skin = requireSkin("CamoText")
    val theme = requireTheme("CamoText")
    val links = skin.text.linkStyles(theme)
    val content = remember(spans, links) { spans.toAnnotated(links) }
    RenderText(content, modifier, style, color, maxLines, overflow, textAlign)
}

@Composable
private fun RenderText(
    content: AnnotatedString,
    modifier: Modifier,
    style: TextStyle,
    color: Color,
    maxLines: Int,
    overflow: TextOverflow,
    textAlign: TextAlign,
) {
    val skin = requireSkin("CamoText")
    val theme = requireTheme("CamoText")
    val resolved = color.takeOrElse { Camouflage.contentColor }
    skin.text.Render(TextState(content, style, resolved, maxLines, overflow, textAlign), theme, modifier)
}

internal fun List<CamoSpan>.toAnnotated(links: TextLinkStyles): AnnotatedString = buildAnnotatedString {
    for (span in this@toAnnotated) {
        val spanStyle = (span.style?.toSpanStyle() ?: SpanStyle()).merge(
            SpanStyle(
                fontWeight = span.fontWeight,
                fontStyle = span.italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
                color = span.color,
                textDecoration = when (span.underline) {
                    true -> TextDecoration.Underline
                    false -> TextDecoration.None
                    null -> null
                },
            ),
        )
        val link = span.link
        if (link != null) {
            withLink(LinkAnnotation.Clickable(tag = span.text, styles = links) { link() }) {
                withStyle(spanStyle) { append(span.text) }
            }
        } else {
            withStyle(spanStyle) { append(span.text) }
        }
    }
}
