// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Opt-in local benchmark: `BRAINDANCE_BENCHMARK=1 ./gradlew :backend:test --tests '*RecommendationBenchmarkTest*'
 * --rerun -i`. It uses [FakeIgdb] with zero network latency, so it measures backend overhead only, not IGDB or
 * Cloud Run latency. It prints results and asserts only request counts, never timings.
 */
class RecommendationBenchmarkTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)

    private fun enabled() = assumeTrue(System.getenv("BRAINDANCE_BENCHMARK") == "1", "Set BRAINDANCE_BENCHMARK=1")

    private fun percentiles(nanos: List<Long>): String {
        val sorted = nanos.sorted()

        fun at(p: Double) = sorted[((sorted.size - 1) * p).toInt()] / 1000.0
        return "p50=%.1fµs p95=%.1fµs n=%d".format(at(0.5), at(0.95), sorted.size)
    }

    @Test
    fun endpointLatencyAndRequestCounts() {
        enabled()
        testApplication {
            val igdb = FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames)
            val service = RecommendationService(IgdbService(igdb, RateLimiter { true }, config), log = {})
            application { api(config, IgdbService(igdb, RateLimiter { true }, config), GiveawayService(igdb), service) }
            val ids = SyntheticCatalog.scenarios.map { it.sourceId }
            val uncached = ids.map { id -> measure { client.get("/v1/games/recommendations?id=$id") } }
            val missRequests = igdb.igdbRequests.get()
            repeat(200) { client.get("/v1/games/recommendations?id=${ids[it % ids.size]}") }
            val cached = List(2000) { measure { client.get("/v1/games/recommendations?id=${ids[it % ids.size]}") } }
            assertEquals(missRequests, igdb.igdbRequests.get())
            println("uncached (zero-latency fake upstream): ${percentiles(uncached)}")
            println("cached: ${percentiles(cached)}")
            println("IGDB requests per miss: ${missRequests.toDouble() / ids.size}; per hit: 0")
            service.close()
        }
    }

    @Test
    fun concurrentColdRequestsAreDeduplicated() {
        enabled()
        runBlocking {
            val igdb = FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames)
            RecommendationService(IgdbService(igdb, RateLimiter { true }, config), log = {}).use { service ->
                List(200) { async(start = CoroutineStart.UNDISPATCHED) { service.recommendations(100) } }.awaitAll()
                println("200 concurrent cold requests for one game -> IGDB requests: ${igdb.igdbRequests.get()}")
                assertEquals(2, igdb.igdbRequests.get())
            }
        }
    }

    /** A worst-case-sized pool: the discovery limits produce at most about 250 candidates. */
    @Test
    fun scoringTimePayloadAndCacheMemory() {
        enabled()
        val random = Random(42)

        fun ids(
            count: Int,
            range: IntRange,
        ) = List(count) { range.random(random) }.distinct()
        val pool =
            List(250) { index ->
                IgdbGame(
                    id = index + 2,
                    name = "Synthetic $index",
                    cover = IgdbCover("//images.igdb.com/igdb/image/upload/t_thumb/co$index.jpg"),
                    genres = ids(3, 1..30),
                    themes = ids(3, 1..45),
                    keywords = ids(60, 1..3000),
                    gameModes = ids(2, 1..6),
                    playerPerspectives = ids(2, 1..7),
                    collections = ids(1, 1..40),
                    firstReleaseDate = 1_400_000_000L + index * 1_000_000L,
                    totalRatingCount = random.nextInt(2000),
                    totalRating = 60.0 + random.nextInt(40),
                    similarGames = ids(10, 2..252),
                )
            }
        val source = pool.first().copy(id = 1, similarGames = (2..11).toList())
        val graph = SourceGraph(source, emptyMap(), pool.take(10), pool.drop(10).take(30))
        val payload = Json.encodeToJsonElement(pool).toString().length
        repeat(200) { RecommendationRanking.rank(graph, RecommendationRanking.candidatePool(graph, pool)) }
        val timings =
            List(1000) {
                measure { RecommendationRanking.rank(graph, RecommendationRanking.candidatePool(graph, pool)) }
            }
        println("scoring 250 candidates x 60 keywords: ${percentiles(timings)}")
        println("upstream JSON for that pool: ${payload / 1024} KiB")

        val runtime = Runtime.getRuntime()
        System.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val retained =
            List(512) { entry ->
                List(RecommendationRanking.MAX_RESULTS) {
                    Recommendation(
                        entry * 100 + it,
                        "Synthetic game title $it",
                        "//images.igdb.com/t_thumb/co$it.jpg",
                        0.5,
                    )
                }
            }
        System.gc()
        val after = runtime.totalMemory() - runtime.freeMemory()
        println("approximate heap for 512 full cache entries: ${(after - before) / 1024 / 1024} MiB (${retained.size})")
    }

    private inline fun measure(block: () -> Unit): Long {
        val started = System.nanoTime()
        block()
        return System.nanoTime() - started
    }
}
