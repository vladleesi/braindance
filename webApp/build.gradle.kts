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
val webPageFile = "index.html"
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
// The public address of the site, for canonical links, link previews, and the sitemap. It defaults to the Braindance
// Web app, as the shared module's APP_LINK_ORIGIN does; independent hosts set WEB_PUBLIC_URL to their own origin.
val webPublicOrigin: String =
    providers
        .gradleProperty("WEB_PUBLIC_URL")
        .orNull
        ?.takeIf(String::isNotBlank)
        .let { url ->
            val uri = URI((url ?: "https://app.braindancegg.com").trim())
            require(uri.scheme == "https" && !uri.host.isNullOrEmpty()) { "WEB_PUBLIC_URL must be an HTTPS URL" }
            "https://${uri.host}${if (uri.port == -1) "" else ":${uri.port}"}"
        }
val webSiteUrl: String = webPublicOrigin + webBasePath.removeSuffix("/")

// HTTPS, or plain HTTP to this computer for an API run locally, as BackendClient accepts: such traffic never leaves it.
val backendOrigin: String =
    backendBaseUrl.takeIf(String::isNotBlank)?.let { url ->
        val uri = URI(url.trim())
        val localHttp = uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1")
        require((uri.scheme == "https" || localHttp) && !uri.host.isNullOrEmpty()) {
            "BACKEND_BASE_URL must be an HTTPS URL, or HTTP on localhost for an API run locally"
        }
        "${uri.scheme}://${uri.host}${if (uri.port == -1) "" else ":${uri.port}"}"
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
// The same `v` query the app adds to Compose resource URLs (shared's WEB_RESOURCES_FINGERPRINT).
val webResourcesFingerprint = rootProject.extra["webResourcesFingerprint"] as String
val resourcePreloads: String =
    startupResourceSources
        .map { (servedPath, sourcePath) ->
            val source = rootProject.file("shared/src/commonMain/composeResources/$sourcePath")
            require(source.isFile) { "Preloaded Compose resource source is missing: $source" }
            // Matches configureVersionedWebResources(): same path and `v` query, fetched in CORS mode.
            val href = "composeResources/dev.vladleesi.braindanceapp.resources/$servedPath?v=$webResourcesFingerprint"
            """    <link rel="preload" href="$href" as="fetch" crossorigin>"""
        }.joinToString("\n")

// The boot screen's icon, inlined into the page so it paints without waiting for another request.
val bootIconDataUri: String =
    file("src/wasmJsMain/resources/favicon.svg").let { icon ->
        require(icon.isFile) { "Boot icon source is missing: $icon" }
        "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(icon.readBytes())
    }

/** A fixed page of the Web app: its address, file, and what search engines and link previews show for it. */
data class WebPage(
    val path: String,
    val file: String,
    val title: String,
    val description: String,
    val indexed: Boolean,
)

// Every fixed address the app opens (see fixedPaths in BrowserHistory.kt). Pages with real public content are indexed
// and listed in the sitemap; placeholders for planned features and personal pages are not. Game and giveaway pages
// are written at the edge by _worker.js.
val webPages =
    listOf(
        WebPage(
            path = "/",
            file = webPageFile,
            title = "Braindance: find what to play next",
            description =
                "Discover popular and upcoming games, explore similar titles, and find free game giveaways in one " +
                    "place. Free, with no account needed.",
            indexed = true,
        ),
        WebPage(
            path = "/popular",
            file = "popular.html",
            title = "Popular games right now | Braindance",
            description =
                "The games players are paying attention to right now, with new releases, full launches, and games " +
                    "that just arrived on a new platform.",
            indexed = true,
        ),
        WebPage(
            path = "/anticipated",
            file = "anticipated.html",
            title = "Most anticipated upcoming games | Braindance",
            description = "Upcoming games ranked by how many players are looking forward to them.",
            indexed = true,
        ),
        WebPage(
            path = "/events",
            file = "events.html",
            title = "Gaming events | Braindance",
            description = "Browse gaming showcases, find their streams, and explore the games they featured.",
            indexed = true,
        ),
        WebPage(
            path = "/giveaways",
            file = "giveaways.html",
            title = "Free game giveaways | Braindance",
            description =
                "Current free games, DLC, and early access giveaways, with when each one ends and how to claim it.",
            indexed = true,
        ),
        WebPage(
            path = "/discover",
            file = "discover.html",
            title = "Discover games | Braindance",
            description = "Search games by title and find what to play next.",
            indexed = false,
        ),
        WebPage(
            path = "/releases",
            file = "releases.html",
            title = "Release calendar | Braindance",
            description = "A calendar of upcoming game releases is coming to Braindance.",
            indexed = false,
        ),
        WebPage(
            path = "/news",
            file = "news.html",
            title = "Game news | Braindance",
            description = "A game news feed is coming to Braindance.",
            indexed = false,
        ),
        WebPage(
            path = "/library",
            file = "library.html",
            title = "Your library | Braindance",
            description = "Track the games you are playing, have finished, and plan to play.",
            indexed = false,
        ),
        WebPage(
            path = "/profile",
            file = "profile.html",
            title = "Profile | Braindance",
            description = "Your Braindance profile, community links, and app information.",
            indexed = false,
        ),
    )

fun canonicalUrl(
    page: WebPage,
    siteUrl: String,
): String = if (page.path == "/") "$siteUrl/" else siteUrl + page.path

/** The head tags for [page]; the same set _worker.js writes for game and giveaway pages. */
fun pageMeta(
    page: WebPage,
    siteUrl: String,
): String {
    val image = "$siteUrl/og-image.png"
    val socialTitle = page.title.removeSuffix(" | Braindance")
    val tags =
        buildList {
            add("<title>${escapeHtml(page.title)}</title>")
            add("""<meta name="description" content="${escapeHtml(page.description)}">""")
            if (!page.indexed) add("""<meta name="robots" content="noindex">""")
            if (page.path.isNotEmpty()) {
                add("""<link rel="canonical" href="${escapeHtml(canonicalUrl(page, siteUrl))}">""")
            }
            add("""<meta property="og:site_name" content="Braindance">""")
            add("""<meta property="og:type" content="website">""")
            add("""<meta property="og:title" content="${escapeHtml(socialTitle)}">""")
            add("""<meta property="og:description" content="${escapeHtml(page.description)}">""")
            if (page.path.isNotEmpty()) {
                add("""<meta property="og:url" content="${escapeHtml(canonicalUrl(page, siteUrl))}">""")
            }
            add("""<meta property="og:image" content="${escapeHtml(image)}">""")
            add("""<meta property="og:image:alt" content="Braindance logo">""")
            add("""<meta name="twitter:card" content="summary_large_image">""")
        }
    return tags.joinToString("\n") { tag -> "    $tag" }
}

fun escapeHtml(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

// Rendered while configuring, so the distribution task only splices strings into the page.
val webPageMetaBlocks: Map<String, String> = webPages.associate { page -> page.file to pageMeta(page, webSiteUrl) }
val webSitemap: String =
    webPages
        .filter(WebPage::indexed)
        .joinToString(
            separator = "\n",
            prefix =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n",
            postfix = "\n</urlset>\n",
        ) { page -> "  <url><loc>${escapeHtml(canonicalUrl(page, webSiteUrl))}</loc></url>" }

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
    val siteUrl = webSiteUrl
    val publicOrigin = webPublicOrigin
    val backendUrl = backendBaseUrl.trim().removeSuffix("/")
    inputs.property("webBundleName", bundleName)
    inputs.property("backendOrigin", origin)
    inputs.property("webBasePath", basePath)
    inputs.property("resourcePreloads", preloads)
    inputs.property("bootIconDataUri", bootIcon)
    inputs.property("webSiteUrl", siteUrl)
    inputs.property("backendBaseUrl", backendUrl)
    val placeholders =
        mapOf(
            "webBundleName" to bundleName,
            "backendOrigin" to origin,
            "webBasePath" to basePath,
            "resourcePreloads" to preloads,
            "bootIconDataUri" to bootIcon,
        )
    filesMatching(listOf(webPageFile, "404.html")) {
        expand(placeholders)
    }
    filesMatching("robots.txt") {
        expand("sitemapUrl" to "$siteUrl/sitemap.xml")
    }
    // The edge worker's JavaScript uses ${...} itself, so its two placeholders are plain tokens.
    filesMatching("_worker.js") {
        filter { line ->
            line
                .replace("__BACKEND_BASE_URL__", backendUrl.takeIf { it.startsWith("https://") }.orEmpty())
                .replace("__PUBLIC_ORIGIN__", publicOrigin)
        }
    }
}

// The page is written before webpack names the production bundle, so the distribution swaps in the hashed name.
// The build fails unless exactly one hashed bundle exists and the page loads the development name it replaces.
tasks.named<Sync>("wasmJsBrowserDistribution") {
    val bundleName = webBundleName
    val hashedBundlePattern = hashedWebBundlePattern
    val pageFile = webPageFile
    val metaBlocks = webPageMetaBlocks
    val sitemap = webSitemap
    inputs.property("webPageMetaBlocks", metaBlocks)
    inputs.property("webSitemap", sitemap)
    doLast {
        val bundles = destinationDir.listFiles { file -> hashedBundlePattern.matches(file.name) }.orEmpty()
        val bundle =
            bundles.singleOrNull()
                ?: error("Expected one hashed web bundle in $destinationDir, found ${bundles.map { it.name }}")
        val page = destinationDir.resolve(pageFile)
        val reference = """src="$bundleName""""
        val html = page.readText()
        check(html.contains(reference)) { "index.html does not load $bundleName" }
        val shell = html.replace(reference, """src="${bundle.name}"""")

        // A copy of the app shell for every fixed address, each with its own head tags. Cloudflare Pages serves
        // `popular.html` at `/popular`, and uses the standalone 404.html from resources for unmatched addresses.
        val start = "<!-- page-meta:start -->"
        val end = "<!-- page-meta:end -->"
        val startIndex = shell.indexOf(start)
        val endIndex = shell.indexOf(end)
        check(startIndex >= 0 && endIndex > startIndex && shell.indexOf(start, startIndex + 1) == -1) {
            "index.html needs one page-meta block"
        }
        metaBlocks.forEach { (file, tags) ->
            val head = shell.substring(0, startIndex + start.length)
            destinationDir.resolve(file).writeText(head + "\n" + tags + "\n    " + shell.substring(endIndex))
        }
        destinationDir.resolve("sitemap.xml").writeText(sitemap)
    }
}
