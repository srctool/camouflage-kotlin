package com.srctool.camouflage.core

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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

// The M5 text inputs in the plain test skin: a bordered column with the label, the input and the messages.

private val Dot = IconSource.Vector(
    ImageVector.Builder("dot", 24.dp, 24.dp, 24f, 24f)
        .path(fill = SolidColor(Color.Black)) { moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); close() }
        .build(),
)

internal object PlainTextField : TextFieldRenderer {
    override fun defaults(theme: CamoTheme): TextFieldStyle {
        val c = theme.colors
        val t = theme.typography
        return TextFieldStyle(
            LabelPlacement.Above, 48.dp, RectangleShape, PaddingValues(horizontal = 12.dp), t.bodyLarge, t.bodyLarge,
            t.bodySmall, t.bodySmall, 24.dp, 1.dp, 2.dp, Dot,
            TextFieldColors(Color.Transparent, c.outline, c.primary, c.error, c.onSurface, c.onSurfaceVariant,
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
        Column(modifier) {
            state.label?.let { BasicText(it, Modifier.clearAndSetSemantics {}) }
            Row(
                Modifier.heightIn(min = style.height).border(style.borderWidth, style.colors.border)
                    .padding(style.contentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slots.prefixIcon?.invoke()
                state.prefixText?.let { BasicText(it) }
                Box(Modifier.widthIn(min = 100.dp)) {
                    if (state.isEmpty) state.placeholder?.let { BasicText(it, Modifier.clearAndSetSemantics {}) }
                    slots.input()
                }
                slots.suffixIcon?.invoke()
            }
            (state.error ?: state.supportingText)?.let { BasicText(it, Modifier.clearAndSetSemantics {}) }
        }
    }
}

internal object PlainSearchField : SearchFieldRenderer {
    override fun defaults(theme: CamoTheme) = SearchFieldStyle(
        48.dp, RectangleShape, theme.typography.bodyLarge, theme.typography.bodyLarge, 24.dp, Dot, Dot,
        SearchFieldColors(Color.Transparent, theme.colors.onSurface, theme.colors.onSurfaceVariant,
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
        Row(modifier.heightIn(min = style.height), verticalAlignment = Alignment.CenterVertically) {
            slots.leading()
            Box(Modifier.widthIn(min = 100.dp)) { slots.input() }
            slots.clear?.invoke()
            slots.trailing?.invoke()
        }
    }
}
