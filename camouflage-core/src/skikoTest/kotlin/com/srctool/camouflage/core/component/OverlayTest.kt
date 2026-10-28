package com.srctool.camouflage.core.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.bottomsheet.CamoSheetFrame
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.dialog.CamoDialog
import com.srctool.camouflage.core.dialog.CamoDialogFrame
import com.srctool.camouflage.core.overlay.OnDismissRequest
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skinA
import com.srctool.camouflage.core.themeT
import kotlin.test.Test
import kotlin.test.assertEquals

// The M6 overlay frames' tests from their Kotlin notes: the dialog frame, dismissal and the veto.
@OptIn(ExperimentalTestApi::class)
class OverlayTest {
    private fun ComposeUiTest.camo(content: @Composable () -> Unit) =
        setContent { CamouflageProvider(skinA, themeT, content) }

    private val dismissAction = SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)

    @Test
    fun aDialogIsNamedByItsTitleAndActionsCloseIt() = runComposeUiTest {
        var open by mutableStateOf(true)
        var result: Boolean? = null
        camo {
            if (open) {
                CamoDialogFrame({ open = false }) {
                    CamoDialog(
                        "This can't be undone.",
                        confirm = { CamoButton("Delete", { result = true; open = false }) },
                        title = "Delete item?",
                        dismiss = { CamoButton("Cancel", { open = false }) },
                    )
                }
            }
        }
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Delete item?")).assertExists()
        onNodeWithText("Delete").performClick()
        waitForIdle()
        assertEquals(true, result)
        onNodeWithText("Delete item?").assertDoesNotExist()
    }

    @Test
    fun escAndTheDismissActionCloseADismissibleDialogOnly() = runComposeUiTest {
        var dismissed = 0
        var dismissible by mutableStateOf(true)
        camo {
            CamoDialogFrame({ dismissed++ }, dismissible = dismissible) {
                CamoDialog("Saved.", confirm = { CamoButton("OK", {}) })
            }
        }
        waitForIdle()
        onNodeWithText("OK").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        assertEquals(1, dismissed)
        onNode(dismissAction).performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertEquals(2, dismissed)
        dismissible = false
        waitForIdle()
        onNodeWithText("OK").performKeyInput { pressKey(Key.Escape) }
        waitForIdle()
        onNode(dismissAction).assertDoesNotExist()
        assertEquals(2, dismissed)
    }

    @Test
    fun onDismissRequestCanKeepADialogOrSheetOpen() = runComposeUiTest {
        var dismissed = 0
        var saving by mutableStateOf(true)
        camo {
            CamoDialogFrame({ dismissed++ }) {
                OnDismissRequest { !saving }
                CamoDialog("Saving…", confirm = { CamoButton("OK", {}) })
            }
            CamoSheetFrame({ dismissed += 10 }) {
                OnDismissRequest { !saving }
                CamoBottomSheet(title = "Filters") { BasicText("Sheet") }
            }
        }
        waitForIdle()
        onNodeWithText("OK").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText("Sheet").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Filters"))
            .performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertEquals(0, dismissed)
        saving = false
        waitForIdle()
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Saving…"))
            .performSemanticsAction(SemanticsActions.Dismiss)
        waitForIdle()
        assertEquals(1, dismissed)
    }
}
