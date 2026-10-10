plugins {
    `java-platform`
    id("com.srctool.publish")
}

description = "Pins camouflage-core, the skins and camouflage-navigation3 to versions tested together."

dependencies {
    constraints {
        rootProject.subprojects
            .filter { it.name.startsWith("camouflage-") && it.name != project.name }
            .forEach { api(it) }
    }
}
