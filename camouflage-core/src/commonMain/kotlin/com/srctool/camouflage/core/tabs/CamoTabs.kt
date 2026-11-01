package com.srctool.camouflage.core.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.button.DebugChecks
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.util.RovingFocus
import com.srctool.camouflage.core.util.collectInteractionState
import com.srctool.camouflage.core.util.rememberRovingFocus
import com.srctool.camouflage.core.util.rovingFocusItem

/** Every color a tab bar uses. */
@Immutable
public data class TabsColors(
    val container: Color,
    val selected: Color,
    val unselected: Color,
    val indicator: Color,
    val divider: Color,
)

/** How wide the selected-tab indicator is: as wide as the tab's content, or as the whole tab. */
public enum class IndicatorWidth { Content, Tab }

/** Overrides for tab bars. `null` keeps the skin's default. */
@Immutable
public data class TabsTheme(
    val defaultAppearance: Appearance? = null,
    val height: Dp? = null,
    val heightWithIcon: Dp? = null,
    val indicatorHeight: Dp? = null,
    val indicatorWidth: IndicatorWidth? = null,
    val labelStyle: TextStyle? = null,
    val iconSize: Dp? = null,
    val iconGap: Dp? = null,
    val tabPadding: Dp? = null,
    val minTabWidth: Dp? = null,
    val showDivider: Boolean? = null,
    val solidColors: TabsColors? = null,
    val ghostColors: TabsColors? = null,
)

/** The resolved tab bar style: every field set. */
@Immutable
public data class TabsStyle(
    val defaultAppearance: Appearance,
    val height: Dp,
    val heightWithIcon: Dp,
    val indicatorHeight: Dp,
    val indicatorWidth: IndicatorWidth,
    val labelStyle: TextStyle,
    val iconSize: Dp,
    val iconGap: Dp,
    val tabPadding: Dp,
    val minTabWidth: Dp,
    val showDivider: Boolean,
    val solidColors: TabsColors,
    val ghostColors: TabsColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: TabsTheme?): TabsStyle = if (overrides == null) {
        this
    } else {
        TabsStyle(
            defaultAppearance = overrides.defaultAppearance ?: defaultAppearance,
            height = overrides.height ?: height,
            heightWithIcon = overrides.heightWithIcon ?: heightWithIcon,
            indicatorHeight = overrides.indicatorHeight ?: indicatorHeight,
            indicatorWidth = overrides.indicatorWidth ?: indicatorWidth,
            labelStyle = overrides.labelStyle ?: labelStyle,
            iconSize = overrides.iconSize ?: iconSize,
            iconGap = overrides.iconGap ?: iconGap,
            tabPadding = overrides.tabPadding ?: tabPadding,
            minTabWidth = overrides.minTabWidth ?: minTabWidth,
            showDivider = overrides.showDivider ?: showDivider,
            solidColors = overrides.solidColors ?: solidColors,
            ghostColors = overrides.ghostColors ?: ghostColors,
        )
    }

    /** The colors for [appearance]: Ghost is the quiet bar, everything else the prominent one. */
    public fun colorsFor(appearance: Appearance): TabsColors =
        if (appearance == Appearance.Ghost) ghostColors else solidColors
}

/** The appearance a tab bar draws for [appearance]: Solid (prominent) or Ghost (quiet); the others are Solid. */
public fun tabsAppearance(appearance: Appearance): Appearance =
    if (appearance == Appearance.Ghost) Appearance.Ghost else Appearance.Solid

/**
 * One tab of a [CamoTabs]. It needs a [label] or an [icon]; an icon-only tab needs a [contentDescription]. The
 * [badge] is the app's own (a `CamoBadge`).
 */
@Immutable
public data class CamoTab<T>(
    val value: T,
    val label: String? = null,
    val icon: (@Composable () -> Unit)? = null,
    val badge: (@Composable () -> Unit)? = null,
    val contentDescription: String? = null,
    val enabled: Boolean = true,
)

/** Where the selected tab's indicator is, in pixels from the start of the tab row. */
@Immutable
public data class TabIndicator(val start: Float, val width: Float)

/**
 * What the container renderer draws. [indicator] is read while drawing, so the animation costs no recomposition;
 * it is `null` when no tab is selected. The container's width is the row's content width when [scrollable].
 */
@Immutable
public data class TabsState(
    val count: Int,
    val selectedIndex: Int,
    val scrollable: Boolean,
    val appearance: Appearance,
    val indicator: () -> TabIndicator?,
)

