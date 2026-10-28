package com.srctool.camouflage.core.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.badge.rememberBadgeText
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme

/** True inside a list row's leading slot: the row says the name, so an avatar there is decorative. */
internal val LocalAvatarInRow = staticCompositionLocalOf { false }

/** The three avatar sizes; the theme sets each one's diameter. */
public enum class AvatarSize { Small, Medium, Large }

/** An avatar's outline: a circle, a rounded square ([AvatarStyle.roundedShape]) or a square. */
public enum class AvatarShape { Circle, Rounded, Square }

/** A person's status, shown as a small dot on the avatar and said after the name. */
public enum class Presence { Online, Away, Busy, Offline }

/** An avatar's background and initials color. */
@Immutable
public data class AvatarColors(val container: Color, val content: Color)

/** The presence dot's color per [Presence], and the [ring] around the dot and around avatars in a group. */
@Immutable
public data class PresenceColors(
    val online: Color,
    val away: Color,
    val busy: Color,
    val offline: Color,
    val ring: Color,
) {
    /** The dot color for [presence]. */
    public fun of(presence: Presence): Color = when (presence) {
        Presence.Online -> online
        Presence.Away -> away
        Presence.Busy -> busy
        Presence.Offline -> offline
    }
}

/** The words avatars say: each [Presence], and [more] for the "+N" avatar of a group. */
@Immutable
public data class AvatarStrings(
    val online: String = "online",
    val away: String = "away",
    val busy: String = "busy",
    val offline: String = "offline",
    val more: (count: Int) -> String = { "$it more" },
) {
    /** The word for [presence]. */
    public fun of(presence: Presence): String = when (presence) {
        Presence.Online -> online
        Presence.Away -> away
        Presence.Busy -> busy
        Presence.Offline -> offline
    }
}

/** Overrides for avatars. `null` keeps the skin's default. */
@Immutable
public data class AvatarTheme(
    /** The shape for the whole brand when a call passes none. */
    val shape: AvatarShape? = null,
    val roundedShape: Shape? = null,
    val sizeSmall: Dp? = null,
    val sizeMedium: Dp? = null,
    val sizeLarge: Dp? = null,
    val textStyleSmall: TextStyle? = null,
    val textStyleMedium: TextStyle? = null,
    val textStyleLarge: TextStyle? = null,
    /** Initials backgrounds, picked by a stable hash of the name. */
    val initialsPalette: List<AvatarColors>? = null,
    /** The colors of an avatar with no name (the person icon) and of a group's "+N". */
    val neutralColors: AvatarColors? = null,
    val presenceSize: Dp? = null,
    val presenceColors: PresenceColors? = null,
    /** The ring around the presence dot and around each avatar in a group. */
    val ringWidth: Dp? = null,
    val groupOverlap: Dp? = null,
    /** The icon of an avatar with no name and no image. */
    val personIcon: IconSource? = null,
    val strings: AvatarStrings? = null,
)

/** The resolved avatar style: every field set. */
@Immutable
public data class AvatarStyle(
    val shape: AvatarShape,
    val roundedShape: Shape,
    val sizeSmall: Dp,
    val sizeMedium: Dp,
    val sizeLarge: Dp,
    val textStyleSmall: TextStyle,
    val textStyleMedium: TextStyle,
    val textStyleLarge: TextStyle,
    val initialsPalette: List<AvatarColors>,
    val neutralColors: AvatarColors,
    val presenceSize: Dp,
    val presenceColors: PresenceColors,
    val ringWidth: Dp,
    val groupOverlap: Dp,
    val personIcon: IconSource,
    val strings: AvatarStrings = AvatarStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: AvatarTheme?): AvatarStyle = if (overrides == null) {
        this
    } else {
        AvatarStyle(
            shape = overrides.shape ?: shape,
            roundedShape = overrides.roundedShape ?: roundedShape,
            sizeSmall = overrides.sizeSmall ?: sizeSmall,
            sizeMedium = overrides.sizeMedium ?: sizeMedium,
            sizeLarge = overrides.sizeLarge ?: sizeLarge,
            textStyleSmall = overrides.textStyleSmall ?: textStyleSmall,
            textStyleMedium = overrides.textStyleMedium ?: textStyleMedium,
            textStyleLarge = overrides.textStyleLarge ?: textStyleLarge,
            initialsPalette = overrides.initialsPalette?.takeIf { it.isNotEmpty() } ?: initialsPalette,
            neutralColors = overrides.neutralColors ?: neutralColors,
            presenceSize = overrides.presenceSize ?: presenceSize,
            presenceColors = overrides.presenceColors ?: presenceColors,
            ringWidth = overrides.ringWidth ?: ringWidth,
            groupOverlap = overrides.groupOverlap ?: groupOverlap,
            personIcon = overrides.personIcon ?: personIcon,
            strings = overrides.strings ?: strings,
        )
    }

    /** The diameter for [size]. */
    public fun sizeOf(size: AvatarSize): Dp = when (size) {
        AvatarSize.Small -> sizeSmall
        AvatarSize.Medium -> sizeMedium
        AvatarSize.Large -> sizeLarge
    }

    /** The initials style for [size]. */
    public fun textStyleOf(size: AvatarSize): TextStyle = when (size) {
        AvatarSize.Small -> textStyleSmall
        AvatarSize.Medium -> textStyleMedium
        AvatarSize.Large -> textStyleLarge
    }

    /** The outline for [shape]. */
    public fun shapeOf(shape: AvatarShape, theme: CamoTheme): Shape = when (shape) {
        AvatarShape.Circle -> theme.shapes.full
        AvatarShape.Rounded -> roundedShape
        AvatarShape.Square -> RectangleShape
    }

    /** The colors for [state]: the palette entry its seed picks, or the neutral colors without a seed. */
    public fun colorsFor(state: AvatarState): AvatarColors =
        state.colorSeed?.let { initialsPalette[(it % initialsPalette.size.toUInt()).toInt()] } ?: neutralColors
}

