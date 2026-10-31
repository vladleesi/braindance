// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Upstream protections that keep visitor traffic from multiplying IGDB and GamerPower requests. */
class UpstreamProtectionTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    /** Counts upstream calls by kind and lets a test fail a specific call. */
    private class CountingUpstream(
        private val giveawayStatus: () -> HttpStatusCode = { HttpStatusCode.OK },
    ) : Upstream {
        val igdbCalls = AtomicInteger()
        val giveawayCalls = AtomicInteger()
        val imageCalls = AtomicInteger()
        private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        override suspend fun token(
            clientId: String,
            clientSecret: String,
        ) = UpstreamResponse(
            HttpStatusCode.OK,
            jsonHeaders,
            """{"access_token":"t","expires_in":3600}""".encodeToByteArray(),
        )

        override suspend fun igdb(
            path: String,
            query: String,
            clientId: String,
            token: String,
        ): UpstreamResponse {
            igdbCalls.incrementAndGet()
            return UpstreamResponse(HttpStatusCode.OK, jsonHeaders, """[{"id":42}]""".encodeToByteArray())
        }

        override suspend fun giveaway(url: String): UpstreamResponse {
            giveawayCalls.incrementAndGet()
            val body = """[{"id":1,"image":"https://www.gamerpower.com/offers/1/a.jpg"}]"""
            return UpstreamResponse(giveawayStatus(), jsonHeaders, body.encodeToByteArray())
        }

        override suspend fun image(url: String): UpstreamResponse {
            imageCalls.incrementAndGet()
            return UpstreamResponse(HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "image/jpeg"), ByteArray(64))
        }
    }

    @Test
    fun cacheBypassingRequestsDoNotMultiplyUpstreamCalls(): Unit =
        testApplication {
            val upstream = CountingUpstream()
            application { api(config, IgdbService(upstream, RateLimiter { true }, config), GiveawayService(upstream)) }
            val image = "/v1/giveaways/image?url=https%3A%2F%2Fwww.gamerpower.com%2Foffers%2F1%2Fa.jpg"
            for (route in listOf("/v1/games/details?id=42", "/v1/giveaways", "/v1/giveaways/1", image)) {
                repeat(3) {
                    // A cookie bypasses public edge caching, so these requests always reach the backend.
                    val response = client.get(route) { header(HttpHeaders.Cookie, "session=$it") }
                    assertEquals(HttpStatusCode.OK, response.status, route)
                }
            }
            assertEquals(1, upstream.igdbCalls.get())
            assertEquals(2, upstream.giveawayCalls.get())
            assertEquals(1, upstream.imageCalls.get())

            // The default anticipated timestamp is rounded, so one cache period shares one IGDB query.
            repeat(3) { client.get("/v1/games/anticipated?pageSize=20") }
            assertEquals(2, upstream.igdbCalls.get())
        }

    @Test
    fun burstWithinIgdbRateLimitIsServedFromOneCall(): Unit =
        testApplication {
            val upstream = CountingUpstream()
            // The real limiter: four IGDB calls per rolling second, rejecting further calls immediately.
            application { api(config, IgdbService(upstream, InMemoryRateLimiter(), config), GiveawayService(upstream)) }
            val statuses =
                coroutineScope {
                    List(50) { index ->
                        async {
                            client
                                .get("/v1/games/popularity?type=34&pageSize=40") {
                                    header(HttpHeaders.Cookie, "s=$index")
                                }.status
                        }
                    }.awaitAll()
                }
            println(
                "burst of 50: ${statuses.groupingBy { it.value }.eachCount()}, IGDB calls: ${upstream.igdbCalls.get()}",
            )
            assertTrue(statuses.all { it == HttpStatusCode.OK })
            assertEquals(1, upstream.igdbCalls.get())
        }

    @Test
    fun upstreamErrorsAreNotCached(): Unit =
        testApplication {
            var status = HttpStatusCode.ServiceUnavailable
            val upstream = CountingUpstream { status }
            application { api(config, IgdbService(upstream, RateLimiter { true }, config), GiveawayService(upstream)) }
            assertEquals(HttpStatusCode.BadGateway, client.get("/v1/giveaways").status)
            status = HttpStatusCode.OK
            assertEquals(HttpStatusCode.OK, client.get("/v1/giveaways").status)
            assertEquals(2, upstream.giveawayCalls.get())
        }

    @Test
    fun igdbOpenRequestsStayWithinUpstreamLimit(): Unit =
        runBlocking {
            val open = AtomicInteger()
            val peak = AtomicInteger()
            val release = CompletableDeferred<Unit>()
            val upstream =
                object : Upstream by CountingUpstream() {
                    override suspend fun igdb(
                        path: String,
                        query: String,
                        clientId: String,
                        token: String,
                    ): UpstreamResponse {
                        peak.accumulateAndGet(open.incrementAndGet(), ::maxOf)
                        release.await()
                        open.decrementAndGet()
                        return UpstreamResponse(HttpStatusCode.OK, json, "[]".encodeToByteArray())
                    }
                }
            val service = IgdbService(upstream, RateLimiter { true }, config)
            val calls =
                List(20) { index ->
                    async(start = CoroutineStart.UNDISPATCHED) { service.execute("/v4/games", "where id = $index;") }
                }
            // Undispatched callers have all reached the upstream call or the permit wait at this point.
            release.complete(Unit)
            assertTrue(calls.awaitAll().all { it.status == 200 })
            assertEquals(8, peak.get())
        }

    @Test
    fun upstreamRedirectsAreNotFollowed(): Unit =
        runBlocking {
            val requests = AtomicInteger()
            val engine =
                MockEngine {
                    requests.incrementAndGet()
                    respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "http://169.254.169.254/"))
                }
            HttpClient(engine) { upstreamDefaults() }.use { client ->
                val upstream = KtorUpstream(client)
                assertEquals(302, upstream.image("https://www.gamerpower.com/offers/1/a.jpg").status.value)
                assertEquals(1, requests.get())
                val (result, _) = GiveawayService(upstream).image("https://www.gamerpower.com/offers/1/a.jpg")
                assertEquals(502, result.status)
                assertEquals(2, requests.get())
            }
        }
}
