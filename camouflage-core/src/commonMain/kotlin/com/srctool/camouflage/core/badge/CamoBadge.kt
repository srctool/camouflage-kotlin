package com.srctool.camouflage.core.badge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family

/** A badge's colors. [border] is drawn only when set (the Outline appearance). */
@Immutable
public data class BadgeColors(val container: Color, val content: Color, val border: Color? = null)

/** The words a badge says when the app passes no description: [count] for a number, [dot] for a dot. */
@Immutable
public data class BadgeStrings(
    val count: (count: String) -> String = { "$it new" },
    val dot: String = "New",
)

/** Overrides for badges. `null` keeps the skin's default. */
@Immutable
public data class BadgeTheme(
    val defaultAppearance: Appearance? = null,
    /** The tone when a call passes none (a brand whose badges are, say, Info sets it once). */
    val defaultTone: Tone? = null,
    val dotSize: Dp? = null,
    val height: Dp? = null,
    val minWidth: Dp? = null,
    val padding: PaddingValues? = null,
    val textStyle: TextStyle? = null,
    /** Where the badge's center sits from the anchor's top-end corner (x toward the end, y down). */
    val offset: DpOffset? = null,
    val borderWidth: Dp? = null,
    val strings: BadgeStrings? = null,
)

/** The resolved badge style: every field set. */
@Immutable
public data class BadgeStyle(
    val defaultAppearance: Appearance,
    val defaultTone: Tone,
    val dotSize: Dp,
    val height: Dp,
    val minWidth: Dp,
    val padding: PaddingValues,
    val textStyle: TextStyle,
    val offset: DpOffset,
    val borderWidth: Dp,
    val strings: BadgeStrings = BadgeStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: BadgeTheme?): BadgeStyle = if (overrides == null) {
        this
    } else {
        BadgeStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            defaultTone = overrides.defaultTone ?: defaultTone,
            dotSize = overrides.dotSize ?: dotSize,
            height = overrides.height ?: height,
            minWidth = overrides.minWidth ?: minWidth,
            padding = overrides.padding ?: padding,
            textStyle = overrides.textStyle ?: textStyle,
            offset = overrides.offset ?: offset,
            borderWidth = overrides.borderWidth ?: borderWidth,
            strings = overrides.strings ?: strings,
        )
    }

    /**
     * The colors for [appearance] in [tone] (`Tone.Default` is the brand's primary family): Solid a strong
     * fill, Soft a container fill, Outline the strong color for the border and text on [surface].
     */
    public fun colorsFor(appearance: Appearance, tone: Tone, theme: CamoTheme): BadgeColors {
        val f = theme.colors.family(tone)
        return when (appearance) {
            Appearance.Solid -> BadgeColors(f.strong, f.onStrong)
            Appearance.Soft -> BadgeColors(f.container, f.onContainer)
            else -> BadgeColors(theme.colors.surface, f.strong, border = f.strong)
        }
    }
}

/** The appearance a badge draws for [appearance]: Solid, Soft or Outline (the others fall back to Solid). */
public fun badgeAppearance(appearance: Appearance): Appearance = when (appearance) {
    Appearance.Soft, Appearance.Outline -> appearance
    else -> Appearance.Solid
}

/** What a badge renderer draws: a dot when [text] is `null`, a pill with [text] ("3", "99+") otherwise. */
@Immutable
public data class BadgeState(val text: String?, val tone: Tone, val appearance: Appearance) {
    /** A dot, no number. */
    public val isDot: Boolean get() = text == null
}

/** Draws badges. Apply `modifier` to the outermost node. */
@Stable
public interface BadgeRenderer {
    /** The complete badge style for [theme]. */
    public fun defaults(theme: CamoTheme): BadgeStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: BadgeState, theme: CamoTheme, style: BadgeStyle, modifier: Modifier)
}

/** The text a badge shows for [count]: `null` for no count (a dot), `"$max+"` above [max]. */
public fun badgeText(count: Int?, max: Int): String? = when {
    count == null -> null
    count > max -> "$max+"
    else -> count.coerceAtLeast(0).toString()
}

/**
 * Carries a badge's description from the [CamoBadge] in a [CamoBadgedBox] to the anchor that announces it.
 * Anchors that are their own merged node (`CamoIconButton`, `CamoAvatar`) claim it with [badgeAnchor].
 */
