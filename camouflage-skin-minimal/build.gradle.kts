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
        jvmTest.dependencies {
            implementation(toolLibs.roborazzi.compose.desktop) // screenshot goldens (src/jvmTest/snapshots)
        }
    }
}
