package com.srctool.camouflage.core.stepper

import androidx.compose.runtime.Immutable
import kotlin.math.absoluteValue

/**
 * A fixed-point decimal: [unscaled] units of 10^-[scale] (`12.50` is 1250 with scale 2). Adding and subtracting
 * are exact, so steps never drift (0.1 × 3 is 0.3). Equality is by representation, like `BigDecimal`: `1.0` and
 * `1.00` compare equal but aren't `==`. Convert to an app's money or quantity type at the edge.
 */
@Immutable
public data class CamoDecimal(val unscaled: Long, val scale: Int) : Comparable<CamoDecimal> {
    init {
        require(scale in 0..MAX_SCALE) { "CamoDecimal scale must be 0 to $MAX_SCALE, was $scale" }
    }

    /** Parses [text] like "12.50", "-3" or "0,25" (either separator). */
    public constructor(text: String) : this(parseParts(text))

    private constructor(parts: Pair<Long, Int>) : this(parts.first, parts.second)

    /** The exact sum. Its scale is the larger of the two. */
    public operator fun plus(other: CamoDecimal): CamoDecimal {
        val s = maxOf(scale, other.scale)
        return CamoDecimal(rescaled(s) + other.rescaled(s), s)
    }

    /** The exact difference. Its scale is the larger of the two. */
    public operator fun minus(other: CamoDecimal): CamoDecimal {
        val s = maxOf(scale, other.scale)
        return CamoDecimal(rescaled(s) - other.rescaled(s), s)
    }

    /** This value [times] times, exactly. */
    public operator fun times(times: Long): CamoDecimal = CamoDecimal(unscaled * times, scale)

    override fun compareTo(other: CamoDecimal): Int {
        val s = maxOf(scale, other.scale)
        return rescaled(s).compareTo(other.rescaled(s))
    }

    /**
     * This value with [newScale] digits after the separator; extra digits are rounded half away from zero.
     */
    public fun withScale(newScale: Int): CamoDecimal = when {
        newScale >= scale -> CamoDecimal(rescaled(newScale), newScale)
        else -> {
            val divisor = pow10(scale - newScale)
            val half = divisor / 2
            val rounded = if (unscaled >= 0) (unscaled + half) / divisor else (unscaled - half) / divisor
            CamoDecimal(rounded, newScale)
        }
    }

    /** The value with [decimalSeparator] and every digit of its scale: `format(',')` of 12.50 is "12,50". */
    public fun format(decimalSeparator: Char = '.'): String {
        val digits = unscaled.absoluteValue.toString().padStart(scale + 1, '0')
        val sign = if (unscaled < 0) "-" else ""
        if (scale == 0) return sign + digits
        return sign + digits.dropLast(scale) + decimalSeparator + digits.takeLast(scale)
    }

    /** The value as `Double`, for display math only (never for stepping). */
    public fun toDouble(): Double = unscaled / pow10(scale).toDouble()

    override fun toString(): String = format()

    internal fun rescaled(to: Int): Long = unscaled * pow10(to - scale)

    public companion object {
        /** The largest scale: 18 digits after the separator. */
        public const val MAX_SCALE: Int = 18

        /** Zero. */
        public val Zero: CamoDecimal = CamoDecimal(0, 0)

        /** [text] parsed, or `null` when it isn't a plain decimal ("1.2.3", "abc", ""). */
        public fun parseOrNull(text: String): CamoDecimal? = runCatching { CamoDecimal(text) }.getOrNull()

        /** A whole number. */
        public fun of(value: Long): CamoDecimal = CamoDecimal(value, 0)
    }
}

private const val RADIX = 10L

private fun pow10(n: Int): Long {
    var p = 1L
    repeat(n) { p *= RADIX }
    return p
}

private fun parseParts(text: String): Pair<Long, Int> {
    val t = text.trim()
    val negative = t.startsWith('-')
    val body = if (negative || t.startsWith('+')) t.drop(1) else t
    val separators = body.count { it == '.' || it == ',' }
    require(body.isNotEmpty() && separators <= 1 && body.all { it.isDigit() || it == '.' || it == ',' }) {
        "Not a decimal: \"$text\""
    }
    val whole = body.substringBefore('.').substringBefore(',')
    val fraction = if (separators == 1) body.substringAfter('.', body.substringAfter(',')) else ""
    require(whole.isNotEmpty() || fraction.isNotEmpty()) { "Not a decimal: \"$text\"" }
    val units = (whole + fraction).ifEmpty { "0" }.toLong()
    return (if (negative) -units else units) to fraction.length
}
