package com.srctool.camouflage.core

import com.srctool.camouflage.core.palette.CamoCommandItem
import com.srctool.camouflage.core.palette.CamoCommandPaletteState
import com.srctool.camouflage.core.palette.CamoCommandSource
import com.srctool.camouflage.core.speedsearch.CamoMatcher
import com.srctool.camouflage.core.speedsearch.MatchKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.coroutines.test.runTest

// Fuzzy scoring and the palette's state (base CamoCommandPalette, M16).
class PaletteTest {
    private fun score(query: String, text: String) = CamoMatcher.score(query, text)?.score

    @Test
    fun aPrefixBeatsASubstringBeatsAHumpBeatsAScatteredMatch() {
        val prefix = score("open", "Open File")!!
        val substring = score("file", "Open File")!!
        val hump = score("of", "Open File")!!
        val scattered = score("ofl", "Open File")!!
        assertTrue(prefix > substring, "prefix $prefix, substring $substring")
        assertTrue(substring > hump, "substring $substring, hump $hump")
        assertTrue(hump > scattered, "hump $hump, scattered $scattered")
    }

    @Test
    fun consecutiveLettersAndWordStartsScoreHigher() {
        assertTrue(score("fl", "featureLogin")!! > score("fl", "fooxxxlogin")!!)
        assertTrue(score("gcm", "Git: Commit Message")!! > score("gcm", "gegcxxmx")!!)
    }

    @Test
    fun aShorterTextWinsATie() {
        assertTrue(score("log", "Login")!! > score("log", "Login with a very long description")!!)
    }

    @Test
    fun noMatchIsNullAndAnEmptyQueryMatchesWithScoreZero() {
        assertNull(CamoMatcher.score("xyz", "Open File"))
        val all = assertNotNull(CamoMatcher.score("", "anything"))
        assertEquals(0, all.score)
        assertEquals(emptyList(), all.ranges)
    }

    @Test
    fun theRangesAreTheHighlight() {
        assertEquals(listOf(0..0, 5..5), CamoMatcher.score("of", "Open File")?.ranges)
    }

    @Test
    fun aSourceRanksAListAndKeepsTheBestFew() = runTest {
        val items = listOf("Open File", "Close File", "File: Save", "Profile", "Reopen Closed").map {
            CamoCommandItem(it, it, {})
        }
        val source = CamoCommandSource.of("Actions", items, limit = 3)
        val found = source.search("file").map { it.title }
        assertEquals(3, found.size)
        assertEquals("File: Save", found.first())
        assertEquals(listOf("Open File", "Close File", "File: Save"), source.search("").map { it.title })
    }

    @Test
    fun tenThousandItemsRankQuickly() = runTest {
        val items = List(10_000) { CamoCommandItem(it, "module$it/src/featureLogin$it.kt", {}) }
        val source = CamoCommandSource.of("Files", items, limit = 8)
        source.search("fLog9")
        val start = TimeSource.Monotonic.markNow()
        val found = source.search("fLog9")
        val millis = start.elapsedNow().inWholeMilliseconds
        assertEquals(8, found.size)
        assertTrue(millis < 2000, "took $millis ms")
    }

    @Test
    fun theStateRemembersTheMostRecentFirstAndRoundTrips() {
        val state = CamoCommandPaletteState(maxRecent = 3)
        listOf("a", "b", "c", "b", "d").forEach { state.remember(it) }
        assertEquals(listOf<Any>("d", "b", "c"), state.recent)
        val again = CamoCommandPaletteState(state.snapshot())
        assertEquals(state.recent, again.recent)
    }

    @Test
    fun showResetsTheQueryAndToggleFlipsIt() {
        val state = CamoCommandPaletteState()
        state.query = "old"
        state.toggle()
        assertEquals(true, state.open)
        assertEquals("", state.query)
        state.toggle()
        assertEquals(false, state.open)
        assertEquals(MatchKind.Fuzzy, CamoCommandSource("x") { emptyList() }.matchKind)
    }
}
