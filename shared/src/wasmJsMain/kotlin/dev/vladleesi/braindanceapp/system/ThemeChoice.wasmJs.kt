// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.vladleesi.braindanceapp.logger.BLogger
import kotlinx.browser.window

@Composable
internal actual fun rememberThemeChoice(): ThemeChoice =
    remember {
        val storage = runCatching { window.localStorage }.getOrNull()
        val stored = runCatching { storage?.getItem(THEME_KEY) }.getOrNull()
        val legacyLight = runCatching { storage?.getItem(LIGHT_THEME_KEY)?.toBooleanStrictOrNull() }.getOrNull()
        ThemeChoice(ThemeMode.restore(stored, legacyLight)) { choice ->
            runCatching { storage?.setItem(THEME_KEY, choice.storedValue) }
                .onFailure { error ->
                    BLogger.warning(throwable = error, message = "The theme choice will not be remembered")
                }
        }
    }
