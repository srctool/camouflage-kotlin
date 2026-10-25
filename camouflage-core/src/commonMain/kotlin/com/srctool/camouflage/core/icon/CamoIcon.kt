package com.srctool.camouflage.core.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.takeOrElse
import com.srctool.camouflage.core.provider.Camouflage
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** An icon asset, plus whether it mirrors in RTL and whether it keeps its own colors. */
@Immutable
public sealed interface IconSource {
    /** Flip horizontally in right-to-left layouts (back arrows, chevrons). */
    public val autoMirror: Boolean

    /** A multicolor asset: never tinted. */
    public val original: Boolean

    /** An [ImageVector]; it mirrors when the vector says so. */
    public data class Vector(
        val image: ImageVector,
        override val autoMirror: Boolean = image.autoMirror,
        override val original: Boolean = false,
    ) : IconSource

    /** A Compose Multiplatform resource, e.g. `Res.drawable.ic_search`. */
    public data class Resource(
        val drawable: DrawableResource,
        override val autoMirror: Boolean = false,
        override val original: Boolean = false,
    ) : IconSource

    /** Any [Painter]. */
    public data class Paint(
        val painter: Painter,
        override val autoMirror: Boolean = false,
        override val original: Boolean = false,
    ) : IconSource
}

/** What an icon renderer draws: the asset at a resolved size and tint. */
@Immutable
public data class IconState(
    val source: IconSource,
    val size: Dp,
    val color: Color,
    val contentDescription: String?,
)

/** Draws icons, and sets the root icon style the provider resets to. */
@Stable
public interface IconRenderer {
    /** The icon style outside any component (size and color). */
    public fun defaults(theme: CamoTheme): IconStyle

    /** Draws [state]. */
    @Composable
    public fun Render(state: IconState, theme: CamoTheme, modifier: Modifier)
}

/**
 * An icon that takes its size and color from the surrounding component unless [size] or [tint] is set.
 * A `null` [contentDescription] makes it decorative: the right choice for icons passed into components,
 * which carry the label themselves.
 */
@Composable
public fun CamoIcon(
    source: IconSource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dp.Unspecified,
    tint: Color = Color.Unspecified,
) {
    val skin = requireSkin("CamoIcon")
    val theme = requireTheme("CamoIcon")
    val ambient = Camouflage.iconStyle
    val semantics = if (contentDescription == null) {
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }
    skin.icon.Render(
        IconState(source, size.takeOrElse { ambient.size }, tint.takeOrElse { ambient.color }, contentDescription),
        theme,
        modifier.then(semantics),
    )
}

/**
 * Draws [state]'s asset at its size and tint, mirrored in right-to-left layouts when the source asks, and
 * untinted when it's `original`. What most icon renderers do; a skin may draw icons its own way instead.
 */
@Composable
public fun IconImage(state: IconState, modifier: Modifier = Modifier) {
    val source = state.source
    val painter = when (source) {
        is IconSource.Vector -> rememberVectorPainter(source.image)
        is IconSource.Resource -> painterResource(source.drawable)
        is IconSource.Paint -> source.painter
    }
    val mirror = source.autoMirror && LocalLayoutDirection.current == LayoutDirection.Rtl
    Image(
        painter,
        contentDescription = null,
        modifier = modifier.size(state.size).graphicsLayer { if (mirror) scaleX = -1f },
        colorFilter = if (source.original) null else ColorFilter.tint(state.color),
    )
}
