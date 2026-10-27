package com.srctool.camouflage.sample

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.search.SearchFieldColors
import com.srctool.camouflage.core.search.SearchFieldRenderer
import com.srctool.camouflage.core.search.SearchFieldSlots
import com.srctool.camouflage.core.search.SearchFieldState
import com.srctool.camouflage.core.search.SearchFieldStyle
import com.srctool.camouflage.core.textfield.LabelPlacement
import com.srctool.camouflage.core.textfield.TextFieldColors
import com.srctool.camouflage.core.textfield.TextFieldRenderer
import com.srctool.camouflage.core.textfield.TextFieldSlots
import com.srctool.camouflage.core.textfield.TextFieldState
import com.srctool.camouflage.core.textfield.TextFieldStyle
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.sample.resources.Res
import com.srctool.camouflage.sample.resources.ic_delete
import com.srctool.camouflage.sample.resources.ic_search

// DebugSkin's text inputs: a bordered column (label, a row of prefix / input / suffix, then the message).

object DebugTextFieldRenderer : TextFieldRenderer {
    override fun defaults(theme: CamoTheme): TextFieldStyle {
        val c = theme.colors
        val t = theme.typography
        return TextFieldStyle(
            LabelPlacement.Above, 48.dp, theme.shapes.none, PaddingValues(horizontal = theme.spacing.m), t.bodyLarge,
            t.bodyLarge, t.labelMedium, t.bodySmall, 20.dp, 1.dp, 2.dp,
            TextFieldColors(Color.Transparent, c.outline, DebugFocus, c.error, c.onSurface, c.onSurface,
                c.onSurfaceVariant, c.onSurfaceVariant),
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
        val border = when {
            state.error != null -> c.errorBorder
            state.focused -> c.focusedBorder
            else -> c.border
        }
        Column(modifier) {
            state.label?.let {
                BasicText("[${style.labelPlacement}] $it", Modifier.clearAndSetSemantics {},
                    style = style.floatingLabelStyle.copy(color = c.label))
            }
            Row(
                Modifier.heightIn(min = style.height).border(if (state.focused) 2.dp else 1.dp, border)
                    .padding(style.contentPadding),
                horizontalArrangement = Arrangement.spacedBy(theme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slots.prefixIcon?.invoke()
                state.prefixText?.let { BasicText(it, style = style.textStyle.copy(color = c.placeholder)) }
                Box(Modifier.weight(1f), propagateMinConstraints = true) {
                    if (state.isEmpty) {
                        state.placeholder?.let {
                            BasicText(
                                it,
                                Modifier.clearAndSetSemantics {},
                                style = style.textStyle.copy(color = c.placeholder),
                            )
                        }
                    }
                    slots.input()
                }
                state.suffixText?.let { BasicText(it, style = style.textStyle.copy(color = c.placeholder)) }
                slots.suffixIcon?.invoke()
            }
            (state.error ?: state.supportingText)?.let {
                val color = if (state.error != null) c.errorBorder else c.placeholder
                BasicText(it, Modifier.clearAndSetSemantics {}, style = style.supportingTextStyle.copy(color = color))
            }
        }
    }
}

object DebugSearchFieldRenderer : SearchFieldRenderer {
    override fun defaults(theme: CamoTheme) = SearchFieldStyle(
        48.dp, theme.shapes.none, theme.typography.bodyLarge, theme.typography.bodyLarge, 20.dp,
        IconSource.Resource(Res.drawable.ic_search), IconSource.Resource(Res.drawable.ic_delete),
        SearchFieldColors(theme.colors.surfaceContainerHigh, theme.colors.onSurface, theme.colors.onSurfaceVariant,
            theme.colors.onSurfaceVariant),
    )

    @Composable
    override fun Render(
        state: SearchFieldState,
        slots: SearchFieldSlots,
        theme: CamoTheme,
        style: SearchFieldStyle,
        modifier: Modifier,
    ) {
        Row(
            modifier.heightIn(min = style.height).border(if (state.focused) 2.dp else 1.dp,
                if (state.focused) DebugFocus else theme.colors.outline).padding(horizontal = theme.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            slots.leading()
            Box(Modifier.weight(1f), propagateMinConstraints = true) {
                if (state.isEmpty) {
                    BasicText(state.placeholder, Modifier.clearAndSetSemantics {},
                        style = style.placeholderStyle.copy(color = style.colors.placeholder))
                }
                slots.input()
            }
            slots.clear?.invoke()
            slots.trailing?.invoke()
        }
    }
}
