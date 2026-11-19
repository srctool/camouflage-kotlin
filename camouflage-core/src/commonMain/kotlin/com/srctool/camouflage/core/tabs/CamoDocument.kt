package com.srctool.camouflage.core.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * A document of a [CamoDocumentTabs]: [id] identifies it, [title] names its tab, [modified] draws the unsaved
 * dot, and a document that is not [closable] has no close button. [icon] is drawn before the title.
 */
@Immutable
public data class CamoDocument(
    val id: Any,
    val title: String,
    val icon: (@Composable () -> Unit)? = null,
    val modified: Boolean = false,
    val closable: Boolean = true,
)