/**
 * What an avatar renderer draws. With [initials] it shows them (under the image, if any); without, the
 * [AvatarStyle.personIcon]. [colorSeed] picks the palette entry (`null`: the neutral colors). [shape] is resolved.
 */
@Immutable
public data class AvatarState(
    val initials: String?,
    val colorSeed: UInt?,
    val size: AvatarSize,
    val shape: AvatarShape,
    val presence: Presence?,
)

/**
 * The app's [image] (its own image-loading composable). The renderer draws it on top of the initials, clipped
 * to the shape, so a missing, loading or failed image leaves the initials showing.
 */
@Stable
public class AvatarSlots(public val image: (@Composable () -> Unit)?)

/** Draws avatars. Apply `modifier` to the outermost node; draw the presence dot outside the clip. */
@Stable
public interface AvatarRenderer {
    /** The complete avatar style for [theme]. */
    public fun defaults(theme: CamoTheme): AvatarStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(state: AvatarState, slots: AvatarSlots, theme: CamoTheme, style: AvatarStyle, modifier: Modifier)
}

/**
 * A person or entity: the app's [image] (Coil's `AsyncImage`, say; core loads no images), on top of initials from
 * [name] in a color picked from the name, or a person icon with no name. It's announced as [name] plus the
 * [presence] ("Dwiki Riyadi, online"); with no name it's decorative, and so it is in a `CamoListItem`'s leading
 * slot, where the row already says the name.
 */
@Composable
public fun CamoAvatar(
    modifier: Modifier = Modifier,
    name: String? = null,
    image: (@Composable () -> Unit)? = null,
    size: AvatarSize = AvatarSize.Medium,
    shape: AvatarShape? = null,
    presence: Presence? = null,
) {
    val skin = requireSkin("CamoAvatar")
    val theme = requireTheme("CamoAvatar")
    val style = remember(skin, theme) { skin.avatar.defaults(theme).merge(theme.components.avatar) }
    val initials = remember(name) { avatarInitials(name) }
    val seed = remember(name, initials) { if (initials != null) avatarColorSeed(name.orEmpty()) else null }
    val badge = rememberBadgeText()
    val description = name?.takeIf { it.isNotBlank() && !LocalAvatarInRow.current }?.let { n ->
        presence?.let { "$n, ${style.strings.of(it)}" } ?: n
    }
    val semantics = modifier.clearAndSetSemantics {
        if (description != null) {
            contentDescription = description
            role = Role.Image
        }
        if (badge != null) stateDescription = badge
    }
    skin.avatar.Render(
        AvatarState(initials, seed, size, shape ?: style.shape, presence),
        AvatarSlots(image),
        theme,
        style,
        semantics,
    )
}

/**
 * Avatars that overlap by [AvatarStyle.groupOverlap], each in a ring, showing at most [max] and then a "+N"
 * avatar ("2 more") for the rest. [size] sizes the "+N".
 */
@Composable
public fun CamoAvatarGroup(
    avatars: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    max: Int = 3,
    size: AvatarSize = AvatarSize.Medium,
) {
    val skin = requireSkin("CamoAvatarGroup")
    val theme = requireTheme("CamoAvatarGroup")
    val style = remember(skin, theme) { skin.avatar.defaults(theme).merge(theme.components.avatar) }
    val shape = style.shapeOf(style.shape, theme)
    val rest = avatars.size - max
    val ring = Modifier.background(style.presenceColors.ring, shape).padding(style.ringWidth)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(-style.groupOverlap)) {
        avatars.take(max).forEach { avatar -> Box(ring) { avatar() } }
        if (rest > 0) {
            Box(ring) {
                skin.avatar.Render(
                    AvatarState("+$rest", colorSeed = null, size = size, shape = style.shape, presence = null),
                    AvatarSlots(null),
                    theme,
                    style,
                    Modifier.clearAndSetSemantics { contentDescription = style.strings.more(rest) },
                )
            }
        }
    }
}
