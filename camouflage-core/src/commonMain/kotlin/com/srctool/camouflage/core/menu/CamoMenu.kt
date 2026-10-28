package com.srctool.camouflage.core.menu

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone

/** Where a checked menu item's checkmark sits. */
public enum class CheckPosition { Leading, Trailing }

/** Every color a menu uses. */
@Immutable
public data class MenuColors(val container: Color, val text: Color, val icon: Color, val trailingText: Color)

/** Overrides for menus and popup panels. `null` keeps the skin's default. */
@Immutable
public data class MenuTheme(
    val shape: Shape? = null,
    val minWidth: Dp? = null,
    val maxWidth: Dp? = null,
    val itemHeight: Dp? = null,
    val contentPadding: PaddingValues? = null,
    val textStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val elevation: ElevationLevel? = null,
    val checkPosition: CheckPosition? = null,
    /** The checkmark of a checked item (and of the current option in a select's sheet). */
    val checkIcon: IconSource? = null,
    val colors: MenuColors? = null,
)

/** The resolved menu style: every field set. */
@Immutable
public data class MenuStyle(
    val shape: Shape,
    val minWidth: Dp,
    val maxWidth: Dp,
    val itemHeight: Dp,
    val contentPadding: PaddingValues,
    val textStyle: TextStyle,
    val iconSize: Dp,
    val elevation: ElevationLevel,
    val checkPosition: CheckPosition,
    val checkIcon: IconSource,
    val colors: MenuColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: MenuTheme?): MenuStyle = if (overrides == null) {
        this
    } else {
        MenuStyle(
            shape = overrides.shape ?: shape,
            minWidth = overrides.minWidth ?: minWidth,
            maxWidth = overrides.maxWidth ?: maxWidth,
            itemHeight = overrides.itemHeight ?: itemHeight,
            contentPadding = overrides.contentPadding ?: contentPadding,
            textStyle = overrides.textStyle ?: textStyle,
            iconSize = overrides.iconSize ?: iconSize,
            elevation = overrides.elevation ?: elevation,
            checkPosition = overrides.checkPosition ?: checkPosition,
            checkIcon = overrides.checkIcon ?: checkIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a menu row renderer draws: [text] with [trailingText] (a shortcut) at the end, in [tone] (`Error` for
 * destructive items) and dimmed when not [enabled]. [checked] is `null` for a plain item; otherwise draw the
 * check when `true`, at `style.checkPosition`. Keep the text aligned across the menu: leave the check column
 * when [reserveCheck] (some item is checkable) and the icon column when [reserveIcon] (some item has an icon).
 * Show the highlight from [interaction] (hovered, focused, pressed).
 */
@Immutable
public data class MenuItemState(
    val text: String,
    val trailingText: String?,
    val tone: Tone,
    val enabled: Boolean,
    val checked: Boolean?,
    val reserveCheck: Boolean,
    val reserveIcon: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** The item's leading icon, if any. */
@Stable
public class MenuItemSlots(public val leadingIcon: (@Composable () -> Unit)?)

/**
 * Draws menus: the popup panel, the surface every anchored popup sits on (a menu, a date picker's calendar),
 * and the menu's rows.
 */
@Stable
public interface MenuRenderer {
    /** The complete menu style for [theme]. */
    public fun defaults(theme: CamoTheme): MenuStyle

    /** The popup's container around [content], within `minWidth`…`maxWidth` unless the content asks for more. */
    @Composable
    public fun Panel(theme: CamoTheme, style: MenuStyle, modifier: Modifier, content: @Composable () -> Unit)

    /**
     * Draws one row, `style.itemHeight` tall and filling the panel's width. Apply `modifier` (the row's
     * behavior) to the outermost node, and keep the text in the semantics tree.
     */
    @Composable
    public fun Item(state: MenuItemState, slots: MenuItemSlots, theme: CamoTheme, style: MenuStyle, modifier: Modifier)
}

/**
 * The skin's popup surface (ADR-30) for custom popup content in a `CamoAnchoredPopup`: Up and Down move focus
 * between its focusable children when they don't use the keys themselves.
 */
@Composable
public fun CamoMenuPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val skin = requireSkin("CamoMenuPanel")
    val theme = requireTheme("CamoMenuPanel")
    val style = remember(skin, theme) { skin.menu.defaults(theme).merge(theme.components.menu) }
    val focus = LocalFocusManager.current
    skin.menu.Panel(
        theme,
        style,
        modifier.onKeyEvent {
            val direction = when (it.key) {
                Key.DirectionDown -> FocusDirection.Next
                Key.DirectionUp -> FocusDirection.Previous
                else -> null
            }
            direction != null && it.type == KeyEventType.KeyDown && focus.moveFocus(direction)
        },
        content,
    )
}
