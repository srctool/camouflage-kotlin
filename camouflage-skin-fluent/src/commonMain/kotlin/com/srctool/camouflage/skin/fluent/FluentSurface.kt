package com.srctool.camouflage.skin.fluent

/**
 * The surface treatments Fluent offers. `Acrylic` makes flyouts, menus and dialogs frosted glass over the app
 * (wrap the app's content in [FluentBackdrop]); other containers stay `Solid`. `Mica` lets the Windows 11 window
 * backdrop show through the base layers and the navigation chrome, and is `Solid` elsewhere ([FluentMica]).
 */
public enum class FluentSurface { Solid, Acrylic, Mica }
