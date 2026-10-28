package com.srctool.camouflage.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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
import com.srctool.camouflage.core.skeleton.SkeletonAnimation
import com.srctool.camouflage.core.skeleton.SkeletonRenderer
import com.srctool.camouflage.core.skeleton.SkeletonState
import com.srctool.camouflage.core.skeleton.SkeletonStyle
import com.srctool.camouflage.core.skin.TitleAlign
import com.srctool.camouflage.core.theme.Appearance
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ElevationLevel
import com.srctool.camouflage.core.topbar.TopBarColors
import com.srctool.camouflage.core.topbar.TopBarDivider
import com.srctool.camouflage.core.topbar.TopBarRenderer
import com.srctool.camouflage.core.topbar.TopBarSlots
import com.srctool.camouflage.core.topbar.TopBarState
import com.srctool.camouflage.core.topbar.TopBarStyle

// The M6 structure components in the plain test skin: boxes, rows and text markers, no decoration.

internal object PlainCard : CardRenderer {
    override fun defaults(theme: CamoTheme): CardStyle {
        val c = theme.colors
        return CardStyle(
            Appearance.Raised, RectangleShape, PaddingValues(), CardColors(c.primary, c.onPrimary),
            CardColors(c.surfaceContainerHighest, c.onSurface), CardColors(c.surfaceContainerLow, c.onSurface),
            CardColors(c.surface, c.onSurface, c.outlineVariant), ElevationLevel.Level1, ElevationLevel.Level2, 1.dp,
            4.dp, c.primary, 2.dp, false,
        )
    }

    @Composable
    override fun Render(state: CardState, slots: CardSlots, theme: CamoTheme, style: CardStyle, modifier: Modifier) {
        val colors = style.colorsFor(state.appearance, state.tone, theme)
        Box(modifier.background(colors.container).semantics { testTag = "card-${state.appearance}" }) {
            slots.content()
        }
    }
}

internal object PlainListItem : ListItemRenderer {
    override fun defaults(theme: CamoTheme): ListItemStyle {
        val c = theme.colors
        val t = theme.typography
        return ListItemStyle(
            56.dp, 72.dp, 88.dp, PaddingValues(horizontal = 16.dp, vertical = 8.dp), 16.dp, 16.dp, t.bodyLarge,
            t.bodyMedium, t.labelSmall, 24.dp, RectangleShape, t.titleSmall, t.bodySmall, ListDividers.Inset, 56.dp,
            ListItemColors(c.onSurface, c.onSurfaceVariant, c.onSurfaceVariant, c.onSurfaceVariant,
                c.secondaryContainer, c.onSurfaceVariant, c.onSurfaceVariant),
        )
    }

    @Composable
    override fun Render(
        state: ListItemState,
        slots: ListItemSlots,
        theme: CamoTheme,
        style: ListItemStyle,
        modifier: Modifier,
    ) = Row(modifier.heightIn(min = style.minHeight(state.lines)), verticalAlignment = Alignment.CenterVertically) {
        slots.leading?.invoke()
        Column(Modifier.weight(1f)) {
            state.overline?.let { BasicText(it) }
            BasicText(state.headline)
            state.supporting?.let { BasicText(it) }
        }
        slots.trailing?.invoke()
    }

    @Composable
    override fun Section(
        state: ListSectionState,
        slots: ListSectionSlots,
        theme: CamoTheme,
        style: ListItemStyle,
        modifier: Modifier,
    ) = Column(modifier) {
        state.title?.let { BasicText(it, Modifier.semantics { heading() }) }
        slots.rows()
        state.footer?.let { BasicText(it) }
    }
}

internal object PlainAccordion : AccordionRenderer {
    override fun defaults(theme: CamoTheme) = AccordionStyle(
        AccordionIndicator.Chevron, IndicatorPosition.End, 56.dp, PaddingValues(horizontal = 16.dp),
        PaddingValues(16.dp), theme.typography.bodyLarge, theme.typography.bodyMedium, 24.dp,
        theme.colors.let { AccordionColors(it.onSurface, it.onSurfaceVariant, it.onSurfaceVariant) },
        AccordionStrings(),
    )

