// SPDX-License-Identifier: MPL-2.0

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.buildkonfig) apply false
}

val detektTaskName = "detekt"

allprojects {
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
    dependsOn(
        ":backend:detektMain",
        ":backend:detektTest",
        ":shared:detektAndroidMain",
    )
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
