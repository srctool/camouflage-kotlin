package com.srctool.camouflage.core.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.bottomsheet.CamoBottomSheet
import com.srctool.camouflage.core.bottomsheet.CamoSheetFrame
import com.srctool.camouflage.core.datepicker.PickerPresentation
import com.srctool.camouflage.core.menu.CamoMenuPanel

/**
 * A field's picker: anchored under the field in the skin's popup panel, or in a bottom sheet. `Auto` picks the
 * popup from [autoPopupMinWidth] of window width. Place it inside the field's layout, so the popup anchors to it.
 */
@Composable
internal fun PickerOverlay(
    presentation: PickerPresentation,
    autoPopupMinWidth: Dp,
    title: String?,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val popup = when (presentation) {
        PickerPresentation.Popup -> true
        PickerPresentation.Sheet -> false
        PickerPresentation.Auto -> width >= autoPopupMinWidth
    }
    if (popup) {
        CamoAnchoredPopup(onDismissRequest) { CamoMenuPanel(content = content) }
    } else {
        CamoSheetFrame(onDismissRequest) {
            CamoBottomSheet(title = title) {
                // Taller than the sheet's max height (a calendar on a small phone in landscape): it scrolls.
                val scroll = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                Box(scroll, contentAlignment = Alignment.TopCenter) { content() }
            }
        }
    }
}
