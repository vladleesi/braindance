// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberThemeChoice(): ThemeChoice =
    remember {
        val defaults = NSUserDefaults.standardUserDefaults
        val legacyLight = defaults.objectForKey(LIGHT_THEME_KEY)?.let { defaults.boolForKey(LIGHT_THEME_KEY) }
        val mode = ThemeMode.restore(defaults.stringForKey(THEME_KEY), legacyLight)
        ThemeChoice(mode) { choice ->
            defaults.setObject(choice.storedValue, forKey = THEME_KEY)
        }
    }
