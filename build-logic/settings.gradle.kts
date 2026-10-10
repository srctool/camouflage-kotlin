rootProject.name = "build-logic"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("kotlinLibs") { from(files("../gradle/kotlin.versions.toml")) }
        create("composeLibs") { from(files("../gradle/compose.versions.toml")) }
        create("androidLibs") { from(files("../gradle/android.versions.toml")) }
        create("toolLibs") { from(files("../gradle/tools.versions.toml")) }
    }
}
