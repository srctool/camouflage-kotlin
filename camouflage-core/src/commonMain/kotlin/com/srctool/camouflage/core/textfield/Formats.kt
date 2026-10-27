package com.srctool.camouflage.core.textfield

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.insert

/**
 * Which input a text field accepts. Core applies it before `onValueChange`, so the app never sees rejected
 * characters: [filter] gets the text before and after an edit and returns the accepted text (return [old] to
 * reject the edit).
 */
public fun interface CamoInputFilter {
    /** The accepted raw text after an edit from [old] to [new]. */
    public fun filter(old: String, new: String): String

    public companion object {
        /** Digits only, at most [maxLength] of them when set. */
        public fun digitsOnly(maxLength: Int? = null): CamoInputFilter = CamoInputFilter { _, new ->
            new.filter(Char::isDigit).let { if (maxLength == null) it else it.take(maxLength) }
        }

        /** At most [n] characters. */
        public fun maxLength(n: Int): CamoInputFilter = CamoInputFilter { _, new -> new.take(n) }

        /** Only the characters [regex] matches, one at a time (e.g. `Regex("[A-Za-z ]")`). */
        public fun pattern(regex: Regex): CamoInputFilter = CamoInputFilter { _, new ->
            new.filter { regex.matches(it.toString()) }
        }

        /**
         * A decimal number: digits and one [decimalSeparator], at most [decimals] digits after it. Pair it with
         * `CamoDisplayFormat.decimal` (which only groups the shown integer part).
         */
        public fun decimal(decimals: Int = 2, decimalSeparator: Char = ','): CamoInputFilter =
            CamoInputFilter { old, new ->
                val kept = new.filter { it.isDigit() || it == decimalSeparator }
                val whole = kept.substringBefore(decimalSeparator)
                val fraction = kept.substringAfter(decimalSeparator, missingDelimiterValue = "")
                when {
                    kept.count { it == decimalSeparator } > 1 -> old
                    decimalSeparator !in kept -> whole
                    decimals == 0 -> whole
                    else -> whole + decimalSeparator + fraction.filter(Char::isDigit).take(decimals)
                }
            }
    }
}

/**
 * How a raw value is shown without storing the formatting: `081234567890` shown as `0812-3456-7890`. The value
 * stays raw; core maps the cursor between the two, so it never jumps. Formats that only insert characters (the
 * built-ins) implement [CamoInsertingFormat] for an exact cursor; a custom format may implement only [format].
 */
public interface CamoDisplayFormat {
    /** What the user sees for [raw]. */
    public fun format(raw: String): String

    /** The shown cursor position for raw position [offset]. */
    public fun rawToShown(offset: Int, raw: String): Int = format(raw.take(offset)).length

    /** The raw cursor position for shown position [offset]. */
    public fun shownToRaw(offset: Int, shown: String): Int = offset

    public companion object {
        /** A mask where each `#` is one raw character: `pattern("####-####-#####")`. Other characters are shown. */
        public fun pattern(mask: String): CamoDisplayFormat = InsertionFormat(mask.filter { it != '#' }.toSet()) {
            raw ->
            buildList {
                var r = 0
                for (c in mask) {
                    if (r >= raw.length) break
                    if (c == '#') r++ else add(r to c.toString())
                }
            }
        }

        /** Groups digits from the right: `grouping('.', 3)` shows `1250000` as `1.250.000`. */
        public fun grouping(separator: Char = '.', size: Int = 3): CamoDisplayFormat =
            InsertionFormat(setOf(separator)) { raw -> groupInsertions(raw, raw.length, separator, size) }

        /**
         * Groups the integer part and keeps the decimal part as typed: `1250000,50` shows as `1.250.000,50`.
         * At most [decimals] digits after [decimalSeparator] come from `CamoInputFilter.decimal`.
         */
        public fun decimal(
            decimals: Int = 2,
            decimalSeparator: Char = ',',
            groupSeparator: Char = '.',
        ): CamoDisplayFormat = InsertionFormat(setOf(groupSeparator)) { raw ->
            check(decimals >= 0) { "decimals must not be negative" }
            val end = raw.indexOf(decimalSeparator).takeIf { it >= 0 } ?: raw.length
            groupInsertions(raw, end, groupSeparator, THOUSANDS)
        }
    }
}

/** A format that only inserts characters into the raw text, so the cursor maps exactly. */
public interface CamoInsertingFormat : CamoDisplayFormat {
    /** Where to insert what, as (raw position, text) pairs in increasing position. */
    public fun insertions(raw: String): List<Pair<Int, String>>

    /** The characters this format inserts (never part of a raw value), used to read raw text back. */
    public val inserted: Set<Char>

    override fun format(raw: String): String = buildString {
        var r = 0
        for ((at, text) in insertions(raw)) {
            append(raw, r, at)
            append(text)
            r = at
        }
        append(raw, r, raw.length)
    }

    override fun rawToShown(offset: Int, raw: String): Int =
        offset + insertions(raw).filter { it.first < offset }.sumOf { it.second.length }

    override fun shownToRaw(offset: Int, shown: String): Int = shown.take(offset).count { it !in inserted }
}

private const val THOUSANDS = 3

private class InsertionFormat(
    override val inserted: Set<Char>,
    private val at: (String) -> List<Pair<Int, String>>,
) : CamoInsertingFormat {
    override fun insertions(raw: String): List<Pair<Int, String>> = at(raw)
}

// A separator before every group of `size` digits counted back from `end`, after an optional leading minus.
private fun groupInsertions(raw: String, end: Int, separator: Char, size: Int): List<Pair<Int, String>> {
    val start = if (raw.startsWith('-')) 1 else 0
    return (start + 1 until end).filter { (end - it) % size == 0 }.map { it to separator.toString() }
}

internal fun CamoInputFilter.toInputTransformation(): InputTransformation = InputTransformation {
    val old = originalText.toString()
    val new = asCharSequence().toString()
    val accepted = filter(old, new)
    when (accepted) {
        new -> Unit
        old -> revertAllChanges()
        else -> replace(0, length, accepted)
    }
}

internal fun CamoDisplayFormat.toOutputTransformation(): OutputTransformation = OutputTransformation {
    val raw = asCharSequence().toString()
    if (this@toOutputTransformation is CamoInsertingFormat) {
        // Inserting from the end keeps the earlier positions valid; Compose maps the cursor across each insert.
        for ((at, text) in insertions(raw).asReversed()) insert(at, text)
    } else {
        replaceAll(format(raw))
    }
}

private fun TextFieldBuffer.replaceAll(text: String) = replace(0, length, text)
