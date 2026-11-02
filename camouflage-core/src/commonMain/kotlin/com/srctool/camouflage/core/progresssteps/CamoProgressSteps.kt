package com.srctool.camouflage.core.progresssteps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color progress steps use. */
@Immutable
public data class StepsColors(
    val complete: Color,
    val onComplete: Color,
    val current: Color,
    val onCurrent: Color,
    val upcoming: Color,
    val error: Color,
    val connector: Color,
    val text: Color,
    val mutedText: Color,
)

/** Overrides for progress steps. `null` keeps the skin's default. */
@Immutable
public data class StepsTheme(
    val markerSize: Dp? = null,
    val connectorWidth: Dp? = null,
    val showNumbers: Boolean? = null,
    val checkIcon: IconSource? = null,
    val errorIcon: IconSource? = null,
    val labelStyle: TextStyle? = null,
    val descriptionStyle: TextStyle? = null,
    val colors: StepsColors? = null,
)

/** The resolved progress steps style: every field set. */
@Immutable
public data class StepsStyle(
    val markerSize: Dp,
    val connectorWidth: Dp,
    val showNumbers: Boolean,
    val checkIcon: IconSource,
    val errorIcon: IconSource,
    val labelStyle: TextStyle,
    val descriptionStyle: TextStyle,
    val colors: StepsColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: StepsTheme?): StepsStyle = if (overrides == null) {
        this
    } else {
        StepsStyle(
            markerSize = overrides.markerSize ?: markerSize,
            connectorWidth = overrides.connectorWidth ?: connectorWidth,
            showNumbers = overrides.showNumbers ?: showNumbers,
            checkIcon = overrides.checkIcon ?: checkIcon,
            errorIcon = overrides.errorIcon ?: errorIcon,
            labelStyle = overrides.labelStyle ?: labelStyle,
            descriptionStyle = overrides.descriptionStyle ?: descriptionStyle,
            colors = overrides.colors ?: colors,
        )
    }
}

/** Where a step is in the process. */
public enum class StepStatus { Complete, Current, Upcoming, Error }

/** Which way the steps run. */
public enum class StepsOrientation { Horizontal, Vertical }

/**
 * One step. A null [status] is derived from the current step (earlier ones are complete); set `Error` to flag
 * a step. A null [onClick] makes it not clickable.
 */
@Immutable
public data class CamoStep(
    val key: Any,
    val label: String,
    val description: String? = null,
    val status: StepStatus? = null,
    val onClick: (() -> Unit)? = null,
)

/**
 * What a step renderer draws: the marker, the label and description, and (unless [last]) the connector to the
 * next step, drawn in the complete color when this step is complete. In a horizontal layout the step gets an
 * equal share of the width (the last one only its own); in a vertical one its own height.
 */
@Immutable
public data class StepItemState(
    val index: Int,
    val count: Int,
    val status: StepStatus,
    val clickable: Boolean,
    val orientation: StepsOrientation,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
) {
    /** Whether this is the last step. */
    val last: Boolean get() = index == count - 1
}

/** A step's content. */
@Stable
public class StepSlots(public val label: String, public val description: String?)

/** Draws progress steps. Apply `modifier` (the behavior) to the outermost node. */
@Stable
public interface ProgressStepsRenderer {
    /** The complete progress steps style for [theme]. */
    public fun defaults(theme: CamoTheme): StepsStyle

    /** Draws one step. */
    @Composable
    public fun Step(state: StepItemState, slots: StepSlots, theme: CamoTheme, style: StepsStyle, modifier: Modifier)
}

/**
 * Shows where you are in a multi-step process; the app swaps the content and sets [current] (zero-based).
 * A step announces "Step 2 of 4, current"; steps with an [CamoStep.onClick] are buttons and the arrow keys move
 * between them. [label] names the list.
 */
@Composable
public fun CamoProgressSteps(
    steps: List<CamoStep>,
    current: Int,
    modifier: Modifier = Modifier,
    orientation: StepsOrientation = StepsOrientation.Horizontal,
    label: String = "Progress",
) {
    val skin = requireSkin("CamoProgressSteps")
    val theme = requireTheme("CamoProgressSteps")
    val style = remember(skin, theme) { skin.progressSteps.defaults(theme).merge(theme.components.progressSteps) }
    val focus = rememberRovingFocus()
    val behavior = modifier.semantics {
        contentDescription = label
        isTraversalGroup = true
    }
    @Composable
    fun StepAt(index: Int, step: CamoStep, extra: Modifier) {
        val status = step.status ?: when {
            index < current -> StepStatus.Complete
            index == current -> StepStatus.Current
            else -> StepStatus.Upcoming
        }
        val source = remember { MutableInteractionSource() }
        val interaction = source.collectInteractionState()
        val clickable = step.onClick != null
        var item = extra
            .rovingFocusItem(focus, index, clickable, selected = index == current, onSelect = {})
            .semantics(mergeDescendants = true) {
                stateDescription = "Step ${index + 1} of ${steps.size}, ${statusWord(status)}"
                selected = index == current
            }
        if (step.onClick != null) item = item.clickable(source, null, true, role = Role.Button, onClick = step.onClick)
        skin.progressSteps.Step(
            StepItemState(index, steps.size, status, clickable, orientation, interaction, source),
            StepSlots(step.label, step.description),
            theme,
            style,
            item,
        )
    }
    when (orientation) {
        StepsOrientation.Horizontal -> Row(behavior.fillMaxWidth().height(IntrinsicSize.Min)) {
            steps.forEachIndexed { i, step -> StepAt(i, step,
                if (i < steps.lastIndex) Modifier.weight(1f) else Modifier) }
        }
        StepsOrientation.Vertical -> Column(behavior) {
            steps.forEachIndexed { i, step -> StepAt(i, step, Modifier) }
        }
    }
}

private fun statusWord(status: StepStatus) = when (status) {
    StepStatus.Complete -> "complete"
    StepStatus.Current -> "current"
    StepStatus.Upcoming -> "upcoming"
    StepStatus.Error -> "error"
}
