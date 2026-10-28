package com.srctool.camouflage.core.select

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.bottomsheet.CamoSheetFrame
import com.srctool.camouflage.core.datepicker.PickerFieldFrame
import com.srctool.camouflage.core.form.CamoFormField
import com.srctool.camouflage.core.form.CamoRule
import com.srctool.camouflage.core.form.ValidateMode
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.list.CamoListItem
import com.srctool.camouflage.core.menu.CamoMenuGroup
import com.srctool.camouflage.core.menu.CamoMenuItem
import com.srctool.camouflage.core.menu.MenuPopup
import com.srctool.camouflage.core.pagedlist.CamoPagedList
import com.srctool.camouflage.core.pagedlist.CamoPagedListState
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.search.CamoSearchField
import com.srctool.camouflage.core.text.CamoText
import kotlinx.coroutines.delay

/** How a select shows its options: a menu under the field, a bottom sheet, or `Auto` (see [SelectTheme]). */
public enum class SelectPresentation { Auto, Menu, Sheet }

/** One choice in a select. [supporting] shows under the label in the sheet. */
@Immutable
public data class SelectOption<out T>(
    val value: T,
    val label: String,
    val supporting: String? = null,
    val leading: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true,
)

/** The words a select shows. */
@Immutable
public data class SelectStrings(val search: String = "Search", val noResults: String = "No results")

/**
 * Overrides for selects. A select has no renderer of its own (the field, menu, sheet and rows are the text
 * field's, menu's, bottom sheet's and list item's), so these are presentation rules with core defaults.
 */
@Immutable
public data class SelectTheme(
    /** The most options `Auto` shows as a menu. */
    val autoMenuMaxOptions: Int? = null,
    /** The window width from which `Auto` may use a menu. */
    val autoMenuMinWidth: Dp? = null,
    /** A checkmark on the current option in the sheet. */
    val showCheckOnSelected: Boolean? = null,
    /** How long the async select waits after typing before `onQueryChange`. */
    val searchDebounceMillis: Int? = null,
    val strings: SelectStrings? = null,
)

/** The resolved select style: every field set. */
@Immutable
public data class SelectStyle(
    val autoMenuMaxOptions: Int = 7,
    val autoMenuMinWidth: Dp = 600.dp,
    val showCheckOnSelected: Boolean = true,
    val searchDebounceMillis: Int = 500,
    val strings: SelectStrings = SelectStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: SelectTheme?): SelectStyle = if (overrides == null) {
        this
    } else {
        SelectStyle(
            autoMenuMaxOptions = overrides.autoMenuMaxOptions ?: autoMenuMaxOptions,
            autoMenuMinWidth = overrides.autoMenuMinWidth ?: autoMenuMinWidth,
            showCheckOnSelected = overrides.showCheckOnSelected ?: showCheckOnSelected,
            searchDebounceMillis = overrides.searchDebounceMillis ?: searchDebounceMillis,
            strings = overrides.strings ?: strings,
        )
    }
}

/**
 * Picks one of [options]: a read-only field (the text field's frame with the skin's dropdown chevron) that
 * opens the options in a menu under it or in a bottom sheet ([presentation]). `Auto` uses the menu for up to
 * `SelectTheme.autoMenuMaxOptions` options on a window at least `autoMenuMinWidth` wide, and the sheet
 * otherwise, and always when [searchable] (the sheet then has a search field filtering by label, ignoring
 * case and accents). Picking an option calls [onValueChange] and closes; Esc or an outside tap closes without
 * a change. A [value] that isn't among the options shows as `value.toString()`.
 */
