plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
    alias(toolLibs.plugins.roborazzi)
}

description = "The Material 3 skin, built on Compose foundation and material-ripple: Paper surfaces, Ripple feedback, M3 proportions. No material3."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
            implementation(composeLibs.ripple)   // the touch ripple node (createRippleModifierNode)
        }
        commonTest.dependencies {
            implementation(projects.camouflageSkinTesting)   // test-only, same layer (ADR-27)
        }
        jvmTest.dependencies {
            implementation(toolLibs.roborazzi.compose.desktop) // screenshot goldens (src/jvmTest/snapshots)
        }
    }
}
