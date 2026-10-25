package com.srctool.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposePlugin
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

            // skikoMain/skikoTest: the targets Compose draws with Skiko (JVM, iOS, wasmJs). Compose UI tests live in
            // skikoTest, because on the Android host they'd need Robolectric.
            applyDefaultHierarchyTemplate {
                common {
                    group("skiko") {
                        withJvm()
                        group("ios") { withIos() }
                        withWasmJs()
                    }
                }
            }

            sourceSets.getByName("commonMain").dependencies {
                implementation(compose.findLibrary("runtime").get())
                implementation(compose.findLibrary("ui").get())
                implementation(compose.findLibrary("foundation").get())
            }
            // Compose UI tests on the JVM render through Skia, whose native library comes with the desktop runtime.
            sourceSets.matching { it.name == "jvmTest" }.configureEach {
                dependencies { implementation(ComposePlugin.DesktopDependencies.currentOs) }
            }
        }
    }
}