/** What a tab renderer draws: one tab at [position]. */
@Immutable
public data class TabState(
    val selected: Boolean,
    val enabled: Boolean,
    val position: ItemPosition,
    val scrollable: Boolean,
    val appearance: Appearance,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/**
 * A tab's content: its [label], [icon] and [badge], or a building block's custom [content] instead of all three.
 * The renderer applies [contentModifier] to the node that wraps what it draws, so the indicator can be as wide
 * as the content.
 */
@Stable
public class TabSlots(
    public val label: String?,
    public val icon: (@Composable () -> Unit)?,
    public val badge: (@Composable () -> Unit)?,
    public val content: (@Composable () -> Unit)?,
    public val contentModifier: Modifier,
)

/**
 * Draws tab bars in two parts: the [Container] (the surface, the divider and the indicator around the row of
 * tabs) and each [Tab]. Apply `modifier` (the behavior) to the outermost node of each.
 */
@Stable
public interface TabsRenderer {
    /** The complete tab bar style for [theme]. */
    public fun defaults(theme: CamoTheme): TabsStyle

    /**
     * Draws the bar around the tab row. Call [row] once with a modifier that draws the indicator behind the
     * tabs (inside the scrolled content, so it moves with them) and place it; draw the divider on the bar.
     */
    @Composable
    public fun Container(
        state: TabsState,
        theme: CamoTheme,
        style: TabsStyle,
        modifier: Modifier,
        row: @Composable (Modifier) -> Unit,
    )

    /** Draws one tab with [slots]. */
    @Composable
    public fun Tab(state: TabState, slots: TabSlots, theme: CamoTheme, style: TabsStyle, modifier: Modifier)
}

// A tab's place in the row, in pixels from the row's start: the whole tab and its content.
internal data class TabBounds(val start: Float, val width: Float, val contentStart: Float, val contentWidth: Float)

internal class TabsRegistry {
    val bounds = mutableStateMapOf<Int, TabBounds>()
    var viewport by mutableStateOf(0)
}

/** The scope of a [CamoTabsContainer]: a row, where [CamoTabItem]s register their place for the indicator. */
@Stable
public class CamoTabsScope internal constructor(
    rowScope: RowScope,
    internal val tabs: TabsRegistry,
    internal val focus: RovingFocus,
    internal val scrollable: Boolean,
    internal val appearance: Appearance,
    internal val enabled: Boolean,
) : RowScope by rowScope

/**
 * A bar of tabs ("Overview / Reviews / Specs"): [selected] in, [onSelect] out, an animated indicator under the
 * selected tab, one Tab stop with the arrow keys moving and selecting, announced "Reviews, tab, 2 of 3". Fixed
 * tabs share the width; with [scrollable] they keep their natural width, the row scrolls and the selection stays
 * in view. The content below is the app's layout; `rememberCamoTabsPagerSync` keeps a pager in step. Pass the
 * sync's position as [indicatorPosition] to make the indicator follow a swipe.
 */
@Composable
public fun <T> CamoTabs(
    selected: T,
    onSelect: (T) -> Unit,
    tabs: List<CamoTab<T>>,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    appearance: Appearance? = null,
    indicatorPosition: (() -> Float)? = null,
) {
    CamoTabsContainer(
        selectedIndex = tabs.indexOfFirst { it.value == selected },
        modifier = modifier,
        scrollable = scrollable,
        appearance = appearance,
        indicatorPosition = indicatorPosition,
    ) {
        tabs.forEachIndexed { index, tab ->
            CamoTabItem(
                tab = tab,
                selected = tab.value == selected,
                onClick = { onSelect(tab.value) },
                position = ItemPosition(index, tabs.size),
            )
        }
    }
}

/**
 * The building block (ADR-30) for custom tab bars: the skin's bar, the animated indicator, the scrolling and
 * the tab-list semantics around the [CamoTabItem]s in [content]. [selectedIndex] is the selected tab's index
 * (`-1` for none, which draws no indicator).
 */
@Composable
@Suppress("LongMethod") // the bar's layout, indicator animation and scrolling are one flow
public fun CamoTabsContainer(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    appearance: Appearance? = null,
    indicatorPosition: (() -> Float)? = null,
    enabled: Boolean = true,
    content: @Composable CamoTabsScope.() -> Unit,
) {
    val skin = requireSkin("CamoTabsContainer")
    val theme = requireTheme("CamoTabsContainer")
    val style = remember(skin, theme) { skin.tabs.defaults(theme).merge(theme.components.tabs) }
    val resolved = tabsAppearance(appearance ?: style.defaultAppearance)
    val registry = remember { TabsRegistry() }
    val focus = rememberRovingFocus()
    val scroll = rememberScrollState()
    val count = registry.bounds.size
    val target = registry.bounds[selectedIndex]?.let { indicatorOf(it, style.indicatorWidth) }
    var placed by remember { mutableStateOf(false) }
    val motion = tween<Float>(theme.motion.durationMedium, easing = theme.motion.easingEmphasized)
    val spec = if (placed) motion else snap()
    val start = animateFloatAsState(target?.start ?: 0f, spec)
    val width = animateFloatAsState(target?.width ?: 0f, spec)
    LaunchedEffect(target != null) { placed = target != null }
    if (scrollable) {
        LaunchedEffect(selectedIndex, registry.bounds[selectedIndex], registry.viewport) {
            val bounds = registry.bounds[selectedIndex] ?: return@LaunchedEffect
            val centered = bounds.start + bounds.width / 2 - registry.viewport / 2
            scroll.animateScrollTo(centered.toInt().coerceIn(0, scroll.maxValue))
        }
    }
    val indicator: () -> TabIndicator? = {
        val position = indicatorPosition?.invoke()
        when {
            position != null -> interpolate(registry, position, style.indicatorWidth)
            target == null -> null
            else -> TabIndicator(start.value, width.value)
        }
    }
    val behavior = modifier
        .onSizeChanged { registry.viewport = it.width }
        .selectableGroup()
        .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = count) }
    val state = TabsState(count, selectedIndex, scrollable, resolved, indicator)
    skin.tabs.Container(state, theme, style, behavior) { draw ->
        val rowModifier = if (scrollable) Modifier.horizontalScroll(scroll) else Modifier
        Row(rowModifier.then(draw).height(IntrinsicSize.Max)) {
            CamoTabsScope(this, registry, focus, scrollable, resolved, enabled).content()
        }
    }
}

