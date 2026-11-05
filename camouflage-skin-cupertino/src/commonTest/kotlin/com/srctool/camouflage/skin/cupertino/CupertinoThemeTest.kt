package com.srctool.camouflage.skin.cupertino

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import com.srctool.camouflage.core.theme.ThemeIssue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CupertinoThemeTest {
    // Apple's published system colors don't all reach WCAG AA (white on systemBlue is 4.0, secondaryLabel on the
    // grouped background 3.1): the skin keeps them as published (base Cupertino Skin §3), so these pairs are the
    // known exceptions. The text roles on the page (`onSurface`, the accent on `surface`) must pass, and a
    // new pair failing is a regression.
    private val appleKnown = setOf(
        "onPrimary/primary", "onPrimaryContainer/primaryContainer", "onSecondary/secondary",
        "onTertiaryContainer/tertiaryContainer", "onError/error", "onErrorContainer/errorContainer",
        "onSuccess/success", "onSuccessContainer/successContainer", "onWarning/warning", "onInfo/info",
        "onInfoContainer/infoContainer", "onSurfaceVariant/surface", "onSurfaceVariant/surfaceContainerLowest",
        "onSurfaceVariant/surfaceContainerLow", "onSurfaceVariant/surfaceContainer",
        "onSurfaceVariant/surfaceContainerHigh", "onSurfaceVariant/surfaceContainerHighest", "outline/surface",
        "onWarningContainer/warningContainer", "onTertiary/tertiary", "primary/surface",
        "onSuccessContainer/successContainer", "onSecondaryContainer/secondaryContainer",
    )

    @Test
    fun onlyApplesOwnColorPairsAreBelowContrast() {
        val accent = Color(0xFF0B57D0)
        listOf(cupertinoLight, cupertinoDark, cupertinoTheme(accent), cupertinoTheme(accent, dark = true)).forEach {
            val failing = it.validate().filterIsInstance<ThemeIssue.LowContrast>().map { issue -> issue.pair }
            assertEquals(emptyList(), failing.filterNot { pair -> pair in appleKnown })
            assertTrue("onSurface/surface" !in failing)
        }
    }

    // The system colors of the HIG tables, pinned: a change here is a decision, not a side effect.
    @Test
    fun systemColorsArePinned() {
        val light = cupertinoLight.colors
        val dark = cupertinoDark.colors
        assertEquals(Color(0xFF007AFF), light.primary)
        assertEquals(Color(0xFF0A84FF), dark.primary)
        assertEquals(Color(0xFFFF3B30), light.error)
        assertEquals(Color(0xFFFF453A), dark.error)
        assertEquals(Color(0xFF34C759), light.success)
        assertEquals(Color(0xFF30D158), dark.success)
        assertEquals(Color(0xFFFF9500), light.warning)
        assertEquals(Color(0xFFFF9F0A), dark.warning)
        assertEquals(Color(0xFF30B0C7), light.info)
        assertEquals(Color(0xFF40CBE0), dark.info)
        assertEquals(Color(0xFFF2F2F7), light.surface) // systemGroupedBackground
        assertEquals(Color.White, light.surfaceContainerLow) // secondarySystemGroupedBackground
        assertEquals(Color.Black, dark.surface)
        assertEquals(Color(0xFF1C1C1E), dark.surfaceContainerLow)
    }

    @Test
    fun anAccentReplacesTheSystemBlueInBothAppearances() {
        val accent = Color(0xFF0B57D0)
        assertEquals(accent, cupertinoTheme(accent).colors.primary)
        assertEquals(accent, cupertinoTheme(accent, dark = true).colors.primary)
    }
}
