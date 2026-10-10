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

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework { baseName = "SampleShared" }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable() }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.camouflageCore)
            implementation(projects.camouflageSkinMinimal)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)   // the desktop runtime for the OS building it
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.srctool.camouflage.sample.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"   // Skiko loads its native library (JDK 24+ warns otherwise)
    }
}
