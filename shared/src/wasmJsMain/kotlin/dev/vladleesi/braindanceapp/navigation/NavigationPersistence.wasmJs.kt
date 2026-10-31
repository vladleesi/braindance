// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import dev.vladleesi.braindanceapp.logger.BLogger
import kotlinx.browser.window
import org.w3c.dom.Storage

private const val STORAGE_KEY = "braindance.navigation"

/**
 * Keeps navigation in `sessionStorage`: it survives reloads of the same tab but not closing it, so a new visit
 * starts on Home. Browsers can block storage, for example in some private or embedded contexts; navigation then
 * works as before without being restored.
 */
internal actual val navigationPersistence: NavigationPersistence? =
    object : NavigationPersistence {
        private var unavailable = false

        override fun read(): String? = storage { it.getItem(STORAGE_KEY) }

        override fun write(snapshotJson: String) {
            storage { it.setItem(STORAGE_KEY, snapshotJson) }
        }

        private fun <T> storage(block: (Storage) -> T): T? {
            if (unavailable) return null
            // JavaScript SecurityError and QuotaExceededError arrive as JsException; stop retrying afterwards.
            return runCatching { block(window.sessionStorage) }
                .onFailure { error ->
                    unavailable = true
                    BLogger.warning(throwable = error, message = "Navigation will not be restored after reload")
                }.getOrNull()
        }
    }
