// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import dev.vladleesi.braindanceapp.data.models.games.WebsiteType
import dev.vladleesi.braindanceapp.data.models.games.Websites
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_switch
import kotlinx.serialization.json.Json
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

    @Test
    fun nintendoWebsiteTypeCreatesAStoreButtonAlongsideTheOfficialWebsite() {
        val url = "https://www.nintendo.com/us/store/products/the-legend-of-zelda-ocarina-of-time-switch-2/"
        val websites =
            Json.decodeFromString<List<Websites>>(
                """
                [
                    {"id":1089134,"url":"$url","type":1},
                    {"id":1069399,"url":"$url","type":24}
                ]
                """.trimIndent(),
            )

        val store = websites.toStoreTypes().single()

        assertEquals("Nintendo Store", store.name)
        assertEquals(Res.drawable.ic_platform_logo_switch, store.image)
        assertEquals(url, store.url)
    }

    @Test
    fun nintendoStoreButtonsRejectUnsafeOrMissingLinks() {
        val websites =
            Json.decodeFromString<List<Websites>>(
                """
                [
                    {"id":1,"url":"javascript:alert(1)","type":24},
                    {"id":2,"url":"http://www.nintendo.com/us/store/products/game/","type":24},
                    {"id":3,"url":"https://user:secret@www.nintendo.com/game/","type":24},
                    {"id":4,"url":null,"type":24}
                ]
                """.trimIndent(),
            )

        assertEquals(emptyList(), websites.toStoreTypes())
    }
}
