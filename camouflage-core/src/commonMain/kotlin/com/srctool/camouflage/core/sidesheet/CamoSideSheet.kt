package com.srctool.camouflage.core.sidesheet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.overlay.SideEdge
import com.srctool.camouflage.core.overlay.SideFrame
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme

/** Every color a side sheet uses. [modalContainer] is the container while it slides over the content. */
@Immutable
public data class SideSheetColors(val container: Color, val modalContainer: Color, val title: Color, val divider: Color)

/** Overrides for side sheets. `null` keeps the skin's default. */
@Immutable
public data class SideSheetTheme(
    val width: Dp? = null,
    val modalShape: Shape? = null,
    val padding: Dp? = null,
    val titleStyle: TextStyle? = null,
    val showDivider: Boolean? = null,
    val showCloseButton: Boolean? = null,
    val closeIcon: IconSource? = null,
    val scrimAlpha: Float? = null,
    val colors: SideSheetColors? = null,
)

/** The resolved side sheets style: every field set. */
@Immutable
public data class SideSheetStyle(
    val width: Dp,
    val modalShape: Shape,
    val padding: Dp,
    val titleStyle: TextStyle,
    val showDivider: Boolean,
    val showCloseButton: Boolean,
    val closeIcon: IconSource,
    val scrimAlpha: Float,
    val colors: SideSheetColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: SideSheetTheme?): SideSheetStyle = if (overrides == null) {
        this
    } else {
        SideSheetStyle(
            width = overrides.width ?: width,
            modalShape = overrides.modalShape ?: modalShape,
            padding = overrides.padding ?: padding,
            titleStyle = overrides.titleStyle ?: titleStyle,
            showDivider = overrides.showDivider ?: showDivider,
            showCloseButton = overrides.showCloseButton ?: showCloseButton,
            closeIcon = overrides.closeIcon ?: closeIcon,
            scrimAlpha = overrides.scrimAlpha ?: scrimAlpha,
            colors = overrides.colors ?: colors,
        )
    }
}

/**
 * What the side sheet renderer draws: [modal] while it slides over the content, standard (docked) in the layout.
 * Draw the [title] and, when [onClose] is set and the style asks for it, a close button that calls it.
 */
@Immutable
public data class SideSheetState(val modal: Boolean, val title: String?, val onClose: (() -> Unit)?)

/** A side sheet content: the scrolling [content] and the [actions] pinned at the bottom. */
@Stable
public class SideSheetSlots(
    public val content: @Composable ColumnScope.() -> Unit,
    public val actions: (@Composable RowScope.() -> Unit)?,
)

/** Draws side sheets. Apply `modifier` (the semantics) to the outermost node. */
@Stable
public interface SideSheetRenderer {
    /** The complete side sheet style for [theme]. */
    public fun defaults(theme: CamoTheme): SideSheetStyle

    /** Draws the sheet: the container, the title row, the scrolling content and the actions. */
    @Composable
    public fun Render(
        state: SideSheetState,
        slots: SideSheetSlots,
        theme: CamoTheme,
        style: SideSheetStyle,
        modifier: Modifier,
    )
}

private val LocalSideSheetModal = compositionLocalOf { false }

/**
 * A panel on the end edge for secondary content next to the main screen: properties, filters, details. Placed in
 * a layout it is the **standard** (docked) mode, and the main content stays usable; inside a [CamoModalSideSheet]
 * it is the **modal** one. [title] names it for screen readers; [onClose] adds a close button (and is what
 * dismissing the modal one asks for). The [content] scrolls; the [actions] stay at the bottom.
 */
@Composable
public fun CamoSideSheet(
    modifier: Modifier = Modifier,
    title: String? = null,
    onClose: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = requireSkin("CamoSideSheet")
    val theme = requireTheme("CamoSideSheet")
    val style = remember(skin, theme) { skin.sideSheet.defaults(theme).merge(theme.components.sideSheet) }
    skin.sideSheet.Render(
        SideSheetState(LocalSideSheetModal.current, title, onClose),
        SideSheetSlots(content, actions),
        theme,
        style,
        modifier.semantics { if (title != null) paneTitle = title },
    )
}

/**
 * The modal mode: a [CamoSideSheet] sliding in from the end edge over the content, with a scrim. Its visibility
 * is state ([open]), like the modal drawer's. A scrim tap, back, Esc and a drag toward the end edge all call
 * [onDismissRequest]; focus is trapped inside while it is open.
 */
@Composable
public fun CamoModalSideSheet(
    open: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val skin = requireSkin("CamoModalSideSheet")
    val theme = requireTheme("CamoModalSideSheet")
    val style = remember(skin, theme) { skin.sideSheet.defaults(theme).merge(theme.components.sideSheet) }
    SideFrame(SideEdge.End, open, onDismissRequest, style.width, style.scrimAlpha, modifier) {
        CompositionLocalProvider(LocalSideSheetModal provides true) { content() }
    }
}
