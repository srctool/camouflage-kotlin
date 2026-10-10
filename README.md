[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](LICENSE)
[![Docs](https://img.shields.io/badge/docs-Website-blue.svg)](https://camouflage-dev.srctool.com)

# camouflage-kotlin

Kotlin implementation of Camouflage (part of the SRC Tool). This repository is also included as the `kotlin-lib` submodule of the umbrella repository [srctool/camouflage](https://github.com/srctool/camouflage), which holds the documentation.

- Issues and pull requests for the Kotlin code go here, into `main`.
- Cross-platform design questions and documentation changes go to [srctool/camouflage](https://github.com/srctool/camouflage).

---

## Documentation

- Usage docs: https://camouflage.srctool.com
- Contributor docs (design, architecture, components, contributing): https://camouflage-dev.srctool.com

---

## Modules

| Module | Holds |
|---|---|
| `camouflage-core` | the components, `CamoTheme` and the Skin contract; visually neutral, no Material |
| `camouflage-skin-minimal` | the Minimal skin and `minimalTheme` |
| `camouflage-navigation3` | Navigation 3 integration: dialog and bottom-sheet scene strategies (M6) |
| `camouflage-skin-testing` | test-only checks for any skin (M3) |
| `camouflage-bom` | pins the modules above to versions tested together |
| `sample` | the sample app: `:sample:shared` (Android, iOS, desktop, web) and `:sample:androidApp` |
| `build-logic` | the `com.srctool.*` convention plugins |

Targets: Android, iOS (arm64, simulator arm64), JVM desktop and wasmJs. Status: milestone **M0** (skeleton): every module holds a placeholder until its milestone.

---

## Development

- Toolchain: JDK 17+, the Android SDK (`local.properties` → `sdk.dir`), Xcode for iOS.
- `./gradlew check`: tests on every target, `checkKotlinAbi`, Detekt, Kover and `checkModuleLayers` (the dependency layers).
- `./gradlew -p build-logic test`: the convention plugins' tests.
- After a deliberate public API change: `./gradlew updateKotlinAbi`, and commit the `api/` dumps.
- Sample: `./gradlew :sample:shared:run` (desktop, with Compose Hot Reload), `:sample:androidApp:installDebug` (Android), `:sample:shared:wasmJsBrowserDevelopmentRun` (web); iOS: open `sample/iosApp/iosApp.xcodeproj`.
- Branch from `main`, open a PR into `main`, squash merge; releases are tags on `main`. See CONTRIBUTING.md.

---

## Code of Conduct

Participation is governed by CODE_OF_CONDUCT.md. For sensitive reports, email contact@srctool.com.

---

## License

Licensed under the Apache License, Version 2.0 (see LICENSE).