// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import kotlinx.browser.window
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LayoutPreferencesTest {
    @AfterTest
    fun forgetChoices() {
        window.localStorage.removeItem(KEY)
    }

    @Test
    fun aCollapsedSidebarIsRememberedAndExpandingForgetsIt() {
        val preferences = assertNotNull(layoutPreferences)

        preferences.isSidebarCollapsed = true
        assertTrue(preferences.isSidebarCollapsed)
        assertEquals("true", window.localStorage.getItem(KEY))

        preferences.isSidebarCollapsed = false
        assertFalse(preferences.isSidebarCollapsed)
        assertNull(window.localStorage.getItem(KEY))
    }

    @Test
    fun anUnknownStoredValueReadsAsExpanded() {
        window.localStorage.setItem(KEY, "yes")

        assertFalse(assertNotNull(layoutPreferences).isSidebarCollapsed)
    }

    private companion object {
        const val KEY = "braindance.sidebarCollapsed"
    }
}
