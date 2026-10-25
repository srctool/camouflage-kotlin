plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
}

description = "Test-only checks for any Camouflage skin: renderers apply behavior, and no literals."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
            api(composeLibs.ui.test)
            implementation(kotlin("test"))
        }
        commonTest.dependencies {
            implementation(projects.camouflageSkinMinimal)   // a real skin to break on purpose; test-only, same layer
        }
    }
}
