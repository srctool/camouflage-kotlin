package com.srctool.camouflage.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.buttongroup.ButtonGroupRenderer
import com.srctool.camouflage.core.buttongroup.ButtonGroupState
import com.srctool.camouflage.core.buttongroup.ButtonGroupStyle
import com.srctool.camouflage.core.buttongroup.GroupButtonSlots
import com.srctool.camouflage.core.buttongroup.GroupButtonState
import com.srctool.camouflage.core.buttongroup.GroupColors
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.splitbutton.SplitButtonBehavior
import com.srctool.camouflage.core.splitbutton.SplitButtonRenderer
import com.srctool.camouflage.core.splitbutton.SplitButtonSlots
import com.srctool.camouflage.core.splitbutton.SplitButtonState
import com.srctool.camouflage.core.splitbutton.SplitButtonStyle
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.toolbar.ToolbarColors
import com.srctool.camouflage.core.toolbar.ToolbarPlacement
import com.srctool.camouflage.core.toolbar.ToolbarRenderer
import com.srctool.camouflage.core.toolbar.ToolbarState
import com.srctool.camouflage.core.toolbar.ToolbarStyle

// The M9 button group, split button and toolbar in the debug skin: boxes, rows and text, no decoration.

private val DebugActionIcon = IconSource.Vector(ImageVector.Builder("t", 24.dp, 24.dp, 24f, 24f).build())

object DebugButtonGroup : ButtonGroupRenderer {
    override fun defaults(theme: CamoTheme) = ButtonGroupStyle(
        Appearance.Outline, 36.dp, RectangleShape, RectangleShape, 4.dp, 1.dp, 16.dp, 8.dp, theme.typography.labelLarge,
        GroupColors(theme.colors.onSurface, theme.colors.outline, theme.colors.primary, theme.colors.onPrimary),
    )

    @Composable
    override fun Container(
        state: ButtonGroupState,
        theme: CamoTheme,
        style: ButtonGroupStyle,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        Row(modifier, content = content)
    }

    @Composable
    override fun Button(
        state: GroupButtonState,
        slots: GroupButtonSlots,
        theme: CamoTheme,
        style: ButtonGroupStyle,
        modifier: Modifier,
    ) {
        Row(modifier.padding(4.dp)) {
            slots.icon?.invoke()
            slots.text?.let { BasicText(it) }
        }
    }
}

object DebugSplitButton : SplitButtonRenderer {
    override fun defaults(theme: CamoTheme) = SplitButtonStyle(
        Appearance.Solid, 40.dp, RectangleShape, RectangleShape, 40.dp, 2.dp, 16.dp, 8.dp, 12.dp,
        theme.typography.labelLarge, DebugActionIcon,
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
        if (!state.hasAction) {
            Row(modifier.then(behavior.main).padding(4.dp)) { BasicText(state.text) }
        } else {
            Row(modifier) {
                Row(behavior.main.padding(4.dp)) {
                    slots.prefixIcon?.invoke()
                    BasicText(state.text)
                }
                Box(behavior.arrow.size(40.dp))
            }
        }
    }
}

object DebugToolbar : ToolbarRenderer {
    override fun defaults(theme: CamoTheme) = ToolbarStyle(
        ToolbarPlacement.Docked, 56.dp, 48.dp, 8.dp, RectangleShape, ElevationLevel.Level2, 4.dp, 8.dp, false,
        DebugActionIcon,
        ToolbarColors(theme.colors.surface, theme.colors.outlineVariant),
    )

    @Composable
    override fun Container(
        state: ToolbarState,
        theme: CamoTheme,
        style: ToolbarStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) {
        Box(modifier.fillMaxWidth().height(style.height).background(style.colors.container)) { content() }
    }
}
