package com.srctool.gradle

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A Kotlin Multiplatform library on every target: Android, iOS (arm64 + simulator arm64), JVM desktop, wasmJs.
 * Explicit API mode, Detekt and Kover are on. ABI validation comes with `com.srctool.publish`.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("dev.detekt")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        val srctool = rootProject.extensions.getByType<SrctoolExtension>()
        val android = extensions.getByType<VersionCatalogsExtension>().named("androidLibs")
        configureKotlin(srctool, android)
        configureDetekt()
    }

    private fun Project.configureKotlin(srctool: SrctoolExtension, android: VersionCatalog) {
        val suffix = name.removePrefix(srctool.modulePrefix.get()).replace('-', '.')
        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()

            // AGP's KMP library target; its DSL name changed between AGP versions ("androidLibrary", then "android").
            val kmpExtensions = (this as ExtensionAware).extensions
            val androidTargetName = listOf("android", "androidLibrary").first { kmpExtensions.findByName(it) != null }
            kmpExtensions.configure<KotlinMultiplatformAndroidLibraryTarget>(androidTargetName) {
                namespace = srctool.namespacePrefix.get() + "." + suffix
                compileSdk = android.findVersion("compileSdk").get().requiredVersion.toInt()
                minSdk = android.findVersion("minSdk").get().requiredVersion.toInt()
                withHostTest {}                                  // commonTest also runs as Android host (JVM) tests
            }

            iosArm64()
            iosSimulatorArm64()
            jvm()
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs { browser() }

            compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }

            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    // One `detekt` task (run by `check`) over every source set: its default, src/main/kotlin, doesn't exist in KMP.
    // One shared config and a baseline per module; a missing baseline file is simply ignored.
    private fun Project.configureDetekt() {
        extensions.configure<DetektExtension> {
            source.setFrom(layout.projectDirectory.dir("src"))
            buildUponDefaultConfig.set(true)
            baseline.set(layout.projectDirectory.file("detekt-baseline.xml"))
            rootProject.layout.projectDirectory.file("config/detekt/detekt.yml").asFile
                .takeIf { it.exists() }?.let { config.setFrom(it) }
        }
    }
}
