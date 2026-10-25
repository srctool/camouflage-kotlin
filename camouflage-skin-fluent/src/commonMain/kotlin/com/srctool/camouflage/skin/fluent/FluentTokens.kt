package com.srctool.camouflage.skin.fluent

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.srctool.camouflage.core.theme.CamoTokenSet

/**
 * Fluent's explicit state steps, which core's roles don't cover: Fluent shows hover and press by swapping to
 * the next token, not with an overlay. [fluentTheme] adds them to its theme; renderers read them with
 * `theme.tokens<FluentTokens>()`.
 */
@Immutable
public data class FluentTokens(
    val brandBackground: Color,
    val brandBackgroundHover: Color,
    val brandBackgroundPressed: Color,
    /** Text and icons on [brandBackground] (white in both modes). */
    val neutralForegroundOnBrand: Color,
    val neutralBackground1Hover: Color,
    val neutralBackground1Pressed: Color,
    val subtleBackgroundHover: Color,
    val subtleBackgroundPressed: Color,
    val neutralForeground2Hover: Color,
    val neutralStroke1: Color,
    val neutralStroke1Hover: Color,
    val neutralStroke1Pressed: Color,
    /** Dividers. */
    val neutralStroke2: Color,
    val brandForegroundLinkHover: Color,
    val brandForegroundLinkPressed: Color,
    /** The focus indicator's inner stroke (1 dp, light in light mode). */
    val strokeFocus1: Color,
    /** The focus indicator's outer stroke (2 dp, dark in light mode). */
    val strokeFocus2: Color,
    val shadowAmbient: Color,
    val shadowKey: Color,
) : CamoTokenSet<FluentTokens> {
    override fun lerp(to: FluentTokens, fraction: Float): FluentTokens = FluentTokens(
        brandBackground = lerp(brandBackground, to.brandBackground, fraction),
        brandBackgroundHover = lerp(brandBackgroundHover, to.brandBackgroundHover, fraction),
        brandBackgroundPressed = lerp(brandBackgroundPressed, to.brandBackgroundPressed, fraction),
        neutralForegroundOnBrand = lerp(neutralForegroundOnBrand, to.neutralForegroundOnBrand, fraction),
        neutralBackground1Hover = lerp(neutralBackground1Hover, to.neutralBackground1Hover, fraction),
        neutralBackground1Pressed = lerp(neutralBackground1Pressed, to.neutralBackground1Pressed, fraction),
        subtleBackgroundHover = lerp(subtleBackgroundHover, to.subtleBackgroundHover, fraction),
        subtleBackgroundPressed = lerp(subtleBackgroundPressed, to.subtleBackgroundPressed, fraction),
        neutralForeground2Hover = lerp(neutralForeground2Hover, to.neutralForeground2Hover, fraction),
        neutralStroke1 = lerp(neutralStroke1, to.neutralStroke1, fraction),
        neutralStroke1Hover = lerp(neutralStroke1Hover, to.neutralStroke1Hover, fraction),
        neutralStroke1Pressed = lerp(neutralStroke1Pressed, to.neutralStroke1Pressed, fraction),
        neutralStroke2 = lerp(neutralStroke2, to.neutralStroke2, fraction),
        brandForegroundLinkHover = lerp(brandForegroundLinkHover, to.brandForegroundLinkHover, fraction),
        brandForegroundLinkPressed = lerp(brandForegroundLinkPressed, to.brandForegroundLinkPressed, fraction),
        strokeFocus1 = lerp(strokeFocus1, to.strokeFocus1, fraction),
        strokeFocus2 = lerp(strokeFocus2, to.strokeFocus2, fraction),
        shadowAmbient = lerp(shadowAmbient, to.shadowAmbient, fraction),
        shadowKey = lerp(shadowKey, to.shadowKey, fraction),
    )
}
