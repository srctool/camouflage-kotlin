plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("com.srctool.publish")
}

description = "Camouflage core: the components, the Theme and the Skin contract. Visually neutral, no Material."

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(toolLibs.material.color.utilities)   // CamoTheme.fromSeed (M1)
            implementation(kotlinLibs.datetime)                  // the date and time pickers' types (M5, ADR-35)
        }
        commonTest.dependencies {
            implementation(composeLibs.ui.test)
        }
    }
}
