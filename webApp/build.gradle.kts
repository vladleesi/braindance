// SPDX-License-Identifier: MPL-2.0

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.WebpackDevtool
import java.io.StringReader
import java.net.URI
import java.util.Base64
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

// The page loads the bundle under this name in development. Production bundles are named by their content instead
// (webApp.<hash>.js), so browsers can cache them forever and a new build never reuses an old name.
val webBundleName = "webApp.js"
val hashedWebBundleName = "webApp.[contenthash].js"
val hashedWebBundlePattern = Regex("""webApp\.[0-9a-f]+\.js""")

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
// Where the site is served: `/` at the root of a domain (the default), or a sub-path such as `/braindance/`.
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

// The boot screen's icon, inlined into the page so it paints without waiting for another request.
val bootIconDataUri: String =
    file("src/wasmJsMain/resources/favicon.svg").let { icon ->
        require(icon.isFile) { "Boot icon source is missing: $icon" }
        "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(icon.readBytes())
    }

// The page marks where the production distribution adds preloads of the content-hashed Wasm modules.
val wasmPreloadMarker = "<!-- wasm-preloads -->"
val hashedWasmPattern = Regex("""[0-9a-f]+\.wasm""")

kotlin {
    val bundleName = webBundleName
    val hashedBundleName = hashedWebBundleName
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = if (mode == KotlinWebpackConfig.Mode.PRODUCTION) hashedBundleName else bundleName
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
    val bootIcon = bootIconDataUri
    inputs.property("webBundleName", bundleName)
    inputs.property("backendOrigin", origin)
    inputs.property("webBasePath", basePath)
    inputs.property("resourcePreloads", preloads)
    inputs.property("bootIconDataUri", bootIcon)
    val placeholders =
        mapOf(
            "webBundleName" to bundleName,
            "backendOrigin" to origin,
            "webBasePath" to basePath,
            "resourcePreloads" to preloads,
            "bootIconDataUri" to bootIcon,
        )
    filesMatching("index.html") {
        expand(placeholders)
    }
}

// The page is written before webpack names the production bundle and Wasm modules, so the distribution swaps in the
// hashed bundle name, preloads every hashed module, and limits the Pages worker to those modules. The build fails
// unless exactly one hashed bundle and at least one module with its Brotli copy exist, and the page has the
// development bundle name and the preload marker that they replace.
tasks.named<Sync>("wasmJsBrowserDistribution") {
    val bundleName = webBundleName
    val hashedBundlePattern = hashedWebBundlePattern
    val preloadMarker = wasmPreloadMarker
    val wasmPattern = hashedWasmPattern
    val basePath = webBasePath
    doLast {
        val bundles = destinationDir.listFiles { file -> hashedBundlePattern.matches(file.name) }.orEmpty()
        val bundle =
            bundles.singleOrNull()
                ?: error("Expected one hashed web bundle in $destinationDir, found ${bundles.map { it.name }}")
        val modules =
            destinationDir
                .listFiles { file -> wasmPattern.matches(file.name) }
                .orEmpty()
                .map { it.name }
                .sorted()
        check(modules.isNotEmpty()) { "Expected hashed Wasm modules in $destinationDir" }
        // webpack.config.d/brotli-wasm.js writes these copies; the Pages worker serves them.
        val uncompressed = modules.filterNot { module -> destinationDir.resolve("$module.br").isFile }
        check(uncompressed.isEmpty()) { "Expected Brotli copies of $uncompressed in $destinationDir" }
        // Pages runs _worker.js only for these exact paths, so every other request stays a free static asset.
        val workerRoutes = modules.joinToString(", ") { module -> "\"$basePath$module\"" }
        val routes = """{"version": 1, "include": [$workerRoutes], "exclude": []}"""
        destinationDir.resolve("_routes.json").writeText(routes + "\n")
        val page = destinationDir.resolve("index.html")
        val reference = """src="$bundleName""""
        val html = page.readText()
        check(html.contains(reference)) { "index.html does not load $bundleName" }
        check(html.contains(preloadMarker)) { "index.html has no $preloadMarker marker" }
        // The bundle fetches each module in CORS mode with same-origin credentials, which `crossorigin` matches.
        val modulePreloads =
            modules.joinToString("\n    ") { module ->
                """<link rel="preload" href="$module" as="fetch" crossorigin>"""
            }
        page.writeText(
            html
                .replace(reference, """src="${bundle.name}"""")
                .replace(preloadMarker, modulePreloads),
        )
    }
}
