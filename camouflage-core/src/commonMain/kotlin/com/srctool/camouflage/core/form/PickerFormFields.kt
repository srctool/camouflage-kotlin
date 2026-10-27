package com.srctool.camouflage.core.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.srctool.camouflage.core.datepicker.CamoDateField
import com.srctool.camouflage.core.datepicker.CamoDateRange
import com.srctool.camouflage.core.datepicker.CamoDateRangeField
import com.srctool.camouflage.core.datepicker.PickerPresentation
import com.srctool.camouflage.core.timepicker.CamoTimeField
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// The date and time fields in a form. Values are null until picked, so `CamoRules.required` applies as is.

/**
 * A [CamoDateField] in a [CamoForm]. It takes the date field's parameters, minus `value`, `onValueChange` and
 * `error`.
 */
@Composable
public fun CamoDateFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: LocalDate? = null,
    rules: List<CamoRule<LocalDate?>> = emptyList(),
    onSaved: ((LocalDate?) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isSelectable: ((LocalDate) -> Boolean)? = null,
    presentation: PickerPresentation = PickerPresentation.Auto,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved, validateMode, enabled, visible, forceError) { field ->
        if (visible) {
            CamoDateField(
                value = field.value,
                onValueChange = field::didChange,
                modifier = modifier,
                label = label,
                placeholder = placeholder,
                supportingText = supportingText,
                error = field.error,
                minDate = minDate,
                maxDate = maxDate,
                isSelectable = isSelectable,
                presentation = presentation,
                enabled = enabled,
                onFocusChanged = field::onFocusChanged,
            )
        }
    }
}

/** A [CamoDateRangeField] in a [CamoForm]. */
@Composable
public fun CamoDateRangeFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: CamoDateRange? = null,
    rules: List<CamoRule<CamoDateRange?>> = emptyList(),
    onSaved: ((CamoDateRange?) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isSelectable: ((LocalDate) -> Boolean)? = null,
    maxLength: Int? = null,
    presentation: PickerPresentation = PickerPresentation.Auto,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved, validateMode, enabled, visible, forceError) { field ->
        if (visible) {
            CamoDateRangeField(
                value = field.value,
                onValueChange = field::didChange,
                modifier = modifier,
                label = label,
                placeholder = placeholder,
                supportingText = supportingText,
                error = field.error,
                minDate = minDate,
                maxDate = maxDate,
                isSelectable = isSelectable,
                maxLength = maxLength,
                presentation = presentation,
                enabled = enabled,
                onFocusChanged = field::onFocusChanged,
            )
        }
    }
}

/** A [CamoTimeField] in a [CamoForm]. */
@Composable
public fun CamoTimeFormField(
    key: String? = null,
    modifier: Modifier = Modifier,
    initialValue: LocalTime? = null,
    rules: List<CamoRule<LocalTime?>> = emptyList(),
    onSaved: ((LocalTime?) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    minuteStep: Int = 1,
    use24Hour: Boolean? = null,
    minTime: LocalTime? = null,
    maxTime: LocalTime? = null,
    presentation: PickerPresentation = PickerPresentation.Auto,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved, validateMode, enabled, visible, forceError) { field ->
        if (visible) {
            CamoTimeField(
                value = field.value,
                onValueChange = field::didChange,
                modifier = modifier,
                label = label,
                placeholder = placeholder,
                supportingText = supportingText,
                error = field.error,
                minuteStep = minuteStep,
                use24Hour = use24Hour,
                minTime = minTime,
                maxTime = maxTime,
                presentation = presentation,
                enabled = enabled,
                onFocusChanged = field::onFocusChanged,
            )
        }
    }
}
