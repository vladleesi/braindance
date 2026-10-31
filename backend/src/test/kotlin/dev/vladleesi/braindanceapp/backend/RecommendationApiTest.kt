// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecommendationApiTest {
    private val base = "https://localhost"
    private val config =
        BackendConfig(
            clientId = "id",
            clientSecret = "secret",
            redisUrl = null,
            allowedOrigins = setOf("https://app.example"),
            port = 8080,
            publicBaseUrl = base,
            publicCacheEnabled = true,
        )

    @Test
    fun returnsRankedClientShapeWithLimitAndSharedCacheLifetime() =
        testApplication {
            val igdb = FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames)
            application { api(config, IgdbService(igdb, RateLimiter { true }, config), GiveawayService(igdb)) }
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }

            val response = client.get("$base/v1/games/recommendations?id=200&limit=3")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(RECOMMENDATIONS_CACHE_CONTROL, response.headers[HttpHeaders.CacheControl])
            val games = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            assertEquals(3, games.size)
            val first = games.first().jsonObject
            assertEquals(setOf("id", "name", "cover"), first.keys)
            assertTrue(first.getValue("cover").jsonObject.keys == setOf("url"))

            // The default limit and other limits reuse the cached ranking without new upstream requests.
            val all =
                Json
                    .parseToJsonElement(
                        client.get("$base/v1/games/recommendations?id=200").bodyAsText(),
                    ).jsonArray
            assertEquals(
                games.map {
                    it.jsonObject
                        .getValue("id")
                        .jsonPrimitive.int
                },
                all.take(3).map {
                    it.jsonObject
                        .getValue("id")
                        .jsonPrimitive.int
                },
            )
            assertEquals(2, igdb.igdbRequests.get())

            for (query in listOf(
                "id=0",
                "id=abc",
                "limit=5",
                "id=200&limit=0",
                "id=200&limit=51",
                "id=1&id=2",
                "id=200&x=1",
            )) {
                val invalid = client.get("$base/v1/games/recommendations?$query")
                assertEquals(HttpStatusCode.BadRequest, invalid.status, query)
                assertEquals(PRIVATE_CACHE_CONTROL, invalid.headers[HttpHeaders.CacheControl], query)
            }
            assertEquals(HttpStatusCode.NotFound, client.post("$base/v1/games/recommendations?id=200").status)

            val missing = client.get("$base/v1/games/recommendations?id=999999")
            assertEquals(HttpStatusCode.NotFound, missing.status)
            assertEquals("""{"error":"Game not found"}""", missing.bodyAsText())
            assertEquals(PRIVATE_CACHE_CONTROL, missing.headers[HttpHeaders.CacheControl])
        }

    @Test
    fun degradedResultsStayPrivate() =
        testApplication {
            val igdb =
                FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames).apply {
                    failures = mapOf(1 to HttpStatusCode.TooManyRequests)
                }
            application { api(config, IgdbService(igdb, RateLimiter { true }, config), GiveawayService(igdb)) }
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
            val response = client.get("$base/v1/games/recommendations?id=100")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(PRIVATE_CACHE_CONTROL, response.headers[HttpHeaders.CacheControl])
            assertTrue(Json.parseToJsonElement(response.bodyAsText()).jsonArray.isNotEmpty())
        }

    @Test
    fun missingCredentialsAndRateLimitsUseStableErrors() =
        testApplication {
            val noCredentials = config.copy(clientSecret = null)
            val igdb = FakeIgdb(SyntheticCatalog.games).apply { failures = mapOf(0 to HttpStatusCode.TooManyRequests) }
            application {
                api(noCredentials, IgdbService(igdb, RateLimiter { true }, noCredentials), GiveawayService(igdb))
            }
            assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/v1/games/recommendations?id=100").status)
            assertEquals(0, igdb.igdbRequests.get())
        }
}
