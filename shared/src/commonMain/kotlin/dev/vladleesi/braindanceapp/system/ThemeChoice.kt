// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

internal const val THEME_KEY = "braindance.theme"
internal const val LIGHT_THEME_KEY = "braindance.lightTheme"

enum class ThemeMode(
    internal val storedValue: String,
    internal val lightOverride: Boolean?,
) {
    Light("light", true),
    System("system", null),
    Dark("dark", false),
    ;

    internal companion object {
        fun restore(
            stored: String?,
            legacyLight: Boolean?,
        ): ThemeMode =
            entries.firstOrNull { it.storedValue == stored }
                ?: when (legacyLight) {
                    true -> Light
                    false -> Dark
                    null -> Dark
                }
    }
}

@Stable
internal class ThemeChoice(
    initialMode: ThemeMode,
    private val save: (ThemeMode) -> Unit,
) {
    var mode by mutableStateOf(initialMode)
        private set

    fun choose(mode: ThemeMode) {
        if (mode == this.mode) return
        this.mode = mode
        save(mode)
    }
}

internal val LocalThemeChoice = staticCompositionLocalOf<ThemeChoice> { error("No app theme choice provided") }

@Composable
internal expect fun rememberThemeChoice(): ThemeChoice
