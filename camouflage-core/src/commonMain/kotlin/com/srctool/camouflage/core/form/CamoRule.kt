package com.srctool.camouflage.core.form

/**
 * One validation check, like a Flutter validator: `null` means valid, otherwise the message to show. Rules are
 * constants or classes, reusable across forms; a field's rules run in order and the first failure wins.
 */
public fun interface CamoRule<in T> {
    /** The message for [value], or `null` when it's valid. */
    public fun validate(value: T?): String?
}

/** The built-in rules. Messages are always passed in, for localization. */
public object CamoRules {
    /** Not null, and not blank for text. */
    public fun required(message: String): CamoRule<Any?> = CamoRule { value ->
        when (value) {
            null -> message
            is CharSequence -> message.takeIf { value.isBlank() }
            is Collection<*> -> message.takeIf { value.isEmpty() }
            else -> null
        }
    }

    /** A boolean that is true (a consent checkbox). */
    public fun requiredTrue(message: String): CamoRule<Boolean> =
        CamoRule { value -> if (value == true) null else message }

    /** At least [n] characters; empty text passes (pair with [required]). */
    public fun minLength(n: Int, message: String): CamoRule<String> =
        CamoRule { value -> if (!value.isNullOrEmpty() && value.length < n) message else null }

    /** At most [n] characters. */
    public fun maxLength(n: Int, message: String): CamoRule<String> =
        CamoRule { value -> if ((value?.length ?: 0) > n) message else null }

    /** A number of at least [n]; text that isn't a number fails, empty text passes. */
    public fun min(n: Double, message: String): CamoRule<String> = number(message) { it >= n }

    /** A number of at most [n]; text that isn't a number fails, empty text passes. */
    public fun max(n: Double, message: String): CamoRule<String> = number(message) { it <= n }

    /** The whole text matches [regex]; empty text passes. */
    public fun pattern(regex: Regex, message: String): CamoRule<String> =
        CamoRule { value -> if (!value.isNullOrEmpty() && !regex.matches(value)) message else null }

    /** A practical email shape (not full RFC 5322); empty text passes. */
    public fun email(message: String): CamoRule<String> = pattern(EMAIL, message)

    /** Equals the field [fieldKey] in the same form (confirm password). Outside a form it passes. */
    public fun matches(fieldKey: String, message: String): CamoRule<String> = MatchesRule(fieldKey, message)

    private val EMAIL = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")

    private fun number(message: String, ok: (Double) -> Boolean) = CamoRule<String> { text ->
        if (text.isNullOrBlank()) return@CamoRule null
        val n = text.trim().replace(',', '.').toDoubleOrNull()
        message.takeIf { n == null || !ok(n) }
    }
}

// Needs the form to read the other field; the form resolves it while validating.
internal class MatchesRule(val fieldKey: String, val message: String) : CamoRule<String> {
    override fun validate(value: String?): String? = null

    fun validate(value: Any?, other: Any?): String? = message.takeIf { value != other }
}
