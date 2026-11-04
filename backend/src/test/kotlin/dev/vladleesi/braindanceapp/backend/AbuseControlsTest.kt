// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Anonymous traffic must not be able to spend the IGDB budget that every visitor's home feed depends on. */
class AbuseControlsTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)
    private val secret = "s".repeat(32)

    private class CountingIgdb : Upstream {
        val calls = AtomicInteger()

        override suspend fun token(
            clientId: String,
            clientSecret: String,
        ) = UpstreamResponse(
            HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "application/json"),
            """{"access_token":"t","expires_in":3600}""".encodeToByteArray(),
        )

        override suspend fun igdb(
            path: String,
            query: String,
            clientId: String,
            token: String,
        ): UpstreamResponse {
            calls.incrementAndGet()
            return UpstreamResponse(
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
                "[]".encodeToByteArray(),
            )
        }

        override suspend fun giveaway(url: String) = error("unused")

        override suspend fun image(url: String) = error("unused")
    }

    private class MutableClock(
        var nowMillis: Long = 0,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }

    @Test
    fun aClientGetsItsBurstThenWaitsForTokens() {
        var now = 0L
        val limiter = ClientRateLimiter(burst = 2, perMinute = 60, nanoTime = { now })

        assertNull(limiter.take("a"))
        assertNull(limiter.take("a"))
        assertEquals(1L, limiter.take("a"), "one token refills every second")
        assertNull(limiter.take("b"), "clients have separate buckets")

        now += 1_000_000_000L
        assertNull(limiter.take("a"))
    }

    @Test
    fun forgottenClientsStartWithAFullBurst() {
        val limiter = ClientRateLimiter(burst = 1, perMinute = 1, maxClients = 2, nanoTime = { 0L })
        assertNull(limiter.take("a"))
        assertNull(limiter.take("b"))
        assertNull(limiter.take("c"))
        // "a" was the least recently seen client and was evicted; forgetting only ever allows a request.
        assertNull(limiter.take("a"))
    }

    @Test
    fun perGameCallsCannotTakeTheFeedsSlot(): Unit =
        runBlocking {
            val shared = InMemoryRateLimiter(IGDB_REQUESTS_PER_SECOND, nanoTime = { 0L })
            val perGame = InMemoryRateLimiter(PER_GAME_REQUESTS_PER_SECOND, nanoTime = { 0L })
            val igdb = IgdbService(CountingIgdb(), shared, config, perGameLimiter = perGame)

            repeat(PER_GAME_REQUESTS_PER_SECOND) {
                assertEquals(200, igdb.execute("/v4/games", "q$it", lane = IgdbLane.PER_GAME).status)
            }
            assertEquals(429, igdb.execute("/v4/games", "extra", lane = IgdbLane.PER_GAME).status)
            assertEquals(200, igdb.execute("/v4/multiquery", "feed", lane = IgdbLane.FEED).status)
        }

    @Test
    fun guardedRequestsAreLimitedPerCloudflareClient() =
        testApplication {
            val guarded = config.copy(cloudflareOriginSecret = secret)
            val igdb = IgdbService(CountingIgdb(), RateLimiter { true }, guarded)
            application {
                api(
                    guarded,
                    igdb,
                    GiveawayService(CountingIgdb()),
                    RecommendationService(igdb),
                    TrendingService(igdb),
                    clientLimiter = ClientRateLimiter(burst = 2, perMinute = 1),
                )
            }

            suspend fun details(
                ip: String,
                id: Int,
            ) = client.get("/v1/games/details?id=$id") {
                header(ORIGIN_SECRET_HEADER, secret)
                header("CF-Connecting-IP", ip)
            }

            assertEquals(HttpStatusCode.OK, details("203.0.113.7", 1).status)
            assertEquals(HttpStatusCode.OK, details("203.0.113.7", 2).status)
            val limited = details("203.0.113.7", 3)
            assertEquals(HttpStatusCode.TooManyRequests, limited.status)
            assertEquals("60", limited.headers[HttpHeaders.RetryAfter])
            assertEquals(HttpStatusCode.OK, details("198.51.100.9", 4).status)
        }

    @Test
    fun withoutTheGuardClientsAreNotLimitedByASharedAddress() =
        testApplication {
            val igdb = IgdbService(CountingIgdb(), RateLimiter { true }, config)
            application {
                api(
                    config,
                    igdb,
                    GiveawayService(CountingIgdb()),
                    RecommendationService(igdb),
                    TrendingService(igdb),
                    clientLimiter = ClientRateLimiter(burst = 1, perMinute = 1),
                )
            }
            // A spoofed Cloudflare header is ignored without the guard, and the proxy address is not used as a key.
            repeat(3) { id ->
                val response = client.get("/v1/games/details?id=${id + 1}") { header("CF-Connecting-IP", "1.1.1.1") }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }

    @Test
    fun unknownGamesAreRememberedForAnHour(): Unit =
        runBlocking {
            val upstream = FakeIgdb(emptyList())
            val clock = MutableClock()
            RecommendationService(IgdbService(upstream, RateLimiter { true }, config), clock, log = {}).use {
                assertEquals(404, it.recommendations(999_999).status)
                clock.nowMillis += 59 * 60_000L
                assertEquals(404, it.recommendations(999_999).status)
                assertEquals(1, upstream.igdbRequests.get(), "the unknown ID was not looked up again")

                clock.nowMillis += 2 * 60_000L
                it.recommendations(999_999)
                assertEquals(2, upstream.igdbRequests.get())
            }
        }
}
