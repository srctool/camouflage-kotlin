package com.srctool.camouflage.core.pagedlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.theme.CamoTheme

/** Which load is running: none, the first page (or a new search), a refresh, or the next page. */
public enum class PagedLoad { None, Initial, Refresh, More }

/** Which page failed: the first one (nothing to show) or the next one (the items stay, with a retry row). */
public enum class PagedErrorScope { FirstPage, NextPage }

/** A failed load: the [message] to show and which page failed. */
@Immutable
public data class PagedError(val message: String, val scope: PagedErrorScope)

/**
 * Everything a [CamoPagedList] shows. The app (or its data layer, through a small adapter) builds it; the list
 * only reads it. [endReached] means there are no more pages.
 */
@Immutable
public data class CamoPagedListState<T>(
    val items: List<T> = emptyList(),
    val loading: PagedLoad = PagedLoad.None,
    val endReached: Boolean = false,
    val error: PagedError? = null,
)

/** The words a paged list shows and announces. */
@Immutable
public data class PagedListStrings(
    val emptyTitle: String = "Nothing here yet",
    val emptyMessage: String = "",
    val errorTitle: String = "Something went wrong",
    val retry: String = "Retry",
    val nextPageFailed: String = "Couldn't load more",
    val loading: String = "Loading",
    val loadingMore: String = "Loading more",
)

/** Overrides for paged lists. `null` keeps the skin's default. */
@Immutable
public data class PagedListTheme(
    val initialSkeletonCount: Int? = null,
    val moreSkeletonCount: Int? = null,
    val prefetchDistance: Int? = null,
    /** `true`: a refresh keeps the items and shows only the indicator. */
    val refreshKeepsItems: Boolean? = null,
    /** How far the user pulls past the top before releasing refreshes. */
    val refreshThreshold: Dp? = null,
    val itemFadeIn: Boolean? = null,
    val strings: PagedListStrings? = null,
)

/** The resolved paged list style: every field set. */
@Immutable
public data class PagedListStyle(
    val initialSkeletonCount: Int,
    val moreSkeletonCount: Int,
    val prefetchDistance: Int,
    val refreshKeepsItems: Boolean,
    val refreshThreshold: Dp,
    val itemFadeIn: Boolean,
    val strings: PagedListStrings,
) {
    /** This style with [overrides]' non-null fields applied. */
    public fun merge(overrides: PagedListTheme?): PagedListStyle = if (overrides == null) {
        this
    } else {
        PagedListStyle(
            initialSkeletonCount = overrides.initialSkeletonCount ?: initialSkeletonCount,
            moreSkeletonCount = overrides.moreSkeletonCount ?: moreSkeletonCount,
            prefetchDistance = overrides.prefetchDistance ?: prefetchDistance,
            refreshKeepsItems = overrides.refreshKeepsItems ?: refreshKeepsItems,
            refreshThreshold = overrides.refreshThreshold ?: refreshThreshold,
            itemFadeIn = overrides.itemFadeIn ?: itemFadeIn,
            strings = overrides.strings ?: strings,
        )
    }
}

/**
 * What a paged list renderer draws around the rows core lays out. [refreshing] is true from a pull past the
 * threshold until the refresh finishes; [pullFraction] is how far the user has pulled (1 at the threshold, more
 * past it): read it while drawing or laying out, so the pull doesn't recompose.
 */
@Stable
public class PagedListState(
    public val loading: PagedLoad,
    public val endReached: Boolean,
    public val errorScope: PagedErrorScope?,
    public val isEmpty: Boolean,
    public val refreshing: Boolean,
    public val pullFraction: () -> Float,
)

/**
 * Draws the parts of a paged list that aren't rows: the refresh indicator, the retry row, the empty and error
 * states.
 */
@Stable
public interface PagedListRenderer {
    /** The complete paged list style for [theme]. */
    public fun defaults(theme: CamoTheme): PagedListStyle

    /**
     * The refresh indicator, over the top of the list (`modifier` aligns it). Show it while [PagedListState.refreshing],
     * while the user pulls, and for `PagedLoad.Refresh`; draw nothing otherwise.
     */
    @Composable
    public fun RefreshIndicator(state: PagedListState, theme: CamoTheme, style: PagedListStyle, modifier: Modifier)

    /** The row after the items when the next page failed: [message] and a retry action calling [onRetry]. */
    @Composable
    public fun RetryRow(
        message: String,
        onRetry: () -> Unit,
        theme: CamoTheme,
        style: PagedListStyle,
        modifier: Modifier,
    )

    /** The default empty state, filling `modifier`'s size. */
    @Composable
    public fun Empty(theme: CamoTheme, style: PagedListStyle, modifier: Modifier)

    /**
     * The default first-page error, filling `modifier`'s size: [message] and a retry button calling [onRetry],
     * with [retryFocus] on the button (core focuses it when the error appears).
     */
    @Composable
    public fun Error(
        message: String,
        onRetry: () -> Unit,
        retryFocus: FocusRequester,
        theme: CamoTheme,
        style: PagedListStyle,
        modifier: Modifier,
    )
}
