package com.srctool.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** Functional tests for `checkModuleLayers` on a tiny fixture: `:base` (layer 0) and `:top` (layer 1). */
class ModuleLayersTest {
    @Test
    fun dependencyOnLowerLayerPasses() {
        val project = fixture(topDependsOn = ":base", baseDependsOn = null)
        val result = runner(project).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkModuleLayers")?.outcome)
    }

    @Test
    fun dependencyOnHigherLayerFails() {
        val project = fixture(topDependsOn = null, baseDependsOn = ":top")
        val result = runner(project).buildAndFail()
        assertContains(result.output, ":base -> :top [implementation]: layer 0 may not depend on layer 1")
    }

    @Test
    fun testOnlyDependencyOnSameLayerFails() {
        val project = fixture(topDependsOn = null, baseDependsOn = null, extraModule = ":peer")
        File(project, "top/build.gradle.kts").appendText("dependencies { testImplementation(project(\":peer\")) }\n")
        val result = runner(project).buildAndFail()
        assertContains(result.output, ":top -> :peer [testImplementation]: layer 1 may not depend on layer 1")
    }

    @Test
    fun undeclaredConfigurationIsIgnored() {
        val project = fixture(topDependsOn = null, baseDependsOn = null)
        File(project, "base/build.gradle.kts").appendText(
            "configurations.create(\"toolingAggregate\")\ndependencies { \"toolingAggregate\"(project(\":top\")) }\n",
        )
        val result = runner(project).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkModuleLayers")?.outcome)
    }

    @Test
    fun moduleWithoutLayerFails() {
        val project = fixture(topDependsOn = ":base", baseDependsOn = null, extraModule = ":stray", strayHasLayer = false)
        File(project, "stray/build.gradle.kts").appendText("dependencies { implementation(project(\":base\")) }\n")
        val result = runner(project).buildAndFail()
        assertContains(result.output, ":stray has no layer in srctool.layers")
    }

    private fun runner(project: File) = GradleRunner.create()
        .withProjectDir(project)
        .withPluginClasspath()
        .withArguments("check", "--stacktrace")

    private fun fixture(
        topDependsOn: String?,
        baseDependsOn: String?,
        extraModule: String? = null,
        strayHasLayer: Boolean = true,
    ): File {
        val dir = createTempDirectory("module-layers").toFile()
        val modules = listOfNotNull(":base", ":top", extraModule)
        File(dir, "settings.gradle.kts").writeText("include(${modules.joinToString { "\"$it\"" }})\n")
        val extraLayer = extraModule?.takeIf { strayHasLayer }?.let { "\"$it\" to 1," }.orEmpty()
        File(dir, "build.gradle.kts").writeText(
            """
            plugins { id("com.srctool.root") }
            srctool { layers.set(mapOf(":base" to 0, ":top" to 1, $extraLayer)) }
            """.trimIndent(),
        )
        modules.forEach { path ->
            val deps = when (path) {
                ":top" -> topDependsOn
                ":base" -> baseDependsOn
                else -> null
            }
            File(dir, path.removePrefix(":")).mkdirs()
            File(dir, path.removePrefix(":") + "/build.gradle.kts").writeText(
                "plugins { `java-library` }\n" +
                    (deps?.let { "dependencies { implementation(project(\"$it\")) }\n" } ?: ""),
            )
        }
        return dir
    }
}
