// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import dev.vladleesi.braindanceapp.data.models.games.WebsiteType
import dev.vladleesi.braindanceapp.data.models.games.Websites
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExternalUrlTest {
    @Test
    fun httpsLinksWithAHostAreAllowed() {
        assertEquals("https://store.steampowered.com/app/1", "https://store.steampowered.com/app/1".toSafeExternalUrl())
        assertEquals("HTTPS://www.gog.com/game", " HTTPS://www.gog.com/game ".toSafeExternalUrl())
    }

    @Test
    fun unsafeOrMissingLinksAreRejected() {
        val rejected =
            listOf(
                "javascript:alert(1)",
                " JavaScript:alert(document.cookie)",
                "data:text/html,<script>alert(1)</script>",
                "http://store.steampowered.com/app/1",
                "https://",
                "https://user:secret@evil.example/",
                "//store.steampowered.com/app/1",
                "store.steampowered.com/app/1",
                "",
                "   ",
                null,
            )
        for (link in rejected) {
            assertNull(link.toSafeExternalUrl(), "accepted $link")
        }
    }

    @Test
    fun storeButtonsAreBuiltOnlyForSafeLinks() {
        val stores =
            listOf(
                Websites(id = 1, url = "https://store.steampowered.com/app/1", type = WebsiteType.STEAM),
                Websites(id = 2, url = "javascript:alert(1)", type = WebsiteType.GOG),
                Websites(id = 3, url = null, type = WebsiteType.EPIC_GAMES),
            ).toStoreTypes()

        assertEquals(listOf("Steam"), stores.map { it.name })
    }
}
