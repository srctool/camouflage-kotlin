package com.srctool.camouflage.core.form

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.srctool.camouflage.core.checkbox.CamoCheckbox
import com.srctool.camouflage.core.radio.CamoRadioGroup
import com.srctool.camouflage.core.radio.RadioOption
import com.srctool.camouflage.core.stepper.CamoStepper
import com.srctool.camouflage.core.switch.CamoSwitch
import com.srctool.camouflage.core.text.CamoSpan
import com.srctool.camouflage.core.textfield.CamoDisplayFormat
import com.srctool.camouflage.core.textfield.CamoInputFilter
import com.srctool.camouflage.core.textfield.CamoTextField

// The ready-made form fields: each is a CamoFormField drawing the plain component with the field's value and
// error. Rules see the raw value, even when a display format shows dashes.

/**
 * A [CamoTextField] in a [CamoForm]. It takes the text field's parameters, minus `value`, `onValueChange` and
 * `error`.
 */
@Composable
public fun CamoTextFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: String = "",
    rules: List<CamoRule<String>> = emptyList(),
    onSaved: ((String) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    prefixIcon: (@Composable () -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    prefixText: String? = null,
    suffixText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    secure: Boolean = false,
    inputFilter: CamoInputFilter? = null,
    displayFormat: CamoDisplayFormat? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    CamoFormField(
        key = key,
        initialValue = initialValue,
        rules = rules,
        onSaved = onSaved?.let { saved -> { v: String? -> saved(v.orEmpty()) } },
        validateMode = validateMode,
        enabled = enabled,
        visible = visible,
        forceError = forceError,
    ) { field ->
        if (visible) {
            CamoTextField(
                value = field.value.orEmpty(),
                onValueChange = field::didChange,
                modifier = modifier,
                label = label,
                placeholder = placeholder,
                supportingText = supportingText,
                error = field.error,
                prefixIcon = prefixIcon,
                suffixIcon = suffixIcon,
                prefixText = prefixText,
                suffixText = suffixText,
                enabled = enabled,
                singleLine = singleLine,
                secure = secure,
                onFocusChanged = field::onFocusChanged,
                inputFilter = inputFilter,
                displayFormat = displayFormat,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = onKeyboardAction,
            )
        }
    }
}

/** A [CamoCheckbox] in a [CamoForm], e.g. a consent box with `CamoRules.requiredTrue`. */
@Composable
public fun CamoCheckboxFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: Boolean = false,
    rules: List<CamoRule<Boolean>> = emptyList(),
    onSaved: ((Boolean) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    labelSpans: List<CamoSpan>? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved?.let { s -> { v: Boolean? -> s(v == true) } }, validateMode,
        enabled, visible, forceError) { field ->
        if (visible) {
            CamoCheckbox(
                checked = field.value == true,
                onCheckedChange = field::didChange,
                modifier = modifier.onFocusChanged { field.onFocusChanged(it.isFocused) },
                label = label,
                labelSpans = labelSpans,
                description = description,
                error = field.error,
                enabled = enabled,
            )
        }
    }
}

/** A [CamoSwitch] in a [CamoForm]. */
@Composable
public fun CamoSwitchFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: Boolean = false,
    rules: List<CamoRule<Boolean>> = emptyList(),
    onSaved: ((Boolean) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    labelSpans: List<CamoSpan>? = null,
    description: String? = null,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved?.let { s -> { v: Boolean? -> s(v == true) } }, validateMode,
        enabled, visible, forceError) { field ->
        if (visible) {
            CamoSwitch(
                checked = field.value == true,
                onCheckedChange = field::didChange,
                modifier = modifier.onFocusChanged { field.onFocusChanged(it.isFocused) },
                label = label,
                labelSpans = labelSpans,
                description = description,
                error = field.error,
                enabled = enabled,
            )
        }
    }
}

/** A [CamoRadioGroup] in a [CamoForm], e.g. with `CamoRules.required` for "pick one". */
@Composable
public fun <T> CamoRadioGroupFormField(
    key: String? = null,
    options: List<RadioOption<T>>,
    modifier: Modifier = Modifier,
    initialValue: T? = null,
    rules: List<CamoRule<T>> = emptyList(),
    onSaved: ((T?) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved, validateMode, enabled, visible, forceError) { field ->
        if (visible) {
            CamoRadioGroup(
                value = field.value,
                onValueChange = field::didChange,
                options = options,
                modifier = modifier.onFocusChanged { field.onFocusChanged(it.hasFocus) },
                label = label,
                error = field.error,
                orientation = orientation,
                enabled = enabled,
            )
        }
    }
}

/** A whole-number [CamoStepper] in a [CamoForm]. */
@Composable
public fun CamoStepperFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: Int = 0,
    rules: List<CamoRule<Int>> = emptyList(),
    onSaved: ((Int) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    range: IntRange = 0..Int.MAX_VALUE,
    step: Int = 1,
    label: String? = null,
    allowTyping: Boolean = true,
    valueText: ((Int) -> String)? = null,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved?.let { s -> { v: Int? -> s(v ?: initialValue) } }, validateMode,
        enabled, visible, forceError) { field ->
        if (visible) {
            CamoStepper(
                value = field.value ?: initialValue,
                onValueChange = field::didChange,
                modifier = modifier.onFocusChanged { field.onFocusChanged(it.hasFocus) },
                range = range,
                step = step,
                label = label,
                allowTyping = allowTyping,
                valueText = valueText,
                error = field.error,
                enabled = enabled,
            )
        }
    }
}
