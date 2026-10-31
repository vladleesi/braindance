// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecommendationServiceTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)

    private class MutableClock(
        var now: Long = 1_000_000L,
    ) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }

    private fun service(
        igdb: FakeIgdb,
        clock: Clock = Clock.systemUTC(),
        limiter: RateLimiter = RateLimiter { true },
        log: (String) -> Unit = {},
    ) = RecommendationService(IgdbService(igdb, limiter, config), clock, log)

    private fun fake() = FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames)

    @Test
    fun cacheHitsAvoidUpstreamRequests() =
        runBlocking {
            val igdb = fake()
            val logs = mutableListOf<String>()
            service(igdb, log = { logs += it }).use { service ->
                val first = service.recommendations(100)
                assertEquals(2, igdb.igdbRequests.get())
                assertEquals(first, service.recommendations(100))
                assertEquals(2, igdb.igdbRequests.get())
                // Cache hits are not logged; the miss emits one structured event without credentials.
                assertEquals(1, logs.size)
                assertContains(logs.single(), "\"igdb_requests\":2")
                assertFalse(logs.single().contains("test-token") || logs.single().contains("secret"))
            }
        }

    @Test
    fun concurrentMissesShareOneComputation() =
        runBlocking {
            val igdb = fake()
            val gate = CompletableDeferred<Unit>()
            igdb.gate = gate
            service(igdb).use { service ->
                // Undispatched callers reach the shared computation before any upstream request is released.
                val callers = List(10) { async(start = CoroutineStart.UNDISPATCHED) { service.recommendations(200) } }
                gate.complete(Unit)
                val results = callers.awaitAll()
                assertEquals(1, results.distinct().size)
                assertEquals(2, igdb.igdbRequests.get())
            }
        }

    @Test
    fun discoveryFailureFallsBackToRankedNativeNeighbors() =
        runBlocking {
            val igdb = fake()
            igdb.failures = mapOf(1 to HttpStatusCode.InternalServerError)
            val clock = MutableClock()
            service(igdb, clock).use { service ->
                val result = service.recommendations(100)
                assertEquals(200, result.status)
                assertEquals(RecommendationResult.Kind.DEGRADED, result.kind)
                val native = SyntheticCatalog.games.first { it.id == 100 }.similarGames
                assertTrue(result.games.isNotEmpty() && result.games.all { it.id in native }, "${result.games}")
                // Editions and DLC from the native list stay filtered in the fallback.
                assertTrue(result.games.none { it.id in setOf(130, 131) })
                // Degraded answers are cached briefly, then discovery is retried.
                service.recommendations(100)
                assertEquals(2, igdb.igdbRequests.get())
                clock.now += 3 * 60 * 1000L
                assertEquals(RecommendationResult.Kind.COMPLETE, service.recommendations(100).kind)
                assertEquals(4, igdb.igdbRequests.get())
            }
        }

    @Test
    fun expiredResultsServeStaleOnlyWhenRefreshFails() =
        runBlocking {
            val igdb = fake()
            val clock = MutableClock()
            service(igdb, clock).use { service ->
                val fresh = service.recommendations(300)
                clock.now += 25 * 60 * 60 * 1000L
                igdb.failures = mapOf(2 to HttpStatusCode.ServiceUnavailable, 3 to HttpStatusCode.ServiceUnavailable)
                val stale = service.recommendations(300)
                assertEquals(RecommendationResult.Kind.STALE, stale.kind)
                assertEquals(fresh.games, stale.games)
                clock.now += 7 * 24 * 60 * 60 * 1000L
                assertEquals(502, service.recommendations(300).status)
            }
        }

    @Test
    fun upstreamErrorsMapToStableStatuses() =
        runBlocking {
            val missing = fake()
            service(missing).use { assertEquals(404, it.recommendations(999_999).status) }
            assertEquals(1, missing.igdbRequests.get())

            val limited = fake().apply { failures = mapOf(0 to HttpStatusCode.TooManyRequests) }
            service(limited).use { assertEquals(429, it.recommendations(100).status) }

            val malformed =
                object : Upstream by fake() {
                    override suspend fun igdb(
                        path: String,
                        query: String,
                        clientId: String,
                        token: String,
                    ) = UpstreamResponse(
                        HttpStatusCode.OK,
                        io.ktor.http.headersOf(io.ktor.http.HttpHeaders.ContentType, "application/json"),
                        """[{"name":"source","result":[{"id":"not-a-number","genres":"x"}]}""".encodeToByteArray(),
                    )
                }
            RecommendationService(IgdbService(malformed, RateLimiter { true }, config), log = {}).use {
                assertEquals(502, it.recommendations(100).status)
            }
        }

    @Test
    fun saturatedLimiterIsAwaitedBriefly() =
        runBlocking {
            val igdb = fake()
            val denials = AtomicInteger(2)
            val limiter = RateLimiter { denials.getAndDecrement() <= 0 }
            service(igdb, limiter = limiter).use { service ->
                assertEquals(RecommendationResult.Kind.COMPLETE, service.recommendations(700).kind)
            }
        }

    @Test
    fun discoveryQueriesUseVerifiedFilterSemantics() =
        runBlocking {
            val igdb = fake()
            service(igdb).use { it.recommendations(100) }
            val source = igdb.bodies[0]
            assertContains(source, "where id = 100;limit 1;")
            assertContains(source, "where similar_games = (100)")
            val discovery = igdb.bodies[1]
            // Distinctive genres are required together ([a,b] is AND); themes and perspectives match any (OR).
            assertContains(discovery, "genres = [5,10] & themes = (38) & player_perspectives = (1,2)")
            assertContains(discovery, "(collections = (900))")
            assertContains(discovery, "themes != (42)")
            val keywords =
                Regex("keywords = \\(([0-9,]+)\\)")
                    .find(discovery)
                    ?.groupValues
                    ?.get(1)
                    .orEmpty()
            assertEquals(listOf(1001, 1002, 1003, 1004, 1005, 1015), keywords.split(',').map(String::toInt))
            // Every subquery is bounded, and the whole discovery fits one multiquery (at most 10 subqueries).
            assertTrue(Regex("query games").findAll(discovery).count() <= 10)
            assertTrue(Regex("limit ([0-9]+);").findAll(discovery).all { it.groupValues[1].toInt() <= 500 })
        }
}
