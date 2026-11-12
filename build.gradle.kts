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
}

val detektTaskName = "detekt"

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
