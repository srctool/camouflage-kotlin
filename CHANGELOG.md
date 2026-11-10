# Changelog (kotlin-lib)

All notable changes for the Kotlin library are documented here.

This changelog follows Keep a Changelog and Semantic Versioning.

## [Unreleased]

## [0.1.0] - 2026-11-06

The first release, on Maven Central (`com.srctool.camouflage`), with the API reference attached to the GitHub Release.

- **Components:** `camouflage-core` has the theme (colors from a seed, type, shape, spacing, elevation, motion, component overrides, token sets, contrast validation, animated changes), the skin contract, the provider and environment, and 52 components: text and icons, buttons, inputs and forms, structure, overlays, feedback, navigation, popovers and toolbars, data components and adaptive layout.
- **Skins:** Minimal, Fluent (Solid, Acrylic and Mica), Material 3, Cupertino (Classic and Liquid Glass) and Carbon.
- **Charts:** `camouflage-charts` (line, area, bar, pie, donut and sparkline) that follow the skin.
- **Navigation 3:** `camouflage-navigation3`.
- **Testing:** `camouflage-skin-testing` with accessibility checks over every gallery (a name, a role or state and a 44 dp target; 200% text and right to left). Tree toggles and icon-only navigation items are named for screen readers.
- **Build:** Android, iOS, JVM desktop and wasmJs; the `com.srctool.*` convention plugins, ABI validation, Detekt, Kover and the module-layer check; the sample catalog on all four platforms.