@Stable
internal class BadgeAnnouncement {
    var text by mutableStateOf<String?>(null)
    var claimed by mutableStateOf(false)
}

internal val LocalBadgeAnnouncement = staticCompositionLocalOf<BadgeAnnouncement?> { null }

/**
 * For a component that is its own merged node and can carry a badge (an icon button, an avatar, a navigation
 * item): adds the description of the badge in the surrounding [CamoBadgedBox] to this node, so it reads
 * "Notifications, 3 new". Outside a badged box it does nothing.
 */
@Composable
public fun Modifier.badgeAnchor(): Modifier {
    val text = rememberBadgeText() ?: return this
    return semantics { stateDescription = text }
}

/**
 * The description of the badge in the surrounding [CamoBadgedBox], claimed for this anchor (so the box doesn't
 * add it too), or `null` outside one. For anchors that set their semantics in one block; others use [badgeAnchor].
 */
@Composable
public fun rememberBadgeText(): String? {
    val announcement = LocalBadgeAnnouncement.current ?: return null
    DisposableEffect(announcement) {
        announcement.claimed = true
        onDispose { announcement.claimed = false }
    }
    return announcement.text
}

/**
 * A count ("3") or, with no [count], a dot, usually in a [CamoBadgedBox] on an icon or avatar. Above [max] it
 * shows "99+"; a count of 0 (or less) shows nothing. A badge is never focusable: its [contentDescription]
 * (default "3 new" / "New") joins its anchor's announcement. Count changes aren't announced; apps announce
 * the ones that matter.
 */
@Composable
public fun CamoBadge(
    modifier: Modifier = Modifier,
    count: Int? = null,
    max: Int = 99,
    tone: Tone? = null,
    appearance: Appearance? = null,
    contentDescription: String? = null,
) {
    val skin = requireSkin("CamoBadge")
    val theme = requireTheme("CamoBadge")
    val style = remember(skin, theme) { skin.badge.defaults(theme).merge(theme.components.badge) }
    val hidden = count != null && count <= 0
    val text = badgeText(count, max)
    val announcement = LocalBadgeAnnouncement.current
    val description = if (hidden) null else contentDescription ?: text?.let(style.strings.count) ?: style.strings.dot
    SideEffect { announcement?.text = description }
    DisposableEffect(announcement) { onDispose { announcement?.text = null } }
    if (hidden) return
    val state = BadgeState(text, tone ?: style.defaultTone, badgeAppearance(appearance ?: style.defaultAppearance))
    val duration = theme.motion.durationShort
    AnimatedContent(
        targetState = state,
        modifier = modifier.clearAndSetSemantics {},
        transitionSpec = { (fadeIn(tween(duration)) + scaleIn(tween(duration))) togetherWith fadeOut(tween(duration)) },
        label = "badge",
    ) { shown -> skin.badge.Render(shown, theme, style, Modifier) }
}

/**
 * Anchors [badge] (a [CamoBadge]) on [content] (an icon, an icon button, an avatar): its center sits at the
 * content's top-end corner plus [BadgeStyle.offset], mirrored in right-to-left layouts. The badge doesn't change
 * the content's size and isn't clipped. Its description joins the content's announcement.
 */
@Composable
public fun CamoBadgedBox(
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoBadgedBox")
    val theme = requireTheme("CamoBadgedBox")
    val style = remember(skin, theme) { skin.badge.defaults(theme).merge(theme.components.badge) }
    val announcement = remember { BadgeAnnouncement() }
    val text = announcement.text
    val claimed = announcement.claimed
    // Plain content (an icon) merges into this node, which adds the badge; a claiming anchor adds it itself.
    val semantics = modifier.semantics(mergeDescendants = true) {
        if (!claimed && text != null) stateDescription = text
    }
    CompositionLocalProvider(LocalBadgeAnnouncement provides announcement) {
        Layout(content = { content(); badge() }, modifier = semantics) { measurables, constraints ->
            val anchor = measurables[0].measure(constraints)
            val badges = measurables.drop(1).map { it.measure(Constraints()) }
            val centerX = anchor.width + style.offset.x.roundToPx()
            val centerY = style.offset.y.roundToPx()
            layout(anchor.width, anchor.height) {
                anchor.placeRelative(0, 0)
                badges.forEach { it.placeRelative(centerX - it.width / 2, centerY - it.height / 2) }
            }
        }
    }
}
