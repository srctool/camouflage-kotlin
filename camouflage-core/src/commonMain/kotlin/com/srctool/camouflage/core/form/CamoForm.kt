package com.srctool.camouflage.core.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.srctool.camouflage.core.button.DebugChecks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalTime

/** When a form field validates (Flutter's `AutovalidateMode`). */
public enum class ValidateMode {
    /** Only when the form's `validate()` is called, usually on submit. */
    Disabled,

    /** On every change and from the start, before any edit. */
    Always,

    /** On every change, after the user's first edit of that field. */
    OnUserInteraction,

    /** When the field loses focus. */
    OnUnfocus,
}

// One field's state, kept by the form under its key whether or not the field is on screen.
internal class FormEntry(val key: String, val initialValue: Any?, val order: Int) {
    var value by mutableStateOf(initialValue)
    var error by mutableStateOf<String?>(null)
    var serverError by mutableStateOf<String?>(null)
    var hasInteracted by mutableStateOf(false)
    var rules: List<CamoRule<Nothing>> = emptyList()
    var mode: ValidateMode? = null
    var enabled = true
    var visible = true
    var onSaved: ((Any?) -> Unit)? = null
    var focus: FocusRequester? = null
    var bringIntoView: BringIntoViewRequester? = null
    var attachments = 0

    val shownError: String? get() = serverError ?: error

    @Suppress("UNCHECKED_CAST")
    fun <T> update(
        rules: List<CamoRule<T>>,
        mode: ValidateMode?,
        enabled: Boolean,
        visible: Boolean,
        onSaved: ((T?) -> Unit)?,
    ) {
        this.rules = rules as List<CamoRule<Nothing>>
        this.mode = mode
        this.enabled = enabled
        this.visible = visible
        this.onSaved = onSaved as ((Any?) -> Unit)?
        if (!enabled) error = null
    }
}

/**
 * The handle an app calls on a [CamoForm], like Flutter's `FormState`. The form owns every field's value by
 * key, so a field scrolled out of a lazy list keeps its value and is still validated and saved. Create it with
 * [rememberCamoFormState].
 */
@Stable
@Suppress("TooManyFunctions") // Flutter FormState's surface: validate, save, reset, setErrors, value, declare, …
public class CamoFormState internal constructor(restored: Map<String, List<Any?>> = emptyMap()) {
    private val entries = mutableStateMapOf<String, FormEntry>()
    private val restored = restored.toMutableMap()
    private var nextOrder = 0
    internal var mode: ValidateMode by mutableStateOf(ValidateMode.Disabled)
    internal var onChanged: (() -> Unit)? = null
    internal var reveal: (suspend (String) -> Unit)? = null
    internal var scope: CoroutineScope? = null
    internal var pendingFocus by mutableStateOf<String?>(null)

    /** `true` after the last `validate()` found no error. */
    public var isValid: Boolean by mutableStateOf(true)
        private set

    /** Any field's value differs from its initial value. */
    public val isDirty: Boolean get() = entries.values.any { it.value != it.initialValue }

    /** Field [key]'s value differs from its initial value. */
    public fun isDirty(key: String): Boolean = entries[key]?.let { it.value != it.initialValue } ?: false

    /** Field [key]'s current value, or `null` when there's no such field. */
    @Suppress("UNCHECKED_CAST")
    public fun <T> value(key: String): T? = entries[key]?.value as T?

    /**
     * Creates field [key]'s entry before its field is ever shown, so a lazy form validates rows the user hasn't
     * scrolled to. A field attaching later takes this entry.
     */
    public fun <T> declare(key: String, initialValue: T?, rules: List<CamoRule<T>> = emptyList()) {
        entry(key, initialValue).rules = rules
    }

    /** Drops field [key]'s entry for good (a removed row). */
    public fun remove(key: String) {
        entries.remove(key)
    }

    /**
     * Validates every visible, enabled field, on screen or not, in the order they first appeared. The first
     * invalid one is brought into view and focused (revealed first through `revealField` when it's off screen).
     */
    public fun validate(): Boolean {
        var first: FormEntry? = null
        for (entry in ordered()) {
            if (!entry.visible || !entry.enabled) continue
            if (!check(entry) && first == null) first = entry
        }
        isValid = first == null
        first?.let { focus(it) }
        return isValid
    }

    /** Calls every visible, enabled field's `onSaved` with its current value. */
    public fun save() {
        for (entry in ordered()) if (entry.visible && entry.enabled) entry.onSaved?.invoke(entry.value)
    }

    /** Every field back to its initial value, with errors and interaction cleared. */
    public fun reset() {
        for (entry in entries.values) {
            entry.value = entry.initialValue
            entry.error = null
            entry.serverError = null
            entry.hasInteracted = false
        }
        isValid = true
    }

