package com.srctool.gradle

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

/**
 * Maven Central publishing (Central Portal) with the srctool POM. For Kotlin libraries, also Dokka and
 * ABI validation: a published module's public API is dumped, and `checkKotlinAbi` (run by `check`) fails on any change
 * until `updateKotlinAbi` is run and the dump committed.
 * Coordinates are project.group : project.name : project.version, so `-Pversion=` from the release tag decides the version.
 * Signing applies when a key is configured.
 */
class PublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.vanniktech.maven.publish")
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            pluginManager.apply("org.jetbrains.dokka")
            extensions.configure<KotlinMultiplatformExtension> {
                @OptIn(ExperimentalAbiValidation::class)
                abiValidation()
            }
        }
        val srctool = rootProject.extensions.getByType<SrctoolExtension>()

        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
            coordinates(project.group.toString(), project.name, project.version.toString())
            pom {
                name.set(project.name)
                description.set(provider { project.description ?: srctool.pomDescription.get() })
                url.set(srctool.repoUrl)
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                scm { url.set(srctool.repoUrl) }
                developers {
                    developer {
                        id.set("srctool")
                        name.set("SRC Tool")
                        email.set("contact@srctool.com")
                    }
                }
            }
        }
    }
}
