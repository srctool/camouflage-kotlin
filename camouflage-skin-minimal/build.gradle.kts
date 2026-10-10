plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
}

description = "The Minimal skin: Camouflage's own neutral dialect. Draws everything itself, no Material."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
        }
    }
}
