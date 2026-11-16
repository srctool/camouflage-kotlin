plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
    alias(toolLibs.plugins.roborazzi)
}

description = "The Minimal skin: Camouflage's own neutral dialect. Draws everything itself, no Material."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
        }
        commonTest.dependencies {
            implementation(projects.camouflageSkinTesting)   // test-only, same layer (ADR-27)
        }
        getByName("androidHostTest").dependencies {
            implementation(toolLibs.roborazzi.android)
            implementation(toolLibs.roborazzi.compose)
            implementation(toolLibs.robolectric)
            implementation(toolLibs.junit)
            implementation(composeLibs.ui.test.junit4)
            implementation(composeLibs.ui.test.manifest)
        }
        jvmTest.dependencies {
            implementation(toolLibs.roborazzi.compose.desktop) // screenshot goldens (src/jvmTest/snapshots)
        }
    }
}
