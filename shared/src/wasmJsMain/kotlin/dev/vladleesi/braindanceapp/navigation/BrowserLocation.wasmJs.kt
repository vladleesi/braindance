// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL

// The tab title for screens other than the one the page was opened at.
private const val APP_TITLE = "Braindance"

/**
 * Real paths such as `/games/42` under the page's base path (`<base href>`), for example `/` or `/braindance/`.
 * The host serves the app's page at every address the app opens, so a reload or a shared link still loads it.
 *
 * Each address's page has its own title, such as a game's name. The tab keeps it while the app shows that screen and
 * shows the app's name on any other, so history entries and tabs never carry a title that no longer applies.
 */
@OptIn(ExperimentalWasmJsInterop::class)
internal actual val browserLocation: BrowserLocation? =
    object : BrowserLocation {
        private val basePath: String = URL(document.baseURI).pathname.let { if (it.endsWith("/")) it else "$it/" }
        private val openedRoute = webPathToRoute(read())
        private val openedTitle = document.title

        private fun updateTitle(path: String?) {
            document.title = if (webPathToRoute(path) == openedRoute) openedTitle else APP_TITLE
        }

        override fun read(): String? {
            val relative =
                window.location.pathname
                    .removePrefix(basePath)
                    .removeSuffix("/")
            return "/$relative"
        }

        override fun currentIndex(): Int? = entryIndex(window.history.state).takeIf { it >= 0 }

        override fun push(
            path: String,
            index: Int,
            snapshotJson: String,
        ) {
            window.history.pushState(entryState(index, snapshotJson), "", url(path))
            updateTitle(path)
        }

        override fun replace(
            path: String,
            index: Int,
            snapshotJson: String,
        ) {
            window.history.replaceState(entryState(index, snapshotJson), "", url(path))
            updateTitle(path)
        }

        override fun back() {
            window.history.back()
        }

        // popstate fires for Back, Forward, and an edited address, but not for pushState or replaceState calls.
        override fun observe(onChange: (path: String?, index: Int?, snapshotJson: String?) -> Unit): () -> Unit {
            val listener: (Event) -> Unit = {
                val state = window.history.state
                val path = read()
                updateTitle(path)
                onChange(path, entryIndex(state).takeIf { it >= 0 }, entrySnapshot(state))
            }
            window.addEventListener("popstate", listener)
            return { window.removeEventListener("popstate", listener) }
        }

        // A route path to a URL under the base path.
        private fun url(path: String): String = basePath + path.removePrefix("/")
    }

@OptIn(ExperimentalWasmJsInterop::class)
private fun entryState(
    index: Int,
    snapshotJson: String,
): JsAny = js("({ braindanceIndex: index, braindanceNavigation: snapshotJson })")

@OptIn(ExperimentalWasmJsInterop::class)
private fun entryIndex(state: JsAny?): Int =
    js("(state && typeof state.braindanceIndex === 'number') ? state.braindanceIndex : -1")

@OptIn(ExperimentalWasmJsInterop::class)
private fun entrySnapshot(state: JsAny?): String? =
    js("(state && typeof state.braindanceNavigation === 'string') ? state.braindanceNavigation : null")
