@file:Suppress("TooManyFunctions") // one renderer object and its paint helper per structure component

package com.srctool.camouflage.skin.minimal.renderer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.srctool.camouflage.core.accordion.AccordionColors
import com.srctool.camouflage.core.accordion.AccordionIndicator
import com.srctool.camouflage.core.accordion.AccordionRenderer
import com.srctool.camouflage.core.accordion.AccordionSlots
import com.srctool.camouflage.core.accordion.AccordionState
import com.srctool.camouflage.core.accordion.AccordionStrings
import com.srctool.camouflage.core.accordion.AccordionStyle
import com.srctool.camouflage.core.accordion.IndicatorPosition
import com.srctool.camouflage.core.button.CamoButton
import com.srctool.camouflage.core.card.CardColors
import com.srctool.camouflage.core.card.CardRenderer
import com.srctool.camouflage.core.card.CardSlots
import com.srctool.camouflage.core.card.CardState
import com.srctool.camouflage.core.card.CardStyle
import com.srctool.camouflage.core.icon.CamoIcon
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.icon.IconStyle
import com.srctool.camouflage.core.list.ListDividers
import com.srctool.camouflage.core.list.ListItemColors
import com.srctool.camouflage.core.list.ListItemRenderer
import com.srctool.camouflage.core.list.ListItemSlots
import com.srctool.camouflage.core.list.ListItemState
import com.srctool.camouflage.core.list.ListItemStyle
import com.srctool.camouflage.core.list.ListSectionSlots
import com.srctool.camouflage.core.list.ListSectionState
import com.srctool.camouflage.core.pagedlist.PagedListRenderer
import com.srctool.camouflage.core.pagedlist.PagedListState
import com.srctool.camouflage.core.pagedlist.PagedListStrings
import com.srctool.camouflage.core.pagedlist.PagedListStyle
import com.srctool.camouflage.core.pagedlist.PagedLoad
import com.srctool.camouflage.core.progress.CamoProgressCircle
import com.srctool.camouflage.core.progress.ProgressSize
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.skeleton.SkeletonAnimation
import com.srctool.camouflage.core.skeleton.SkeletonRenderer
import com.srctool.camouflage.core.skeleton.SkeletonState
import com.srctool.camouflage.core.skeleton.SkeletonStyle
import com.srctool.camouflage.core.skeleton.drawSkeleton
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.surface.surfaceOf
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.theme.Tone
import com.srctool.camouflage.core.theme.of
import com.srctool.camouflage.core.topbar.TopBarColors
import com.srctool.camouflage.core.topbar.TopBarDivider
import com.srctool.camouflage.core.topbar.TopBarRenderer
import com.srctool.camouflage.core.topbar.TopBarRow
import com.srctool.camouflage.core.topbar.TopBarSlots
import com.srctool.camouflage.core.topbar.TopBarState
import com.srctool.camouflage.core.topbar.TopBarStyle
import com.srctool.camouflage.core.util.ProvideContentStyle
import com.srctool.camouflage.core.util.disabled
import com.srctool.camouflage.core.util.rememberSkinIndication
import kotlin.math.min

// Minimal's structure components (base Minimal Skin §3.4): hairlines instead of fills, ink for emphasis.

private val Hairline = 1.dp
private val IconSize = 20.dp
private val ProgressSpot = 20.dp
private val HeaderGap = 12.dp
private const val DISABLED = 0.38f
private const val ARC_START = -90f
private const val FULL_TURN = 360f
private const val HALF_TURN = 180f

/** A semibold copy of a type role (Minimal's headings are 600). */
private fun TextStyle.semibold() = copy(fontWeight = FontWeight.SemiBold)

internal object MinimalCardRenderer : CardRenderer {
    override fun defaults(theme: CamoTheme): CardStyle {
        val c = theme.colors
        return CardStyle(
            defaultAppearance = Appearance.Raised,
            shape = theme.shapes.large,
            contentPadding = PaddingValues(),
            solidColors = CardColors(c.primary, c.onPrimary),
            softColors = CardColors(c.surfaceContainer, c.onSurface),
            raisedColors = CardColors(c.surface, c.onSurface, border = c.outlineVariant),
            outlineColors = CardColors(c.surface, c.onSurface, border = c.outline),
            raisedElevation = ElevationLevel.Level0,
            hoveredElevation = ElevationLevel.Level0,
            borderWidth = Hairline,
            toneBorderWidth = 4.dp,
            selectedBorderColor = c.primary,
            selectedBorderWidth = 2.dp,
            showSelectedCheck = false,
        )
    }

