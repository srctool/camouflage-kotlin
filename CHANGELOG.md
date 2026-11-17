# Changelog (kotlin-lib)

All notable changes for the Kotlin library are documented here.

This changelog follows Keep a Changelog and Semantic Versioning.

## [Unreleased]

### Added

- **Split panes:** `CamoSplitPane(axis, panes, state)` lays out two or more panes side by side or stacked with a draggable divider between each pair. The layout is app-owned `CamoSplitState` (pane sizes as fractions, so a saved layout restores on any window size, and which panes are collapsed; `snapshot()` and `apply()` for persistence); `CamoPane` has `minSize` and `maxSize` in dp and can be `collapsible`. A divider is focusable and adjustable by a screen reader ("Resize panel, 35 percent"); arrow keys resize (Shift for a larger step), Home and End go to a limit, Enter collapses or restores, a double-click resets, and right to left mirrors a horizontal split. **The skin contract grows by one renderer, `splitPane` (`SplitPaneRenderer`), so every skin, including your own, must add it** (ADR-45): the five skins draw a hairline divider that darkens on hover and takes the accent while dragged.
- **Menu submenus:** `CamoMenuSubmenu(text, items)` is a new `CamoMenuEntry`: a row that opens a nested menu beside it (Reset ▸ Soft, Mixed, Hard). Hover opens it after 250 ms, a click, Enter and the arrow toward the child open it at once and focus its first item; the arrow back and Esc close only the submenu, and a leaf closes every level. It flips to the start side near the window's edge and mirrors in right-to-left. `MenuItemState` gains `hasSubmenu` and `submenuOpen`, which every skin draws as a trailing chevron and an open row. A menu with submenus lays out its levels in one window-sized popup layer; menus without them are unchanged.
- **Carbon density:** `carbonTheme(density = CarbonDensity.Md)` sets every control's size (`Xs` 24, `Sm` 32, `Md` 40, `Lg` 48) in one place: buttons, icon buttons, text and search fields, steppers, segmented controls, menu items, list rows, tree rows, data table rows, tabs, button groups and toolbar actions. `null` follows the input: a pointer gets `Md`, fingers get `Lg`. A touch target stays 48.

### Changed

- **Carbon's default sizes change:** a pointer now draws 40 (it drew 48 for most controls), and fields, menus, tabs and similar draw 48 with fingers (they drew 40). Pass `density = CarbonDensity.Lg` for the old buttons and rows.
- `CamoCarousel`'s `itemKey` must be a type Android can save (a `String` or a number); a `Color.value` throws there. Documented.

### Fixed

- Text fields and search fields announce their name on TalkBack and VoiceOver (the label is now the editable node's content description).

## [0.1.0] - 2026-11-06

The first release, on Maven Central (`com.srctool.camouflage`), with the API reference attached to the GitHub Release.

- **Components:** `camouflage-core` has the theme (colors from a seed, type, shape, spacing, elevation, motion, component overrides, token sets, contrast validation, animated changes), the skin contract, the provider and environment, and 52 components: text and icons, buttons, inputs and forms, structure, overlays, feedback, navigation, popovers and toolbars, data components and adaptive layout.
- **Skins:** Minimal, Fluent (Solid, Acrylic and Mica), Material 3, Cupertino (Classic and Liquid Glass) and Carbon.
- **Charts:** `camouflage-charts` (line, area, bar, pie, donut and sparkline) that follow the skin.
- **Navigation 3:** `camouflage-navigation3`.
- **Testing:** `camouflage-skin-testing` with accessibility checks over every gallery (a name, a role or state and a 44 dp target; 200% text and right to left). Tree toggles and icon-only navigation items are named for screen readers.
- **Build:** Android, iOS, JVM desktop and wasmJs; the `com.srctool.*` convention plugins, ABI validation, Detekt, Kover and the module-layer check; the sample catalog on all four platforms.
