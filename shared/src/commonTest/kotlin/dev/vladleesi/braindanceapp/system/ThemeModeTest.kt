// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {
    @Test
    fun darkIsTheDefaultAndLegacyChoicesAreRestored() {
        assertEquals(ThemeMode.Dark, ThemeMode.restore(null, null))
        assertEquals(ThemeMode.Dark, ThemeMode.restore(null, false))
        assertEquals(ThemeMode.Light, ThemeMode.restore(null, true))
        assertEquals(ThemeMode.Dark, ThemeMode.restore("unknown", null))
    }

    @Test
    fun aSavedModeTakesPrecedenceOverTheLegacyChoice() {
        assertEquals(ThemeMode.System, ThemeMode.restore("system", true))
        assertEquals(ThemeMode.Dark, ThemeMode.restore("dark", true))
        assertEquals(ThemeMode.Light, ThemeMode.restore("light", false))
    }
}