@Composable
public fun <T> CamoSelect(
    value: T?,
    onValueChange: (T) -> Unit,
    options: List<SelectOption<T>>,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    presentation: SelectPresentation = SelectPresentation.Auto,
    searchable: Boolean = false,
    prefixIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val style = selectStyle()
    val wide = windowWidth() >= style.autoMenuMinWidth
    val menu = when (presentation) {
        SelectPresentation.Menu -> true
        SelectPresentation.Sheet -> false
        SelectPresentation.Auto -> !searchable && wide && options.size <= style.autoMenuMaxOptions
    }
    val text = options.firstOrNull { it.value == value }?.label ?: value?.toString()
    val scope = rememberCoroutineScope()
    SelectField(text, label, placeholder, supportingText, error, prefixIcon, enabled, modifier) { field, close ->
        if (menu) {
            val items = options.map { option ->
                CamoMenuItem(
                    text = option.label,
                    onClick = { onValueChange(option.value) },
                    leadingIcon = option.leading,
                    enabled = option.enabled,
                    checked = option.value == value,
                )
            }
            MenuPopup(listOf(CamoMenuGroup(items, exclusive = true)), null, scope, onClose = close, minWidth = field)
        } else {
            SelectSheet(label, close) {
                OptionList(options, value, searchable, style) {
                    close()
                    onValueChange(it)
                }
            }
        }
    }
}

/**
 * A select whose options come from an API, controlled like `CamoPagedList` (ADR-29): it always opens a sheet
 * with a search field and a paged list over [options]. It calls [onOpen] once each time the sheet opens (load
 * the first page), and [onQueryChange] `SelectTheme.searchDebounceMillis` after the user stops typing; it never
 * fetches. [selectedLabel] is the current value's label, since its option may not be loaded.
 */
@Composable
public fun <T> CamoAsyncSelect(
    value: T?,
    onValueChange: (T) -> Unit,
    options: CamoPagedListState<SelectOption<T>>,
    onOpen: () -> Unit,
    onQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    selectedLabel: String?,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    prefixIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val style = selectStyle()
    val text = selectedLabel ?: value?.toString()
    val opened by rememberUpdatedState(onOpen)
    val queried by rememberUpdatedState(onQueryChange)
    SelectField(text, label, placeholder, supportingText, error, prefixIcon, enabled, modifier) { _, close ->
        SelectSheet(label, close) {
            var query by rememberSaveable { mutableStateOf("") }
            var typed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { opened() }
            LaunchedEffect(query) {
                if (typed) {
                    delay(style.searchDebounceMillis.toLong())
                    queried(query)
                }
            }
            Column {
                SearchRow(query, style) {
                    typed = true
                    query = it
                }
                CamoPagedList(
                    state = options,
                    itemKey = { it.value.toString() },
                    onLoadMore = onLoadMore,
                    onRetry = onRetry,
                    onRefresh = onRefresh,
                    item = { option, _ ->
                        OptionRow(option, option.value == value, style) {
                            close()
                            onValueChange(option.value)
                        }
                    },
                )
            }
        }
    }
}

