// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import dev.vladleesi.braindanceapp.BuildKonfig
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources

/**
 * Adds a fingerprint of the resources' content to Compose resource URLs; call it before the first composition.
 * Resource files keep their names across releases, so without it a cache could pair new code with a previous release's
 * string file. Strings are read at byte offsets compiled into the code, so that mix would show garbled or missing text.
 *
 * Because a changed file always gets a new URL, the server marks these files immutable (webApp's `_headers`), and a
 * repeat visit loads them from the browser cache without a request. A release that changes no resource keeps them.
 */
@OptIn(ExperimentalResourceApi::class)
fun configureVersionedWebResources() {
    configureWebResources {
        resourcePathMapping(::versionedResourceUrl)
    }
}

/**
 * The address of the Compose resource at [path], relative to the page's base and with the content fingerprint. The
 * page's preloads use the same address, so the app's request is served by them.
 */
internal fun versionedResourceUrl(path: String): String = "./$path?v=${BuildKonfig.WEB_RESOURCES_FINGERPRINT}"
