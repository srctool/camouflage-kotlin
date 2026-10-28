package com.srctool.camouflage.skin.minimal.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.avatar.AvatarColors
import com.srctool.camouflage.core.avatar.AvatarRenderer
import com.srctool.camouflage.core.avatar.AvatarShape
import com.srctool.camouflage.core.avatar.AvatarSlots
import com.srctool.camouflage.core.avatar.AvatarState
import com.srctool.camouflage.core.avatar.AvatarStyle
import com.srctool.camouflage.core.avatar.PresenceColors
import com.srctool.camouflage.core.badge.BadgeRenderer
import com.srctool.camouflage.core.badge.BadgeState
import com.srctool.camouflage.core.badge.BadgeStyle
import com.srctool.camouflage.core.chip.ChipColors
import com.srctool.camouflage.core.chip.ChipKind
import com.srctool.camouflage.core.chip.ChipRenderer
import com.srctool.camouflage.core.chip.ChipSlots
import com.srctool.camouflage.core.chip.ChipState
import com.srctool.camouflage.core.chip.ChipStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.rememberSkinIndication

// Minimal's data display components (base Minimal Skin §3): hairline chips with an ink selection, small pills,
// neutral avatars.

private val Hairline = 1.dp
private const val DISABLED = 0.38f
private const val PERSON_ICON_SCALE = 0.6f
private const val UNBOUNDED_CAP = 100_000

internal object MinimalChipRenderer : ChipRenderer {
    override fun defaults(theme: CamoTheme): ChipStyle {
        val c = theme.colors
        val s = theme.spacing
        return ChipStyle(
            defaultAppearance = Appearance.Outline,
            height = 28.dp,
            shape = theme.shapes.small,
            padding = PaddingValues(horizontal = s.m),
            iconPadding = PaddingValues(horizontal = s.s),
            iconGap = s.xs + s.xxs,
            textStyle = theme.typography.labelLarge,
            iconSize = 16.dp,
            borderWidth = Hairline,
            showCheckWhenSelected = false,
            outlineColors = ChipColors(c.surface, c.onSurface, border = c.outline),
            softColors = ChipColors(c.surfaceContainer, c.onSurface),
            solidColors = ChipColors(c.primary, c.onPrimary),
            selectedColors = ChipColors(c.primary, c.onPrimary),
            groupSpacing = s.s,
            checkIcon = IconSource.Vector(CheckIcon),
            removeIcon = IconSource.Vector(ClearIcon),
        )
    }

    @Composable
    override fun Render(state: ChipState, slots: ChipSlots, theme: CamoTheme, style: ChipStyle, modifier: Modifier) =
        PaintChip(state, slots, theme, style, modifier)
}

/** A chip: the surface treatment and border, the check or leading icon, the label and the remove button. */
@Composable
internal fun PaintChip(state: ChipState, slots: ChipSlots, theme: CamoTheme, style: ChipStyle, modifier: Modifier) {
    val colors = style.colorsFor(state, theme)
    val showCheck = state.selected == true && style.showCheckWhenSelected
    val direction = LocalLayoutDirection.current
    val start = if (showCheck || slots.leading != null) style.iconPadding else style.padding
    val end = if (slots.remove != null) style.iconPadding else style.padding
    val feedback = if (state.kind != ChipKind.Tag) {
        Modifier.indication(state.interactionSource, rememberSkinIndication(colors.content, style.shape))
    } else {
        Modifier
    }
    Row(
        modifier
            .boundedWidth()
            .alpha(if (state.enabled) 1f else DISABLED)
            .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape, 0.dp, theme))
            .then(colors.border?.let { Modifier.border(style.borderWidth, it, style.shape) } ?: Modifier)
            .clip(style.shape)
            .then(feedback)
            .heightIn(min = style.height)
            .padding(start = start.calculateStartPadding(direction), end = end.calculateEndPadding(direction)),
        horizontalArrangement = Arrangement.spacedBy(style.iconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
            if (showCheck) CamoIcon(style.checkIcon, contentDescription = null) else slots.leading?.invoke()
            BasicText(
                state.label,
                Modifier.weight(1f, fill = false),
                style.textStyle.copy(color = colors.content),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            slots.remove?.invoke()
        }
    }
}