    /** Shows [errors] (from a server) on the fields by key, until each field's next change. */
    public fun setErrors(errors: Map<String, String>) {
        for ((key, message) in errors) entries[key]?.serverError = message
        if (errors.isNotEmpty()) {
            isValid = false
            ordered().firstOrNull { it.key in errors }?.let { focus(it) }
        }
    }

    internal fun entry(key: String, initialValue: Any?): FormEntry = entries.getOrPut(key) {
        FormEntry(key, initialValue, nextOrder++).also { entry ->
            restored.remove(key)?.let { (value, error, interacted) ->
                entry.value = restoredValue(value)
                entry.error = error as String?
                entry.hasInteracted = interacted == true
            }
        }
    }

    // Runs [entry]'s rules and stores the result; true when valid.
    internal fun check(entry: FormEntry): Boolean {
        if (!entry.enabled) {
            entry.error = null
            return true
        }
        entry.error = entry.rules.firstNotNullOfOrNull { rule ->
            when (rule) {
                is MatchesRule -> rule.validate(entry.value, entries[rule.fieldKey]?.value)
                else -> @Suppress("UNCHECKED_CAST") (rule as CamoRule<Any?>).validate(entry.value)
            }
        }
        return entry.shownError == null
    }

    internal fun changed() = onChanged?.invoke()

    private fun ordered() = entries.values.sortedBy { it.order }

    private fun focus(entry: FormEntry) {
        val requester = entry.focus
        if (requester != null) {
            scope?.launch { entry.bringIntoView?.bringIntoView() }
            requester.requestFocus()
        } else {
            pendingFocus = entry.key
            val reveal = reveal ?: return
            scope?.launch {
                reveal(entry.key)
                // The row may have attached mid-scroll; once the scroll has settled, focus it here too.
                entry.focus?.let { requester ->
                    pendingFocus = null
                    focusSoon(requester, entry.bringIntoView)
                }
            }
        }
    }

    internal fun saved(): Map<String, List<Any?>> = entries.values
        .filter { it.value.let { v -> v == null || v is String || v is Boolean || v is Number || savable(v) != null } }
        .associate { it.key to listOf(savable(it.value) ?: it.value, it.error, it.hasInteracted) }

    public companion object {
        /** Saves values (text, booleans, numbers, dates, times, ranges), errors and interaction across recreation. */
        public val Saver: Saver<CamoFormState, Any> = Saver(
            save = { it.saved() },
            restore = { @Suppress("UNCHECKED_CAST") CamoFormState(it as Map<String, List<Any?>>) },
        )
    }
}

/** A [CamoFormState] that survives configuration changes and process death. */
@Composable
public fun rememberCamoFormState(): CamoFormState = rememberSaveable(saver = CamoFormState.Saver) { CamoFormState() }

internal val LocalCamoForm = staticCompositionLocalOf<CamoFormState?> { null }

/**
 * Flutter's `Form` in Compose: the scope [CamoFormField]s attach to by key. [validateMode] is the default for
 * fields that don't set their own; [onChanged] fires when any field changes. In a lazy list, [revealField]
 * scrolls the row for a key into the tree so `validate()` can focus an off-screen field.
 */