    @Composable
    override fun Render(state: CardState, slots: CardSlots, theme: CamoTheme, style: CardStyle, modifier: Modifier) =
        PaintCard(state, slots, theme, style, modifier)
}

/** A card: the surface treatment, its border (or the selected border), a tone strip on a toned Raised card. */
@Composable
internal fun PaintCard(state: CardState, slots: CardSlots, theme: CamoTheme, style: CardStyle, modifier: Modifier) {
    val colors = style.colorsFor(state.appearance, state.tone, theme)
    val toned = state.appearance == Appearance.Raised && state.tone != Tone.Default
    val border = cardBorder(state, style, colors, toned)
    val content = if (state.enabled || !state.clickable) colors.content else colors.content.disabled(DISABLED)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val strip = if (toned && state.selected != true) {
        val stripColor = colors.border ?: colors.content
        Modifier.drawWithContent {
            drawContent()
            val w = style.toneBorderWidth.toPx()
            drawRect(stripColor, Offset(if (rtl) size.width - w else 0f, 0f), Size(w, size.height))
        }
    } else {
        Modifier
    }
    val feedback = if (state.clickable) {
        Modifier.indication(state.interactionSource, rememberSkinIndication(content, style.shape))
    } else {
        Modifier
    }
    Box(
        modifier
            .then(Camouflage.skin.surfaceTreatment.modifier(colors.container, style.shape,
                theme.elevation.of(cardDepth(state, style)), theme))
            .then(border?.let { (color, width) -> Modifier.border(width, color, style.shape) } ?: Modifier)
            .clip(style.shape)
            .then(strip)
            .then(feedback)
            .padding(style.contentPadding),
    ) {
        val icons = Camouflage.skin.icon.defaults(theme).merge(theme.components.icon)
        ProvideContentStyle(content, icons.copy(color = content)) { slots.content() }
    }
}

// Raised cards rise on hover only when clickable; the others are flat.
private fun cardDepth(state: CardState, style: CardStyle): ElevationLevel = when {
    state.appearance != Appearance.Raised -> ElevationLevel.Level0
    state.clickable && state.interaction.hovered -> style.hoveredElevation
    else -> style.raisedElevation
}

// The selected border wins; a toned Raised card keeps its own border and shows the tone as a strip.
private fun cardBorder(state: CardState, style: CardStyle, colors: CardColors, toned: Boolean): Pair<Color, Dp>? =
    when {
        state.selected == true -> style.selectedBorderColor to style.selectedBorderWidth
        toned -> style.raisedColors.border?.let { it to style.borderWidth }
        else -> colors.border?.let { it to style.borderWidth }
    }