/** A [CamoSelect] in a `CamoForm`, e.g. with `CamoRules.required` for "pick one". */
@Composable
public fun <T> CamoSelectFormField(
    options: List<SelectOption<T>>,
    modifier: Modifier = Modifier,
    key: String? = null,
    initialValue: T? = null,
    rules: List<CamoRule<T>> = emptyList(),
    onSaved: ((T?) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    forceError: String? = null,
    visible: Boolean = true,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    presentation: SelectPresentation = SelectPresentation.Auto,
    searchable: Boolean = false,
    prefixIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    CamoFormField(key, initialValue, rules, onSaved, validateMode, enabled, visible, forceError) { field ->
        if (visible) {
            CamoSelect(
                value = field.value,
                onValueChange = field::didChange,
                options = options,
                modifier = modifier.onFocusChanged { field.onFocusChanged(it.hasFocus) },
                label = label,
                placeholder = placeholder,
                supportingText = supportingText,
                error = field.error,
                presentation = presentation,
                searchable = searchable,
                prefixIcon = prefixIcon,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun selectStyle(): SelectStyle {
    val theme = requireTheme("CamoSelect")
    return remember(theme) { SelectStyle().merge(theme.components.select) }
}

@Composable
private fun windowWidth(): Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

// The read-only field, and [picker] while open with the field's width and a close that returns focus to it.
@Composable
private fun SelectField(
    text: String?,
    label: String?,
    placeholder: String?,
    supportingText: String?,
    error: String?,
    prefixIcon: (@Composable () -> Unit)?,
    enabled: Boolean,
    modifier: Modifier,
    picker: @Composable (fieldWidth: Dp, close: () -> Unit) -> Unit,
) {
    val skin = requireSkin("CamoSelect")
    val theme = requireTheme("CamoSelect")
    val icon = remember(skin, theme) { skin.textField.defaults(theme).merge(theme.components.textField).dropdownIcon }
    var open by rememberSaveable { mutableStateOf(false) }
    var width by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val opener = remember { FocusRequester() }
    val keyboard = LocalInputModeManager.current
    val close = {
        open = false
        if (keyboard.inputMode == InputMode.Keyboard) opener.requestFocus()
    }
    Box(modifier.onSizeChanged { width = with(density) { it.width.toDp() } }) {
        PickerFieldFrame(
            text = text,
            label = label,
            placeholder = placeholder,
            supportingText = supportingText,
            error = error,
            icon = icon,
            open = open,
            enabled = enabled,
            focusRequester = opener,
            onFocusChanged = null,
            prefixIcon = prefixIcon,
            modifier = Modifier
                .onKeyEvent {
                    val down = enabled && it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown
                    if (down) open = true
                    down
                }
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" },
        ) { open = true }
        if (open) picker(width, close)
    }
}

@Composable
private fun SelectSheet(title: String?, onClose: () -> Unit, content: @Composable () -> Unit) {
    CamoSheetFrame(onClose) {
        CamoBottomSheet(title = title) { content() }
    }
}

@Composable
private fun <T> OptionList(
    options: List<SelectOption<T>>,
    value: T?,
    searchable: Boolean,
    style: SelectStyle,
    onPick: (T) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(options, query) {
        val folded = fold(query.trim())
        options.filter { fold(it.label).contains(folded) }
    }
    Column {
        if (searchable) SearchRow(query, style) { query = it }
        if (shown.isEmpty()) {
            CamoText(style.strings.noResults, Modifier.padding(Camouflage.spacing.l))
        } else {
            LazyColumn {
                items(shown, key = { it.value.toString() }) { option ->
                    OptionRow(option, option.value == value, style) { onPick(option.value) }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(query: String, style: SelectStyle, onQueryChange: (String) -> Unit) {
    CamoSearchField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Camouflage.spacing.l, vertical = Camouflage.spacing.s),
        placeholder = style.strings.search,
    )
}

@Composable
private fun <T> OptionRow(option: SelectOption<T>, selected: Boolean, style: SelectStyle, onClick: () -> Unit) {
    val skin = requireSkin("CamoSelect")
    val theme = requireTheme("CamoSelect")
    val check = remember(skin, theme) { skin.menu.defaults(theme).merge(theme.components.menu).checkIcon }
    CamoListItem(
        headline = option.label,
        supporting = option.supporting,
        leading = option.leading,
        trailing = if (selected && style.showCheckOnSelected) ({ CamoIcon(check, null) }) else null,
        onClick = onClick,
        selected = selected,
        enabled = option.enabled,
    )
}

private val Accents = mapOf(
    "aàáâãäåā" to 'a', "cçćč" to 'c', "eèéêëēė" to 'e', "iìíîïī" to 'i', "nñń" to 'n',
    "oòóôõöøō" to 'o', "sśš" to 's', "uùúûüū" to 'u', "yýÿ" to 'y', "zźżž" to 'z',
).flatMap { (chars, base) -> chars.map { it to base } }.toMap()

// Lowercase, with the common Latin accents removed ("Zürich" matches "zur").
internal fun fold(text: String): String = text.lowercase().map { Accents[it] ?: it }.joinToString("")
