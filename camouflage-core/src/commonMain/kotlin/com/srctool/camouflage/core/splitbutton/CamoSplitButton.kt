package com.srctool.camouflage.core.splitbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.button.requireLabel
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.menu.CamoMenuAnchor
import com.srctool.camouflage.core.menu.CamoMenuController
import com.srctool.camouflage.core.menu.CamoMenuEntry
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

/** Overrides for split buttons. `null` keeps the skin's default. */
@Immutable
public data class SplitButtonTheme(
    val defaultAppearance: Appearance? = null,
    val height: Dp? = null,
    val shape: Shape? = null,
    val innerShape: Shape? = null,
    val arrowWidth: Dp? = null,
    val gap: Dp? = null,
    val iconSize: Dp? = null,
    val iconGap: Dp? = null,
    val padding: Dp? = null,
    val textStyle: TextStyle? = null,
    val arrowIcon: IconSource? = null,
)

/** The resolved split buttons style: every field set. */
@Immutable
public data class SplitButtonStyle(
    val defaultAppearance: Appearance,
    val height: Dp,
    val shape: Shape,
    val innerShape: Shape,
    val arrowWidth: Dp,
    val gap: Dp,
    val iconSize: Dp,
    val iconGap: Dp,
    val padding: Dp,
    val textStyle: TextStyle,
    val arrowIcon: IconSource,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: SplitButtonTheme?): SplitButtonStyle = if (overrides == null) {
        this
    } else {
        SplitButtonStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            height = overrides.height ?: height,
            shape = overrides.shape ?: shape,
            innerShape = overrides.innerShape ?: innerShape,
            arrowWidth = overrides.arrowWidth ?: arrowWidth,
            gap = overrides.gap ?: gap,
            iconSize = overrides.iconSize ?: iconSize,
            iconGap = overrides.iconGap ?: iconGap,
            padding = overrides.padding ?: padding,
            textStyle = overrides.textStyle ?: textStyle,
            arrowIcon = overrides.arrowIcon ?: arrowIcon,
        )
    }
}

/**
 * What a split button renderer draws. [hasAction] is `false` for a menu button: one part, the whole button opens
 * the menu, and the arrow is drawn inside it. Draw the chevron turned when [expanded]. The colors are the
 * button's: `skin.button`'s style, resolved with `colorsFor(appearance, tone, theme)`.
 */
@Immutable
public data class SplitButtonState(
    val text: String,
    val hasAction: Boolean,
    val expanded: Boolean,
    val appearance: Appearance,
    val tone: Tone,
    val enabled: Boolean,
    val mainInteraction: InteractionState,
    val arrowInteraction: InteractionState,
    val mainInteractionSource: InteractionSource,
    val arrowInteractionSource: InteractionSource,
)

/** The caller's icon before the text. */
@Stable
public class SplitButtonSlots(public val prefixIcon: (@Composable () -> Unit)?)

/** The behavior of the two parts: apply [main] to the label part and [arrow] to the arrow part. */
@Stable
public class SplitButtonBehavior(public val main: Modifier, public val arrow: Modifier)

/** Draws split buttons. Apply `modifier` to the outermost node and the part behaviors to the parts. */
@Stable
public interface SplitButtonRenderer {
    /** The complete split button style for [theme]. */
    public fun defaults(theme: CamoTheme): SplitButtonStyle

    /** Draws [state] with [slots]. For a menu button (`!hasAction`) apply `behavior.main` to the whole button. */
    @Composable
    public fun Render(
        state: SplitButtonState,
        slots: SplitButtonSlots,
        behavior: SplitButtonBehavior,
        theme: CamoTheme,
        style: SplitButtonStyle,
        modifier: Modifier,
    )
}

private const val EXPANDED = "Expanded"
private const val COLLAPSED = "Collapsed"

/**
 * A main action with a menu of related ones ("Save", with "Save as…" behind the arrow). Tab reaches the main part,
 * then the arrow; Enter or Space presses the focused part; Down or Alt+Down on either opens the [menu] and
 * focuses its first item; Esc closes it and returns focus. An [onClick] of `null` makes a menu button: one part
 * that opens the menu. [menuContentDescription] names the arrow.
 */
@Composable
public fun CamoSplitButton(
    text: String,
    onClick: (() -> Unit)?,
    menu: List<CamoMenuEntry>,
    modifier: Modifier = Modifier,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    enabled: Boolean = true,
    prefixIcon: (@Composable () -> Unit)? = null,
    menuContentDescription: String = "More options",
) {
    requireLabel(text, "CamoSplitButton")
    val skin = requireSkin("CamoSplitButton")
    val theme = requireTheme("CamoSplitButton")
    val style = remember(skin, theme) { skin.splitButton.defaults(theme).merge(theme.components.splitButton) }
    val controller = remember { CamoMenuController() }
    val mainSource = remember { MutableInteractionSource() }
    val arrowSource = remember { MutableInteractionSource() }
    val expanded = controller.isOpen
    val opens = Modifier.onKeyEvent {
        val open = enabled && it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown
        if (open) controller.open()
        open
    }
    val popup = if (expanded) EXPANDED else COLLAPSED
    val mainBehavior = Modifier
        .then(opens)
        .semantics(mergeDescendants = true) { if (onClick == null) stateDescription = popup }
        .clickable(mainSource, indication = null, enabled = enabled, role = Role.Button) {
            if (onClick == null) controller.open() else onClick()
        }
        .minimumTouchTarget()
    val arrowBehavior = Modifier
        .then(opens)
        .semantics(mergeDescendants = true) {
            contentDescription = menuContentDescription
            stateDescription = popup
        }
        .clickable(arrowSource, indication = null, enabled = enabled, role = Role.Button) { controller.open() }
        .minimumTouchTarget()
    CamoMenuAnchor(modifier, items = menu, controller = controller) {
        skin.splitButton.Render(
            SplitButtonState(
                text = text,
                hasAction = onClick != null,
                expanded = expanded,
                appearance = appearance ?: style.defaultAppearance,
                tone = tone,
                enabled = enabled,
                mainInteraction = mainSource.collectInteractionState(),
                arrowInteraction = arrowSource.collectInteractionState(),
                mainInteractionSource = mainSource,
                arrowInteractionSource = arrowSource,
            ),
            SplitButtonSlots(prefixIcon),
            SplitButtonBehavior(mainBehavior, arrowBehavior),
            theme,
            style,
            Modifier,
        )
    }
}
