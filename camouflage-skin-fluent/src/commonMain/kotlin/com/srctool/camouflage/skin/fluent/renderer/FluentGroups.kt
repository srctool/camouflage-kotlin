package com.srctool.camouflage.skin.fluent.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.buttongroup.ButtonGroupRenderer
import com.srctool.camouflage.core.buttongroup.ButtonGroupState
import com.srctool.camouflage.core.buttongroup.ButtonGroupStyle
import com.srctool.camouflage.core.buttongroup.GroupButtonSlots
import com.srctool.camouflage.core.buttongroup.GroupButtonState
import com.srctool.camouflage.core.buttongroup.GroupColors
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.splitbutton.SplitButtonBehavior
import com.srctool.camouflage.core.splitbutton.SplitButtonRenderer
import com.srctool.camouflage.core.splitbutton.SplitButtonSlots
import com.srctool.camouflage.core.splitbutton.SplitButtonState
import com.srctool.camouflage.core.splitbutton.SplitButtonStyle
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.toolbar.ToolbarColors
import com.srctool.camouflage.core.toolbar.ToolbarPlacement
import com.srctool.camouflage.core.toolbar.ToolbarRenderer
import com.srctool.camouflage.core.toolbar.ToolbarState
import com.srctool.camouflage.core.toolbar.ToolbarStyle
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication

// Fluent's button groups, split buttons and toolbars (base Fluent Skin):
// subtle fills, small corners and a flat docked command bar.

private val Hairline = 1.dp
private const val DISABLED_CONTAINER = 0.12f
private const val DISABLED_CONTENT = 0.38f
private const val DIVIDER_ALPHA = 0.4f
private const val HALF_TURN = 180f

internal object FluentButtonGroupRenderer : ButtonGroupRenderer {
    override fun defaults(theme: CamoTheme): ButtonGroupStyle {
        val c = theme.colors
        return ButtonGroupStyle(
            defaultAppearance = Appearance.Outline,
            height = 32.dp,
            shape = theme.shapes.small,
            innerShape = theme.shapes.small,
            spacing = theme.spacing.xs,
            borderWidth = Hairline,
            iconSize = 20.dp,
            iconGap = theme.spacing.s,
            textStyle = theme.typography.labelLarge,
            colors = GroupColors(c.onSurface, c.outlineVariant, c.secondaryContainer, c.onSecondaryContainer),
        )
    }

    @Composable
    override fun Container(
        state: ButtonGroupState,
        theme: CamoTheme,
        style: ButtonGroupStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        // Connected outlined buttons overlap by one border width so the shared edge is a single line.
        val gap = when {
            !state.connected -> style.spacing
            state.appearance == Appearance.Outline -> -style.borderWidth
            else -> 0.dp
        }
        Row(modifier, Arrangement.spacedBy(gap), Alignment.CenterVertically, content)
    }

    @Composable
    override fun Button(
        state: GroupButtonState,
        slots: GroupButtonSlots,
        theme: CamoTheme,
        style: ButtonGroupStyle,
        modifier: Modifier,
    ) {
        val look = groupLook(state, style, theme)
        val shape = groupShape(style, state.position, state.connected)
        Box(
            modifier
                .heightIn(min = style.height)
                .then(Camouflage.skin.surfaceTreatment.modifier(look.container, shape, 0.dp, theme))
                .then(look.border?.let { Modifier.border(style.borderWidth, it, shape) } ?: Modifier)
                .indication(state.interactionSource, rememberSkinIndication(look.content, shape))
                .padding(horizontal = theme.spacing.m),
            contentAlignment = Alignment.Center,
        ) {
            ProvideContentStyle(look.content, IconStyle(style.iconSize, look.content)) {
                Row(Modifier, Arrangement.spacedBy(style.iconGap), Alignment.CenterVertically) {
                    slots.icon?.invoke()
                    slots.text?.let { Label(it, style.textStyle, look.content) }
                }
            }
        }
    }
}

private class GroupLook(val container: Color, val content: Color, val border: Color?)

