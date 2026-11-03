package com.srctool.camouflage.core.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.core.carouselButtons
import com.srctool.camouflage.core.carousel.CamoCarousel
import com.srctool.camouflage.core.fileuploader.CamoFileUploader
import com.srctool.camouflage.core.fileuploader.DropTargetCallbacks
import com.srctool.camouflage.core.fileuploader.DroppedFile
import com.srctool.camouflage.core.fileuploader.UploadFile
import com.srctool.camouflage.core.fileuploader.UploadStatus
import com.srctool.camouflage.core.fileuploader.formatSize
import com.srctool.camouflage.core.lastCarousel
import com.srctool.camouflage.core.lastZone
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import com.srctool.camouflage.core.uploadRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The M10 carousel and file uploader tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class CollectionsTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    private val slides = listOf("One", "Two", "Three")

    @Composable
    private fun Carousel(items: List<String> = slides) =
        CamoCarousel(items, { it }, "Gallery", showButtons = true) { BasicText("slide $it") }

    // --- carousel ---

    @Test
    fun theButtonsStepThroughTheItemsAndStopAtTheEnds() = runComposeUiTest {
        camo { Carousel() }
        assertEquals(0, lastCarousel?.page)
        assertEquals(3, lastCarousel?.pageCount)
        onNodeWithContentDescription("Previous item").assertIsNotEnabled()
        onNodeWithContentDescription("Next item").performClick()
        waitForIdle()
        assertEquals(1, lastCarousel?.page)
        onNodeWithContentDescription("Next item").performClick()
        waitForIdle()
        onNodeWithContentDescription("Next item").assertIsNotEnabled()
        assertEquals(false, carouselButtons["Next"]?.enabled)
    }

    @Test
    fun arrowsHomeAndEndMoveAndJump() = runComposeUiTest {
        camo { Carousel() }
        onNodeWithContentDescription("Gallery").requestFocus()
        onNodeWithContentDescription("Gallery").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        assertEquals(1, lastCarousel?.page)
        onNodeWithContentDescription("Gallery").performKeyInput { pressKey(Key.MoveEnd) }
        waitForIdle()
        assertEquals(2, lastCarousel?.page)
        onNodeWithContentDescription("Gallery").performKeyInput { pressKey(Key.MoveHome) }
        waitForIdle()
        assertEquals(0, lastCarousel?.page)
    }

    @Test
    fun anItemAnnouncesItsPlaceAndThePageDotsAreThere() = runComposeUiTest {
        camo { Carousel() }
        onNodeWithContentDescription("item 1 of 3").assertExists()
        onNodeWithContentDescription("Page 2").assertExists()
    }

    @Test
    fun aSingleItemHasNoButtonsOrDots() = runComposeUiTest {
        camo { Carousel(listOf("Only")) }
        onNodeWithContentDescription("Next item").assertDoesNotExist()
        onNodeWithContentDescription("Page 1").assertDoesNotExist()
    }

    // --- file uploader ---

    private val files = listOf(
        UploadFile("a", "a.png", 1_048_576, progress = 0.45f),
        UploadFile("b", "b.pdf", 2_048, status = UploadStatus.Failed("Too large")),
        UploadFile("c", "c.txt", 12, status = UploadStatus.Done),
    )

    @Test
    fun sizesReadAsAPersonWouldSayThem() {
        assertEquals("12 B", formatSize(12))
        assertEquals("2.0 KB", formatSize(2_048))
        assertEquals("1.0 MB", formatSize(1_048_576))
    }

    @Test
    fun theZoneIsOneButtonThatOpensThePicker() = runComposeUiTest {
        var picked = 0
        camo { CamoFileUploader(emptyList(), { picked++ }, {}, "Upload files", hint = "PNG up to 5 MB") }
        onNodeWithContentDescription("Upload files").performClick()
        assertEquals(1, picked)
    }

    @Test
    fun aDisabledZoneDoesNothing() = runComposeUiTest {
        var picked = 0
        camo { CamoFileUploader(emptyList(), { picked++ }, {}, "Upload files", enabled = false) }
        onNodeWithContentDescription("Upload files").assertIsNotEnabled()
        assertEquals(false, lastZone?.enabled)
        assertEquals(0, picked)
    }

    @Test
    fun rowsShowTheirProgressAndStateAndRemoveAndRetryAreButtons() = runComposeUiTest {
        val removed = mutableListOf<String>()
        val retried = mutableListOf<String>()
        camo {
            CamoFileUploader(files, {}, { removed += it.name }, "Upload files", onRetry = { retried += it.name })
        }
        assertEquals(UploadStatus.Uploading, uploadRows["a.png"]?.status)
        assertEquals(0.45f, uploadRows["a.png"]?.progress)
        onNodeWithContentDescription("Remove a.png").performClick()
        onNodeWithContentDescription("Retry b.pdf").performClick()
        assertEquals(listOf("a.png"), removed)
        assertEquals(listOf("b.pdf"), retried)
        onNodeWithContentDescription("Retry a.png").assertDoesNotExist()
    }

    @Test
    fun aRowAnnouncesItsStateAndAFailureIsAnAssertiveAlert() = runComposeUiTest {
        camo { CamoFileUploader(files, {}, {}, "Upload files") }
        onNodeWithContentDescription("Remove a.png").assertExists()
        val states = listOf("1.0 MB, 45%", "2.0 KB, Too large", "12 B, Done")
        states.forEach { expected ->
            onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected),
            ).fetchSemanticsNodes().also { assertTrue(it.isNotEmpty(), expected) }
        }
        onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive),
        ).fetchSemanticsNodes().also { assertEquals(1, it.size) }
    }

    @Test
    fun theAppsDropTargetDrivesTheZone() = runComposeUiTest {
        var callbacks: DropTargetCallbacks? = null
        var dropped: List<DroppedFile> = emptyList()
        camo {
            CamoFileUploader(
                emptyList(), {}, {}, "Upload files",
                dropTarget = { callbacks = it; Modifier },
                onFilesDropped = { dropped = it },
            )
        }
        callbacks!!.setDragOver(true)
        waitForIdle()
        assertEquals(true, lastZone?.dragOver)
        callbacks!!.onDropped(listOf(DroppedFile("x.txt", 10)))
        waitForIdle()
        assertEquals(false, lastZone?.dragOver)
        assertEquals(listOf("x.txt"), dropped.map { it.name })
    }
}
