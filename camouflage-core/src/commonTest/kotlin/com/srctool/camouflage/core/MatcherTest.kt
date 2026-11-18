package com.srctool.camouflage.core

import com.srctool.camouflage.core.speedsearch.CamoMatcher
import com.srctool.camouflage.core.speedsearch.CamoSpeedSearchState
import com.srctool.camouflage.core.speedsearch.MatchKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.TimeSource

// The speed search matcher (base CamoSpeedSearch, M16): pure, no window.
class MatcherTest {
    private fun match(query: String, text: String, kind: MatchKind = MatchKind.Auto) =
        CamoMatcher.match(query, text, kind)

    @Test
    fun anEmptyQueryMatchesEverythingWithNoRanges() {
        assertEquals(emptyList(), match("", "anything"))
    }

    @Test
    fun aPrefixMatchesFirstAndSaysWhere() {
        assertEquals(listOf(0..2), match("fea", "featureLogin"))
    }

    @Test
    fun aSubstringMatchesWhenThereIsNoPrefix() {
        assertEquals(listOf(7..11), match("login", "featureLogin"))
    }

    @Test
    fun camelHumpsMatchTheStartsOfWords() {
        assertEquals(listOf(0..0, 7..9), match("fLog", "featureLogin"))
        assertEquals(listOf(0..0, 7..7), match("fl", "featureLogin"))
        assertNull(match("fLx", "featureLogin"))
    }

    @Test
    fun humpsAlsoStartAfterSeparatorsAndDigits() {
        assertEquals(listOf(0..0, 3..3), match("mc", "my_class", MatchKind.CamelHump))
        assertEquals(listOf(0..1, 4..4), match("ab2", "abc 2x", MatchKind.CamelHump))
    }

    @Test
    fun caseAndAccentsAreIgnored() {
        assertEquals(listOf(0..2), match("zur", "Zürich"))
        assertEquals(listOf(0..2), match("ZÜR", "zurich"))
    }

    @Test
    fun fuzzyFindsTheLettersInOrder() {
        assertEquals(listOf(0..0, 7..7, 11..11), match("fln", "featureLogin", MatchKind.Fuzzy))
        assertNull(match("nlf", "featureLogin", MatchKind.Fuzzy))
    }

    @Test
    fun theKindsAreStrict() {
        assertNull(match("login", "featureLogin", MatchKind.Prefix))
        assertNull(match("fLog", "featureLogin", MatchKind.Substring))
    }

    @Test
    fun theStateMatchesWithItsKindAndClears() {
        val state = CamoSpeedSearchState(kind = MatchKind.Substring)
        assertEquals(true, state.matches("anything"))
        state.type("og")
        state.type("i")
        assertEquals("ogi", state.query)
        assertEquals(listOf(8..10), state.match("featureLogin"))
        state.backspace()
        assertEquals("og", state.query)
        state.clear()
        assertEquals(false, state.active)
    }

    @Test
    fun fiveThousandTextsFilterInWellUnderAFrame() {
        val texts = List(5000) { "module$it/src/featureLogin$it.kt" }
        val warm = texts.count { CamoMatcher.match("fLog9", it) != null }
        val start = TimeSource.Monotonic.markNow()
        val found = texts.count { CamoMatcher.match("fLog9", it) != null }
        val millis = start.elapsedNow().inWholeMilliseconds
        assertEquals(warm, found)
        assert(millis < 100) { "took $millis ms" }
    }
}
