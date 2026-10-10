package com.srctool.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Compose Multiplatform and the Compose compiler, with the base Compose dependencies (runtime, ui, foundation). */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val compose = extensions.getByType<VersionCatalogsExtension>().named("composeLibs")
        extensions.configure<KotlinMultiplatformExtension> {
            // Compose UI tests on wasmJs run from a webpack bundle, which needs an executable binary (CMP-4906).
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs { binaries.executable() }

            sourceSets.getByName("commonMain").dependencies {
                implementation(compose.findLibrary("runtime").get())
                implementation(compose.findLibrary("ui").get())
                implementation(compose.findLibrary("foundation").get())
            }
        }
    }
}
