package com.srctool.camouflage.core.textfield

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.text
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.camoError
import com.srctool.camouflage.core.util.collectInteractionState

/** Where a field's label sits: `Above` the field, or `Floating` inside it and moving to the top edge. */
public enum class LabelPlacement { Above, Floating }

/** Every color a text field uses. */
@Immutable
public data class TextFieldColors(
    val container: Color,
    val border: Color,
    val focusedBorder: Color,
    val errorBorder: Color,
    val text: Color,
    val label: Color,
    val placeholder: Color,
    val icon: Color,
)

/** Overrides for text fields (and the fields built on their frame). `null` keeps the skin's default. */
@Immutable
public data class TextFieldTheme(
    val labelPlacement: LabelPlacement? = null,
    val height: Dp? = null,
    val shape: Shape? = null,
    val contentPadding: PaddingValues? = null,
    val textStyle: TextStyle? = null,
    val labelStyle: TextStyle? = null,
    val floatingLabelStyle: TextStyle? = null,
    val supportingTextStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val borderWidth: Dp? = null,
    val focusedBorderWidth: Dp? = null,
    /** The chevron at the end of a field that opens a list (`CamoSelect`). */
    val dropdownIcon: IconSource? = null,
    val colors: TextFieldColors? = null,
)

/** The resolved text field style: every field set. */
@Immutable
public data class TextFieldStyle(
    val labelPlacement: LabelPlacement,
    val height: Dp,
    val shape: Shape,
    val contentPadding: PaddingValues,
    val textStyle: TextStyle,
    val labelStyle: TextStyle,
    val floatingLabelStyle: TextStyle,
    val supportingTextStyle: TextStyle,
    val iconSize: Dp,
    val borderWidth: Dp,
    val focusedBorderWidth: Dp,
    val dropdownIcon: IconSource,
    val colors: TextFieldColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: TextFieldTheme?): TextFieldStyle = if (overrides == null) {
        this
    } else {
        TextFieldStyle(
            labelPlacement = overrides.labelPlacement ?: labelPlacement,
            height = overrides.height ?: height,
            shape = overrides.shape ?: shape,
            contentPadding = overrides.contentPadding ?: contentPadding,
            textStyle = overrides.textStyle ?: textStyle,
            labelStyle = overrides.labelStyle ?: labelStyle,
            floatingLabelStyle = overrides.floatingLabelStyle ?: floatingLabelStyle,
            supportingTextStyle = overrides.supportingTextStyle ?: supportingTextStyle,
            iconSize = overrides.iconSize ?: iconSize,
            borderWidth = overrides.borderWidth ?: borderWidth,
            focusedBorderWidth = overrides.focusedBorderWidth ?: focusedBorderWidth,
            dropdownIcon = overrides.dropdownIcon ?: dropdownIcon,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What a text field renderer draws around the input: the label (by `style.labelPlacement`), the placeholder
 * while [isEmpty], [prefixText] and [suffixText] while focused or not empty, the border for the focused and
 * error states, and [supportingText] or [error] below. Core names the input and marks the error on it, so the
 * label, placeholder and error texts exclude their own semantics.
 */
@Immutable
public data class TextFieldState(
    val value: String,
    val label: String?,
    val placeholder: String?,
    val supportingText: String?,
    val error: String?,
    val prefixText: String?,
    val suffixText: String?,
    val enabled: Boolean,
    val focused: Boolean,
    val singleLine: Boolean,
    val secure: Boolean,
    val isEmpty: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/**
 * The caller's icons and core's [input]: the editable node, already filtered, formatted and styled. The
 * renderer places [input] and never recreates it, so a skin switch keeps focus, text and selection.
 */
@Stable
public class TextFieldSlots(
    public val prefixIcon: (@Composable () -> Unit)?,
    public val suffixIcon: (@Composable () -> Unit)?,
    public val input: @Composable () -> Unit,
)

/** Draws text fields and every field built on their frame. Apply `modifier` (the behavior) outermost. */
@Stable
public interface TextFieldRenderer {
    /** The complete text field style for [theme]. */
    public fun defaults(theme: CamoTheme): TextFieldStyle

    /** Draws [state] with [slots]. */
    @Composable
    public fun Render(
        state: TextFieldState,
        slots: TextFieldSlots,
        theme: CamoTheme,
        style: TextFieldStyle,
        modifier: Modifier,
    )
}

/**
 * A controlled text field. [value] is raw text: [inputFilter] blocks or trims input before it reaches
 * [onValueChange], and [displayFormat] shows formatting (dashes, thousand separators) without storing it, with
 * the cursor kept in place. If the app doesn't take a change, the field keeps showing [value]. [error] replaces
 * [supportingText] and marks it invalid. [prefixText] / [suffixText] ("+62", "kg") are never part of the value.
 * The editable node lives here, not in the skin, so switching skins while typing keeps focus and selection.
 */
@Composable
public fun CamoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    prefixIcon: (@Composable () -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    prefixText: String? = null,
    suffixText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    secure: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    inputFilter: CamoInputFilter? = null,
    displayFormat: CamoDisplayFormat? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    require(!(secure && displayFormat != null)) { "CamoTextField: a secure field can't have a displayFormat." }
    val skin = requireSkin("CamoTextField")
    val theme = requireTheme("CamoTextField")
    val style = remember(skin, theme) { skin.textField.defaults(theme).merge(theme.components.textField) }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val buffer = rememberControlledText(value, onValueChange)
    val focusChanged by rememberUpdatedState(onFocusChanged)
    val textStyle = style.textStyle.copy(color = style.colors.text)
    val focus = remember { FocusRequester() }
    val inputModifier = Modifier
        .focusRequester(focus)
        .onFocusChanged { focusChanged?.invoke(it.isFocused) }
        .semantics { if (secure) password() }
        .camoError(error)
    val named = namedBy(label ?: placeholder)
    val input: @Composable () -> Unit = {
        EditableInput(
            buffer, inputModifier, enabled, secure, inputFilter, displayFormat, textStyle, keyboardOptions,
            onKeyboardAction, singleLine, source, named,
        )
    }
    val shown = buffer.text.toString()
    skin.textField.Render(
        TextFieldState(
            value = shown,
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            error = error,
            prefixText = prefixText,
            suffixText = suffixText,
            enabled = enabled,
            focused = interaction.focused,
            singleLine = singleLine,
            secure = secure,
            isEmpty = shown.isEmpty(),
            interaction = interaction,
            interactionSource = source,
        ),
        TextFieldSlots(prefixIcon, suffixIcon, input),
        theme,
        style,
        // A tap anywhere on the field (the label, a prefix) focuses the input.
        modifier.pointerInput(enabled) { if (enabled) detectTapGestures { focus.requestFocus() } },
    )
}

/**
 * Names an editable node: the label, as text with no size inside the node, so it's announced as the field's
 * name together with what's typed (a content description would replace the typed text). Renderers hide their
 * visible label from semantics.
 */
internal fun namedBy(name: String?): TextFieldDecorator? = name?.let {
    TextFieldDecorator { inner ->
        Box {
            Box(Modifier.size(0.dp).semantics { text = AnnotatedString(it) })
            inner()
        }
    }
}

// The editable node: a secure field (obscured, no copy) or a plain one with the filter and the display format.
@Suppress("LongParameterList")
@Composable
private fun EditableInput(
    buffer: androidx.compose.foundation.text.input.TextFieldState,
    modifier: Modifier,
    enabled: Boolean,
    secure: Boolean,
    inputFilter: CamoInputFilter?,
    displayFormat: CamoDisplayFormat?,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    onKeyboardAction: KeyboardActionHandler?,
    singleLine: Boolean,
    source: MutableInteractionSource,
    decorator: TextFieldDecorator?,
) {
    val cursor = SolidColor(textStyle.color)
    if (secure) {
        BasicSecureTextField(
            state = buffer,
            modifier = modifier,
            enabled = enabled,
            inputTransformation = inputFilter?.toInputTransformation(),
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            interactionSource = source,
            cursorBrush = cursor,
            decorator = decorator,
        )
    } else {
        BasicTextField(
            state = buffer,
            modifier = modifier,
            enabled = enabled,
            inputTransformation = inputFilter?.toInputTransformation(),
            outputTransformation = displayFormat?.toOutputTransformation(),
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
            interactionSource = source,
            cursorBrush = cursor,
            decorator = decorator,
        )
    }
}

/**
 * A Compose text buffer kept in step with the app's [value]: user edits call [onValueChange]; when the app
 * changes [value], the buffer takes it with the cursor at the end; when the app ignores an edit, the buffer
 * returns to [value] on the next frame.
 */
@Composable
internal fun rememberControlledText(
    value: String,
    onValueChange: (String) -> Unit,
): androidx.compose.foundation.text.input.TextFieldState {
    val buffer = rememberTextFieldState(value)
    val latest by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    // Bumped with each reported edit, in the same snapshot as the app's update (if any): the effect below then
    // sees both, and puts the app's value back only when the app didn't take the edit.
    var reported by remember { mutableIntStateOf(0) }
    LaunchedEffect(value, reported) {
        if (buffer.text.toString() != value) buffer.setTextAndPlaceCursorAtEnd(value)
    }
    LaunchedEffect(buffer) {
        snapshotFlow { buffer.text.toString() }.collect { text ->
            if (text == latest) return@collect
            change(text)
            reported++
        }
    }
    return buffer
}

/**
 * The building block (ADR-30): the text field's frame (label, border and focus, icons, supporting and error
 * text) around any [content], such as OTP boxes or a country picker with a number field, so custom inputs match
 * the brand's fields. The frame can't see inside [content], so the caller passes [focused] and [isEmpty]. The
 * content keeps its own semantics; the frame names it with [label] and marks it with [error].
 */
@Composable
public fun CamoFieldFrame(
    focused: Boolean,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    error: String? = null,
    prefixIcon: (@Composable () -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoFieldFrame")
    val theme = requireTheme("CamoFieldFrame")
    val style = remember(skin, theme) { skin.textField.defaults(theme).merge(theme.components.textField) }
    val source = remember { MutableInteractionSource() }
    skin.textField.Render(
        TextFieldState(
            value = "",
            label = label,
            placeholder = null,
            supportingText = supportingText,
            error = error,
            prefixText = null,
            suffixText = null,
            enabled = enabled,
            focused = focused,
            singleLine = true,
            secure = false,
            isEmpty = isEmpty,
            interaction = InteractionState(focused = focused),
            interactionSource = source,
        ),
        TextFieldSlots(prefixIcon, suffixIcon, content),
        theme,
        style,
        modifier.semantics { if (label != null) contentDescription = label }.camoError(error),
    )
}
