import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("com.srctool.kmp.library")
    id("com.srctool.compose")
    id("org.jetbrains.compose.hot-reload")
}

kotlin {
    // The sample is an app, not a library: no explicit API mode.
    explicitApi = null

    // The AppIcon drawables are compose resources: package them into the Android app (the library target skips
    // Android resources unless asked).
    val kmpExtensions = (this as ExtensionAware).extensions
    val androidTarget = listOf("android", "androidLibrary").first { kmpExtensions.findByName(it) != null }
    kmpExtensions.configure<KotlinMultiplatformAndroidLibraryTarget>(androidTarget) {
        androidResources { enable = true }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework { baseName = "SampleShared" }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable() }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.camouflageCore)
            implementation(projects.camouflageCharts)
            implementation(projects.camouflageSkinMinimal)
            implementation(projects.camouflageSkinFluent)
            implementation(projects.camouflageSkinMaterial)
            implementation(projects.camouflageSkinCupertino)
            implementation(projects.camouflageNavigation3)
            implementation(composeLibs.lifecycle.viewmodel.navigation3) // each overlay entry's own ViewModel
            implementation(composeLibs.resources)       // the AppIcon drawables (Res)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)   // the desktop runtime for the OS building it
        }
    }
}

compose.resources {
    packageOfResClass = "com.srctool.camouflage.sample.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "com.srctool.camouflage.sample.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"   // Skiko loads its native library (JDK 24+ warns otherwise)
    }
}
