plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
}

description = "Test-only checks for any Camouflage skin: renderers apply behavior, and no literals. Filled in at M3."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
        }
    }
}
