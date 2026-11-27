// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import dev.vladleesi.braindanceapp.logger.BLogger
import kotlinx.browser.window
import org.w3c.dom.Storage

private const val SIDEBAR_COLLAPSED_KEY = "braindance.sidebarCollapsed"

/**
 * Keeps layout choices in `localStorage`, so they last across visits on this browser. Browsers can block storage, for
 * example in some private or embedded contexts. The layout then starts with its defaults and forgets changes.
 */
internal actual val layoutPreferences: LayoutPreferences? =
    object : LayoutPreferences {
        private var unavailable = false

        override var isSidebarCollapsed: Boolean
            get() = storage { it.getItem(SIDEBAR_COLLAPSED_KEY) } == "true"
            set(value) {
                storage {
                    if (value) {
                        it.setItem(
                            SIDEBAR_COLLAPSED_KEY,
                            "true",
                        )
                    } else {
                        it.removeItem(SIDEBAR_COLLAPSED_KEY)
                    }
                }
            }

        private fun <T> storage(block: (Storage) -> T): T? {
            if (unavailable) return null
            // JavaScript SecurityError and QuotaExceededError arrive as JsException. Stop retrying afterwards.
            return runCatching { block(window.localStorage) }
                .onFailure { error ->
                    unavailable = true
                    BLogger.warning(throwable = error, message = "Layout choices will not be remembered")
                }.getOrNull()
        }
    }
