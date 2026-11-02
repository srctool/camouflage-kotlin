package com.srctool.camouflage.core.navigation

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.srctool.camouflage.core.skin.InteractionState
import com.srctool.camouflage.core.skin.ItemPosition
import com.srctool.camouflage.core.util.RovingFocus

/** How a navigation item marks the selected destination (ADR-28). Each skin draws the ones it supports. */
public enum class NavIndicator { Pill, Line, Dot, Fill, None }

/** Whether items show their labels: an app-wide choice, so it is a theme field (ADR-23). */
public enum class LabelVisibility { Always, Selected, Never }

/** Where the bar sits: edge to edge on the bottom inset, or detached above it (ADR-30). */
public enum class NavPlacement { Pinned, Floating }

/** Where a rail items sit (ADR-23: only Material centers them). */
public enum class RailAlignment { Top, Center }

/**
 * A top-level destination, shared by the bar, the rail and the drawer. [value] is the app own key (a route
 * name, a `NavKey`); navigation chrome only shows and reports the selection, routing stays with the app
 * router (ADR-19). [selectedIcon] is an optional filled variant; [badge] is a `CamoBadge`.
 */
@Immutable
public data class CamoNavItem<T>(
    val value: T,
    val label: String,
    val icon: @Composable () -> Unit,
    val selectedIcon: (@Composable () -> Unit)? = null,
    val badge: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true,
)

/** A titled group of destinations in a drawer; one untitled section makes a plain list. */
@Immutable
public data class CamoNavSection<T>(val title: String? = null, val items: List<CamoNavItem<T>>)

/** Every color a navigation bar or rail uses. */
@Immutable
public data class NavColors(
    val container: Color,
    val indicator: Color,
    val selectedIcon: Color,
    val selectedLabel: Color,
    val icon: Color,
    val label: Color,
)

/** Every color a navigation drawer uses. [modalContainer] is the container while it slides over the content. */
@Immutable
public data class DrawerColors(
    val container: Color,
    val modalContainer: Color,
    val indicator: Color,
    val selectedContent: Color,
    val content: Color,
    val sectionTitle: Color,
)

/**
 * What an item renderer draws: one destination at [position]. [showLabel] is already decided from the theme
 * [LabelVisibility]; [indicator] is the style one. Draw press feedback from [interactionSource].
 */
@Immutable
public data class NavItemState(
    val selected: Boolean,
    val enabled: Boolean,
    val position: ItemPosition,
    val showLabel: Boolean,
    val indicator: NavIndicator,
    val interaction: InteractionState,
    val interactionSource: InteractionSource,
)

/** An item content: the [icon] already chosen for its state, the [label] and the optional [badge]. */
@Stable
public class NavItemSlots(
    public val label: String,
    public val icon: @Composable () -> Unit,
    public val badge: (@Composable () -> Unit)?,
)

// Which kind of chrome an item sits in, so `CamoNavItemButton` asks the right renderer.
internal enum class NavHost { Bar, Rail, Drawer }

internal class NavGroup(val host: NavHost, val focus: RovingFocus, val enabled: Boolean)
