plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.srctool.camouflage.sample"
    compileSdk = androidLibs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.srctool.camouflage.sample"
        minSdk = androidLibs.versions.minSdk.get().toInt()
        targetSdk = androidLibs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(projects.sample.shared)
    implementation(androidLibs.activity.compose)
}
