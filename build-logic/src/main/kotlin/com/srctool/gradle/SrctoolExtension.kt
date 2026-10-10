package com.srctool.gradle

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/** Project-specific values for the srctool convention plugins, set once in each project's root build. */
abstract class SrctoolExtension {
    /** The Android namespace prefix; a module's namespace is the prefix plus its name without [modulePrefix]. */
    abstract val namespacePrefix: Property<String>

    /** The prefix every library module name starts with (e.g. "camouflage-"), dropped when building namespaces. */
    abstract val modulePrefix: Property<String>

    /** The public repository URL, used in POMs. */
    abstract val repoUrl: Property<String>

    /** A one-line description for POMs of modules that don't set their own. */
    abstract val pomDescription: Property<String>

    /**
     * The dependency layer of every module, by project path (":core" to 0). A module may depend only on modules
     * of a lower layer, never on its own layer or a higher one. `checkModuleLayers` enforces it.
     */
    abstract val layers: MapProperty<String, Int>
}
