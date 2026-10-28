package com.srctool.camouflage.core.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.theme.CamoTheme

/** When a top bar draws the line under it. */
public enum class TopBarDivider { Never, WhenScrolled, Always }

/** Every color a top bar uses. */
@Immutable
public data class TopBarColors(
    val container: Color,
    val scrolledContainer: Color,
    val title: Color,
    val navigation: Color,
    val actions: Color,
    val divider: Color,
)

/** Overrides for top bars. `null` keeps the skin's default. */
@Immutable
public data class TopBarTheme(
    val titleAlign: TitleAlign? = null,
    val divider: TopBarDivider? = null,
    val height: Dp? = null,
    val titleStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    /** The row's padding: start and end, and the start beside a title with no navigation. */
    val padding: PaddingValues? = null,
    val actionGap: Dp? = null,
    val dividerWidth: Dp? = null,
    val colors: TopBarColors? = null,
)

/** The resolved top bar style: every field set. */
@Immutable
public data class TopBarStyle(
    val titleAlign: TitleAlign,
    val divider: TopBarDivider,
    val height: Dp,
    val titleStyle: TextStyle,
    val iconSize: Dp,
    val padding: PaddingValues,
    val actionGap: Dp,
    val dividerWidth: Dp,
    val colors: TopBarColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: TopBarTheme?): TopBarStyle = if (overrides == null) {
        this
    } else {
        TopBarStyle(
            titleAlign = overrides.titleAlign ?: titleAlign,
            divider = overrides.divider ?: divider,
            height = overrides.height ?: height,
            titleStyle = overrides.titleStyle ?: titleStyle,
            iconSize = overrides.iconSize ?: iconSize,
            padding = overrides.padding ?: padding,
            actionGap = overrides.actionGap ?: actionGap,
            dividerWidth = overrides.dividerWidth ?: dividerWidth,
            colors = overrides.colors ?: colors,
        )
    }

    /** Whether the bar shows its divider at [scrollOffset]. */
    public fun showsDivider(scrollOffset: Dp): Boolean = when (divider) {
        TopBarDivider.Never -> false
        TopBarDivider.WhenScrolled -> scrollOffset > 0.dp
        TopBarDivider.Always -> true
    }
}

/**
 * What a top bar renderer draws. [scrollOffset] is how far content has scrolled under the bar (`Dp.Infinity` when
 * the exact distance is unknown: fully scrolled). Draw the background behind the status bar and pad only the
 * content row by [windowInsets].
 */
@Immutable
public data class TopBarState(
    val title: String,
    val scrollOffset: Dp,
    val windowInsets: WindowInsets,
) {
    /** Whether any content is under the bar. */
    val scrolled: Boolean get() = scrollOffset > 0.dp
}

/**
 * The caller's parts. [titleContent] replaces the drawn title (a search field, a logo); core has already added
 * the title as an invisible heading in front of it. Draw the [TopBarState.title] otherwise, marked with
 * `Modifier.semantics { heading() }`. Set the ambient icon style around [navigation] and [actions].
 */
@Stable
public class TopBarSlots(
    public val navigation: (@Composable () -> Unit)?,
    public val actions: @Composable RowScope.() -> Unit,
    public val titleContent: (@Composable () -> Unit)?,
)

/** Draws top bars. Apply `modifier` (the bar's semantics) to the outermost node. */
@Stable
public interface TopBarRenderer {
    /** The complete top bar style for [theme]. */
    public fun defaults(theme: CamoTheme): TopBarStyle

    /** Draws the bar: navigation, the title placed by `style.titleAlign`, and the actions at the end. */
    @Composable
    public fun Render(state: TopBarState, slots: TopBarSlots, theme: CamoTheme, style: TopBarStyle, modifier: Modifier)

    /** Draws the bar's surface, scrolled tint, divider and insets around a custom header's [content]. */
    @Composable
    public fun Container(
        state: TopBarState,
        content: @Composable BoxScope.() -> Unit,
        theme: CamoTheme,
        style: TopBarStyle,
        modifier: Modifier,
    )
}

/** The insets a top bar pads its content by: the status bar and display cutout, at the top and sides. */
public val TopBarDefaultInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

