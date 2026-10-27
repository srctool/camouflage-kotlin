package com.srctool.camouflage.core.search

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.textfield.CamoFieldAction
import com.srctool.camouflage.core.textfield.namedBy
import com.srctool.camouflage.core.textfield.rememberControlledText
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState

/** Every color a search field uses. */
@Immutable
public data class SearchFieldColors(val container: Color, val text: Color, val placeholder: Color, val icon: Color)

/** Overrides for search fields. `null` keeps the skin's default. */
@Immutable
public data class SearchFieldTheme(
    val height: Dp? = null,
    val shape: Shape? = null,
    val textStyle: TextStyle? = null,
    val placeholderStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val searchIcon: IconSource? = null,
    val clearIcon: IconSource? = null,
    val colors: SearchFieldColors? = null,
)

/** The resolved search field style: every field set. */
@Immutable
public data class SearchFieldStyle(
    val height: Dp,
    val shape: Shape,
    val textStyle: TextStyle,
    val placeholderStyle: TextStyle,
    val iconSize: Dp,
    val searchIcon: IconSource,
    val clearIcon: IconSource,
    val colors: SearchFieldColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SearchFieldTheme?): SearchFieldStyle = if (overrides == null) {
        this
    } else {
        SearchFieldStyle(
            height = overrides.height ?: height,
            shape = overrides.shape ?: shape,
            textStyle = overrides.textStyle ?: textStyle,
            placeholderStyle = overrides.placeholderStyle ?: placeholderStyle,
            iconSize = overrides.iconSize ?: iconSize,
            searchIcon = overrides.searchIcon ?: searchIcon,
            clearIcon = overrides.clearIcon ?: clearIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/** What a search field renderer draws: the container, the placeholder while [isEmpty], and the slots. */
@Immutable
public data class SearchFieldState(
    val query: String,
    val placeholder: String,
    val enabled: Boolean,
    val focused: Boolean,
    val isEmpty: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/**
 * The search field's parts in order: [leading] (the caller's, or core's search icon), core's [input], the
 * [clear] button (present only while there's text) and the caller's [trailing].
 */
@Stable
public class SearchFieldSlots(
    public val leading: @Composable () -> Unit,
    public val input: @Composable () -> Unit,
    public val clear: (@Composable () -> Unit)?,
    public val trailing: (@Composable () -> Unit)?,
)

/** Draws search fields. Apply `modifier` (the behavior) outermost. */
@Stable
public interface SearchFieldRenderer {
    /** The complete search field style for [theme]. */
    public fun defaults(theme: CamoTheme): SearchFieldStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(
        state: SearchFieldState,
        slots: SearchFieldSlots,
        theme: CamoTheme,
        style: SearchFieldStyle,
        modifier: Modifier,
    )
}

/**
 * A search field: the search icon, the keyboard's Search action (Enter on desktop and web) calling [onSearch],
 * and a "Clear search" button while there's text, which clears and keeps focus. Esc clears too. [leading]
 * replaces the search icon (a back button when a search is open); [trailing] adds e.g. a mic. Debouncing is the
 * app's: the field reports every change.
 */
@Composable
public fun CamoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val skin = requireSkin("CamoSearchField")
    val theme = requireTheme("CamoSearchField")
    val style = remember(skin, theme) { skin.searchField.defaults(theme).merge(theme.components.searchField) }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val buffer = rememberControlledText(query, onQueryChange)
    val search by rememberUpdatedState(onSearch)
    val focusChanged by rememberUpdatedState(onFocusChanged)
    val focus = remember { FocusRequester() }
    val input: @Composable () -> Unit = {
        SearchInput(buffer, focus, placeholder, enabled, style, source, { search(it) }, { focusChanged?.invoke(it) })
    }
    val clear: (@Composable () -> Unit)? = if (buffer.text.isEmpty()) {
        null
    } else {
        {
            CamoFieldAction(
                icon = { CamoIcon(style.clearIcon, null) },
                contentDescription = "Clear search",
                onClick = {
                    buffer.clearText()
                    focus.requestFocus()
                },
                enabled = enabled,
            )
        }
    }
    skin.searchField.Render(
        SearchFieldState(
            query = buffer.text.toString(),
            placeholder = placeholder,
            enabled = enabled,
            focused = interaction.focused,
            isEmpty = buffer.text.isEmpty(),
            interaction = interaction,
            interactionSource = source,
        ),
        SearchFieldSlots(leading ?: { CamoIcon(style.searchIcon, null) }, input, clear, trailing),
        theme,
        style,
        modifier,
    )
}

// The editable node: Search on the keyboard and Enter call [onSearch]; Esc clears when there's text.
@Composable
private fun SearchInput(
    buffer: androidx.compose.foundation.text.input.TextFieldState,
    focus: FocusRequester,
    placeholder: String,
    enabled: Boolean,
    style: SearchFieldStyle,
    source: MutableInteractionSource,
    onSearch: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    BasicTextField(
        state = buffer,
        modifier = Modifier
            .focusRequester(focus)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onPreviewKeyEvent { event ->
                val down = event.type == KeyEventType.KeyDown
                when {
                    down && event.key == Key.Enter -> {
                        onSearch(buffer.text.toString())
                        true
                    }
                    down && event.key == Key.Escape && buffer.text.isNotEmpty() -> {
                        buffer.clearText()
                        true
                    }
                    else -> false
                }
            },
        enabled = enabled,
        textStyle = style.textStyle.copy(color = style.colors.text),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
        onKeyboardAction = { onSearch(buffer.text.toString()) },
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = source,
        cursorBrush = SolidColor(style.colors.text),
        decorator = namedBy(placeholder),
    )
}
