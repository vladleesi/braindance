// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * Hash addresses, such as `#/game/42`. They need no server routing, so a reload or a shared link always loads the app
 * from GitHub Pages; the path after `#` is the route.
 */
internal actual val browserLocation: BrowserLocation? =
    object : BrowserLocation {
        override fun read(): String? =
            window.location.hash
                .removePrefix("#")
                .takeIf(String::isNotEmpty)

        override fun push(path: String) {
            window.history.pushState(null, "", "#$path")
        }

        override fun replace(path: String) {
            window.history.replaceState(null, "", "#$path")
        }

        // hashchange fires for Back, Forward, and edited addresses, but not for pushState or replaceState calls.
        override fun observe(onChange: (String?) -> Unit): () -> Unit {
            val listener: (Event) -> Unit = { onChange(read()) }
            window.addEventListener("hashchange", listener)
            return { window.removeEventListener("hashchange", listener) }
        }
    }
