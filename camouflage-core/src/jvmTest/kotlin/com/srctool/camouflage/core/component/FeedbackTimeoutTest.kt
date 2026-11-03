package com.srctool.camouflage.core.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.snackbar.CamoMessenger
import com.srctool.camouflage.core.snackbar.SnackbarResult
import com.srctool.camouflage.core.snackbar.messenger
import com.srctool.camouflage.core.themeT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertNull

// A screen reader's timeout keeps a snackbar with an action. JVM only: advancing the clock a minute is too slow for
// the browser test runner on wasm, where it failed intermittently; the logic is the same on every target.
@OptIn(ExperimentalTestApi::class)
class FeedbackTimeoutTest {
    private lateinit var messenger: CamoMessenger
    private lateinit var scope: CoroutineScope

    @Test
    fun aScreenReaderTimeoutKeepsTheSnackbar() = runComposeUiTest {
        var result: SnackbarResult? = null
        setContent {
            CompositionLocalProvider(LocalAccessibilityManager provides NeverTimeOut) {
                CamouflageProvider(skinA, themeT) {
                    messenger = Camouflage.messenger
                    scope = rememberCoroutineScope()
                }
            }
        }
        scope.launch { result = messenger.show("Archived", "Undo") }
        waitForIdle()
        mainClock.advanceTimeBy(60_000)
        waitForIdle()
        assertNull(result)
        onNodeWithText("Undo").assertIsDisplayed()
    }

    private object NeverTimeOut : AccessibilityManager {
        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean,
        ): Long = if (containsControls) Long.MAX_VALUE else originalTimeoutMillis
    }
}