@Composable
public fun CamoForm(
    state: CamoFormState,
    modifier: Modifier = Modifier,
    validateMode: ValidateMode = ValidateMode.Disabled,
    onChanged: (() -> Unit)? = null,
    revealField: (suspend (key: String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    SideEffect {
        state.mode = validateMode
        state.onChanged = onChanged
        state.reveal = revealField
        state.scope = scope
    }
    Box(modifier, propagateMinConstraints = true) {
        CompositionLocalProvider(LocalCamoForm provides state, content = content)
    }
}

/** [CamoForm] in a `LazyColumn`: `validate()` scrolls [lazyListState] to the row [indexOfKey] gives. */
@Composable
public fun CamoForm(
    state: CamoFormState,
    lazyListState: LazyListState,
    indexOfKey: (key: String) -> Int,
    modifier: Modifier = Modifier,
    validateMode: ValidateMode = ValidateMode.Disabled,
    onChanged: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CamoForm(state, modifier, validateMode, onChanged, revealField = { key ->
        indexOfKey(key).takeIf { it >= 0 }?.let { lazyListState.animateScrollToItem(it) }
    }, content = content)
}

/** A form field's view of its entry: what its builder reads and calls. */
@Stable
public class CamoFormFieldState<T> internal constructor(
    private val entry: FormEntry,
    private val form: CamoFormState?,
    private val mode: () -> ValidateMode,
    private val forced: () -> String?,
) {
    /** The current value. */
    @Suppress("UNCHECKED_CAST")
    public val value: T? get() = entry.value as T?

    /** The error to show: the forced error, a server error, or the rules' result. */
    public val error: String? get() = forced() ?: entry.shownError

    /** The user has changed this field. */
    public val hasInteracted: Boolean get() = entry.hasInteracted

    /** The value differs from the initial value. */
    public val isDirty: Boolean get() = entry.value != entry.initialValue

    /** Wire to the input's change callback. */
    public fun didChange(value: T?) {
        entry.value = value
        entry.hasInteracted = true
        entry.serverError = null
        if (mode() == ValidateMode.OnUserInteraction || mode() == ValidateMode.Always) validate()
        form?.changed()
    }

    /** Wire to the input's focus callback (for `OnUnfocus`). */
    public fun onFocusChanged(focused: Boolean) {
        if (!focused && mode() == ValidateMode.OnUnfocus && entry.hasInteracted) validate()
    }

    /** Runs the rules now; true when valid. */
    public fun validate(): Boolean = (form ?: standalone).check(entry)

    private companion object {
        val standalone = CamoFormState()
    }
}

/**
 * Flutter's `FormField`: puts any input into a [CamoForm]. Inside a form [key] is required: it names the entry
 * that holds the value (so it survives scrolling in a lazy list), and it's the key for `setErrors`, `matches`
 * and `value(key)`. Outside a form the field validates on its own. `visible = false` keeps the value but skips
 * validation and saving; a disabled field isn't validated. [forceError] shows instead of the rules' result.
 */
@Composable
public fun <T> CamoFormField(
    key: String? = null,
    initialValue: T? = null,
    rules: List<CamoRule<T>> = emptyList(),
    onSaved: ((T?) -> Unit)? = null,
    validateMode: ValidateMode? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    forceError: String? = null,
    builder: @Composable (CamoFormFieldState<T>) -> Unit,
) {
    val form = LocalCamoForm.current
    if (form != null && key == null) {
        check(!DebugChecks.enabled) { "A CamoFormField inside a CamoForm needs a key." }
    }
    val entry = remember(form, key) {
        if (form != null && key != null) form.entry(key, initialValue) else FormEntry(key ?: "", initialValue, 0)
    }
    val latestForce by androidx.compose.runtime.rememberUpdatedState(forceError)
    val latestMode by androidx.compose.runtime.rememberUpdatedState(validateMode)
    SideEffect { entry.update(rules, validateMode, enabled, visible, onSaved) }
    val (requester, bring) = attach(entry, form)
    val state = remember(entry, form) {
        CamoFormFieldState<T>(
            entry,
            form,
            mode = { latestMode ?: form?.mode ?: ValidateMode.Disabled },
            forced = { latestForce },
        )
    }
    if ((validateMode ?: form?.mode) == ValidateMode.Always && enabled && visible) {
        LaunchedEffect(entry, entry.value) { state.validate() }
    }
    Box(Modifier.bringIntoViewRequester(bring).focusRequester(requester), propagateMinConstraints = true) {
        builder(state)
    }
}

// Attaches the field on screen to its entry (focus and bring-into-view), and takes focus when validate()
// revealed it. The entry stays when the field leaves.
@Composable
private fun attach(entry: FormEntry, form: CamoFormState?): Pair<FocusRequester, BringIntoViewRequester> {
    val requester = remember { FocusRequester() }
    val bring = remember { BringIntoViewRequester() }
    DisposableEffect(entry) {
        entry.attachments++
        if (DebugChecks.enabled && entry.attachments > 1) {
            entry.attachments--
            error("Two CamoFormFields share the key \"${entry.key}\". Keys must be unique in a form.")
        }
        entry.focus = requester
        entry.bringIntoView = bring
        onDispose {
            entry.attachments--
            if (entry.focus === requester) {
                entry.focus = null
                entry.bringIntoView = null
            }
        }
    }
    if (form != null && form.pendingFocus == entry.key) {
        LaunchedEffect(entry) {
            form.pendingFocus = null
            focusSoon(requester, bring)
        }
    }
    return requester to bring
}

private const val FOCUS_FRAMES = 10

// Brings a just-attached field into view and focuses it, retrying for a few frames while its row settles.
internal suspend fun focusSoon(requester: FocusRequester, bring: BringIntoViewRequester?) {
    repeat(FOCUS_FRAMES) {
        withFrameNanos { }
        bring?.bringIntoView()
        if (requester.requestFocus()) return
    }
}

// Dates, times and date ranges saved as tagged text, so a form keeps a picked date across process death.
private const val DATE = "camo:date:"
private const val TIME = "camo:time:"
private const val RANGE = "camo:range:"

private fun savable(value: Any?): String? = when (value) {
    is LocalDate -> DATE + value
    is LocalTime -> TIME + value
    is LocalDateRange -> RANGE + value.start + "/" + value.endInclusive
    else -> null
}

private fun restoredValue(value: Any?): Any? = when {
    value !is String -> value
    value.startsWith(DATE) -> LocalDate.parse(value.removePrefix(DATE))
    value.startsWith(TIME) -> LocalTime.parse(value.removePrefix(TIME))
    value.startsWith(RANGE) -> value.removePrefix(RANGE).split('/').let { (start, end) ->
        LocalDate.parse(start)..LocalDate.parse(end)
    }
    else -> value
}
