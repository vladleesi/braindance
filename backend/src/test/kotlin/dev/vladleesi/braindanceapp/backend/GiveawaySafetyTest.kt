// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Links and images that GiveawayService passes on from GamerPower, whose data the backend does not control. */
class GiveawaySafetyTest {
    private fun service(
        giveawaysJson: String = "[]",
        imageType: String = "image/png",
    ): Pair<GiveawayService, HttpClient> {
        val http =
            HttpClient(
                MockEngine { request ->
                    if (request.url.encodedPath.startsWith("/offers/")) {
                        respond(byteArrayOf(1, 2), headers = headersOf(HttpHeaders.ContentType, imageType))
                    } else {
                        respond(giveawaysJson, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                },
            ) { upstreamDefaults() }
        return GiveawayService(KtorUpstream(http)) to http
    }

    @Test
    fun unsafeOutgoingLinksAreRemovedAndSafeOnesKept(): Unit =
        runBlocking {
            val json =
                """[{"id":1,"open_giveaway_url":"https://www.gamerpower.com/open/a",""" +
                    """"open_giveaway":"javascript:alert(1)","gamerpower_url":"data:text/html,x"},""" +
                    """{"id":2,"open_giveaway_url":"http://www.gamerpower.com/open/b",""" +
                    """"open_giveaway":"https://user:pw@evil.example/","gamerpower_url":42}]"""
            val (service, http) = service(giveawaysJson = json)

            val result = service.giveaway("/v1/giveaways", "https://api.example")
            val items = Json.parseToJsonElement(checkNotNull(result.body).decodeToString()).jsonArray
            val first = items[0].jsonObject
            val second = items[1].jsonObject

            assertEquals("https://www.gamerpower.com/open/a", first.getValue("open_giveaway_url").jsonPrimitive.content)
            assertFalse("open_giveaway" in first)
            assertFalse("gamerpower_url" in first)
            assertTrue(second.keys.none { it in setOf("open_giveaway_url", "open_giveaway", "gamerpower_url") })
            assertEquals("2", second.getValue("id").jsonPrimitive.content)
            http.close()
        }

    @Test
    fun encodedTraversalInImagePathsIsRejected() {
        val (service, http) = service()
        val rejected =
            listOf(
                "https://www.gamerpower.com/offers/%2e%2e/api/giveaways",
                "https://www.gamerpower.com/offers/%2E%2E/api",
                "https://www.gamerpower.com/offers/a%2fb.png",
                "https://www.gamerpower.com/offers/a%5Cb.png",
                "https://www.gamerpower.com/offers/../api/giveaways",
            )
        for (url in rejected) assertNull(service.validImageUrl(url), url)
        assertEquals(
            "https://www.gamerpower.com/offers/1/a.png",
            service.validImageUrl("https://www.gamerpower.com/offers/1/a.png"),
        )
        http.close()
    }

    @Test
    fun onlyRasterImageTypesAreProxied(): Unit =
        runBlocking {
            for (type in listOf("image/svg+xml", "image/svg+xml; charset=utf-8", "text/html", "image/x-icon")) {
                val (service, http) = service(imageType = type)
                assertEquals(502, service.image("https://www.gamerpower.com/offers/a.png").first.status, type)
                http.close()
            }
            for (type in listOf("image/png", "image/jpeg", "IMAGE/WEBP", "image/gif; q=1")) {
                val (service, http) = service(imageType = type)
                assertEquals(200, service.image("https://www.gamerpower.com/offers/a.png").first.status, type)
                http.close()
            }
        }

    @Test
    fun safeLinkRules() {
        assertEquals("https://a.example/x", safeHttpsLink("https://a.example/x"))
        for (link in listOf(
            null,
            "",
            "javascript:alert(1)",
            "http://a.example",
            "https://",
            "https://u:p@a.example",
            "//a.example",
        )) {
            assertNull(safeHttpsLink(link), "accepted $link")
        }
    }
}
