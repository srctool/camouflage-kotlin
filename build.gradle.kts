plugins {
    id("com.srctool.root")
    alias(kotlinLibs.plugins.multiplatform) apply false
    alias(kotlinLibs.plugins.compose.compiler) apply false
    alias(androidLibs.plugins.android.application) apply false
    alias(androidLibs.plugins.android.kmp.library) apply false
    alias(composeLibs.plugins.compose.multiplatform) apply false
    alias(composeLibs.plugins.compose.hot.reload) apply false
    alias(toolLibs.plugins.vanniktech.publish) apply false
    alias(toolLibs.plugins.dokka) apply false
    alias(toolLibs.plugins.detekt) apply false
    alias(toolLibs.plugins.kover) apply false
}

srctool {
    namespacePrefix = "com.srctool.camouflage"
    modulePrefix = "camouflage-"
    repoUrl = "https://github.com/srctool/camouflage-kotlin"
    pomDescription = "Camouflage: components whose look comes from a swappable Skin and Theme."
    // Dependency layers (see docs: Roadmap, ADR-27). A module may depend only on lower layers.
    layers = mapOf(
        ":camouflage-core" to 0,
        ":camouflage-skin-minimal" to 1,
        ":camouflage-navigation3" to 1,
        ":camouflage-skin-testing" to 1,
        ":camouflage-bom" to 3,
        ":sample:shared" to 3,
        ":sample:androidApp" to 4,                     // the app shell around :sample:shared
    )
}
