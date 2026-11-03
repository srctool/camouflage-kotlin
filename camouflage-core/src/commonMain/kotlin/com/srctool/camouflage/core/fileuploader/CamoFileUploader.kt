package com.srctool.camouflage.core.fileuploader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.iconbutton.CamoIconButton
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState
import kotlin.math.roundToInt

/** Every color a file uploader uses: the zone and its border, the border while dragging over, errors, text. */
@Immutable
public data class UploaderColors(
    val zone: Color,
    val border: Color,
    val dragBorder: Color,
    val error: Color,
    val text: Color,
    val mutedText: Color,
)

/** Overrides for file uploaders. `null` keeps the skin's default. */
@Immutable
public data class UploaderTheme(
    val zoneShape: Shape? = null,
    val dashed: Boolean? = null,
    val zoneHeight: Dp? = null,
    val icon: IconSource? = null,
    val removeIcon: IconSource? = null,
    val retryIcon: IconSource? = null,
    val titleStyle: TextStyle? = null,
    val hintStyle: TextStyle? = null,
    val colors: UploaderColors? = null,
)

/** The resolved file uploaders style: every field set. */
@Immutable
public data class UploaderStyle(
    val zoneShape: Shape,
    val dashed: Boolean,
    val zoneHeight: Dp,
    val icon: IconSource,
    val removeIcon: IconSource,
    val retryIcon: IconSource,
    val titleStyle: TextStyle,
    val hintStyle: TextStyle,
    val colors: UploaderColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: UploaderTheme?): UploaderStyle = if (overrides == null) {
        this
    } else {
        UploaderStyle(
            zoneShape = overrides.zoneShape ?: zoneShape,
            dashed = overrides.dashed ?: dashed,
            zoneHeight = overrides.zoneHeight ?: zoneHeight,
            icon = overrides.icon ?: icon,
            removeIcon = overrides.removeIcon ?: removeIcon,
            retryIcon = overrides.retryIcon ?: retryIcon,
            titleStyle = overrides.titleStyle ?: titleStyle,
            hintStyle = overrides.hintStyle ?: hintStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/** Where a file's upload is. */
@Immutable
public sealed interface UploadStatus {
    /** Waiting or in progress. */
    public data object Uploading : UploadStatus

    /** Finished. */
    public data object Done : UploadStatus

    /** Failed with [message] (the app validates types and sizes). */
    public data class Failed(val message: String) : UploadStatus
}

/** A file row: [progress] is `0..1` while uploading, or `null` while it waits. The app runs the upload. */
@Immutable
public data class UploadFile(
    val id: Any,
    val name: String,
    val sizeBytes: Long? = null,
    val progress: Float? = null,
    val status: UploadStatus = UploadStatus.Uploading,
)

/** A file dropped on the zone: its name and size; the app reads it however its platform allows. */
@Immutable
public data class DroppedFile(val name: String, val sizeBytes: Long?, val handle: Any? = null)

/**
 * What a platform drop target needs to report: [setDragOver] while files hover over the zone and [onDropped] with
 * what was dropped. Core touches no filesystem or windowing API (ADR-27), so the app builds the `Modifier` for its
 * platform (Compose Multiplatform's drag and drop on desktop and web) from these callbacks.
 */
@Stable
public class DropTargetCallbacks(public val setDragOver: (Boolean) -> Unit,
    public val onDropped: (List<DroppedFile>) -> Unit)

/** What the zone draws: [dragOver] while files hover over it. */
@Immutable
public data class UploaderZoneState(
    val dragOver: Boolean,
    val enabled: Boolean,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** The zone's content. */
@Stable
public class UploaderZoneSlots(public val title: String, public val hint: String?)

/** What a file row draws. */
@Immutable
public data class UploadRowState(val status: UploadStatus, val progress: Float?)

/** A file row's content: its [name], a [detail] line ("2.4 MB, 45%") and the remove and retry buttons core builds. */
@Stable
public class UploadRowSlots(
    public val name: String,
    public val detail: String,
    public val retry: (@Composable () -> Unit)?,
    public val remove: @Composable () -> Unit,
)

/** Draws file uploaders in parts: the drop zone and each file row. */
@Stable
public interface FileUploaderRenderer {
    /** The complete file uploader style for [theme]. */
    public fun defaults(theme: CamoTheme): UploaderStyle

    /** The zone. Apply `modifier` (the behavior) to the outermost node. */
    @Composable
    public fun Zone(state: UploaderZoneState, slots: UploaderZoneSlots, theme: CamoTheme, style: UploaderStyle,
        modifier: Modifier)

    /** One file row: the name and detail, the progress (a `CamoProgressBar`) while uploading, and the buttons. */
    @Composable
    public fun Row(state: UploadRowState, slots: UploadRowSlots, theme: CamoTheme, style: UploaderStyle,
        modifier: Modifier)
}

/**
 * A drop zone and button for choosing files, with a list of each file's progress. Core opens no picker and reads
 * no file (ADR-27): a click, Enter or Space on the zone calls [onPick], where the app opens its picker; [dropTarget]
 * lets the app wrap the zone with its platform's drop API. The app runs the uploads and reports them in [files].
 * The zone announces as one button with its [hint]; a row announces its name and state, and a failure is announced
 * once. [label] names the zone.
 */
@Composable
public fun CamoFileUploader(
    files: List<UploadFile>,
    onPick: () -> Unit,
    onRemove: (UploadFile) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    dropTarget: ((DropTargetCallbacks) -> Modifier)? = null,
    onFilesDropped: ((List<DroppedFile>) -> Unit)? = null,
    onRetry: ((UploadFile) -> Unit)? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    val skin = requireSkin("CamoFileUploader")
    val theme = requireTheme("CamoFileUploader")
    val style = remember(skin, theme) { skin.fileUploader.defaults(theme).merge(theme.components.fileUploader) }
    var dragOver by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val drop = dropTarget?.invoke(DropTargetCallbacks({ dragOver = it },
        { dropped -> dragOver = false; onFilesDropped?.invoke(dropped) }))
    Column(modifier, Arrangement.spacedBy(8.dp)) {
        val zone = Modifier
            .semantics { contentDescription = label }
            .clickable(source, null, enabled, role = Role.Button, onClick = onPick)
            .then(drop ?: Modifier)
        skin.fileUploader.Zone(
            UploaderZoneState(dragOver, enabled, interaction, source),
            UploaderZoneSlots(label, hint),
            theme,
            style,
            zone,
        )
        files.forEach { file ->
            androidx.compose.runtime.key(file.id) {
                FileRow(skin.fileUploader, theme, style, file, onRemove, onRetry)
            }
        }
    }
}

@Composable
private fun FileRow(
    renderer: FileUploaderRenderer,
    theme: CamoTheme,
    style: UploaderStyle,
    file: UploadFile,
    onRemove: (UploadFile) -> Unit,
    onRetry: ((UploadFile) -> Unit)?,
) {
    val failed = file.status as? UploadStatus.Failed
    val state = when {
        failed != null -> failed.message
        file.status == UploadStatus.Done -> "Done"
        file.progress != null -> "${(file.progress * PERCENT).roundToInt()}%"
        else -> "Waiting"
    }
    val detail = listOfNotNull(file.sizeBytes?.let(::formatSize), state).joinToString(", ")
    val behavior = Modifier.semantics(mergeDescendants = true) {
        stateDescription = detail
        if (failed != null) liveRegion = LiveRegionMode.Assertive
    }
    renderer.Row(
        UploadRowState(file.status, file.progress),
        UploadRowSlots(
            file.name,
            detail,
            if (failed != null && onRetry != null) {
                { CamoIconButton({ CamoIcon(style.retryIcon, null) }, "Retry ${file.name}", { onRetry(file) },
                    appearance = Appearance.Ghost) }
            } else {
                null
            },
        ) { CamoIconButton({ CamoIcon(style.removeIcon, null) }, "Remove ${file.name}", { onRemove(file) },
            appearance = Appearance.Ghost) },
        theme,
        style,
        behavior,
    )
}

private const val PERCENT = 100
private const val KILO = 1024L

// A size as a person reads it: bytes, KB or MB with one decimal.
internal fun formatSize(bytes: Long): String = when {
    bytes < KILO -> "$bytes B"
    bytes < KILO * KILO -> "${tenths(bytes.toDouble() / KILO)} KB"
    else -> "${tenths(bytes.toDouble() / (KILO * KILO))} MB"
}

private const val TENTH = 10.0

private fun tenths(value: Double): String = ((value * TENTH).roundToInt() / TENTH).toString()
