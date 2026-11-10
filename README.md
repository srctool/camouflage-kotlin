[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.srctool.camouflage/camouflage-core.svg)](https://central.sonatype.com/search?q=com.srctool.camouflage)
[![Docs](https://img.shields.io/badge/docs-camouflage.srctool.com-blue.svg)](https://camouflage.srctool.com)

# camouflage-kotlin

Kotlin implementation of Camouflage (part of the SRC Tool). This repository is also included as the `kotlin-lib` submodule of the umbrella repository [srctool/camouflage](https://github.com/srctool/camouflage), which holds the documentation.

- Issues and pull requests for the Kotlin code go here, into `main`.
- Cross-platform design questions and documentation changes go to [srctool/camouflage](https://github.com/srctool/camouflage).

---

## Getting started

Camouflage is on Maven Central (group `com.srctool.camouflage`) for Android, iOS, desktop (JVM) and web (wasmJs). Add the BOM, the core and one skin:

```kotlin
commonMain.dependencies {
    implementation(project.dependencies.platform("com.srctool.camouflage:camouflage-bom:0.1.0"))
    implementation("com.srctool.camouflage:camouflage-core")
    implementation("com.srctool.camouflage:camouflage-skin-material") // or -minimal, -cupertino, -fluent, -carbon
}
```

Wrap the app once, then use components:

```kotlin
CamouflageProvider(skin = MaterialSkin, theme = materialTheme(seed = brand)) {
    CamoButton("Sign up", onClick = { signUp() })
}
```

---

## Documentation

- Usage docs (getting started, theming, skins, all 52 components): https://camouflage.srctool.com
- Contributor docs (design, architecture, contributing): https://camouflage-dev.srctool.com
- API reference: the Dokka archive attached to each GitHub release.

---

## Modules

| Module | Holds |
|---|---|
| `camouflage-core` | the components, `CamoTheme` and the Skin contract; visually neutral, no Material |
| `camouflage-skin-minimal`, `-material`, `-cupertino`, `-fluent`, `-carbon` | the five skins, each with its theme function |
| `camouflage-navigation3` | Navigation 3 integration: dialog and bottom-sheet scene strategies |
| `camouflage-charts` | line, area, bar, pie, donut and sparkline charts without a chart library, styled by the theme and a preset per skin |
| `camouflage-skin-testing` | test-only accessibility and layout checks for any skin |
| `camouflage-bom` | pins the modules above to versions tested together |
| `sample` | the sample catalog: `:sample:shared` (Android, iOS, desktop, web) and `:sample:androidApp` |
| `build-logic` | the `com.srctool.*` convention plugins |

Targets: Android, iOS (arm64, simulator arm64), JVM desktop and wasmJs. Status: **v0.1.0 released**; the API can still change before 1.0.

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