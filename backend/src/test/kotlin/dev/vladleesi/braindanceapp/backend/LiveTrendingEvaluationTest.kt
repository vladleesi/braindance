// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.Instant
import java.util.Locale
import kotlin.test.Test
import kotlin.time.measureTimedValue

/**
 * Opt-in Trending Now report on live IGDB data (about 6 IGDB requests):
 *
 * ```sh
 * BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
 *   ./gradlew :backend:test --tests '*LiveTrendingEvaluationTest*' --rerun -i
 * ```
 *
 * It prints release status names, recent IGDB events, each top-20 game's attention, relevance, and reason, the
 * hydration cutoff, request latency, ranking time, and payload size, then the Popular Right Now feed for comparison.
 * It asserts nothing about ranking quality, which needs human review.
 */
class LiveTrendingEvaluationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun printTrendingReport() {
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
                val statuses = igdb.execute("/v4/release_date_statuses", "fields name;limit 50;", 2000)
                println("release_date_statuses: ${statuses.body?.decodeToString()}")
                val nowSeconds = Instant.now().epochSecond
                val eventNames =
                    igdb.execute(
                        "/v4/events",
                        "fields name,start_time,games;where start_time >= ${nowSeconds - 30 * 86_400};" +
                            "sort start_time desc;limit 50;",
                        2000,
                    )
                println("events (30 days): ${eventNames.body?.decodeToString()?.take(4000)}")

                val (discovery, discoveryTime) =
                    measureTimedValue {
                        igdb.execute(
                            RecommendationService.MULTIQUERY_PATH,
                            TrendingService.discoveryQuery(nowSeconds),
                            2000,
                        )
                    }
                val discoveryBody = checkNotNull(discovery.body)
                val results = RecommendationQueries.parseMultiquery(discoveryBody)
                val signals = validPopularitySignals(results, System.currentTimeMillis())
                println("discovery: ${discoveryBody.size} bytes in $discoveryTime; valid signals ${signals.keys}")
                val events = results["events"].orEmpty().map { json.decodeFromJsonElement<IgdbEvent>(it) }
                val attention = TrendingRanking.attention(signals)
                val showcases = TrendingRanking.showcaseAges(events, nowSeconds)
                val candidates = TrendingRanking.hydrationOrder(attention)
                val cutoff = attention.getValue(candidates.last()).score
                println("candidates ${attention.size}, hydrated ${candidates.size}, cutoff attention $cutoff")

                val (hydration, hydrationTime) =
                    measureTimedValue {
                        igdb.execute("/v4/games", TrendingService.hydrationQuery(candidates), 2000)
                    }
                val hydrationBody = checkNotNull(hydration.body)
                println("hydration: ${hydrationBody.size} bytes in $hydrationTime")
                val games =
                    RecommendationQueries
                        .parseGames(json.parseToJsonElement(hydrationBody.decodeToString()).jsonArray)
                        .associateBy { it.id }
                val (feed, rankingTime) =
                    measureTimedValue { TrendingRanking.rank(attention, games, showcases, nowSeconds) }
                println("ranking: $rankingTime; payload ${trendingJson(feed).encodeToByteArray().size} bytes")
                printFeed(feed, attention)
                printPopularNow(igdb)
            }
        } finally {
            upstream.close()
        }
    }

    private fun printFeed(
        feed: List<TrendingGame>,
        attention: Map<Int, Attention>,
    ) {
        feed.forEachIndexed { index, game ->
            println(
                String.format(
                    Locale.ROOT,
                    "%2d. %-45s score %.3f attention %.3f (%d signals) reason %s %s",
                    index + 1,
                    game.name,
                    game.score,
                    game.attention,
                    attention.getValue(game.id).signals,
                    game.reason?.wireName ?: "-",
                    game.platform.orEmpty(),
                ),
            )
        }
    }

    private suspend fun printPopularNow(igdb: IgdbService) {
        val popular = PopularNowService(igdb, log = {}).popularNow()
        val names =
            (popular as? ResponseCache.LoadResult.Hit)
                ?.response
                ?.body
                ?.decodeToString()
                ?.let { body ->
                    json.parseToJsonElement(body).jsonArray.joinToString(" | ") {
                        it.jsonObject["name"]
                            ?.jsonPrimitive
                            ?.content
                            .orEmpty()
                    }
                }
        println("popular-now for comparison: ${names ?: "unavailable [$popular]"}")
    }
}
