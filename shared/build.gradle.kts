// SPDX-License-Identifier: MPL-2.0

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.StringReader
import java.util.Properties

val appNamespace = "dev.vladleesi.braindanceapp"
val backendBaseUrlProperty = "BACKEND_BASE_URL"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val frontendVersionProperties =
    Properties().apply {
        rootProject.file("version.xcconfig").inputStream().use(::load)
    }

kotlin {
    android {
        namespace = "$appNamespace.shared"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.components.resources)
            // Navigation
            implementation(libs.compose.navigation3)
            // View Model
            implementation(libs.compose.viewmodel)
            implementation(libs.compose.lifecycle)
            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Datetime
            implementation(libs.kotlinx.datetime)
            // Serialization
            implementation(libs.kotlinx.serialization.json)
            // Coroutines
            implementation(libs.kotlinx.coroutines)
            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            // Logger
            implementation(libs.logging.napier)
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
        }
        commonTest.dependencies {
            implementation(libs.unit.tests.kotlin)
        }
        androidMain.dependencies {
            // Network
            implementation(libs.ktor.client.okhttp)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.unit.tests.junit)
            }
        }
        val iosArm64Main = getByName("iosArm64Main")
        val iosSimulatorArm64Main = getByName("iosSimulatorArm64Main")
        iosMain.dependencies {
            // Network
            implementation(libs.ktor.client.darwin)
        }
        wasmJsMain.dependencies {
            // Network
            implementation(libs.ktor.client.js)
        }
        val iosArm64Test = getByName("iosArm64Test")
        val iosSimulatorArm64Test = getByName("iosSimulatorArm64Test")
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "$appNamespace.resources"
    generateResClass = always
}

buildkonfig {
    packageName = appNamespace

    defaultConfigs {
        buildConfigField(
            FieldSpec.Type.STRING,
            "APP_VERSION",
            frontendVersionProperties.getProperty("MARKETING_VERSION"),
        )
        val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
        val localBackendUrl =
            project.providers
                .fileContents(localPropertiesFile)
                .asText
                .map { contents ->
                    Properties()
                        .apply { load(StringReader(contents)) }
                        .getProperty(backendBaseUrlProperty)
                        .orEmpty()
                }
        val backendUrl =
            project.providers
                .gradleProperty(backendBaseUrlProperty)
                .orElse(localBackendUrl)
                .orNull
                .orEmpty()
        buildConfigField(FieldSpec.Type.STRING, backendBaseUrlProperty, backendUrl)
    }
}

tasks.named("detekt") {
    dependsOn("detektAndroidMain")
}

tasks.named<Detekt>("detektAndroidMain") {
    setSource(files("src/commonMain/kotlin", "src/androidMain/kotlin"))
    config.setFrom(rootProject.files("config/detekt-nullability.yml"))
}