    @Composable
    override fun Render(
        state: AccordionState,
        slots: AccordionSlots,
        theme: CamoTheme,
        style: AccordionStyle,
        headerModifier: Modifier,
        modifier: Modifier,
    ) = Column(modifier) {
        Row(headerModifier.heightIn(min = style.headerMinHeight), verticalAlignment = Alignment.CenterVertically) {
            slots.leading?.invoke()
            BasicText(state.title)
            BasicText(if (state.expanded) "-" else "+")
        }
        Box(Modifier.padding(style.contentPadding)) { slots.content() }
    }
}

internal object PlainSkeleton : SkeletonRenderer {
    override fun defaults(theme: CamoTheme) = SkeletonStyle(
        SkeletonAnimation.Shimmer, theme.colors.surfaceContainerHighest, theme.colors.surfaceContainerLow, 1500, 8.dp,
    )

    @Composable
    override fun Render(state: SkeletonState, theme: CamoTheme, style: SkeletonStyle, modifier: Modifier) =
        Box(modifier.background(style.baseColor, state.shape))
}

internal object PlainTopBar : TopBarRenderer {
    override fun defaults(theme: CamoTheme) = TopBarStyle(
        TitleAlign.Start, TopBarDivider.WhenScrolled, 64.dp, theme.typography.titleLarge, 24.dp,
        PaddingValues(horizontal = 4.dp), 4.dp, 1.dp,
        theme.colors.let {
            TopBarColors(it.surface, it.surfaceContainer, it.onSurface, it.onSurface, it.onSurfaceVariant,
                it.outlineVariant)
        },
    )

    @Composable
    override fun Render(
        state: TopBarState,
        slots: TopBarSlots,
        theme: CamoTheme,
        style: TopBarStyle,
        modifier: Modifier,
    ): Unit = theme.colors.outline.let { outline ->
        Row(
            modifier
                .then(if (style.showsDivider(state.scrollOffset)) Modifier.border(1.dp, outline) else Modifier)
                .semantics { testTag = if (state.scrolled) "bar-scrolled" else "bar-resting" }
                .windowInsetsPadding(state.windowInsets)
                .heightIn(min = style.height),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            slots.navigation?.invoke()
            Box(Modifier.weight(1f)) {
                val custom = slots.titleContent
                if (custom != null) custom() else BasicText(state.title, Modifier.semantics { heading() })
            }
            slots.actions(this)
        }
    }

    @Composable
    override fun Container(
        state: TopBarState,
        content: @Composable BoxScope.() -> Unit,
        theme: CamoTheme,
        style: TopBarStyle,
        modifier: Modifier,
    ) = Box(modifier.windowInsetsPadding(state.windowInsets), content = content)
}

internal object PlainPagedList : PagedListRenderer {
    override fun defaults(theme: CamoTheme) = PagedListStyle(4, 2, 2, false, 64.dp, false, PagedListStrings())

    @Composable
    override fun RefreshIndicator(state: PagedListState, theme: CamoTheme, style: PagedListStyle, modifier: Modifier) {
        if (state.refreshing) BasicText("refreshing", modifier)
    }

    @Composable
    override fun RetryRow(
        message: String,
        onRetry: () -> Unit,
        theme: CamoTheme,
        style: PagedListStyle,
        modifier: Modifier,
    ) = Row(modifier) {
        BasicText(message)
        CamoButton(style.strings.retry, onRetry)
    }

    @Composable
    override fun Empty(theme: CamoTheme, style: PagedListStyle, modifier: Modifier) =
        Box(modifier) { BasicText(style.strings.emptyTitle) }

    @Composable
    override fun Error(
        message: String,
        onRetry: () -> Unit,
        retryFocus: FocusRequester,
        theme: CamoTheme,
        style: PagedListStyle,
        modifier: Modifier,
    ) = Column(modifier) {
        BasicText(message)
        CamoButton(style.strings.retry, onRetry, Modifier.focusRequester(retryFocus))
    }
}