// A pressed toggle fills with the selected color; otherwise outlined and plain groups stay transparent.
private fun groupLook(state: GroupButtonState, style: ButtonGroupStyle, theme: CamoTheme): GroupLook {
    val colors = style.colorsFor(state.tone, theme)
    val onSurface = theme.colors.onSurface
    val pressed = state.selected == true
    val rest = when {
        pressed -> colors.selectedContainer
        state.appearance.isPlain() -> Color.Transparent
        else -> theme.colors.surfaceContainerHigh
    }
    val container = if (state.enabled || rest == Color.Transparent) rest else onSurface.disabled(DISABLED_CONTAINER)
    val tint = if (pressed) colors.selectedContent else colors.content
    val edge = if (state.enabled) colors.border else onSurface.disabled(DISABLED_CONTAINER)
    return GroupLook(
        container,
        if (state.enabled) tint else onSurface.disabled(DISABLED_CONTENT),
        edge.takeIf { state.appearance.isOutlined() && !pressed },
    )
}

private fun Appearance.isPlain() = this == Appearance.Outline || this == Appearance.Ghost || this == Appearance.Link

private fun Appearance.isOutlined() = this == Appearance.Outline || this == Appearance.Raised

// A connected group rounds only its outer corners: the first button's start side, the last button's end side.
private fun groupShape(style: ButtonGroupStyle, position: ItemPosition, connected: Boolean): Shape {
    val outer = style.shape as? CornerBasedShape
    val inner = style.innerShape as? CornerBasedShape
    val joined = connected && position.count > 1
    if (!joined || outer == null || inner == null) return style.shape
    return when {
        position.isFirst -> outer.copy(topEnd = inner.topEnd, bottomEnd = inner.bottomEnd)
        position.isLast -> outer.copy(topStart = inner.topStart, bottomStart = inner.bottomStart)
        else -> inner
    }
}

internal object FluentSplitButtonRenderer : SplitButtonRenderer {
    override fun defaults(theme: CamoTheme) = SplitButtonStyle(
        defaultAppearance = Appearance.Solid,
        height = 32.dp,
        shape = theme.shapes.small,
        innerShape = theme.shapes.small,
        arrowWidth = 32.dp,
        gap = 1.dp,
        iconSize = 20.dp,
        iconGap = theme.spacing.s,
        padding = theme.spacing.l,
        textStyle = theme.typography.labelLarge,
        arrowIcon = IconSource.Vector(ChevronDown),
    )

    @Composable
    override fun Render(
        state: SplitButtonState,
        slots: SplitButtonSlots,
        behavior: SplitButtonBehavior,
        theme: CamoTheme,
        style: SplitButtonStyle,
        modifier: Modifier,
    ) {
        val skin = Camouflage.skin
        val rest = remember(skin, theme, state.appearance, state.tone) {
            skin.button.defaults(theme).merge(theme.components.button).colorsFor(state.appearance, state.tone, theme)
        }
        val onSurface = theme.colors.onSurface
        val container = when {
            rest.container == Color.Transparent -> Color.Transparent
            state.enabled -> rest.container
            else -> onSurface.disabled(DISABLED_CONTAINER)
        }
        val content = if (state.enabled) rest.content else onSurface.disabled(DISABLED_CONTENT)
        val border = rest.border?.let { if (state.enabled) it else onSurface.disabled(DISABLED_CONTAINER) }
        val parts = Parts(state, style, theme, container, content, border)
        if (!state.hasAction) {
            Box(
                modifier.then(behavior.main).then(parts.part(style.shape, state.mainInteractionSource)),
                contentAlignment = Alignment.Center,
            ) {
                PartRow(style) {
                    ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                        slots.prefixIcon?.invoke()
                        Label(state.text, style.textStyle, content)
                        Chevron(style, state.expanded)
                    }
                }
            }
            return
        }
        Row(modifier.height(IntrinsicSize.Min).heightIn(min = style.height), Arrangement.spacedBy(style.gap)) {
            Box(
                Modifier.then(behavior.main).then(parts.part(startShape(style), state.mainInteractionSource)),
                contentAlignment = Alignment.Center,
            ) {
                PartRow(style) {
                    ProvideContentStyle(content, IconStyle(style.iconSize, content)) {
                        slots.prefixIcon?.invoke()
                        Label(state.text, style.textStyle, content)
                    }
                }
            }
            if (style.gap == 0.dp && border == null) {
                Box(Modifier.width(Hairline).fillMaxHeight().background(content.copy(alpha = DIVIDER_ALPHA)))
            }
            Box(
                Modifier
                    .then(behavior.arrow)
                    .width(style.arrowWidth)
                    .fillMaxHeight()
                    .then(parts.part(endShape(style), state.arrowInteractionSource)),
                contentAlignment = Alignment.Center,
            ) {
                ProvideContentStyle(content, IconStyle(style.iconSize, content)) { Chevron(style, state.expanded) }
            }
        }
    }
}

