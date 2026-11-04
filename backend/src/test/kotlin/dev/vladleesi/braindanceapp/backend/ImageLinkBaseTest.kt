// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/** Giveaway image links never come from the request's Host header, which any client can set. */
class ImageLinkBaseTest {
    private val giveawaysJson = """[{"id":1,"image":"https://www.gamerpower.com/offers/1/a.jpg"}]"""

    private fun giveawaysBody(publicBaseUrl: String?): String {
        val config = BackendConfig("id", "secret", null, emptySet(), 8080, publicBaseUrl = publicBaseUrl)
        val http =
            HttpClient(
                MockEngine { respond(giveawaysJson, headers = headersOf(HttpHeaders.ContentType, "application/json")) },
            ) { upstreamDefaults() }
        var body = ""
        testApplication {
            application {
                val upstream = KtorUpstream(http)
                api(config, IgdbService(upstream, RateLimiter { true }, config), GiveawayService(upstream))
            }
            body =
                client
                    .get("/v1/giveaways") { header(HttpHeaders.Host, "attacker.example") }
                    .bodyAsText()
        }
        http.close()
        return body
    }

    @Test
    fun withoutAPublicBaseImagesKeepTheirUpstreamUrls() {
        val body = giveawaysBody(publicBaseUrl = null)
        assertFalse(body.contains("attacker.example"), body)
        assertContains(body, "https://www.gamerpower.com/offers/1/a.jpg")
    }

    @Test
    fun withAPublicBaseImagesUseItWhateverTheHost() {
        val body = giveawaysBody(publicBaseUrl = "https://api.example")
        assertFalse(body.contains("attacker.example"), body)
        assertContains(body, "https://api.example/v1/giveaways/image?url=")
    }
}
