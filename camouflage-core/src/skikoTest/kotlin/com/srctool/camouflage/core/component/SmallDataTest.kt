package com.srctool.camouflage.core.component

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.breadcrumb.CamoBreadcrumb
import com.srctool.camouflage.core.breadcrumb.CamoBreadcrumbItem
import com.srctool.camouflage.core.breadcrumb.planBreadcrumb
import com.srctool.camouflage.core.image.CamoImage
import com.srctool.camouflage.core.image.ImagePhase
import com.srctool.camouflage.core.image.ImageSource
import com.srctool.camouflage.core.lastBreadcrumb
import com.srctool.camouflage.core.lastImage
import com.srctool.camouflage.core.lastPagination
import com.srctool.camouflage.core.lastRating
import com.srctool.camouflage.core.pagination.CamoPagination
import com.srctool.camouflage.core.pagination.PageItem
import com.srctool.camouflage.core.pagination.PaginationVariant
import com.srctool.camouflage.core.pagination.pageItems
import com.srctool.camouflage.core.pageButtons
import com.srctool.camouflage.core.progresssteps.CamoProgressSteps
import com.srctool.camouflage.core.progresssteps.CamoStep
import com.srctool.camouflage.core.progresssteps.StepStatus
import com.srctool.camouflage.core.progresssteps.StepsOrientation
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.rating.CamoRating
import com.srctool.camouflage.core.rating.RatingStep
import com.srctool.camouflage.core.rating.formatRating
import com.srctool.camouflage.core.rating.snap
import com.srctool.camouflage.core.rating.valueAt
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.steps
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// The M10 image, rating, pagination, breadcrumb and progress steps tests from their Kotlin notes.
@OptIn(ExperimentalTestApi::class)
class SmallDataTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) = setContent {
        CamouflageProvider(skinA, themeT, content)
    }

    // --- image ---

    @Test
    fun anImageReportsItsPhase() = runComposeUiTest {
        camo { CamoImage(ImageSource.Loading, "Photo") }
        assertEquals(ImagePhase.Loading, lastImage?.phase)
        camo { CamoImage(ImageSource.Loaded(ColorPainter(Color.Red)), "Photo") }
        assertEquals(ImagePhase.Loaded, lastImage?.phase)
    }

    @Test
    fun aFailedImageKeepsItsDescriptionAndSaysItFailed() = runComposeUiTest {
        camo { CamoImage(ImageSource.Failed(), "Photo") }
        assertEquals(ImagePhase.Failed, lastImage?.phase)
        onNodeWithContentDescription("Photo")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Could not load"))
    }

    @Test
    fun aDecorativeImageIsHiddenFromScreenReaders() = runComposeUiTest {
        camo { CamoImage(ImageSource.Loaded(ColorPainter(Color.Red)), null) }
        onAllNodesWithContentDescription("Photo").assertCountEquals(0)
    }

    // --- rating ---

    @Test
    fun theRatingMapsAPointerPositionToAValue() {
        assertEquals(1f, valueAt(1f, 100, 5, 1f, rtl = false))
        assertEquals(3f, valueAt(50f, 100, 5, 1f, rtl = false))
        assertEquals(5f, valueAt(100f, 100, 5, 1f, rtl = false))
        assertEquals(2.5f, valueAt(45f, 100, 5, 0.5f, rtl = false))
        assertEquals(5f, valueAt(1f, 100, 5, 1f, rtl = true))
        assertEquals(3f, snap(2.2f, 1f, 5))
        assertEquals("3.5", formatRating(3.5f))
        assertEquals("4", formatRating(4f))
    }

    @Test
    fun aTapOnARatingSetsTheValue() = runComposeUiTest {
        var value = 2f
        camo { CamoRating(value, "Quality", onValueChange = { value = it }) }
        onNodeWithContentDescription("Quality").performTouchInput { click(Offset(width * 0.95f, centerY)) }
        assertEquals(5f, value)
    }

    @Test
    fun aReadOnlyRatingAnnouncesItsValueAndCannotBeChanged() = runComposeUiTest {
        camo { CamoRating(3.5f, "Quality") }
        onNodeWithContentDescription("Quality")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3.5 of 5 stars"))
        assertEquals(false, lastRating?.interactive)
        onNodeWithContentDescription("Quality").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
    }

    @Test
    fun arrowsAndTheAccessibilityActionChangeTheRatingByAStep() = runComposeUiTest {
        var value = 3f
        camo { CamoRating(value, "Quality", onValueChange = { value = it }, step = RatingStep.Half) }
        onNodeWithContentDescription("Quality").requestFocus()
        onNodeWithContentDescription("Quality").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(3.5f, value)
        onNodeWithContentDescription("Quality").performKeyInput { pressKey(Key.MoveEnd) }
        assertEquals(5f, value)
        onNodeWithContentDescription("Quality").performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        assertEquals(2f, value)
    }

    @Test
    fun aDisabledRatingIsNotInteractive() = runComposeUiTest {
        camo { CamoRating(3f, "Quality", onValueChange = {}, enabled = false) }
        assertEquals(false, lastRating?.interactive)
        assertEquals(false, lastRating?.enabled)
    }

    // --- pagination ---

    private fun pages(total: Int, page: Int, siblings: Int = 1) =
        pageItems(total, page, siblings).map { if (it is PageItem.Page) (it.index + 1).toString() else "…" }

    @Test
    fun theNumberedFormKeepsAFixedLengthAndTheEnds() {
        assertEquals(listOf("1", "2", "3"), pages(3, 0))
        assertEquals(listOf("1", "2", "3", "4", "5", "…", "10"), pages(10, 0))
        assertEquals(listOf("1", "…", "4", "5", "6", "…", "10"), pages(10, 4))
        assertEquals(listOf("1", "…", "6", "7", "8", "9", "10"), pages(10, 9))
        for (page in 0 until 30) {
            val items = pageItems(30, page, 1)
            assertEquals(7, items.size)
            assertTrue(PageItem.Page(0) in items && PageItem.Page(29) in items && PageItem.Page(page) in items)
        }
    }

    @Test
    fun aPageButtonChangesThePageAndTheArrowsStepAndStop() = runComposeUiTest {
        var page by mutableStateOf(0)
        camo { CamoPagination(5, page, { page = it }) }
        onNodeWithContentDescription("Previous page").assertIsNotEnabled()
        onNodeWithContentDescription("Page 3").performClick()
        assertEquals(2, page)
        onNodeWithContentDescription("Next page").performClick()
        assertEquals(3, page)
    }

    @Test
    fun theCurrentPageIsSelectedAndAnnounced() = runComposeUiTest {
        camo { CamoPagination(5, 1, {}) }
        onNodeWithContentDescription("Page 2").assertIsSelected()
        onNodeWithContentDescription("Page 2")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "current page"))
        assertEquals(true, pageButtons[1]?.current)
    }

    @Test
    fun theCompactAndDotsFormsDrawTheirParts() = runComposeUiTest {
        camo { CamoPagination(9, 2, {}, variant = PaginationVariant.Compact) }
        onNodeWithText("Page 3 of 9").assertExists()
        camo { CamoPagination(4, 0, {}, variant = PaginationVariant.Dots) }
        onNodeWithContentDescription("Page 4").assertExists()
    }

    @Test
    fun oneOrFewerPagesDrawNothing() = runComposeUiTest {
        lastPagination = null
        camo { CamoPagination(1, 0, {}) }
        assertNull(lastPagination)
    }

    // --- breadcrumb ---

    @Test
    fun thePlanKeepsEverythingThatFitsAndCollapsesTheMiddleOtherwise() {
        val widths = listOf(40, 60, 60, 60, 80)
        val all = planBreadcrumb(widths, 10, 30, 1000, null)
        assertEquals(5, all.head)
        assertEquals(0, all.tail)
        val narrow = planBreadcrumb(widths, 10, 30, 200, null)
        assertEquals(1, narrow.head)
        assertTrue(narrow.tail in 1..2)
        val capped = planBreadcrumb(widths, 10, 30, 1000, 3)
        assertEquals(1, capped.head)
        assertEquals(2, capped.tail)
        assertEquals(2, planBreadcrumb(listOf(40, 60), 10, 30, 10, null).head)
    }

    private val trail = listOf("Home", "Projects", "Camouflage", "Design", "Notes")

    @Test
    fun aBreadcrumbShowsEveryItemWhenItFits() = runComposeUiTest {
        camo { CamoBreadcrumb(trail.map { CamoBreadcrumbItem(it, it, onClick = {}) }) }
        trail.forEach { onNodeWithText(it).assertExists() }
        assertEquals(0, lastBreadcrumb?.collapsed)
    }

    @Test
    fun theCurrentItemIsNotALinkAndLinksRun() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo { CamoBreadcrumb(trail.map { name -> CamoBreadcrumbItem(name, name, onClick = { log += name }) }) }
        onNodeWithText("Projects").performClick()
        onNodeWithText("Notes").performClick()
        assertEquals(listOf("Projects"), log)
        onNodeWithText("Notes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "current page"))
    }

    @Test
    fun aNarrowBreadcrumbCollapsesTheMiddleIntoAMenu() = runComposeUiTest {
        val log = mutableListOf<String>()
        camo {
            CamoBreadcrumb(trail.map { name -> CamoBreadcrumbItem(name, name, onClick = { log += name }) },
                Modifier.width(150.dp))
        }
        onNodeWithText("Home").assertExists()
        onNodeWithText("Notes").assertExists()
        onNodeWithText("Projects").assertDoesNotExist()
        onNodeWithContentDescription("More").performClick()
        onNodeWithText("Projects").performClick()
        waitForIdle()
        assertEquals(listOf("Projects"), log)
    }

    // --- progress steps ---

    private val wizard = listOf("Account", "Shipping", "Payment", "Review")

    @Test
    fun stepsDeriveTheirStatusFromTheCurrentStep() = runComposeUiTest {
        camo { CamoProgressSteps(wizard.map { CamoStep(it, it) }, current = 1) }
        assertEquals(
            listOf(StepStatus.Complete, StepStatus.Current, StepStatus.Upcoming, StepStatus.Upcoming),
            (0..3).map { steps.getValue(it).status },
        )
        assertEquals(true, steps.getValue(3).last)
    }

    @Test
    fun aStepCanBeFlaggedWithAnError() = runComposeUiTest {
        camo { CamoProgressSteps(wizard.mapIndexed { i, n -> CamoStep(n, n,
            status = if (i == 2) StepStatus.Error else null) }, 1) }
        assertEquals(StepStatus.Error, steps.getValue(2).status)
    }

    @Test
    fun aStepAnnouncesItsPlaceAndClickableOnesRun() = runComposeUiTest {
        var jumped = -1
        camo {
            CamoProgressSteps(wizard.mapIndexed { i, n -> CamoStep(n, n,
                onClick = if (i < 2) ({ jumped = i }) else null) }, 1)
        }
        onNodeWithText("Shipping")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Step 2 of 4, current"))
        onNodeWithText("Account").performClick()
        assertEquals(0, jumped)
        assertEquals(true, steps.getValue(0).clickable)
        assertEquals(false, steps.getValue(3).clickable)
    }

    @Test
    fun stepsCanRunVertically() = runComposeUiTest {
        camo { CamoProgressSteps(wizard.map { CamoStep(it, it, description = "$it details") }, 0,
            orientation = StepsOrientation.Vertical) }
        assertEquals(StepsOrientation.Vertical, steps.getValue(0).orientation)
        onNodeWithText("Payment details").assertExists()
    }

    @Test
    fun arrowsMoveBetweenClickableSteps() = runComposeUiTest {
        camo { CamoProgressSteps(wizard.mapIndexed { i, n -> CamoStep(n, n, onClick = if (i < 2) ({}) else null) }, 1) }
        onNodeWithText("Shipping").requestFocus()
        onNodeWithText("Shipping").performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        onNodeWithText("Account").assertIsFocused()
    }
}