// In a horizontally scrolling row a chip gets an unbounded width, where its weighted label would measure to
// nothing: give it a finite one there (the label's ellipsis still works in bounded rows).
private fun Modifier.boundedWidth() = layout { measurable, constraints ->
    val bounded = if (constraints.hasBoundedWidth) constraints else constraints.copy(maxWidth = UNBOUNDED_CAP)
    val placeable = measurable.measure(bounded)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

internal object MinimalBadgeRenderer : BadgeRenderer {
    override fun defaults(theme: CamoTheme) = BadgeStyle(
        defaultAppearance = Appearance.Solid,
        defaultTone = Tone.Error,
        dotSize = 6.dp,
        height = 16.dp,
        minWidth = 16.dp,
        padding = PaddingValues(horizontal = theme.spacing.xs),
        textStyle = theme.typography.labelSmall,
        offset = DpOffset(-theme.spacing.xs, theme.spacing.xs),
        borderWidth = Hairline,
    )

    @Composable
    override fun Render(state: BadgeState, theme: CamoTheme, style: BadgeStyle, modifier: Modifier) =
        PaintBadge(state, theme, style, modifier)
}

/** A badge: a dot, or a pill with the count; the Outline appearance adds a border. */
@Composable
internal fun PaintBadge(state: BadgeState, theme: CamoTheme, style: BadgeStyle, modifier: Modifier) {
    val colors = style.colorsFor(state.appearance, state.tone, theme)
    val pill = theme.shapes.full
    val border = colors.border?.let { Modifier.border(style.borderWidth, it, pill) } ?: Modifier
    val text = state.text
    if (text == null) {
        Box(modifier.size(style.dotSize).background(colors.container, pill).then(border))
    } else {
        Box(
            modifier
                .heightIn(min = style.height)
                .defaultMinSize(minWidth = style.minWidth)
                .background(colors.container, pill)
                .then(border)
                .padding(style.padding),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text, style = style.textStyle.copy(color = colors.content), maxLines = 1)
        }
    }
}

internal object MinimalAvatarRenderer : AvatarRenderer {
    override fun defaults(theme: CamoTheme): AvatarStyle {
        val c = theme.colors
        val t = theme.typography
        val neutral = AvatarColors(c.surfaceContainerHighest, c.onSurface)
        return AvatarStyle(
            shape = AvatarShape.Circle,
            roundedShape = theme.shapes.small,
            sizeSmall = 24.dp,
            sizeMedium = 36.dp,
            sizeLarge = 48.dp,
            textStyleSmall = t.labelSmall,
            textStyleMedium = t.labelLarge,
            textStyleLarge = t.titleMedium,
            initialsPalette = listOf(neutral),
            neutralColors = neutral,
            presenceSize = 10.dp,
            presenceColors = PresenceColors(c.success, c.warning, c.error, c.outline, ring = c.surface),
            ringWidth = 2.dp,
            groupOverlap = 8.dp,
            personIcon = IconSource.Vector(PersonIcon),
        )
    }

    @Composable
    override fun Render(
        state: AvatarState,
        slots: AvatarSlots,
        theme: CamoTheme,
        style: AvatarStyle,
        modifier: Modifier,
    ) = PaintAvatar(state, slots, theme, style, modifier)
}

/** An avatar: initials (or the person icon) under the app's image, clipped; the presence dot outside the clip. */
@Composable
internal fun PaintAvatar(
    state: AvatarState,
    slots: AvatarSlots,
    theme: CamoTheme,
    style: AvatarStyle,
    modifier: Modifier,
) {
    val colors = style.colorsFor(state)
    val shape = style.shapeOf(state.shape, theme)
    val diameter = style.sizeOf(state.size)
    Box(modifier.size(diameter)) {
        Box(Modifier.matchParentSize().clip(shape).background(colors.container, shape), Alignment.Center) {
            val initials = state.initials
            if (initials != null) {
                val text = style.textStyleOf(state.size).copy(color = colors.content)
                BasicText(initials, style = text, maxLines = 1)
            } else {
                CamoIcon(style.personIcon, null, size = diameter * PERSON_ICON_SCALE, tint = colors.content)
            }
            slots.image?.let { Box(Modifier.matchParentSize()) { it() } }
        }
        state.presence?.let { presence ->
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(style.presenceSize)
                    .background(style.presenceColors.ring, CircleShape)
                    .padding(style.ringWidth)
                    .background(style.presenceColors.of(presence), CircleShape),
            )
        }
    }
}

internal val PersonIcon: ImageVector = ImageVector.Builder("person", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(16f, 8f)
        arcTo(4f, 4f, 0f, true, true, 8f, 8f)
        arcTo(4f, 4f, 0f, true, true, 16f, 8f)
        moveTo(4.5f, 20.5f)
        curveTo(5.5f, 16.5f, 8.5f, 14.5f, 12f, 14.5f)
        curveTo(15.5f, 14.5f, 18.5f, 16.5f, 19.5f, 20.5f)
    }
    .build()
