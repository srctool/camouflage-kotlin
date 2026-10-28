package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
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

// The M7 data display components in the plain test skin: boxes and text, no decoration.

private val TestIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())

internal object PlainChip : ChipRenderer {
    override fun defaults(theme: CamoTheme): ChipStyle {
        val c = theme.colors
        val plain = ChipColors(c.surface, c.onSurface, c.outline)
        return ChipStyle(
            Appearance.Outline, 32.dp, RectangleShape, PaddingValues(horizontal = 16.dp),
            PaddingValues(horizontal = 8.dp), 8.dp, theme.typography.labelLarge, 18.dp, 1.dp, true, plain, plain,
            plain, ChipColors(c.primary, c.onPrimary), 8.dp, TestIcon, TestIcon,
        )
    }

    @Composable
    override fun Render(state: ChipState, slots: ChipSlots, theme: CamoTheme, style: ChipStyle, modifier: Modifier) {
        val colors = style.colorsFor(state, theme)
        Row(
            modifier.height(style.height).background(colors.container).padding(style.padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideContentStyle(colors.content, IconStyle(style.iconSize, colors.content)) {
                if (state.selected == true && style.showCheckWhenSelected) {
                    CamoIcon(style.checkIcon, contentDescription = null)
                } else {
                    slots.leading?.invoke()
                }
                BasicText(state.label, maxLines = 1)
                slots.remove?.invoke()
            }
        }
    }
}

internal object PlainBadge : BadgeRenderer {
    override fun defaults(theme: CamoTheme) = BadgeStyle(
        Appearance.Solid, Tone.Error, 6.dp, 16.dp, 16.dp, PaddingValues(horizontal = 4.dp),
        theme.typography.labelSmall, DpOffset((-4).dp, 4.dp), 1.dp,
    )

    @Composable
    override fun Render(state: BadgeState, theme: CamoTheme, style: BadgeStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.appearance, state.tone, theme)
        if (state.text == null) {
            Box(modifier.size(style.dotSize).background(colors.container))
        } else {
            BasicText(
                state.text,
                modifier.height(style.height).defaultMinSize(minWidth = style.minWidth).background(colors.container),
            )
        }
    }
}

internal object PlainAvatar : AvatarRenderer {
    override fun defaults(theme: CamoTheme): AvatarStyle {
        val c = theme.colors
        val t = theme.typography
        return AvatarStyle(
            AvatarShape.Circle, theme.shapes.small, 24.dp, 40.dp, 56.dp, t.labelLarge, t.titleMedium, t.titleLarge,
            listOf(
                AvatarColors(c.primaryContainer, c.onPrimaryContainer),
                AvatarColors(c.infoContainer, c.onInfoContainer),
            ),
            AvatarColors(c.surfaceContainerHighest, c.onSurface), 10.dp,
            PresenceColors(c.success, c.warning, c.error, c.outline, c.surface), 2.dp, 8.dp, TestIcon,
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
        Box(modifier.size(style.sizeOf(state.size)).clip(shape).background(colors.container), Alignment.Center) {
            if (state.initials != null) BasicText(state.initials) else CamoIcon(style.personIcon, null)
            slots.image?.invoke()
        }
    }
}
