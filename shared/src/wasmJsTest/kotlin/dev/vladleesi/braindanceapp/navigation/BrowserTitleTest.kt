// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import kotlinx.browser.document
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BrowserTitleTest {
    @Test
    fun aScreenNamesTheTabAndForgettingTheNameRestoresIt() {
        val location = assertNotNull(browserLocation)
        val path = assertNotNull(location.read())
        val original = document.title

        location.setTitle(path, "Discover")
        assertEquals("Discover | Braindance", document.title)

        // A name for another address waits until that address is shown.
        location.setTitle("/somewhere-else", "Elsewhere")
        assertEquals("Discover | Braindance", document.title)

        location.setTitle(path, null)
        assertEquals(original, document.title)
    }
}
