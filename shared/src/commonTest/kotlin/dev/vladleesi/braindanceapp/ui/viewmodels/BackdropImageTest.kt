// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import kotlin.test.Test
import kotlin.test.assertEquals

class BackdropImageTest {
    private val backdrop = BackdropImage(compactUrl = "med", wideUrl = "big", hdUrl = "720p", fullHdUrl = "1080p")

    @Test
    fun aWindowUpToScreenshotBigUsesIt() {
        assertEquals("big", backdrop.wideUrlFor(widthPx = 800))
        assertEquals("big", backdrop.wideUrlFor(widthPx = 889))
    }

    @Test
    fun aWiderWindowUses720pUpToItsWidth() {
        assertEquals("720p", backdrop.wideUrlFor(widthPx = 890))
        assertEquals("720p", backdrop.wideUrlFor(widthPx = 1280))
    }

    @Test
    fun largeAndHighDensityScreensUse1080p() {
        assertEquals("1080p", backdrop.wideUrlFor(widthPx = 1281))
        // A 14-inch MacBook's 1512 dp at two pixels per dp.
        assertEquals("1080p", backdrop.wideUrlFor(widthPx = 3024))
    }
}
