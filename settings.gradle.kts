rootProject.name = "camouflage-kotlin"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("kotlinLibs") { from(files("gradle/kotlin.versions.toml")) }      // kotlin, compose compiler, datetime
        create("composeLibs") { from(files("gradle/compose.versions.toml")) }    // compose multiplatform, hot reload
        create("androidLibs") { from(files("gradle/android.versions.toml")) }    // AGP, SDK levels, activity
        create("toolLibs") { from(files("gradle/tools.versions.toml")) }         // materialkolor, publish, dokka, detekt, kover
    }
}

include(
    ":camouflage-core", ":camouflage-skin-minimal", ":camouflage-navigation3", ":camouflage-skin-testing", ":camouflage-bom",
    ":sample:shared", ":sample:androidApp",
)
