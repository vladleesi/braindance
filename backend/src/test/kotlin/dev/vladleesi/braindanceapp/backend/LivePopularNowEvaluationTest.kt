// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.Instant
import kotlin.test.Test

/**
 * Opt-in comparison of the old and new Popular Right Now feeds on live IGDB data (about 6 IGDB requests):
 *
 * ```sh
 * BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
 *   ./gradlew :backend:test --tests '*LivePopularNowEvaluationTest*' --rerun -i
 * ```
 *
 * It prints the live popularity type names and signal freshness so the type IDs in [PopularitySignal] can be checked,
 * then both top-20 lists. It asserts nothing about ranking quality, which needs human review.
 */
class LivePopularNowEvaluationTest {
    @Test
    fun compareOldAndNewFeeds() {
        val clientId = System.getenv("TWITCH_CLIENT_ID")
        val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")
        assumeTrue(
            System.getenv("BRAINDANCE_LIVE_EVAL") == "1" && !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank(),
            "Set BRAINDANCE_LIVE_EVAL=1 and Twitch credentials",
        )
        val config = BackendConfig(clientId, clientSecret, null, emptySet(), 8080)
        val upstream = KtorUpstream(HttpClient(CIO) { upstreamDefaults() })
        val igdb = IgdbService(upstream, InMemoryRateLimiter(), config)
        try {
            runBlocking {
                val types =
                    igdb.execute(
                        "/v4/popularity_types",
                        "fields name,external_popularity_source;limit 100;",
                        2000,
                    )
                println("popularity_types: ${types.body?.decodeToString()}")
                val discovery =
                    igdb.execute(
                        RecommendationService.MULTIQUERY_PATH,
                        PopularNowService.discoveryQuery(),
                        2000,
                    )
                for ((name, rows) in RecommendationQueries.parseMultiquery(checkNotNull(discovery.body))) {
                    val newest =
                        rows
                            .mapNotNull {
                                it.jsonObject["calculated_at"]
                                    ?.jsonPrimitive
                                    ?.content
                                    ?.toLongOrNull()
                            }.maxOrNull()
                    println(
                        "signal $name: ${rows.size} rows, newest calculated_at ${newest?.let(Instant::ofEpochSecond)}",
                    )
                }

                val twitch =
                    igdb.query(
                        "/v1/games/popularity",
                        JsonObject(
                            mapOf(
                                "type" to JsonPrimitive(34),
                                "pageSize" to JsonPrimitive(40),
                            ),
                        ),
                    )
                val ids =
                    Json.parseToJsonElement(checkNotNull(twitch.body).decodeToString()).jsonArray.map {
                        it.jsonObject.getValue("game_id")
                    }
                val old =
                    igdb.query(
                        "/v1/games/popular",
                        JsonObject(
                            mapOf(
                                "ids" to JsonArray(ids),
                                "pageSize" to JsonPrimitive(20),
                            ),
                        ),
                    )
                println("old (Twitch top 40, sorted by hypes): ${names(old.body)}")

                val new = PopularNowService(igdb).popularNow()
                println("new (fused signals): ${names((new as? ResponseCache.LoadResult.Hit)?.response?.body)} [$new]")
            }
        } finally {
            upstream.close()
        }
    }

    private fun names(body: ByteArray?): String =
        body
            ?.let { Json.parseToJsonElement(it.decodeToString()).jsonArray }
            ?.joinToString(" | ") {
                it.jsonObject["name"]
                    ?.jsonPrimitive
                    ?.content
                    .orEmpty()
            }
            ?: "unavailable"
}
