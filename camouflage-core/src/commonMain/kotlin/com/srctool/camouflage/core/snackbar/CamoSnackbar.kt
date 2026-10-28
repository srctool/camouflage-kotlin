package com.srctool.camouflage.core.snackbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.family

/** How long a snackbar stays. `Auto` is `Short`, or `Long` when it has an action. */
public enum class SnackbarDuration { Short, Long, Indefinite, Auto }

/** How a snackbar left: its action was tapped, or it was dismissed (timeout, swipe, ✕, `dismissCurrent`). */
public enum class SnackbarResult { ActionPerformed, Dismissed }

/** Where snackbars appear, on every window size. */
public enum class SnackbarPosition { Bottom, Top }

/** Where a snackbar narrower than the window sits along its edge (start and end follow the reading direction). */
public enum class SnackbarAlignment { Start, Center, End }

/** A snackbar's colors for `Tone.Default`. */
@Immutable
public data class SnackbarColors(val container: Color, val text: Color, val action: Color)

/** The words snackbars say themselves: the ✕ button's name. */
@Immutable
public data class SnackbarStrings(val dismiss: String = "Dismiss")

/** Overrides for snackbars. `null` keeps the skin's default. */
@Immutable
public data class SnackbarTheme(
    val shape: Shape? = null,
    val maxWidth: Dp? = null,
    /** The distance from the window's edges (beyond the system insets and `CamoMessengerInsets`). */
    val margin: Dp? = null,
    val padding: PaddingValues? = null,
    val minHeight: Dp? = null,
    val textStyle: TextStyle? = null,
    val actionStyle: TextStyle? = null,
    val elevation: ElevationLevel? = null,
    /** Where snackbars appear (a brand choice). */
    val position: SnackbarPosition? = null,
    val alignment: SnackbarAlignment? = null,
    /** A ✕ on every snackbar whose call passes no `withDismiss`. */
    val showDismiss: Boolean? = null,
    /** A leading status icon for the status tones (`BannerTheme.toneIcons`, so banners and snackbars match). */
    val showToneIcon: Boolean? = null,
    val durationShortMillis: Int? = null,
    val durationLongMillis: Int? = null,
    val colors: SnackbarColors? = null,
    val dismissIcon: IconSource? = null,
    val strings: SnackbarStrings? = null,
)

/** The resolved snackbar style: every field set. [colors] is for `Tone.Default`. */
@Immutable
public data class SnackbarStyle(
    val shape: Shape,
    val maxWidth: Dp,
    val margin: Dp,
    val padding: PaddingValues,
    val minHeight: Dp,
    val textStyle: TextStyle,
    val actionStyle: TextStyle,
    val elevation: ElevationLevel,
    val position: SnackbarPosition,
    val alignment: SnackbarAlignment,
    val showDismiss: Boolean,
    val showToneIcon: Boolean,
    val durationShortMillis: Int,
    val durationLongMillis: Int,
    val colors: SnackbarColors,
    val dismissIcon: IconSource,
    val strings: SnackbarStrings = SnackbarStrings(),
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: SnackbarTheme?): SnackbarStyle = if (overrides == null) {
        this
    } else {
        SnackbarStyle(
            shape = overrides.shape ?: shape,
            maxWidth = overrides.maxWidth ?: maxWidth,
            margin = overrides.margin ?: margin,
            padding = overrides.padding ?: padding,
            minHeight = overrides.minHeight ?: minHeight,
            textStyle = overrides.textStyle ?: textStyle,
            actionStyle = overrides.actionStyle ?: actionStyle,
            elevation = overrides.elevation ?: elevation,
            position = overrides.position ?: position,
            alignment = overrides.alignment ?: alignment,
            showDismiss = overrides.showDismiss ?: showDismiss,
            showToneIcon = overrides.showToneIcon ?: showToneIcon,
            durationShortMillis = overrides.durationShortMillis ?: durationShortMillis,
            durationLongMillis = overrides.durationLongMillis ?: durationLongMillis,
            colors = overrides.colors ?: colors,
            dismissIcon = overrides.dismissIcon ?: dismissIcon,
            strings = overrides.strings ?: strings,
        )
    }

    /** The colors for [tone]: this style's for `Tone.Default`, the tone's strong family otherwise. */
    public fun colorsFor(tone: Tone, theme: CamoTheme): SnackbarColors = if (tone == Tone.Default) {
        colors
    } else {
        val f = theme.colors.family(tone)
        SnackbarColors(f.strong, f.onStrong, f.onStrong)
    }
}

/** What a snackbar renderer draws. [withDismiss] and [showToneIcon] are resolved; the slots hold those parts. */
@Immutable
public data class SnackbarState(
    val message: String,
    val action: String?,
    val tone: Tone,
    val withDismiss: Boolean,
    val showToneIcon: Boolean,
    val position: SnackbarPosition,
    val interaction: InteractionState,
)

/**
 * The parts core builds and the renderer places: the tone's [icon] (before the message), the [action] button and
 * the [dismiss] ✕ (after it). Each is `null` when the snackbar has none. The buttons are their own focusable
 * nodes. They draw in the content color the renderer provides with `ProvideContentStyle`: the action color around
 * [action], the text or icon color around the others.
 */
@Stable
public class SnackbarSlots(
    public val icon: (@Composable () -> Unit)?,
    public val action: (@Composable () -> Unit)?,
    public val dismiss: (@Composable () -> Unit)?,
)

/** Draws snackbars. Apply `modifier` (core's gestures and semantics) to the outermost node. */
@Stable
public interface SnackbarRenderer {
    /** The complete snackbar style for [theme]. */
    public fun defaults(theme: CamoTheme): SnackbarStyle

    /** Draws [state] with [slots], as wide as it's allowed (core caps it at `maxWidth`). */
    @Composable
    public fun Render(
        state: SnackbarState,
        slots: SnackbarSlots,
        theme: CamoTheme,
        style: SnackbarStyle,
        modifier: Modifier,
    )
}
