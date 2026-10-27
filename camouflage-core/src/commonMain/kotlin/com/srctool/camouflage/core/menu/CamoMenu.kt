package com.srctool.camouflage.core.menu

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
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel

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
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * Draws menus. In M5 it draws the popup panel, the surface every anchored popup sits on (a date picker's
 * calendar, a custom menu); the menu's rows arrive with `CamoMenu` in M6.
 */
@Stable
public interface MenuRenderer {
    /** The complete menu style for [theme]. */
    public fun defaults(theme: CamoTheme): MenuStyle

    /** The popup's container around [content], within `minWidth`…`maxWidth` unless the content asks for more. */
    @Composable
    public fun Panel(theme: CamoTheme, style: MenuStyle, modifier: Modifier, content: @Composable () -> Unit)
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
