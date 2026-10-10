package com.srctool.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.create

/**
 * Applied to the root project: creates the `srctool { }` extension the other convention plugins read,
 * and registers `checkModuleLayers` (run by `check`).
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("base")
        val srctool = extensions.create<SrctoolExtension>("srctool")

        // Every project-to-project dependency a build script declares, in any source set (main, test, platform-specific).
        // Only declaration buckets count: plugins also create internal configurations that reference other modules
        // (e.g. Kotlin's SwiftPM lock-file aggregation), which aren't dependencies of the module.
        val declared = Regex("(?i).*(api|implementation|compileOnly|runtimeOnly)$")
        val edges = provider {
            subprojects.flatMap { p ->
                p.configurations.filter { declared.matches(it.name) }.flatMap { c ->
                    c.dependencies.withType(ProjectDependency::class.java).map { Triple(p.path, it.path, c.name) }
                }.filter { (from, to) -> from != to }          // a module's tests may depend on the module itself
            }.map { (from, to, configuration) -> "$from -> $to [$configuration]" }.distinct().sorted()
        }
        val layerCheck = tasks.register("checkModuleLayers") {
            group = "verification"
            description = "Checks that every module depends only on modules of a lower layer (srctool.layers)."
            val layers = srctool.layers
            inputs.property("layers", layers)
            inputs.property("edges", edges)
            doLast {
                val layerOf = layers.get()
                val problems = edges.get().mapNotNull { edge ->
                    val (from, to) = edge.substringBefore(" [").split(" -> ")
                    val fromLayer = layerOf[from] ?: return@mapNotNull "$edge: $from has no layer in srctool.layers"
                    val toLayer = layerOf[to] ?: return@mapNotNull "$edge: $to has no layer in srctool.layers"
                    if (toLayer < fromLayer) null else "$edge: layer $fromLayer may not depend on layer $toLayer"
                }
                check(problems.isEmpty()) { "Module layer violations:\n" + problems.joinToString("\n") { "  $it" } }
            }
        }
        tasks.named("check") { dependsOn(layerCheck) }
    }
}
