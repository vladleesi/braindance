// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import dev.vladleesi.braindanceapp.BuildKonfig
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources

/**
 * Adds the app version to Compose resource URLs; call it before the first composition. Resource files keep their
 * names across releases, so without it a cache can pair new code with a previous release's string file. Strings are
 * read at byte offsets compiled into the code, so that mix shows garbled or missing text until the cache expires.
 */
@OptIn(ExperimentalResourceApi::class)
fun configureVersionedWebResources() {
    configureWebResources {
        resourcePathMapping { path -> "./$path?v=${BuildKonfig.APP_VERSION}" }
    }
}
