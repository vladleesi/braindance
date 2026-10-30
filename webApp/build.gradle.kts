// SPDX-License-Identifier: MPL-2.0

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.language.jvm.tasks.ProcessResources
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val frontendVersionProperties =
    Properties().apply {
        rootProject.file("version.xcconfig").inputStream().use(::load)
    }
version = frontendVersionProperties.getProperty("MARKETING_VERSION")
val webBundleName = "webApp-$version.js"

kotlin {
    val bundleName = webBundleName
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = bundleName
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui.multiplatform)
        }
    }
}

tasks.named<ProcessResources>("wasmJsProcessResources") {
    val bundleName = webBundleName
    inputs.property("webBundleName", bundleName)
    filesMatching("index.html") {
        expand("webBundleName" to bundleName)
    }
}
