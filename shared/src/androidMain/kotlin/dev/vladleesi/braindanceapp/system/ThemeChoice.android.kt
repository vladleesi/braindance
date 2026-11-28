// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberThemeChoice(): ThemeChoice {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("braindance", Context.MODE_PRIVATE)
        val legacyLight = preferences.all[LIGHT_THEME_KEY] as? Boolean
        val mode = ThemeMode.restore(preferences.getString(THEME_KEY, null), legacyLight)
        ThemeChoice(mode) { choice ->
            preferences.edit().putString(THEME_KEY, choice.storedValue).apply()
        }
    }
}