/**
 * A screen's top bar: a [title], a [navigation] button, and [actions] (icon buttons; extras go in a "More" menu).
 * The skin decides where they sit. Pass [scrollOffset] from [rememberTopBarScrollOffset] so the bar separates
 * from content scrolled under it. [titleContent] replaces the drawn title; [title] stays the bar's heading for
 * screen readers. `visible = false` slides the bar up out of view (with [rememberHideOnScroll]).
 */
@Composable
public fun CamoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollOffset: Dp = 0.dp,
    titleContent: (@Composable () -> Unit)? = null,
    visible: Boolean = true,
    windowInsets: WindowInsets = TopBarDefaultInsets,
) {
    val skin = requireSkin("CamoTopBar")
    val theme = requireTheme("CamoTopBar")
    val style = remember(skin, theme) { skin.topBar.defaults(theme).merge(theme.components.topBar) }
    val custom: (@Composable () -> Unit)? = titleContent?.let { content ->
        {
            Box {
                // The bar's name, first in reading order, over the custom title.
                Spacer(Modifier.matchParentSize().semantics { heading(); contentDescription = title })
                content()
            }
        }
    }
    HideOnScroll(visible, theme) {
        skin.topBar.Render(
            TopBarState(title, scrollOffset, windowInsets),
            TopBarSlots(navigation, actions, custom),
            theme,
            style,
            modifier.semantics { isTraversalGroup = true },
        )
    }
}

/**
 * A custom header (a profile hero, a big search header) with the top bar's surface, scrolled tint, divider and
 * insets. Place your own title (marked with `Modifier.semantics { heading() }`) and buttons in [content].
 */
@Composable
public fun CamoTopBarContainer(
    modifier: Modifier = Modifier,
    scrollOffset: Dp = 0.dp,
    visible: Boolean = true,
    windowInsets: WindowInsets = TopBarDefaultInsets,
    content: @Composable BoxScope.() -> Unit,
) {
    val skin = requireSkin("CamoTopBarContainer")
    val theme = requireTheme("CamoTopBarContainer")
    val style = remember(skin, theme) { skin.topBar.defaults(theme).merge(theme.components.topBar) }
    HideOnScroll(visible, theme) {
        skin.topBar.Container(
            TopBarState("", scrollOffset, windowInsets),
            content,
            theme,
            style,
            modifier.semantics { isTraversalGroup = true },
        )
    }
}

@Composable
private fun HideOnScroll(visible: Boolean, theme: CamoTheme, content: @Composable () -> Unit) {
    val spec = theme.motion.durationMedium
    AnimatedVisibility(
        visible,
        enter = slideInVertically(tween(spec)) { -it } + expandVertically(tween(spec)),
        exit = slideOutVertically(tween(spec)) { -it } + shrinkVertically(tween(spec)),
    ) { content() }
}

/** How far [state] has scrolled under a top bar: `Dp.Infinity` once the first item is gone (fully scrolled). */
@Composable
public fun rememberTopBarScrollOffset(state: LazyListState): Dp {
    val density = LocalDensity.current
    return remember(state, density) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) {
                Dp.Infinity
            } else {
                with(density) { state.firstVisibleItemScrollOffset.toDp() }
            }
        }
    }.value
}

/** How far [state] has scrolled under a top bar. */
@Composable
public fun rememberTopBarScrollOffset(state: ScrollState): Dp {
    val density = LocalDensity.current
    return remember(state, density) { derivedStateOf { with(density) { state.value.toDp() } } }.value
}

/** Whether a bar over [state] should show: hidden while scrolling down, shown when scrolling up or at the top. */
@Composable
public fun rememberHideOnScroll(state: LazyListState): Boolean {
    var visible by remember(state) { mutableStateOf(true) }
    LaunchedEffect(state) {
        var lastIndex = state.firstVisibleItemIndex
        var lastOffset = state.firstVisibleItemScrollOffset
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            val delta = if (index == lastIndex) offset - lastOffset else index - lastIndex
            visible = when {
                index == 0 && offset == 0 -> true
                delta > 0 -> false
                delta < 0 -> true
                else -> visible
            }
            lastIndex = index
            lastOffset = offset
        }
    }
    return visible
}
