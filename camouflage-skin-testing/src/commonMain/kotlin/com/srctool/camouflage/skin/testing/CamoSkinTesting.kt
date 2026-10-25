package com.srctool.camouflage.skin.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.CamouflageProvider
import com.srctool.camouflage.core.skin.CamoSkin
import com.srctool.camouflage.core.theme.CamoComponentThemes
import com.srctool.camouflage.core.theme.CamoTheme
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The test tag of the box [setCamoContent] wraps the content in. */
public const val CAMO_CONTENT_TAG: String = "camo-content"

/**
 * Sets [content] under a provider with [skin] and [theme], at [fontScale] and in [rtl] if asked. The content is
 * wrapped in a box tagged [CAMO_CONTENT_TAG] that sizes to it.
 */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.setCamoContent(
    skin: CamoSkin,
    theme: CamoTheme,
    fontScale: Float = 1f,
    rtl: Boolean = false,
    content: @Composable () -> Unit,
) {
    setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale),
            LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            CamouflageProvider(skin = skin, theme = theme) {
                Box(Modifier.testTag(CAMO_CONTENT_TAG)) { content() }
            }
        }
    }
}

/**
 * Checks that a clickable component's renderer applies the component's behavior modifier to its outermost
 * node: a click at the rendered box's corner and one at its center both fire, and the node has a role.
 * [renderClickable] draws one clickable component (a `CamoButton`, an icon button, a card) calling `onClick`.
 */
@OptIn(ExperimentalTestApi::class)
public fun ComposeUiTest.assertRendererAppliesBehavior(
    skin: CamoSkin,
    theme: CamoTheme,
    renderClickable: @Composable (onClick: () -> Unit) -> Unit,
) {
    var clicks = 0
    setCamoContent(skin, theme) { renderClickable { clicks++ } }
    val content = onNodeWithTag(CAMO_CONTENT_TAG, useUnmergedTree = true)
    content.performTouchInput { click(Offset(1f, 1f)) }
    content.performTouchInput { click(center) }
    waitForIdle()
    assertEquals(2, clicks, "a click at the corner and one at the center of the rendered box must both fire")
    val clickable = onAllNodes(hasClickAction()).fetchSemanticsNodes()
    assertEquals(1, clickable.size, "the component must expose one clickable node")
    assertNotNull(clickable.single().config.getOrNull(SemanticsProperties.Role), "the clickable node needs a role")
}

/**
 * The "no literals" check: renders with [oddStyle], a style holding values no skin would pick, and asserts
 * [measure] on the content's node, so a renderer that ignores a component-theme field fails.
 */
@OptIn(ExperimentalTestApi::class)
public fun <S> ComposeUiTest.assertNoLiterals(
    skin: CamoSkin,
    theme: CamoTheme,
    oddStyle: S,
    render: @Composable (style: S) -> Unit,
    measure: (SemanticsNode) -> Boolean,
) {
    setCamoContent(skin, theme) { render(oddStyle) }
    waitForIdle()
    val node = onNodeWithTag(CAMO_CONTENT_TAG, useUnmergedTree = true).fetchSemanticsNode()
    assertTrue(measure(node), "the renderer ignored a field of $oddStyle")
}

/** [content] under the current theme with its component themes replaced by [components]. */
@Composable
public fun ProvideComponentThemes(components: CamoComponentThemes, content: @Composable () -> Unit) {
    CamouflageProvider(theme = Camouflage.theme.copy(components = components), content = content)
}
