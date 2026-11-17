// SPDX-License-Identifier: MPL-2.0

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension

// The Android Gradle Plugin and other build plugins bring library versions with published advisories into the build
// classpath. Constraints only raise versions that are present; the catalog's bundle explains each floor.
buildscript {
    dependencies {
        constraints {
            libs.bundles.tool.security.floors
                .get()
                .forEach { floor -> classpath(floor) }
        }
    }
}

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

val detektTaskName = "detekt"

/**
 * A fingerprint of what the Web app serves under `composeResources/`, used as the `v` query of those URLs (see the
 * shared module's `VersionedWebResources.kt` and the web module's preloads). The files keep their names across
 * releases, so the query is what lets browsers and the edge keep them as immutable: any change gives new URLs, and an
 * unchanged release keeps every cached copy.
 *
 * It covers every module's own resource files and the build scripts that configure their compilation, and the version
 * catalog, which pins the library resources and the Compose plugin that compiles the strings. Changing
 * [WEB_RESOURCES_FINGERPRINT_SALT] forces new URLs if a cached copy ever needs replacing without a content change.
 *
 * It is a [ValueSource] so the configuration cache stays correct: Gradle computes it again before reusing a cached
 * configuration and discards the cache when it differs. A plain directory walk in the script would miss added files.
 */
abstract class WebResourcesFingerprint : ValueSource<String, WebResourcesFingerprint.Parameters> {
    interface Parameters : ValueSourceParameters {
        val rootDirectory: DirectoryProperty
    }

    override fun obtain(): String {
        val root = parameters.rootDirectory.get().asFile
        // Every module's `src/<source set>/composeResources`, so a module that gains resources is covered too.
        val resourceDirs =
            root
                .listFiles { dir -> dir.isDirectory && !dir.name.startsWith(".") && dir.name != "build" }
                .orEmpty()
                .flatMap { module ->
                    module
                        .resolve("src")
                        .listFiles { dir -> dir.isDirectory }
                        .orEmpty()
                        .toList()
                }.map { sourceSet -> sourceSet.resolve("composeResources") }
                .filter { it.isDirectory }
        val resourceFiles =
            resourceDirs.flatMap { dir ->
                dir.walkTopDown().filter { it.isFile && !it.name.startsWith(".") }.toList()
            }
        // The build scripts of those modules configure how their resources are compiled.
        val buildScripts =
            resourceDirs
                .map { dir ->
                    dir.parentFile.parentFile.parentFile
                        .resolve("build.gradle.kts")
                }.filter { it.isFile }
                .distinct()
        val inputs = resourceFiles + buildScripts + root.resolve("gradle/libs.versions.toml")
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(WEB_RESOURCES_FINGERPRINT_SALT.toByteArray())
        // Sorted relative paths make the result independent of file system order and of the checkout location.
        inputs.sortedBy { it.relativeTo(root).invariantSeparatorsPath }.forEach { input ->
            digest.update(input.relativeTo(root).invariantSeparatorsPath.toByteArray())
            digest.update(0)
            digest.update(input.readBytes())
            digest.update(0)
        }
        // 48 bits: far more than enough to tell releases apart, and short in every resource URL.
        return digest.digest().take(FINGERPRINT_BYTES).joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val WEB_RESOURCES_FINGERPRINT_SALT = "1"
        const val FINGERPRINT_BYTES = 6
    }
}

extra["webResourcesFingerprint"] =
    providers
        .of(WebResourcesFingerprint::class) { parameters.rootDirectory.set(layout.projectDirectory) }
        .get()

plugins.withType<WasmYarnPlugin> {
    extensions.configure<WasmYarnRootExtension> {
        // GHSA-96hv-2xvq-fx4p: Kotlin's WebSocket dependency needs bounded fragment buffering.
        resolution("ws", libs.versions.ws.get())
    }
}

// Android lint, ktlint, and Kotlin's Swift export resolve their own tool classpaths with the same kinds of libraries.
// None of these classpaths ships in an app.
val toolClasspaths = setOf("androidLintTool", "ktlint", "swiftExportClasspath")

allprojects {
    configurations.matching { it.name in toolClasspaths }.configureEach {
        libs.bundles.tool.security.floors.get().forEach { floor ->
            dependencyConstraints.add(project.dependencies.constraints.create(floor))
        }
    }
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        extensions.configure<DetektExtension> {
            config.setFrom(rootProject.files("config/detekt-config.yml"))
        }
        configureDetektTasks(tasks)
    }
}

tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// The built-in nullable-call rules require the classpath supplied by these platform tasks.
tasks.named(detektTaskName) {
    dependsOn(":shared:detektAndroidMain")
}

fun configureDetektTasks(tasks: NamedDomainObjectContainer<Task>) {
    tasks.withType<Detekt>().configureEach {
        config.setFrom(rootProject.files("config/detekt-config.yml"))
        parallel = true
        autoCorrect = true

        reports {
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)

            html {
                required = true
                outputLocation = layout.buildDirectory.file("reports/detekt/${project.name}.html")
            }
        }
    }
    tasks.withType<Detekt> {
        if (name == detektTaskName) setSource(files(project.projectDir))
        exclude("**/build/**")
        val buildDir =
            project.layout.buildDirectory.asFile
                .get()
        exclude {
            val isInBuildDir = it.file.toPath().startsWith(buildDir.toPath())
            return@exclude isInBuildDir
        }
    }
}