private class Parts(
    val state: SplitButtonState,
    val style: SplitButtonStyle,
    val theme: CamoTheme,
    val container: Color,
    val content: Color,
    val border: Color?,
) {
    @Composable
    fun part(shape: Shape, source: InteractionSource): Modifier =
        Modifier
            .heightIn(min = style.height)
            .then(Camouflage.skin.surfaceTreatment.modifier(container, shape, 0.dp, theme))
            .then(border?.let { Modifier.border(Hairline, it, shape) } ?: Modifier)
            .indication(source, rememberSkinIndication(content, shape))
}

@Composable
private fun Label(text: String, style: TextStyle, content: Color) {
    BasicText(text, style = style.copy(color = content), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun PartRow(style: SplitButtonStyle, content: @Composable () -> Unit) {
    Row(Modifier.padding(horizontal = style.padding), Arrangement.spacedBy(style.iconGap), Alignment.CenterVertically) {
        content()
    }
}

@Composable
private fun Chevron(style: SplitButtonStyle, expanded: Boolean) {
    Box(Modifier.rotate(if (expanded) HALF_TURN else 0f), contentAlignment = Alignment.Center) {
        CamoIcon(style.arrowIcon, contentDescription = null)
    }
}

private fun startShape(style: SplitButtonStyle): Shape = splitShape(style, main = true)

private fun endShape(style: SplitButtonStyle): Shape = splitShape(style, main = false)

// The two parts round their outer corners; where they meet they use the inner corners (a plain edge when joined).
private fun splitShape(style: SplitButtonStyle, main: Boolean): Shape {
    val outer = style.shape
    val inner = style.innerShape
    if (outer !is CornerBasedShape || inner !is CornerBasedShape) return outer
    return if (main) {
        outer.copy(topEnd = inner.topEnd, bottomEnd = inner.bottomEnd)
    } else {
        outer.copy(topStart = inner.topStart, bottomStart = inner.bottomStart)
    }
}

internal object FluentToolbarRenderer : ToolbarRenderer {
    override fun defaults(theme: CamoTheme) = ToolbarStyle(
        placement = ToolbarPlacement.Docked,
        height = 44.dp,
        actionSize = 48.dp,
        floatingMargin = theme.spacing.l,
        floatingShape = theme.shapes.medium,
        floatingElevation = ElevationLevel.Level4,
        actionSpacing = theme.spacing.xxs,
        padding = theme.spacing.s,
        showDivider = false,
        overflowIcon = IconSource.Vector(MoreIcon),
        colors = ToolbarColors(theme.colors.surfaceContainerLow, theme.colors.outlineVariant),
    )

    @Composable
    override fun Container(
        state: ToolbarState,
        theme: CamoTheme,
        style: ToolbarStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) {
        val floating = state.placement == ToolbarPlacement.Floating
        val colors = style.colors
        val shape = if (floating) style.floatingShape else RectangleShape
        val depth = if (floating) theme.elevation.of(style.floatingElevation) else 0.dp
        val frame = if (floating) Modifier.padding(style.floatingMargin) else Modifier
        val line = if (!floating && style.showDivider) {
            Modifier.drawBehind {
                val y = size.height - Hairline.toPx() / 2
                drawLine(colors.divider, Offset(0f, y), Offset(size.width, y), Hairline.toPx())
            }
        } else {
            Modifier
        }
        Box(
            modifier
                .then(frame)
                .fillMaxWidth()
                .heightIn(min = style.height)
                .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, shape, depth, theme))
                .then(line)
                .padding(horizontal = style.padding),
            contentAlignment = Alignment.CenterStart,
        ) { content() }
    }
}

internal val MoreIcon: ImageVector = ImageVector.Builder("more-vertical", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
        moveTo(12f, 5f)
        lineTo(12f, 5.01f)
        moveTo(12f, 12f)
        lineTo(12f, 12.01f)
        moveTo(12f, 19f)
        lineTo(12f, 19.01f)
    }
    .build()
