// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiTest {
    private val config = BackendConfig("id", "secret", null, setOf("https://app.example"), 8080)

    private fun upstream(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(
            io.ktor.client.request.HttpRequestData,
        ) -> io.ktor.client.request.HttpResponseData,
    ): Pair<Upstream, HttpClient> {
        val client =
            HttpClient(MockEngine(handler)) {
                install(HttpTimeout)
                expectSuccess = false
            }
        return KtorUpstream(client) to client
    }

    @Test
    fun getQueriesMatchLegacyPostAndRejectInvalidParameters() =
        testApplication {
            val queries = mutableListOf<String>()
            val (remote, http) =
                upstream { request ->
                    if (request.url.host == "id.twitch.tv") {
                        respond(
                            """{"access_token":"abc","expires_in":3600}""",
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        queries += (request.body as TextContent).text
                        respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                }
            application { api(config, IgdbService(remote, RateLimiter { true }, config), GiveawayService(remote)) }
            val cases =
                mapOf(
                    "details?id=42" to """{"id":42}""",
                    "anticipated?currentTimestamp=1780000000&pageSize=20" to
                        """{"currentTimestamp":1780000000,"pageSize":20}""",
                    "popular?ids=42,43&pageSize=20" to """{"ids":[42,43],"pageSize":20}""",
                    "popularity?type=34&pageSize=40" to """{"type":34,"pageSize":40}""",
                )
            for ((endpoint, body) in cases) {
                val get = client.get("/v1/games/$endpoint")
                val post =
                    client.post("/v1/games/${endpoint.substringBefore('?')}") {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body)
                    }
                assertEquals(HttpStatusCode.OK, get.status)
                assertEquals(post.bodyAsText(), get.bodyAsText())
                assertEquals(queries[queries.lastIndex - 1], queries.last())
                assertEquals(PRIVATE_CACHE_CONTROL, post.headers[HttpHeaders.CacheControl])
            }
            val count = queries.size
            for (endpoint in listOf(
                "details?id=0",
                "details?id=42&id=43",
                "details?id=42&token=dummy",
                "details?id=2147483648",
                "anticipated?pageSize=51",
                "popular?ids=1,0&pageSize=20",
                "popular?ids=${(1..51).joinToString(",")}&pageSize=20",
                "popularity?type=1&pageSize=20",
            )) {
                val response = client.get("/v1/games/$endpoint")
                assertEquals(HttpStatusCode.BadRequest, response.status, endpoint)
                assertEquals(PRIVATE_CACHE_CONTROL, response.headers[HttpHeaders.CacheControl])
            }
            assertEquals(count, queries.size)
            assertEquals(HttpStatusCode.OK, client.get("/v1/games/anticipated?pageSize=20").status)
            assertContains(queries.last(), "first_release_date > ")
            assertEquals(
                HttpStatusCode.OK,
                client.get("/v1/games/popular?ids=${(1..50).joinToString(",")}&pageSize=50").status,
            )
            http.close()
        }

    @Test
    fun healthValidationCorsAndMissingRoutes() =
        testApplication {
            val (remote, http) =
                upstream {
                    respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            application { api(config, IgdbService(remote, RateLimiter { true }, config), GiveawayService(remote)) }
            assertEquals(HttpStatusCode.OK, client.get("/healthz").status)
            assertEquals(HttpStatusCode.OK, client.get("/health").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/games/details").status)
            assertEquals(HttpStatusCode.UnsupportedMediaType, client.post("/v1/games/details").status)
            assertEquals(
                HttpStatusCode.BadRequest,
                client
                    .post("/v1/games/details") {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody("{")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.BadRequest,
                client
                    .post("/v1/games/details") {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody("""{"id":0}""")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.PayloadTooLarge,
                client
                    .post("/v1/games/details") {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody("x".repeat(2049))
                    }.status,
            )
            val preflight = client.options("/v1/games/details") { header(HttpHeaders.Origin, "https://app.example") }
            assertEquals(HttpStatusCode.NoContent, preflight.status)
            assertEquals("https://app.example", preflight.headers["Access-Control-Allow-Origin"])
            assertEquals(
                HttpStatusCode.Forbidden,
                client
                    .options("/v1/games/details") {
                        header(HttpHeaders.Origin, "https://denied.example")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.OK,
                client
                    .get("/healthz") {
                        header(HttpHeaders.Origin, "https://denied.example")
                    }.status,
            )
            http.close()
        }

    @Test
    fun allIgdbQueriesAndTokenCache() =
        testApplication {
            val queries = mutableListOf<String>()
            var tokenCalls = 0
            val (remote, http) =
                upstream { request ->
                    when (request.url.host) {
                        "id.twitch.tv" -> {
                            tokenCalls++
                            respond(
                                """{"access_token":"abc","expires_in":3600}""",
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }
                        else -> {
                            queries += request.body.toString()
                            respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                        }
                    }
                }
            application { api(config, IgdbService(remote, RateLimiter { true }, config), GiveawayService(remote)) }
            val cases =
                mapOf(
                    "details" to """{"id":42}""",
                    "anticipated" to """{"currentTimestamp":1780000000,"pageSize":20}""",
                    "popular" to """{"ids":[42,43],"pageSize":20}""",
                    "popularity" to """{"type":34,"pageSize":40}""",
                )
            for ((endpoint, body) in cases) {
                assertEquals(
                    HttpStatusCode.OK,
                    client
                        .post("/v1/games/$endpoint") {
                            header(HttpHeaders.ContentType, "application/json")
                            setBody(body)
                        }.status,
                )
            }
            assertEquals(1, tokenCalls)
            assertEquals(4, queries.size)
            val service = IgdbService(remote, RateLimiter { true }, config)
            val details =
                service.buildQuery(
                    "/v1/games/details",
                    Json.parseToJsonElement("""{"id":42}""") as JsonObject,
                )
            assertEquals("/v4/games", details?.first)
            assertNotNull(details)
            assertContains(details.second, "where id = 42;limit 15;")
            assertContains(
                assertNotNull(
                    service.buildQuery(
                        "/v1/games/details",
                        Json.parseToJsonElement("""{"id":42.0}""") as JsonObject,
                    ),
                ).second,
                "where id = 42;",
            )
            val popularity =
                service.buildQuery(
                    "/v1/games/popularity",
                    Json.parseToJsonElement("""{"type":34,"pageSize":40}""") as JsonObject,
                )
            assertEquals("/v4/popularity_primitives", popularity?.first)
            assertNotNull(popularity)
            assertContains(popularity.second, "where popularity_type = 34;sort value desc;limit 40;")
            http.close()
        }

    @Test
    fun rateLimitAndRefreshRetry() =
        runBlocking {
            var calls = 0
            var tokenCalls = 0
            val (remote, http) =
                upstream { request ->
                    if (request.url.host == "id.twitch.tv") {
                        tokenCalls++
                        respond(
                            """{"access_token":"token$tokenCalls","expires_in":3600}""",
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        calls++
                        if (calls == 1) {
                            respond("", HttpStatusCode.Unauthorized)
                        } else {
                            respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                        }
                    }
                }
            var slots = 0
            val service =
                IgdbService(
                    remote,
                    RateLimiter {
                        slots++
                        true
                    },
                    config,
                )
            val result = service.query("/v1/games/details", Json.parseToJsonElement("""{"id":42}""") as JsonObject)
            assertEquals(200, result.status)
            assertEquals(2, calls)
            assertEquals(2, tokenCalls)
            assertEquals(2, slots)
            assertEquals(
                429,
                IgdbService(remote, RateLimiter { false }, config)
                    .query("/v1/games/details", Json.parseToJsonElement("""{"id":42}""") as JsonObject)
                    .status,
            )
            http.close()
        }

    @Test
    fun giveawayImagesAndUpstreamStatusMapping() =
        testApplication {
            val publicConfig = config.copy(publicBaseUrl = "https://api.example")
            val (remote, http) =
                upstream { request ->
                    when {
                        request.url.host == "www.gamerpower.com" && request.url.encodedPath.startsWith("/offers/") ->
                            respond(byteArrayOf(1, 2), headers = headersOf(HttpHeaders.ContentType, "image/png"))
                        request.url.encodedPath == "/api/giveaway" -> respond("", HttpStatusCode.NotFound)
                        else ->
                            respond(
                                """[{"image":"https://www.gamerpower.com/offers/a.png",""" +
                                    """"thumbnail":"https://evil.test/a"}]""",
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                    }
                }
            application {
                api(publicConfig, IgdbService(remote, RateLimiter { true }, publicConfig), GiveawayService(remote))
            }
            val list = client.get("/v1/giveaways")
            assertEquals(HttpStatusCode.OK, list.status)
            assertContains(list.bodyAsText(), "/v1/giveaways/image?url=")
            assertContains(list.bodyAsText(), "https://api.example/v1/giveaways/image")
            assertContains(list.bodyAsText(), "https://evil.test/a")
            assertEquals(HttpStatusCode.NotFound, client.get("/v1/giveaways/42").status)
            assertEquals(
                HttpStatusCode.BadRequest,
                client.get("/v1/giveaways/image?url=https://evil.test/offers/a").status,
            )
            val image = client.get("/v1/giveaways/image?url=https%3A%2F%2Fwww.gamerpower.com%2Foffers%2Fa.png")
            assertEquals(HttpStatusCode.OK, image.status)
            assertEquals(PRIVATE_CACHE_CONTROL, image.headers[HttpHeaders.CacheControl])
            http.close()
        }

    @Test
    fun upstreamErrorsAndResponseLimits() =
        runBlocking {
            val (remote, http) =
                upstream { request ->
                    when {
                        request.url.host == "id.twitch.tv" ->
                            respond(
                                """{"access_token":"abc","expires_in":3600}""",
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        request.url.host == "api.igdb.com" -> respond("[]", HttpStatusCode.TooManyRequests)
                        request.url.encodedPath.startsWith("/offers/") ->
                            respond(
                                ByteArray(5 * 1024 * 1024 + 1),
                                headers = headersOf(HttpHeaders.ContentType, "image/png"),
                            )
                        else -> respond("not-json", headers = headersOf(HttpHeaders.ContentType, "text/plain"))
                    }
                }
            val input = Json.parseToJsonElement("""{"id":42}""") as JsonObject
            assertEquals(
                429,
                IgdbService(remote, RateLimiter { true }, config).query("/v1/games/details", input).status,
            )
            assertEquals(502, GiveawayService(remote).giveaway("/v1/giveaways", "https://api.example").status)
            assertEquals(502, GiveawayService(remote).image("https://www.gamerpower.com/offers/a.png").first.status)
            http.close()
        }

    @Test
    fun missingCredentialsAndConfiguredLocalCors() =
        testApplication {
            val missing = config.copy(clientSecret = null, allowedOrigins = setOf("http://localhost:8765"))
            val (remote, http) = upstream { respond("[]") }
            application { api(missing, IgdbService(remote, RateLimiter { true }, missing), GiveawayService(remote)) }
            assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/healthz").status)
            assertEquals(HttpStatusCode.OK, client.get("/health").status)
            assertEquals(
                HttpStatusCode.ServiceUnavailable,
                client
                    .post("/v1/games/details") {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody("""{"id":42}""")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.NoContent,
                client
                    .options("/v1/games/details") {
                        header(HttpHeaders.Origin, "http://localhost:8765")
                    }.status,
            )
            assertEquals(
                HttpStatusCode.Forbidden,
                client
                    .options("/v1/games/details") {
                        header(HttpHeaders.Origin, "http://localhost:8766")
                    }.status,
            )
            http.close()
        }

    @Test
    fun imageTransportFailureKeepsImageError() =
        testApplication {
            val (remote, http) = upstream { throw java.io.IOException("upstream disconnected") }
            application { api(config, IgdbService(remote, RateLimiter { true }, config), GiveawayService(remote)) }
            val response = client.get("/v1/giveaways/image?url=https%3A%2F%2Fwww.gamerpower.com%2Foffers%2Fa.png")
            assertEquals(HttpStatusCode.BadGateway, response.status)
            assertEquals("""{"error":"Upstream image unavailable"}""", response.bodyAsText())
            http.close()
        }

    @Test
    fun oversizedJsonResponseStatusParity() =
        testApplication {
            val (remote, http) =
                upstream { request ->
                    when (request.url.host) {
                        "id.twitch.tv" ->
                            respond(
                                """{"access_token":"abc","expires_in":3600}""",
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        "api.igdb.com" ->
                            respond(
                                ByteArray(1024 * 1024 + 1),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        else ->
                            respond(
                                ByteArray(2 * 1024 * 1024 + 1),
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                    }
                }
            application { api(config, IgdbService(remote, RateLimiter { true }, config), GiveawayService(remote)) }
            val game =
                client.post("/v1/games/details") {
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("""{"id":42}""")
                }
            assertEquals(HttpStatusCode.PayloadTooLarge, game.status)
            assertEquals("""{"error":"Query too large"}""", game.bodyAsText())
            assertEquals(HttpStatusCode.BadGateway, client.get("/v1/giveaways").status)
            http.close()
        }
}
