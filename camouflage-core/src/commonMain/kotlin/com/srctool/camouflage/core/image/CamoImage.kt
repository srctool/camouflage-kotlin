package com.srctool.camouflage.core.image

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.srctool.camouflage.core.icon.IconSource
import com.srctool.camouflage.core.provider.requireSkin
import com.srctool.camouflage.core.provider.requireTheme
import com.srctool.camouflage.core.theme.CamoTheme
import com.srctool.camouflage.core.theme.ShapeRole
import com.srctool.camouflage.core.theme.of

/** Every color an image frame uses. */
@Immutable
public data class ImageColors(val placeholder: Color, val icon: Color, val border: Color)

/** Overrides for images. `null` keeps the skin's default. */
@Immutable
public data class ImageTheme(
    val placeholderIcon: IconSource? = null,
    val errorIcon: IconSource? = null,
    val fadeMillis: Int? = null,
    val borderWidth: Dp? = null,
    val colors: ImageColors? = null,
)

/** The resolved images style: every field set. */
@Immutable
public data class ImageStyle(
    val placeholderIcon: IconSource,
    val errorIcon: IconSource,
    val fadeMillis: Int,
    val borderWidth: Dp,
    val colors: ImageColors,
) {
    /** This style with [overrides]' non-null fields applied. */
    @Suppress("CyclomaticComplexMethod") // one `?:` per field
    public fun merge(overrides: ImageTheme?): ImageStyle = if (overrides == null) {
        this
    } else {
        ImageStyle(
            placeholderIcon = overrides.placeholderIcon ?: placeholderIcon,
            errorIcon = overrides.errorIcon ?: errorIcon,
            fadeMillis = overrides.fadeMillis ?: fadeMillis,
            borderWidth = overrides.borderWidth ?: borderWidth,
            colors = overrides.colors ?: colors,
        )
    }
}

/** Where an image is: loading, shown, or failed. */
public enum class ImagePhase { Loading, Loaded, Failed }

/**
 * What [CamoImage] shows. Core loads nothing (ADR-27): the app maps its image loader's state to one of these.
 */
@Immutable
public sealed interface ImageSource {
    /** The loaded [painter]. */
    public data class Loaded(val painter: Painter) : ImageSource

    /** Still loading: the skin's placeholder shows. */
    public data object Loading : ImageSource

    /** Loading failed with [error]: the skin's error look shows. */
    public data class Failed(val error: Throwable? = null) : ImageSource
}

/** What the image renderer draws: the frame in [shape], and the placeholder or error look unless [phase] is loaded. */
@Immutable
public data class ImageState(val phase: ImagePhase, val shape: Shape)

/** Draws the frame (shape, placeholder, error, border) around the image that core draws as `content`. */
@Stable
public interface ImageRenderer {
    /** The complete image style for [theme]. */
    public fun defaults(theme: CamoTheme): ImageStyle

    /** Draws the frame; call [content] for the loaded image, which core fades in. */
    @Composable
    public fun Frame(
        state: ImageState,
        theme: CamoTheme,
        style: ImageStyle,
        modifier: Modifier,
        content: @Composable () -> Unit,
    )
}

private const val FAILED = "Could not load"

/**
 * A picture with a shape, a placeholder while it loads and an error look when it fails. [contentDescription]
 * describes it, or is `null` to hide a decorative image from screen readers; a failed image adds "could not
 * load". [aspectRatio] fixes the frame's proportions.
 */
@Composable
public fun CamoImage(
    source: ImageSource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: ShapeRole = ShapeRole.Medium,
    contentScale: ContentScale = ContentScale.Crop,
    aspectRatio: Float? = null,
    alignment: Alignment = Alignment.Center,
) {
    val skin = requireSkin("CamoImage")
    val theme = requireTheme("CamoImage")
    val style = remember(skin, theme) { skin.image.defaults(theme).merge(theme.components.image) }
    val phase = when (source) {
        is ImageSource.Loaded -> ImagePhase.Loaded
        ImageSource.Loading -> ImagePhase.Loading
        is ImageSource.Failed -> ImagePhase.Failed
    }
    val fade by animateFloatAsState(if (phase == ImagePhase.Loaded) 1f else 0f, tween(style.fadeMillis))
    val semantics = if (contentDescription == null) {
        Modifier.clearAndSetSemantics {}
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
            if (phase == ImagePhase.Failed) stateDescription = FAILED
        }
    }
    skin.image.Frame(
        ImageState(phase, theme.shapes.of(shape)),
        theme,
        style,
        modifier.then(if (aspectRatio != null) Modifier.aspectRatio(aspectRatio) else Modifier).then(semantics),
    ) {
        if (source is ImageSource.Loaded) {
            Image(
                source.painter,
                contentDescription = null,
                modifier = Modifier.alpha(fade).fillMaxSize(),
                alignment = alignment,
                contentScale = contentScale,
            )
        }
    }
}
