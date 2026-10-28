plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
}

description = "Navigation 3 integration for Camouflage: dialog and bottom-sheet scene strategies, results."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
            api(composeLibs.navigation3.ui)                      // SceneStrategy, NavDisplay (ADR-19)
        }
        commonTest.dependencies {
            implementation(composeLibs.ui.test)
            implementation(projects.camouflageSkinMinimal)              // a real skin for the scenes (test-only, same layer)
            implementation(composeLibs.lifecycle.viewmodel.navigation3) // the ViewModel entry decorator
        }
    }
}
