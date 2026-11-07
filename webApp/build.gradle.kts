// SPDX-License-Identifier: MPL-2.0

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import java.io.StringReader
import java.net.URI
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

// The page's content security policy may connect only to this backend. Read the same way as the shared module's
// BACKEND_BASE_URL: a Gradle property in CI, otherwise local.properties.
val backendBaseUrl: String =
    providers
        .gradleProperty("BACKEND_BASE_URL")
        .orElse(
            providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.map {
                Properties().apply { load(StringReader(it)) }.getProperty("BACKEND_BASE_URL").orEmpty()
            },
        ).orNull
        .orEmpty()
// Where the site is served: `/` on a custom domain, `/<repository>/` on a default GitHub Pages address.
val webBasePath: String =
    providers
        .gradleProperty("WEB_BASE_PATH")
        .orNull
        ?.takeIf(String::isNotBlank)
        ?.let { path -> "/" + path.trim('/') + "/" }
        ?.replace("//", "/")
        ?: "/"
val backendOrigin: String =
    backendBaseUrl.takeIf(String::isNotBlank)?.let { url ->
        val uri = URI(url.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrEmpty()) { "BACKEND_BASE_URL must be an HTTPS URL" }
        "https://${uri.host}${if (uri.port == -1) "" else ":${uri.port}"}"
    } ?: ""

// Startup-critical Compose resources that the page preloads: the app's fonts and its strings. Each key is the served
// path (the resources package is the shared module's `packageOfResClass`); each value is the source file that produces
// it. The build fails when a source is missing, so a rename never leaves the page preloading a file that 404s.
val startupResourceSources =
    mapOf(
        "font/poppins_regular.ttf" to "font/poppins_regular.ttf",
        "font/poppins_medium.ttf" to "font/poppins_medium.ttf",
        "font/poppins_semibold.ttf" to "font/poppins_semibold.ttf",
        "values/strings.commonMain.cvr" to "values/strings.xml",
    )
val resourcePreloads: String =
    startupResourceSources
        .map { (servedPath, sourcePath) ->
            val source = rootProject.file("shared/src/commonMain/composeResources/$sourcePath")
            require(source.isFile) { "Preloaded Compose resource source is missing: $source" }
            // Matches configureVersionedWebResources(): same path and `v` query, fetched in CORS mode.
            val href = "composeResources/dev.vladleesi.braindanceapp.resources/$servedPath?v=$version"
            """    <link rel="preload" href="$href" as="fetch" crossorigin>"""
        }.joinToString("\n")

kotlin {
    val bundleName = webBundleName
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = bundleName
                // The default development source maps wrap each module in eval(), which the page's content security
                // policy blocks, so the dev server's app would never start. Separate .map files need no eval.
                // Production already uses them.
                if (mode == KotlinWebpackConfig.Mode.DEVELOPMENT) {
                    devtool = WebpackDevtool.SOURCE_MAP
                }
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
    val origin = backendOrigin
    val basePath = webBasePath
    val preloads = resourcePreloads
    inputs.property("webBundleName", bundleName)
    inputs.property("backendOrigin", origin)
    inputs.property("webBasePath", basePath)
    inputs.property("resourcePreloads", preloads)
    val placeholders =
        mapOf(
            "webBundleName" to bundleName,
            "backendOrigin" to origin,
            "webBasePath" to basePath,
            "resourcePreloads" to preloads,
        )
    filesMatching("index.html") {
        expand(placeholders)
    }
    // GitHub Pages serves 404.html for any unknown path, so deep links such as /game/42 load the same page.
    from("src/wasmJsMain/resources/index.html") {
        rename { "404.html" }
        expand(placeholders)
    }
}
