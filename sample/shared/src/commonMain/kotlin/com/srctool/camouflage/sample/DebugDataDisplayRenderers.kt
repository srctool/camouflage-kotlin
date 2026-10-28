package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
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
import com.srctool.camouflage.core.chip.ChipRenderer
import com.srctool.camouflage.core.chip.ChipSlots
import com.srctool.camouflage.core.chip.ChipState
import com.srctool.camouflage.core.chip.ChipStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.sample.resources.Res
import com.srctool.camouflage.sample.resources.ic_check
import com.srctool.camouflage.sample.resources.ic_delete
import com.srctool.camouflage.sample.resources.ic_star

// The M7 data display components in the debug skin: outlined boxes and plain text.

object DebugChipRenderer : ChipRenderer {
    override fun defaults(theme: CamoTheme): ChipStyle {
        val c = theme.colors
        val outline = ChipColors(c.surface, c.onSurface, c.outline)
        return ChipStyle(
            Appearance.Outline, 32.dp, RectangleShape, PaddingValues(horizontal = 12.dp),
            PaddingValues(horizontal = 8.dp), 4.dp, theme.typography.labelLarge, 18.dp, 1.dp, true, outline,
            ChipColors(c.secondaryContainer, c.onSecondaryContainer), ChipColors(c.primary, c.onPrimary),
            ChipColors(c.secondaryContainer, c.onSecondaryContainer), 8.dp, IconSource.Resource(Res.drawable.ic_check),
            IconSource.Resource(Res.drawable.ic_delete),
        )
    }

    @Composable
    override fun Render(state: ChipState, slots: ChipSlots, theme: CamoTheme, style: ChipStyle, modifier: Modifier) {
        val colors = style.colorsFor(state, theme)
        Row(
            modifier
                .background(colors.container)
                .then(colors.border?.let { Modifier.border(1.dp, it) } ?: Modifier)
                .heightIn(min = style.height)
                .padding(style.padding),
            horizontalArrangement = Arrangement.spacedBy(style.iconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                if (state.selected == true && style.showCheckWhenSelected) {
                    CamoIcon(style.checkIcon, null)
                } else {
                    slots.leading?.invoke()
                }
                BasicText(state.label, style = style.textStyle.copy(color = colors.content), maxLines = 1)
                slots.remove?.invoke()
            }
        }
    }
}

object DebugBadgeRenderer : BadgeRenderer {
    override fun defaults(theme: CamoTheme) = BadgeStyle(
        Appearance.Solid, Tone.Error, 8.dp, 16.dp, 16.dp, PaddingValues(horizontal = 4.dp),
        theme.typography.labelSmall, DpOffset(-theme.spacing.xs, theme.spacing.xs), 1.dp,
    )

    @Composable
    override fun Render(state: BadgeState, theme: CamoTheme, style: BadgeStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.appearance, state.tone, theme)
        val text = state.text
    if (text == null) {
            Box(modifier.size(style.dotSize).background(colors.container))
        } else {
            Box(
                modifier.heightIn(min = style.height).defaultMinSize(minWidth = style.minWidth)
                    .background(colors.container).padding(style.padding),
                contentAlignment = Alignment.Center,
            ) { BasicText(text, style = style.textStyle.copy(color = colors.content)) }
        }
    }
}

object DebugAvatarRenderer : AvatarRenderer {
    override fun defaults(theme: CamoTheme): AvatarStyle {
        val c = theme.colors
        val t = theme.typography
        return AvatarStyle(
            AvatarShape.Square, RectangleShape, 24.dp, 40.dp, 56.dp, t.labelSmall, t.labelLarge, t.titleMedium,
            listOf(
                AvatarColors(c.primaryContainer, c.onPrimaryContainer),
                AvatarColors(c.secondaryContainer, c.onSecondaryContainer),
                AvatarColors(c.tertiaryContainer, c.onTertiaryContainer),
            ),
            AvatarColors(c.surfaceContainerHighest, c.onSurface), 10.dp,
            PresenceColors(c.success, c.warning, c.error, c.outline, c.surface), 2.dp, 8.dp,
            IconSource.Resource(Res.drawable.ic_star),
        )
    }

    @Composable
    override fun Render(
        state: AvatarState,
        slots: AvatarSlots,
        theme: CamoTheme,
        style: AvatarStyle,
        modifier: Modifier,
    ) {
        val colors = style.colorsFor(state)
        val shape = style.shapeOf(state.shape, theme)
        Box(modifier.size(style.sizeOf(state.size))) {
            Box(Modifier.matchParentSize().clip(shape).background(colors.container), Alignment.Center) {
                val initials = state.initials
                if (initials != null) {
                    BasicText(initials, style = style.textStyleOf(state.size).copy(color = colors.content))
                } else {
                    CamoIcon(style.personIcon, null, tint = colors.content)
                }
                slots.image?.let { Box(Modifier.matchParentSize()) { it() } }
            }
            state.presence?.let {
                val dot = Modifier.align(Alignment.BottomEnd).size(style.presenceSize)
                Box(dot.background(style.presenceColors.of(it)))
            }
        }
    }
}
