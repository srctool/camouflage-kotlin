package com.srctool.camouflage.sample

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.srctool.camouflage.core.dialog.DialogActionsLayout
import com.srctool.camouflage.core.dialog.DialogColors
import com.srctool.camouflage.core.dialog.DialogRenderer
import com.srctool.camouflage.core.dialog.DialogSlots
import com.srctool.camouflage.core.dialog.DialogState
import com.srctool.camouflage.core.dialog.DialogStyle
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.list.ListDividers
import com.srctool.camouflage.core.list.ListItemColors
import com.srctool.camouflage.core.list.ListItemRenderer
import com.srctool.camouflage.core.list.ListItemSlots
import com.srctool.camouflage.core.list.ListItemState
import com.srctool.camouflage.core.list.ListItemStyle
import com.srctool.camouflage.core.list.ListSectionSlots
import com.srctool.camouflage.core.list.ListSectionState
import com.srctool.camouflage.core.overlay.StackedScrim
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

// The M6 structure components in the debug skin: outlined boxes, text markers for the indicator and refresh.

private const val CYCLE_MILLIS = 1500
private const val INITIAL_ROWS = 6
private const val MORE_ROWS = 2
private const val PREFETCH = 3

object DebugCardRenderer : CardRenderer {
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
        Box(modifier.background(colors.container).then(colors.border?.let { Modifier.border(1.dp, it) } ?: Modifier)) {
            slots.content()
        }
    }
}

object DebugListItemRenderer : ListItemRenderer {
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

object DebugAccordionRenderer : AccordionRenderer {
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

object DebugSkeletonRenderer : SkeletonRenderer {
    override fun defaults(theme: CamoTheme) = SkeletonStyle(
        SkeletonAnimation.None, theme.colors.surfaceContainerHighest, theme.colors.surfaceContainerLow, CYCLE_MILLIS,
        8.dp,
    )

    @Composable
    override fun Render(state: SkeletonState, theme: CamoTheme, style: SkeletonStyle, modifier: Modifier) =
        Box(modifier.background(style.baseColor, state.shape))
}

object DebugTopBarRenderer : TopBarRenderer {
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
                .background(if (state.scrolled) style.colors.scrolledContainer else style.colors.container)
                .then(if (style.showsDivider(state.scrollOffset)) Modifier.border(1.dp, outline) else Modifier)
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

object DebugPagedListRenderer : PagedListRenderer {
    override fun defaults(theme: CamoTheme) = PagedListStyle(
        INITIAL_ROWS, MORE_ROWS, PREFETCH, false, 64.dp, false, PagedListStrings(),
    )

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

private const val MIN_WIDTH = 280
private const val MAX_WIDTH = 560
private const val PADDING = 24
private const val GAP = 8
private const val SECTION = 16
private const val SCRIM = 0.32f
private const val ICON = 24
private const val ICON_GRID = 24f

/** Debug dialogs: a column of texts and a row of actions, outlined. */
object DebugDialogRenderer : DialogRenderer {
    override fun defaults(theme: CamoTheme) = DialogStyle(
        RectangleShape, MIN_WIDTH.dp, MAX_WIDTH.dp, PaddingValues(PADDING.dp), theme.typography.titleLarge,
        theme.typography.bodyMedium, TitleAlign.Start, DialogActionsLayout.End, GAP.dp, SECTION.dp, false,
        IconSource.Vector(ImageVector.Builder("x", ICON.dp, ICON.dp, ICON_GRID, ICON_GRID).build()),
        ElevationLevel.Level0, SCRIM,
        StackedScrim.Each, theme.colors.let { DialogColors(it.surface, it.onSurface, it.onSurfaceVariant) },
    )

    @Composable
    override fun Render(
        state: DialogState,
        slots: DialogSlots,
        theme: CamoTheme,
        style: DialogStyle,
        modifier: Modifier,
    ) = Column(modifier.background(style.colors.container).border(1.dp, theme.colors.outline).padding(style.padding)) {
        state.title?.let { BasicText(it) }
        BasicText(state.message)
        slots.content?.invoke(this)
        Row {
            slots.dismiss?.invoke()
            slots.confirm()
        }
    }
}
