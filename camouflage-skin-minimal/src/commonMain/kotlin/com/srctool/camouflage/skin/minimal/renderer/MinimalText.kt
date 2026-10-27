package com.srctool.camouflage.skin.minimal.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.search.SearchFieldColors
import com.srctool.camouflage.core.search.SearchFieldRenderer
import com.srctool.camouflage.core.search.SearchFieldSlots
import com.srctool.camouflage.core.search.SearchFieldState
import com.srctool.camouflage.core.search.SearchFieldStyle
import com.srctool.camouflage.core.text.CamoText
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.core.textfield.TextFieldColors
import com.srctool.camouflage.core.textfield.TextFieldRenderer
import com.srctool.camouflage.core.textfield.TextFieldSlots
import com.srctool.camouflage.core.textfield.TextFieldState
import com.srctool.camouflage.core.textfield.TextFieldStyle
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.FloatingLabelBox
import com.srctool.camouflage.core.util.ProvideContentStyle

// Minimal's text field and search field (base Minimal Skin §3.4): a 44 field with a hairline, a 2 dp primary
// ring when focused, the label above by default.

private val Hairline = 1.dp
private const val DISABLED = 0.38f

internal data class MinimalTextFieldRenderer(private val labelPlacement: LabelPlacement) : TextFieldRenderer {
    override fun defaults(theme: CamoTheme): TextFieldStyle {
        val c = theme.colors
        val t = theme.typography
        return TextFieldStyle(
            labelPlacement = labelPlacement,
            height = 44.dp,
            shape = theme.shapes.medium,
            contentPadding = PaddingValues(horizontal = theme.spacing.m),
            textStyle = t.bodyLarge,
            labelStyle = t.bodyLarge,
            floatingLabelStyle = t.labelMedium,
            supportingTextStyle = t.bodySmall,
            iconSize = 18.dp,
            borderWidth = Hairline,
            focusedBorderWidth = 2.dp,
            colors = TextFieldColors(
                container = Color.Transparent,
                border = c.outline,
                focusedBorder = c.primary,
                errorBorder = c.error,
                text = c.onSurface,
                label = c.onSurface,
                placeholder = c.onSurfaceVariant,
                icon = c.onSurfaceVariant,
            ),
        )
    }

    @Composable
    override fun Render(
        state: TextFieldState,
        slots: TextFieldSlots,
        theme: CamoTheme,
        style: TextFieldStyle,
        modifier: Modifier,
    ) {
        val c = style.colors
        val error = state.error != null
        val border = when {
            error -> c.errorBorder
            state.focused -> c.focusedBorder
            else -> c.border
        }
        val width = if (state.focused) style.focusedBorderWidth else style.borderWidth
        val floating = style.labelPlacement == LabelPlacement.Floating && state.label != null
        val frame: @Composable () -> Unit = {
            FieldRow(
                state = state,
                slots = slots,
                style = style,
                theme = theme,
                showPlaceholder = state.isEmpty && (!floating || state.focused),
                modifier = Modifier
                    .background(c.container, style.shape)
                    .border(width, border, style.shape),
            )
        }
        Column(modifier.alpha(if (state.enabled) 1f else DISABLED)) {
            Labelled(state, slots, style, theme, floating, frame)
            Message(state, style, theme)
        }
    }
}

internal object MinimalSearchFieldRenderer : SearchFieldRenderer {
    override fun defaults(theme: CamoTheme): SearchFieldStyle {
        val c = theme.colors
        return SearchFieldStyle(
            height = 40.dp,
            shape = theme.shapes.medium,
            textStyle = theme.typography.bodyLarge,
            placeholderStyle = theme.typography.bodyLarge,
            iconSize = 18.dp,
            searchIcon = IconSource.Vector(SearchIcon),
            clearIcon = IconSource.Vector(ClearIcon),
            colors = SearchFieldColors(
                container = c.surfaceContainer,
                text = c.onSurface,
                placeholder = c.onSurfaceVariant,
                icon = c.onSurfaceVariant,
            ),
        )
    }

