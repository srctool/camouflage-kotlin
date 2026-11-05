package com.srctool.camouflage.skin.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.skin.CamoSkin

/** The smallest touch target any screen reader user can hit, in dp (iOS asks 44, Android 48). */
private const val MIN_TARGET_DP = 44

/**
 * The accessibility checks every interactive element must pass in a gallery (Testing §3.4): it has a name a
 * screen reader can say (text, a content description or editable text), a role or a toggle/selection state, and
 * a touch target of at least 44 dp each way. Returns the problems, one line per node, empty when none.
 */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.accessibilityProblems(
    skin: CamoSkin,
    theme: CamoTheme,
    fontScale: Float = 1f,
    rtl: Boolean = false,
    content: @Composable () -> Unit,
): List<String> {
    setCamoContent(skin, theme, fontScale, rtl, content)
    waitForIdle()
    val minimum = MIN_TARGET_DP * density.density
    return onAllNodes(hasClickAction()).fetchSemanticsNodes().mapNotNull { node ->
        // An item scrolled out of its row has no size on screen, and nothing to hit.
        if (node.boundsInRoot.isEmpty) return@mapNotNull null
        val problems = buildList {
            if (!node.hasName()) add("no name (${node.config.joinToString { it.key.name }})")
            if (!node.hasRole()) add("no role or state (${node.config.joinToString { it.key.name }})")
            val bounds = node.touchBoundsInRoot
            // A scrolled row or column may show only part of an item: its size along the scroll axis is not tested.
            val scrollsAcross = node.scrollsHorizontally()
            val scrollsDown = node.scrollsVertically()
            val tooNarrow = !scrollsAcross && bounds.width < minimum - 1
            val tooShort = !scrollsDown && bounds.height < minimum - 1
            if (tooNarrow || tooShort) {
                add("touch target ${bounds.width / density.density} x ${bounds.height / density.density} dp")
            }
        }
        if (problems.isEmpty()) null else "node ${node.id} at ${node.boundsInRoot}: ${problems.joinToString()}"
    }
}

private fun SemanticsNode.scrollsHorizontally(): Boolean =
    generateSequence(parent) { it.parent }
        .any { it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null }

private fun SemanticsNode.scrollsVertically(): Boolean =
    generateSequence(parent) { it.parent }
        .any { it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null }

private fun SemanticsNode.hasName(): Boolean {
    val config = config
    return config.getOrNull(SemanticsProperties.Text)?.isNotEmpty() == true ||
        config.getOrNull(SemanticsProperties.ContentDescription)?.isNotEmpty() == true ||
        config.getOrNull(SemanticsProperties.EditableText) != null ||
        config.getOrNull(SemanticsActions.OnClick)?.label?.isNotBlank() == true
}

private fun SemanticsNode.hasRole(): Boolean {
    val config = config
    return config.getOrNull(SemanticsProperties.Role) != null ||
        config.getOrNull(SemanticsProperties.EditableText) != null ||
        config.getOrNull(SemanticsProperties.ToggleableState) != null ||
        config.getOrNull(SemanticsProperties.Selected) != null
}

/** The dp value 44, for tests that print it. */
internal val MinimumTarget = MIN_TARGET_DP.dp
