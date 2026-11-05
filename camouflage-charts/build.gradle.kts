plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
    alias(toolLibs.plugins.roborazzi)
}

description = "Camouflage charts: line, bar, pie and sparkline charts drawn without a chart library (ADR-40)."

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.camouflageCore)
        }
        commonTest.dependencies {
            implementation(composeLibs.ui.test)
            implementation(projects.camouflageSkinMinimal)   // a real skin for the scenes (test-only, same layer)
            implementation(projects.camouflageSkinFluent)
            implementation(projects.camouflageSkinMaterial)
            implementation(projects.camouflageSkinCupertino)
            implementation(projects.camouflageSkinCarbon)
        }
        jvmTest.dependencies {
            implementation(projects.camouflageSkinTesting)    // the golden font (test-only, same layer)
            implementation(toolLibs.roborazzi.compose.desktop) // screenshot goldens (src/jvmTest/snapshots)
        }
    }
}
