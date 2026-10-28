package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.banner.CamoBanner
import com.srctool.camouflage.core.progress.CamoProgressBar
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.snackbar.CamoMessenger
import com.srctool.camouflage.core.snackbar.CamoMessengerInsets
import com.srctool.camouflage.core.snackbar.LocalCamoMessenger
import com.srctool.camouflage.core.snackbar.SnackbarDuration
import com.srctool.camouflage.core.snackbar.SnackbarResult
import com.srctool.camouflage.core.snackbar.SnackbarTheme
import com.srctool.camouflage.core.snackbar.messenger
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.themeT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The M7 feedback components' tests from their Kotlin notes: progress bar, messenger and snackbar, banner.
@OptIn(ExperimentalTestApi::class)
class FeedbackTest {
    private lateinit var messenger: CamoMessenger
    private lateinit var scope: CoroutineScope

    private fun ComposeUiTest.camo(theme: CamoTheme = themeT, content: @Composable () -> Unit = {}) = setContent {
        CamouflageProvider(skinA, theme) {
            messenger = Camouflage.messenger
            scope = rememberCoroutineScope()
            content()
        }
    }

    private fun ComposeUiTest.show(
        message: String,
        action: String? = null,
        tone: Tone = Tone.Default,
        duration: SnackbarDuration = SnackbarDuration.Auto,
        withDismiss: Boolean? = null,
        onResult: (SnackbarResult) -> Unit = {},
    ): Job {
        val job = scope.launch { onResult(messenger.show(message, action, tone, duration, withDismiss)) }
        waitForIdle()
        return job
    }

    // Progress bars

    @Test
    fun aBarAnnouncesItsValueOrThatItsWorking() = runComposeUiTest {
        camo {
            Column {
                CamoProgressBar(Modifier.testTag("upload"), progress = 1.4f, label = "Uploading photo")
                CamoProgressBar(Modifier.testTag("loading"))
            }
        }
        val range = SemanticsProperties.ProgressBarRangeInfo
        onNodeWithTag("upload")
            .assert(SemanticsMatcher.expectValue(range, ProgressBarRangeInfo(1f, 0f..1f)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Uploading photo")))
        onNodeWithTag("loading").assert(SemanticsMatcher.expectValue(range, ProgressBarRangeInfo.Indeterminate))
    }

    // Messenger and snackbars

    @Test
    fun showReturnsActionPerformedWhenTheActionIsTapped() = runComposeUiTest {
        var result: SnackbarResult? = null
        camo()
        show("Message archived", action = "Undo") { result = it }
        onNodeWithText("Message archived").assertIsDisplayed()
        onNodeWithText("Undo").performClick()
        waitForIdle()
        assertEquals(SnackbarResult.ActionPerformed, result)
    }

    @Test
    fun aSnackbarTimesOutAsDismissed() = runComposeUiTest {
        var result: SnackbarResult? = null
        camo()
        show("Saved", duration = SnackbarDuration.Short) { result = it }
        mainClock.advanceTimeBy(3_000)
        assertNull(result)
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertEquals(SnackbarResult.Dismissed, result)
    }

    @Test
    fun messagesQueueInOrder() = runComposeUiTest {
        val results = mutableListOf<String>()
        camo()
        show("First", duration = SnackbarDuration.Indefinite) { results += "First $it" }
        show("Second", duration = SnackbarDuration.Indefinite) { results += "Second $it" }
        onNodeWithText("First").assertIsDisplayed()
        assertTrue(runCatching { onNodeWithText("Second").assertIsDisplayed() }.isFailure)
        messenger.dismissCurrent()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        onNodeWithText("Second").assertIsDisplayed()
        assertEquals(listOf("First Dismissed"), results)
    }

    @Test
    fun cancellingTheCallerRemovesItsSnackbar() = runComposeUiTest {
        camo()
        val job = show("Leaving soon", duration = SnackbarDuration.Indefinite)
        onNodeWithText("Leaving soon").assertIsDisplayed()
        job.cancel()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(runCatching { onNodeWithText("Leaving soon").assertExists() }.isFailure)
    }

    @Test
    fun withDismissFollowsTheThemeAndTheDismissButtonDismisses() = runComposeUiTest {
        var result: SnackbarResult? = null
        camo(themeT.copy(components = CamoComponentThemes(snackbar = SnackbarTheme(showDismiss = true))))
        show("Copied", duration = SnackbarDuration.Indefinite) { result = it }
        onNodeWithContentDescription("Dismiss").performClick()
        waitForIdle()
        assertEquals(SnackbarResult.Dismissed, result)
        show("No close", duration = SnackbarDuration.Indefinite, withDismiss = false)
        mainClock.advanceTimeBy(1_000)
        assertTrue(runCatching { onNodeWithContentDescription("Dismiss").assertExists() }.isFailure)
    }

    @Test
    fun anErrorIsAnnouncedAssertivelyAndOthersPolitely() = runComposeUiTest {
        camo()
        show("Couldn't send", tone = Tone.Error, duration = SnackbarDuration.Indefinite)
        onNodeWithText("Couldn't send", useUnmergedTree = true).assertIsDisplayed()
        val error = onAllNodesWithLiveRegion(LiveRegionMode.Assertive)
        assertEquals(1, error)
    }

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
        show("Archived", action = "Undo") { result = it }
        mainClock.advanceTimeBy(60_000)
        waitForIdle()
        assertNull(result)
        onNodeWithText("Undo").assertIsDisplayed()
    }

    @Test
    fun insetsRegisterWhileTheirScreenIsShown() = runComposeUiTest {
        val state = mutableStateOf(true)
        setContent {
            CamouflageProvider(skinA, themeT) {
                val messengerState = LocalCamoMessenger.current!!
                if (state.value) CamoMessengerInsets(bottom = 80.dp) { BasicText("Screen") }
                BasicText("${messengerState.insets.size}", Modifier.testTag("count"))
            }
        }
        onNodeWithTag("count").assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(text("1"))))
        state.value = false
        waitForIdle()
        onNodeWithTag("count").assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(text("0"))))
    }

    // Banners

    @Test
    fun aBannerDismissesAndAnnouncesWarningsAndErrors() = runComposeUiTest {
        var dismissed = 0
        camo {
            Column {
                CamoBanner("Couldn't sync", Modifier.testTag("error"), tone = Tone.Error, onDismiss = { dismissed++ })
                CamoBanner("Trial ends in 3 days", Modifier.testTag("info"))
            }
        }
        onNodeWithContentDescription("Dismiss").performClick()
        assertEquals(1, dismissed)
        onNodeWithTag("error")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        onNodeWithTag("info").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun aBannerReadsItsTextsAsOneNode() = runComposeUiTest {
        camo { CamoBanner("Your trial ends in 3 days", Modifier.testTag("b"), title = "Trial") }
        onNodeWithTag("b").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Text,
                listOf(text("Trial"), text("Your trial ends in 3 days")),
            ),
        )
    }

    private fun text(s: String) = AnnotatedString(s)

    private fun ComposeUiTest.onAllNodesWithLiveRegion(mode: LiveRegionMode): Int =
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, mode)).fetchSemanticsNodes().size

    private object NeverTimeOut : AccessibilityManager {
        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean,
        ): Long = if (containsControls) Long.MAX_VALUE else originalTimeoutMillis
    }
}
