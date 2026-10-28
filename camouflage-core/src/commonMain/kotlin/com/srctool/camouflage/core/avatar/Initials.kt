package com.srctool.camouflage.core.avatar

// Initials and the initials color, the same on every platform (and in the Flutter library).

private const val FNV_OFFSET = 0x811C9DC5u
private const val FNV_PRIME = 0x01000193u
private const val BYTE_MASK = 0xFF

/**
 * The 32-bit FNV-1a hash of [name]'s UTF-8 bytes: the seed that picks a person's initials color. Unlike
 * `hashCode()` it's specified, so a name gets the same color on every Kotlin target and in Flutter.
 */
public fun avatarColorSeed(name: String): UInt {
    var hash = FNV_OFFSET
    for (byte in name.encodeToByteArray()) {
        hash = (hash xor (byte.toInt() and BYTE_MASK).toUInt()) * FNV_PRIME
    }
    return hash
}

/**
 * The initials for [name]: the first grapheme of its first and last words, uppercased ("Dwiki Riyadi" → "DR",
 * "Dwiki" → "D"). A name in a script without word spaces gets its first grapheme. `null` when there's nothing to
 * show: an empty name, or one that starts with an emoji or a symbol (the avatar shows its person icon instead).
 */
public fun avatarInitials(name: String?): String? {
    val words = name?.trim()?.split(Whitespace)?.filter { it.isNotEmpty() }.orEmpty()
    val first = words.firstOrNull()?.let(::firstLetterGrapheme) ?: return null
    val last = if (words.size > 1) firstLetterGrapheme(words.last()) else null
    return (first + last.orEmpty()).uppercase()
}

private val Whitespace = Regex("\\s+")

// The word's first grapheme when it's a letter; null for an emoji, a digit or a symbol.
private fun firstLetterGrapheme(word: String): String? {
    val grapheme = graphemes(word).firstOrNull() ?: return null
    return grapheme.takeIf { it[0].isLetter() }
}

/**
 * Splits [text] into user-perceived characters, close enough for initials: a code point plus any combining
 * marks, variation selectors, emoji modifiers, tag characters and zero-width-joined parts that follow it, and
 * regional-indicator pairs (flags). No `BreakIterator`, which common code doesn't have.
 */
internal fun graphemes(text: String): List<String> {
    val clusters = mutableListOf<StringBuilder>()
    var i = 0
    var previous = -1
    var regionalRun = 0
    while (i < text.length) {
        val cp = codePointAt(text, i)
        val width = if (cp > Char.MAX_VALUE.code) 2 else 1
        val regional = cp in RegionalIndicators
        val extends = clusters.isNotEmpty() &&
            (cp.isExtending() || previous == ZWJ || (regional && regionalRun % 2 == 1))
        if (extends) {
            clusters.last().append(text, i, i + width)
        } else {
            clusters += StringBuilder(text.substring(i, i + width))
        }
        regionalRun = if (regional) regionalRun + 1 else 0
        previous = cp
        i += width
    }
    return clusters.map { it.toString() }
}

private const val ZWJ = 0x200D
private val RegionalIndicators = 0x1F1E6..0x1F1FF
private val Extending = listOf(
    0x0300..0x036F, 0x1AB0..0x1AFF, 0x1DC0..0x1DFF, 0x20D0..0x20FF, 0xFE20..0xFE2F, // combining marks
    0xFE00..0xFE0F, 0xE0100..0xE01EF, // variation selectors
    0x1F3FB..0x1F3FF, // emoji skin tones
    0xE0020..0xE007F, // tag characters
    ZWJ..ZWJ,
)

private fun Int.isExtending() = Extending.any { this in it }

private fun codePointAt(text: String, index: Int): Int {
    val high = text[index]
    if (high.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
        return (high.code - Char.MIN_HIGH_SURROGATE.code shl SURROGATE_BITS) +
            (text[index + 1].code - Char.MIN_LOW_SURROGATE.code) + SUPPLEMENTARY_BASE
    }
    return high.code
}

private const val SUPPLEMENTARY_BASE = 0x10000
private const val SURROGATE_BITS = 10
