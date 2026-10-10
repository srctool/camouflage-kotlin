plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
}

description = "Navigation 3 integration for Camouflage: dialog and bottom-sheet scene strategies, results. Filled in at M6."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
        }
    }
}