internal object MinimalListItemRenderer : ListItemRenderer {
    override fun defaults(theme: CamoTheme): ListItemStyle {
        val c = theme.colors
        val t = theme.typography
        val padding = theme.spacing.l
        return ListItemStyle(
            minHeightOneLine = 48.dp,
            minHeightTwoLines = 64.dp,
            minHeightThreeLines = 80.dp,
            contentPadding = PaddingValues(horizontal = padding, vertical = theme.spacing.s),
            leadingGap = padding,
            trailingGap = padding,
            headlineStyle = t.bodyLarge,
            supportingStyle = t.bodyMedium,
            overlineStyle = t.labelSmall,
            iconSize = IconSize,
            sectionShape = theme.shapes.none,
            sectionTitleStyle = t.titleSmall.semibold(),
            sectionFooterStyle = t.bodySmall,
            dividers = ListDividers.Inset,
            dividerInsetStart = padding + IconSize + padding,
            colors = ListItemColors(c.onSurface, c.onSurfaceVariant, c.onSurfaceVariant, c.onSurfaceVariant,
                c.surfaceContainerHigh, c.onSurfaceVariant, c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Render(
        state: ListItemState,
        slots: ListItemSlots,
        theme: CamoTheme,
        style: ListItemStyle,
        modifier: Modifier,
    ) = PaintListItem(state, slots, style, modifier, RectShape)

    @Composable
    override fun Section(
        state: ListSectionState,
        slots: ListSectionSlots,
        theme: CamoTheme,
        style: ListItemStyle,
        modifier: Modifier,
    ) = PaintListSection(state, slots, theme, style, modifier)
}

private val RectShape = RectangleShape

/** A list row: leading, the text column and trailing, with the selected fill and the press feedback. */
@Composable
internal fun PaintListItem(
    state: ListItemState,
    slots: ListItemSlots,
    style: ListItemStyle,
    modifier: Modifier,
    shape: Shape,
) {
    val c = style.colors
    val alpha = if (state.enabled) 1f else DISABLED
    val fill = if (state.selected) Modifier.background(c.selectedContainer, shape) else Modifier
    val feedback = if (state.clickable) {
        Modifier.indication(state.interactionSource, rememberSkinIndication(c.headline, shape))
    } else {
        Modifier
    }
    Row(
        modifier
            .then(fill)
            .then(feedback)
            .fillMaxWidth()
            .heightIn(min = style.minHeight(state.lines))
            .padding(style.contentPadding),
        verticalAlignment = if (state.lines >= 3) Alignment.Top else Alignment.CenterVertically,
    ) {
        ProvideContentStyle(c.icon.disabled(alpha), IconStyle(style.iconSize, c.icon.disabled(alpha))) {
            slots.leading?.let { Box(Modifier.padding(end = style.leadingGap)) { it() } }
            Column(Modifier.weight(1f).alpha(alpha)) {
                state.overline?.let { BasicText(it, style = style.overlineStyle.copy(color = c.overline)) }
                BasicText(state.headline, style = style.headlineStyle.copy(color = c.headline))
                state.supporting?.let { BasicText(it, style = style.supportingStyle.copy(color = c.supporting)) }
            }
            slots.trailing?.let { Box(Modifier.padding(start = style.trailingGap)) { it() } }
        }
    }
}

/** A list section: the title (a heading), the rows on the section's fill, and the footer. */
@Composable
internal fun PaintListSection(
    state: ListSectionState,
    slots: ListSectionSlots,
    theme: CamoTheme,
    style: ListItemStyle,
    modifier: Modifier,
) {
    val pad = theme.spacing.l
    Column(modifier) {
        state.title?.let {
            BasicText(
                it,
                Modifier.padding(start = pad, end = pad, top = pad, bottom = theme.spacing.s).semantics { heading() },
                style = style.sectionTitleStyle.copy(color = style.colors.sectionTitle),
            )
        }
        Box(Modifier.background(theme.colors.surfaceOf(state.level), style.sectionShape).clip(style.sectionShape)) {
            slots.rows()
        }
        state.footer?.let {
            BasicText(
                it,
                Modifier.padding(start = pad, end = pad, top = theme.spacing.s),
                style = style.sectionFooterStyle.copy(color = style.colors.sectionFooter),
            )
        }
    }
}

internal object MinimalAccordionRenderer : AccordionRenderer {
    override fun defaults(theme: CamoTheme): AccordionStyle {
        val c = theme.colors
        val pad = theme.spacing.l
        return AccordionStyle(
            indicator = AccordionIndicator.Chevron,
            indicatorPosition = IndicatorPosition.End,
            headerMinHeight = 52.dp,
            headerPadding = PaddingValues(horizontal = pad),
            contentPadding = PaddingValues(start = pad, end = pad, bottom = pad),
            titleStyle = theme.typography.titleSmall.semibold(),
            subtitleStyle = theme.typography.bodySmall,
            iconSize = IconSize,
            colors = AccordionColors(c.onSurface, c.onSurfaceVariant, c.onSurfaceVariant),
            strings = AccordionStrings(),
        )
    }

    @Composable
    override fun Render(
        state: AccordionState,
        slots: AccordionSlots,
        theme: CamoTheme,
        style: AccordionStyle,
        headerModifier: Modifier,
        modifier: Modifier,
    ) = PaintAccordion(state, slots, style, headerModifier, modifier, RectShape)
}

/** An accordion: the header row (leading, texts, the indicator on its side) and the core-animated content. */
@Composable
internal fun PaintAccordion(
    state: AccordionState,
    slots: AccordionSlots,
    style: AccordionStyle,
    headerModifier: Modifier,
    modifier: Modifier,
    shape: Shape,
) {
    val c = style.colors
    val alpha = if (state.enabled) 1f else DISABLED
    val open = c.expandedContainer
    Column(modifier.then(if (state.expanded && open != null) Modifier.background(open, shape) else Modifier)) {
        Row(
            headerModifier
                .indication(state.interactionSource, rememberSkinIndication(c.title, shape))
                .fillMaxWidth()
                .heightIn(min = style.headerMinHeight)
                .padding(style.headerPadding)
                .alpha(alpha),
            horizontalArrangement = Arrangement.spacedBy(HeaderGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideContentStyle(c.indicator, IconStyle(style.iconSize, c.indicator)) {
                if (style.indicatorPosition == IndicatorPosition.Start) Indicator(state, style)
                slots.leading?.invoke()
                Column(Modifier.weight(1f)) {
                    BasicText(state.title, style = style.titleStyle.copy(color = c.title))
                    state.subtitle?.let { BasicText(it, style = style.subtitleStyle.copy(color = c.subtitle)) }
                }
                if (style.indicatorPosition == IndicatorPosition.End) Indicator(state, style)
            }
        }
        slots.content()
    }
}

@Composable
private fun Indicator(state: AccordionState, style: AccordionStyle) {
    when (style.indicator) {
        AccordionIndicator.Chevron ->
            CamoIcon(IconSource.Vector(ChevronDown), null, Modifier.rotate(state.openFraction * HALF_TURN))
        AccordionIndicator.PlusMinus ->
            CamoIcon(IconSource.Vector(if (state.expanded) Minus else Plus), null)
    }
}

internal object MinimalSkeletonRenderer : SkeletonRenderer {
    override fun defaults(theme: CamoTheme) = SkeletonStyle(
        animation = SkeletonAnimation.Pulse,
        baseColor = theme.colors.surfaceContainerHigh,
        highlightColor = theme.colors.surfaceContainerLowest,
        cycleDuration = 1500,
        textLineGap = 6.dp,
    )

    @Composable
    override fun Render(state: SkeletonState, theme: CamoTheme, style: SkeletonStyle, modifier: Modifier) {
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(modifier.drawSkeleton(state, style, rtl))
    }
}

internal object MinimalTopBarRenderer : TopBarRenderer {
    override fun defaults(theme: CamoTheme): TopBarStyle {
        val c = theme.colors
        return TopBarStyle(
            titleAlign = TitleAlign.Start,
            divider = TopBarDivider.WhenScrolled,
            height = 56.dp,
            titleStyle = theme.typography.titleLarge.semibold(),
            iconSize = IconSize,
            padding = PaddingValues(start = theme.spacing.xs, end = theme.spacing.xs),
            actionGap = theme.spacing.xs,
            dividerWidth = Hairline,
            colors = TopBarColors(c.surface, c.surface, c.onSurface, c.onSurface, c.onSurfaceVariant, c.outlineVariant),
        )
    }

    @Composable
    override fun Render(
        state: TopBarState,
        slots: TopBarSlots,
        theme: CamoTheme,
        style: TopBarStyle,
        modifier: Modifier,
    ) =
        PaintTopBar(state, slots, theme, style, modifier)

    @Composable
    override fun Container(
        state: TopBarState,
        content: @Composable BoxScope.() -> Unit,
        theme: CamoTheme,
        style: TopBarStyle,
        modifier: Modifier,
    ) = PaintTopBarContainer(state, content, theme, style, modifier)
}

/** The bar's surface (tinted when scrolled), the content row padded by the insets, and the divider. */
@Composable
internal fun PaintTopBarContainer(
    state: TopBarState,
    content: @Composable BoxScope.() -> Unit,
    theme: CamoTheme,
    style: TopBarStyle,
    modifier: Modifier,
) {
    val container by animateColorAsState(
        if (state.scrolled) style.colors.scrolledContainer else style.colors.container,
        tween(theme.motion.durationShort),
        label = "top bar",
    )
    Column(modifier.fillMaxWidth().background(container)) {
        Box(Modifier.windowInsetsPadding(state.windowInsets).fillMaxWidth(), content = content)
        if (style.showsDivider(state.scrollOffset)) {
            Box(Modifier.fillMaxWidth().height(style.dividerWidth).background(style.colors.divider))
        }
    }
}

/** A top bar: navigation, the title placed by `titleAlign` (a heading), and the actions, on the bar's surface. */
@Composable
internal fun PaintTopBar(
    state: TopBarState,
    slots: TopBarSlots,
    theme: CamoTheme,
    style: TopBarStyle,
    modifier: Modifier,
) {
    val c = style.colors
    PaintTopBarContainer(state, {
        TopBarRow(
            titleAlign = style.titleAlign,
            minHeight = style.height,
            actionGap = style.actionGap,
            navigation = slots.navigation?.let { nav ->
                { ProvideContentStyle(c.navigation, IconStyle(style.iconSize, c.navigation)) { nav() } }
            },
            title = {
                // Without navigation, a start-aligned title keeps the bar's text inset.
                val inset = if (slots.navigation == null && style.titleAlign == TitleAlign.Start) {
                    Modifier.padding(start = theme.spacing.l - theme.spacing.xs)
                } else {
                    Modifier.padding(horizontal = theme.spacing.s)
                }
                Box(inset) {
                    val custom = slots.titleContent
                    if (custom != null) {
                        custom()
                    } else {
                        BasicText(
                            state.title,
                            Modifier.semantics { heading() },
                            style = style.titleStyle.copy(color = c.title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            actions = { ProvideContentStyle(c.actions, IconStyle(style.iconSize, c.actions)) { slots.actions(this) } },
            modifier = Modifier.padding(style.padding),
        )
    }, theme, style, modifier)
}

internal object MinimalPagedListRenderer : PagedListRenderer {
    override fun defaults(theme: CamoTheme) = PagedListStyle(
        initialSkeletonCount = 10,
        moreSkeletonCount = 3,
        prefetchDistance = 5,
        refreshKeepsItems = false,
        refreshThreshold = 72.dp,
        itemFadeIn = true,
        strings = PagedListStrings(),
    )

    @Composable
    override fun RefreshIndicator(state: PagedListState, theme: CamoTheme, style: PagedListStyle, modifier: Modifier) =
        PaintRefreshIndicator(state, theme, style, modifier, theme.colors.primary, null)

    @Composable
    override fun RetryRow(
        message: String,
        onRetry: () -> Unit,
        theme: CamoTheme,
        style: PagedListStyle,
        modifier: Modifier,
    ) = PaintRetryRow(message, onRetry, theme, style, modifier)

    @Composable
    override fun Empty(theme: CamoTheme, style: PagedListStyle, modifier: Modifier) =
        PaintPagedMessage(style.strings.emptyTitle, style.strings.emptyMessage, null, null, theme, style, modifier)

    @Composable
    override fun Error(
        message: String,
        onRetry: () -> Unit,
        retryFocus: FocusRequester,
        theme: CamoTheme,
        style: PagedListStyle,
        modifier: Modifier,
    ) = PaintPagedMessage(style.strings.errorTitle, message, onRetry, retryFocus, theme, style, modifier)
}

/**
 * The refresh spot: an arc that fills as the user pulls (following the finger down), then a spinning circle
 * while refreshing. [disc] draws a filled circle behind it when set.
 */
@Composable
internal fun PaintRefreshIndicator(
    state: PagedListState,
    theme: CamoTheme,
    style: PagedListStyle,
    modifier: Modifier,
    color: Color,
    disc: Color?,
) {
    val active = state.refreshing || state.loading == PagedLoad.Refresh
    val travel = style.refreshThreshold
    Box(
        modifier
            .padding(top = theme.spacing.l)
            .offset {
                val pull = if (active) 0f else min(state.pullFraction(), 1f)
                IntOffset(0, (pull * travel.toPx() - theme.spacing.l.toPx()).toInt().coerceAtLeast(0))
            }
            .then(disc?.let { Modifier.background(it, theme.shapes.full).padding(theme.spacing.s) } ?: Modifier),
    ) {
        if (active) {
            CamoProgressCircle(Modifier.size(ProgressSpot), size = ProgressSize.Small)
        } else {
            Box(
                Modifier.size(ProgressSpot).drawBehind {
                    val f = state.pullFraction()
                    if (f > 0f) {
                        val stroke = 2.dp.toPx()
                        drawArc(color, ARC_START, FULL_TURN * min(f, 1f), false,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                },
            )
        }
    }
}

/** The next-page retry row: the message and a Ghost retry button. */
@Composable
internal fun PaintRetryRow(
    message: String,
    onRetry: () -> Unit,
    theme: CamoTheme,
    style: PagedListStyle,
    modifier: Modifier,
) {
    Row(
        modifier.padding(horizontal = theme.spacing.l, vertical = theme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            message,
            Modifier.weight(1f),
            style = theme.typography.bodyMedium.copy(color = theme.colors.onSurfaceVariant),
        )
        CamoButton(style.strings.retry, onRetry, appearance = Appearance.Ghost)
    }
}

/** A centered empty or error message; with [onRetry], a retry button under it. */
@Composable
internal fun PaintPagedMessage(
    title: String,
    message: String,
    onRetry: (() -> Unit)?,
    retryFocus: FocusRequester?,
    theme: CamoTheme,
    style: PagedListStyle,
    modifier: Modifier,
) {
    Column(
        modifier.padding(theme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            title,
            style = theme.typography.titleMedium.semibold().copy(color = theme.colors.onSurface,
                textAlign = TextAlign.Center),
        )
        if (message.isNotBlank()) {
            BasicText(
                message,
                style = theme.typography.bodyMedium.copy(color = theme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center),
            )
        }
        if (onRetry != null) {
            val focus = retryFocus?.let { Modifier.focusRequester(it) } ?: Modifier
            CamoButton(style.strings.retry, onRetry, focus.padding(top = theme.spacing.s))
        }
    }
}

internal val ChevronDown: ImageVector = ImageVector.Builder("chevron-down", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
        moveTo(6f, 9f)
        lineTo(12f, 15f)
        lineTo(18f, 9f)
    }
    .build()