    @Composable
    override fun Render(
        state: SearchFieldState,
        slots: SearchFieldSlots,
        theme: CamoTheme,
        style: SearchFieldStyle,
        modifier: Modifier,
    ) {
        val c = style.colors
        Row(
            modifier
                .alpha(if (state.enabled) 1f else DISABLED)
                .heightIn(min = style.height)
                .background(c.container, style.shape)
                .border(
                    if (state.focused) 2.dp else 0.dp,
                    if (state.focused) theme.colors.primary else Color.Transparent,
                    style.shape,
                )
                .padding(start = theme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideContentStyle(c.icon, IconStyle(style.iconSize, c.icon)) { slots.leading() }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart, propagateMinConstraints = true) {
                if (state.isEmpty) {
                    BasicText(state.placeholder, Modifier.clearAndSetSemantics {},
                        style = style.placeholderStyle.copy(color = c.placeholder), maxLines = 1)
                }
                slots.input()
            }
            ProvideContentStyle(c.icon, IconStyle(style.iconSize, c.icon)) {
                slots.clear?.invoke()
                slots.trailing?.invoke()
            }
        }
    }
}

// The field with its label: above it, floating in it, or none.
@Composable
private fun Labelled(
    state: TextFieldState,
    slots: TextFieldSlots,
    style: TextFieldStyle,
    theme: CamoTheme,
    floating: Boolean,
    frame: @Composable () -> Unit,
) {
    val c = style.colors
    val error = state.error != null
    val floated = state.focused || !state.isEmpty
    val label = state.label
    when {
        label == null -> frame()
        floating -> FloatingLabelBox(
            label = label,
            floated = floated,
            restingStyle = style.labelStyle,
            floatedStyle = style.floatingLabelStyle,
            color = if (error) c.errorBorder else if (state.focused) c.focusedBorder else c.placeholder,
            cutout = theme.colors.surface,
            startInset = labelStart(style, slots, theme),
            theme = theme,
            field = frame,
        )
        else -> {
            CamoText(
                label,
                Modifier.clearAndSetSemantics {}.padding(bottom = theme.spacing.xs),
                style = style.floatingLabelStyle,
                color = c.label,
            )
            frame()
        }
    }
}

// The framed row: prefix icon and text, the input with its placeholder, suffix text and icon.
@Composable
private fun FieldRow(
    state: TextFieldState,
    slots: TextFieldSlots,
    style: TextFieldStyle,
    theme: CamoTheme,
    showPlaceholder: Boolean,
    modifier: Modifier,
) {
    val c = style.colors
    val icon = if (state.error != null) c.errorBorder else c.icon
    val affix = state.focused || !state.isEmpty
    val affixStyle = style.textStyle.copy(color = c.placeholder)
    Row(
        modifier.heightIn(min = style.height).padding(style.contentPadding),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvideContentStyle(c.icon, IconStyle(style.iconSize, c.icon)) { slots.prefixIcon?.invoke() }
        if (affix) state.prefixText?.let { BasicText(it, style = affixStyle) }
        Box(
            Modifier.weight(1f).padding(vertical = theme.spacing.s),
            contentAlignment = Alignment.CenterStart,
            propagateMinConstraints = true,
        ) {
            if (showPlaceholder) {
                state.placeholder?.let {
                    BasicText(it, Modifier.clearAndSetSemantics {}, style = style.textStyle.copy(color = c.placeholder),
                        maxLines = 1)
                }
            }
            slots.input()
        }
        if (affix) state.suffixText?.let { BasicText(it, style = affixStyle) }
        ProvideContentStyle(icon, IconStyle(style.iconSize, icon)) { slots.suffixIcon?.invoke() }
    }
}

// The supporting text, or the error in its place. Core marks the error on the input, so the text hides.
@Composable
private fun Message(state: TextFieldState, style: TextFieldStyle, theme: CamoTheme) {
    val error = state.error
    val text = error ?: state.supportingText ?: return
    CamoText(
        text,
        Modifier.padding(top = theme.spacing.xs).let { if (error != null) it.clearAndSetSemantics {} else it },
        style = style.supportingTextStyle,
        color = if (error != null) style.colors.errorBorder else style.colors.placeholder,
    )
}

// Where a floating label rests: after the padding and the prefix icon.
@Composable
private fun labelStart(style: TextFieldStyle, slots: TextFieldSlots, theme: CamoTheme): Dp {
    val start = style.contentPadding.calculateStartPadding(LocalLayoutDirection.current)
    return if (slots.prefixIcon != null) start + style.iconSize + theme.spacing.s else start
}

internal val SearchIcon: ImageVector = ImageVector.Builder("search", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(17f, 10.5f)
        arcTo(6.5f, 6.5f, 0f, true, true, 4f, 10.5f)
        arcTo(6.5f, 6.5f, 0f, true, true, 17f, 10.5f)
        moveTo(15.5f, 15.5f)
        lineTo(20f, 20f)
    }
    .build()

internal val ClearIcon: ImageVector = ImageVector.Builder("clear", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f)
    }
    .build()
