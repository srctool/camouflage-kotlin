package com.srctool.camouflage.core.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.minimumTouchTarget

// Announced on a loading button ("Save, button, busy").
internal const val BUSY = "Busy"

/**
 * A button with a text label. [appearance] is its visual weight (`null` uses the theme's default), [tone] its
 * color family (`Error` for destructive actions). While [loading], it shows a progress circle in place of the
 * text, keeps its width and focus, and ignores clicks. The parent sets the width (`Modifier.fillMaxWidth()`).
 */
@Composable
public fun CamoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: Appearance? = null,
    tone: Tone = Tone.Default,
    enabled: Boolean = true,
    loading: Boolean = false,
    prefixIcon: (@Composable () -> Unit)? = null,
    suffixIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    requireLabel(text, "CamoButton")
    val skin = requireSkin("CamoButton")
    val theme = requireTheme("CamoButton")
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val style = remember(skin, theme) { skin.button.defaults(theme).merge(theme.components.button) }
    val behavior = modifier
        .semantics(mergeDescendants = true) { if (loading && enabled) stateDescription = BUSY }
        .clickable(source, indication = null, enabled = enabled, role = Role.Button) { if (!loading) onClick() }
        .minimumTouchTarget()
    skin.button.Render(
        state = ButtonState(
            text = text,
            enabled = enabled,
            loading = loading && enabled,
            appearance = appearance ?: style.defaultAppearance,
            tone = tone,
            interaction = interaction,
            interactionSource = source,
        ),
        slots = ButtonSlots(prefixIcon, suffixIcon),
        theme = theme,
        style = style,
        modifier = behavior,
    )
}

private var blankLabelLogged = false

// A blank label fails in debug builds; release builds render it and log once (server text, a missing translation).
internal fun requireLabel(text: String, component: String) {
    if (text.isNotBlank()) return
    check(!DebugChecks.enabled) { "$component needs a non-blank text. An icon-only action is a CamoIconButton." }
    if (!blankLabelLogged) {
        blankLabelLogged = true
        println("Camouflage: $component was given a blank text.")
    }
}

/** Debug-only checks (blank labels). On by default; a release build turns them off once, at startup. */
public object DebugChecks {
    /** Whether debug-only checks throw. */
    public var enabled: Boolean = true
}
