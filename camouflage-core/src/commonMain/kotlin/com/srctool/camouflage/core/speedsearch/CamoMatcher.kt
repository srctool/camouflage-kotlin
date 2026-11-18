package com.srctool.camouflage.core.speedsearch

/** How [CamoMatcher] compares a query with a text. */
public enum class MatchKind {
    /** The text starts with the query. */
    Prefix,

    /** The text contains the query. */
    Substring,

    /** Each query letter starts a word or hump of the text, or continues the one before (`fLog`: `featureLogin`). */
    CamelHump,

    /** The query letters appear in order, anywhere (`ftl` finds `featureLogin`). */
    Fuzzy,

    /** [Prefix], then [Substring], then [CamelHump]: the first that matches. */
    Auto,
}

/**
 * Matches a typed query against a text, case- and accent-insensitively ("zur" finds "Zürich"), and says which
 * characters matched so they can be highlighted. Pure and window-free: the same rules back speed search and the
 * command palette.
 */
public object CamoMatcher {
    /**
     * The ranges of [text] that match [query], or `null` when it doesn't match. An empty query matches everything
     * with no ranges. Ranges are indexes into [text]; they never overlap and are in order.
     */
    public fun match(query: String, text: String, kind: MatchKind = MatchKind.Auto): List<IntRange>? {
        if (query.isEmpty()) return emptyList()
        val q = foldChars(query)
        val t = foldChars(text)
        return when (kind) {
            MatchKind.Prefix -> prefix(q, t)
            MatchKind.Substring -> substring(q, t)
            MatchKind.CamelHump -> hump(q, t, text)
            MatchKind.Fuzzy -> fuzzy(q, t)
            MatchKind.Auto -> prefix(q, t) ?: substring(q, t) ?: hump(q, t, text)
        }
    }

    private fun prefix(q: String, t: String): List<IntRange>? =
        if (t.startsWith(q)) listOf(0 until q.length) else null

    private fun substring(q: String, t: String): List<IntRange>? =
        t.indexOf(q).takeIf { it >= 0 }?.let { listOf(it until it + q.length) }

    private fun fuzzy(q: String, t: String): List<IntRange>? {
        val hits = ArrayList<Int>(q.length)
        var from = 0
        for (c in q) {
            val at = t.indexOf(c, from)
            if (at < 0) return null
            hits += at
            from = at + 1
        }
        return merge(hits)
    }

    // The query's letters, each starting a hump or continuing the previous letter's hump.
    private fun hump(q: String, t: String, original: String): List<IntRange>? {
        val starts = BooleanArray(t.length) { isHumpStart(original, it) }
        val hits = IntArray(q.length)
        fun place(qi: Int, from: Int, afterHit: Int): Boolean =
            qi == q.length || (from until t.length).any { ti ->
                val continues = afterHit >= 0 && ti == afterHit + 1
                val fits = t[ti] == q[qi] && (starts[ti] || continues)
                fits && run {
                    hits[qi] = ti
                    place(qi + 1, ti + 1, ti)
                }
            }
        return if (place(0, 0, -1)) merge(hits.asList()) else null
    }

    private fun isHumpStart(text: String, i: Int): Boolean {
        if (i == 0) return true
        val c = text[i]
        val before = text[i - 1]
        return (c.isUpperCase() && !before.isUpperCase()) ||
            (!before.isLetterOrDigit() && c.isLetterOrDigit()) ||
            (c.isDigit() != before.isDigit() && c.isLetterOrDigit())
    }

    // Adjacent indexes become one range.
    private fun merge(hits: List<Int>): List<IntRange> {
        val out = ArrayList<IntRange>()
        var start = hits.first()
        var end = start
        for (i in 1 until hits.size) {
            if (hits[i] == end + 1) {
                end = hits[i]
            } else {
                out += start..end
                start = hits[i]
                end = start
            }
        }
        out += start..end
        return out
    }
}

private val Accents = mapOf(
    "aàáâãäåā" to 'a', "cçćč" to 'c', "eèéêëēė" to 'e', "iìíîïī" to 'i', "nñń" to 'n',
    "oòóôõöøō" to 'o', "sśš" to 's', "uùúûüū" to 'u', "yýÿ" to 'y', "zźżž" to 'z',
).flatMap { (chars, base) -> chars.map { it to base } }.toMap()

// One character in, one out, so a folded text keeps the indexes of the original.
internal fun foldChar(c: Char): Char = c.lowercaseChar().let { Accents[it] ?: it }

private fun foldChars(text: String): String = CharArray(text.length) { foldChar(text[it]) }.concatToString()

// Lowercase, with the common Latin accents removed ("Zürich" matches "zur").
internal fun fold(text: String): String = foldChars(text)
