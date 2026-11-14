// SPDX-License-Identifier: MPL-2.0

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import java.io.StringReader
import java.util.Properties

val appNamespace = "dev.vladleesi.braindanceapp"
val backendBaseUrlProperty = "BACKEND_BASE_URL"
val apiFixturesProperty = "API_FIXTURES"

// Must match API_FIXTURES_BASE_URL in src/apiFixtures. The reserved .invalid domain never resolves.
val apiFixturesBaseUrl = "https://fixtures.invalid"

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

// A Gradle property (CI, -P) wins over the ignored root local.properties.
val localProperties =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { contents -> Properties().apply { load(StringReader(contents)) } }

fun configuredProperty(name: String): String =
    providers
        .gradleProperty(name)
        .orElse(localProperties.map { properties -> properties.getProperty(name).orEmpty() })
        .orNull
        .orEmpty()
        .trim()

val configuredBackendUrl = configuredProperty(backendBaseUrlProperty)

// Offline sample data for developer builds. Off unless explicitly enabled, and never combined with a real API, so a
// build that talks to an API can never contain or serve fixtures. Disabled builds do not compile the fixture code.
val apiFixtures =
    when (val value = configuredProperty(apiFixturesProperty)) {
        "", "false" -> false
        "true" -> true
        else -> error("$apiFixturesProperty must be true or false, not '$value'.")
    }
require(!apiFixtures || configuredBackendUrl.isEmpty()) {
    "Set either $apiFixturesProperty=true or $backendBaseUrlProperty, not both."
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
            // Logging reaches android.util.Log, whose host-test stubs otherwise throw "not mocked".
            isReturnDefaultValues = true
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
        commonMain {
            kotlin.srcDir(if (apiFixtures) "src/apiFixturesEnabled/kotlin" else "src/apiFixturesDisabled/kotlin")
            if (apiFixtures) {
                kotlin.srcDir("src/apiFixtures/kotlin")
                dependencies { implementation(libs.ktor.client.mock) }
            }
        }
        // Tests always cover the fixtures, so they keep working while the app build leaves them out.
        if (!apiFixtures) commonTest { kotlin.srcDir("src/apiFixtures/kotlin") }
        commonMain.dependencies {
            // Compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.navigation.suite)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.components.resources)
            // Navigation
            implementation(libs.compose.navigation3)
            implementation(libs.compose.navigation3.viewmodel)
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
            // Sharing
            implementation(libs.sharekit)
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
        }
        commonTest.dependencies {
            implementation(libs.unit.tests.kotlin)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
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
        // Compose UI tests run natively on iOS; Android host tests would need Robolectric.
        iosTest.dependencies {
            implementation(libs.compose.ui.test)
        }
        wasmJsMain.dependencies {
            // Network
            implementation(libs.ktor.client.js)
        }
        val iosArm64Test = getByName("iosArm64Test")
        val iosSimulatorArm64Test = getByName("iosSimulatorArm64Test")
    }
}

dependencies {
    // The IDE preview renderer runs on Android; commonMain only needs the annotation dependency.
    add("androidRuntimeClasspath", libs.compose.ui.tooling)
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
        buildConfigField(
            FieldSpec.Type.STRING,
            backendBaseUrlProperty,
            if (apiFixtures) apiFixturesBaseUrl else configuredBackendUrl,
        )
    }
}

tasks.named("detekt") {
    dependsOn("detektAndroidMain")
}

// ApiContractTest (Android host tests) compares the published API contract with the client's endpoints and models.
val apiContractFile =
    rootProject.layout.projectDirectory
        .file("docs/openapi.yaml")
        .asFile
tasks.withType<Test>().configureEach {
    inputs.file(apiContractFile).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("apiContractFile", apiContractFile.absolutePath)
}

// CI passes an iPhone simulator that exists on its runner; locally the Kotlin plugin's default device is used.
providers.gradleProperty("iosTestSimulator").orNull?.let { simulator ->
    tasks.withType<KotlinNativeSimulatorTest>().configureEach { device.set(simulator) }
}

// Stability reports for reviewing recomposition: ./gradlew :shared:compileKotlinIosSimulatorArm64
// -PcomposeCompilerReports=true writes them to shared/build/compose-compiler/.
if (providers.gradleProperty("composeCompilerReports").orNull == "true") {
    composeCompiler {
        reportsDestination.set(layout.buildDirectory.dir("compose-compiler/reports"))
        metricsDestination.set(layout.buildDirectory.dir("compose-compiler/metrics"))
    }
}

tasks.named<Detekt>("detektAndroidMain") {
    setSource(files("src/commonMain/kotlin", "src/androidMain/kotlin"))
    config.setFrom(rootProject.files("config/detekt-nullability.yml"))
}
