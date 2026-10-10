// The srctool convention plugins. Plugin ids are generic (com.srctool.*) and nothing here names Camouflage:
// this folder is extracted into a published convention-plugin project after the first release (see docs: Project Setup).
plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(kotlinLibs.gradle.plugin)
    compileOnly(kotlinLibs.compose.compiler.gradle.plugin)
    compileOnly(androidLibs.gradle.plugin)
    compileOnly(composeLibs.gradle.plugin)
    compileOnly(toolLibs.vanniktech.publish.gradle.plugin)
    compileOnly(toolLibs.dokka.gradle.plugin)
    compileOnly(toolLibs.detekt.gradle.plugin)
    compileOnly(toolLibs.kover.gradle.plugin)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("root") {
            id = "com.srctool.root"
            implementationClass = "com.srctool.gradle.RootConventionPlugin"
        }
        register("kmpLibrary") {
            id = "com.srctool.kmp.library"
            implementationClass = "com.srctool.gradle.KmpLibraryConventionPlugin"
        }
        register("compose") {
            id = "com.srctool.compose"
            implementationClass = "com.srctool.gradle.ComposeConventionPlugin"
        }
        register("publish") {
            id = "com.srctool.publish"
            implementationClass = "com.srctool.gradle.PublishConventionPlugin"
        }
    }
}