private fun indicatorOf(bounds: TabBounds, mode: IndicatorWidth): TabIndicator = when (mode) {
    IndicatorWidth.Content -> TabIndicator(bounds.contentStart, bounds.contentWidth)
    IndicatorWidth.Tab -> TabIndicator(bounds.start, bounds.width)
}

// The indicator between two tabs for a fractional [position] (page 1.4 is 40% of the way from tab 1 to tab 2).
private fun interpolate(registry: TabsRegistry, position: Float, mode: IndicatorWidth): TabIndicator? {
    val lower = position.toInt().coerceAtLeast(0)
    val from = registry.bounds[lower]?.let { indicatorOf(it, mode) }
    val to = registry.bounds[lower + 1]?.let { indicatorOf(it, mode) }
    val fraction = (position - lower).coerceIn(0f, 1f)
    return if (from == null || to == null) {
        from
    } else {
        TabIndicator(from.start + (to.start - from.start) * fraction, from.width + (to.width - from.width) * fraction)
    }
}

/**
 * One skin-styled tab inside a [CamoTabsContainer], with tab semantics ("Reviews, tab, 2 of 3"). [content]
 * replaces the tab's label, icon and badge (a counter, two lines); the tab's label still names it.
 */
@Composable
public fun <T> CamoTabsScope.CamoTabItem(
    tab: CamoTab<T>,
    selected: Boolean,
    onClick: () -> Unit,
    position: ItemPosition,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    val skin = requireSkin("CamoTabItem")
    val theme = requireTheme("CamoTabItem")
    if (DebugChecks.enabled) {
        check(tab.label != null || tab.icon != null || content != null) { "A CamoTab needs a label or an icon." }
        check(tab.label != null || tab.contentDescription != null || content != null) {
            "An icon-only CamoTab needs a contentDescription."
        }
    }
    val style = remember(skin, theme) { skin.tabs.defaults(theme).merge(theme.components.tabs) }
    val source = remember { MutableInteractionSource() }
    val interaction = source.collectInteractionState()
    val enabled = this.enabled && tab.enabled
    val registry = this.tabs
    var tabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var contentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val report = {
        val tabBox = tabCoordinates
        val contentBox = contentCoordinates
        if (tabBox != null && tabBox.isAttached) {
            val start = tabBox.positionInParent().x
            val inset = contentBox?.takeIf { it.isAttached }?.let { tabBox.localPositionOf(it, Offset.Zero).x }
            registry.bounds[position.index] = TabBounds(
                start,
                tabBox.size.width.toFloat(),
                start + (inset ?: 0f),
                contentBox?.size?.width?.toFloat() ?: tabBox.size.width.toFloat(),
            )
        }
    }
    DisposableEffect(position.index) { onDispose { registry.bounds.remove(position.index) } }
    val contentModifier = Modifier.onGloballyPositioned {
        contentCoordinates = it
        report()
    }
    val behavior = modifier
        .then(if (scrollable) Modifier else Modifier.weight(1f))
        .onGloballyPositioned {
            tabCoordinates = it
            report()
        }
        .rovingFocusItem(focus, position.index, enabled, selected, onClick)
        .semantics {
            tab.contentDescription?.let { contentDescription = it }
            collectionItemInfo = CollectionItemInfo(0, 1, position.index, 1)
        }
        .selectable(selected, source, indication = null, enabled = enabled, role = Role.Tab, onClick = onClick)
    skin.tabs.Tab(
        TabState(selected, enabled, position, scrollable, appearance, interaction, source),
        TabSlots(tab.label, tab.icon, tab.badge, content, contentModifier),
        theme,
        style,
        behavior,
    )
}
