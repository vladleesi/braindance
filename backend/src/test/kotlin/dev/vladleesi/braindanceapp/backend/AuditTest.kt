// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuditTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)
    private val input = Json.parseToJsonElement("""{"id":42}""") as JsonObject

    @Test
    fun streamingLimitStopsUnfinishedBody(): Unit =
        runBlocking {
            val channel = ByteChannel(autoFlush = true)
            val producer =
                launch {
                    runCatching { while (true) channel.writeFully(ByteArray(8192)) }
                }
            HttpClient(
                MockEngine {
                    respond(channel, headers = headersOf("Content-Type", "application/json"))
                },
            ).use { client ->
                try {
                    withTimeout(2000) {
                        assertFailsWith<BodyTooLarge> {
                            KtorUpstream(
                                client,
                            ).giveaway("https://www.gamerpower.com/api/giveaways")
                        }
                    }
                } finally {
                    producer.cancelAndJoin()
                }
            }
        }

    @Test
    fun errorBodiesAreDiscarded(): Unit =
        runBlocking {
            val channel = ByteChannel(autoFlush = true)
            HttpClient(MockEngine { respond(channel, HttpStatusCode.BadGateway) }).use { client ->
                val response =
                    withTimeout(2000) { KtorUpstream(client).giveaway("https://www.gamerpower.com/api/giveaways") }
                assertEquals(502, response.status.value)
                assertEquals(0, response.body.size)
            }
        }

    @Test
    fun tokenTimeoutAndCancellation(): Unit =
        runBlocking {
            HttpClient(
                MockEngine {
                    delay(10_000)
                    respond("unused")
                },
            ) {
                install(HttpTimeout)
            }.use { client ->
                val upstream = KtorUpstream(client)
                assertFailsWith<TimeoutCancellationException> {
                    withTimeout(50) { upstream.token("id", "secret") }
                }
                assertFailsWith<HttpRequestTimeoutException> { upstream.token("id", "secret") }
            }
        }

    @Test
    fun rateSlotIsTakenAfterTokenWait(): Unit =
        runBlocking {
            var ready = false
            val remote = StubUpstream()
            remote.tokenHandler = {
                ready = true
                token("old")
            }
            val service =
                IgdbService(
                    remote,
                    RateLimiter {
                        check(ready)
                        true
                    },
                    config,
                )
            assertEquals(200, service.query("/v1/games/details", input).status)
        }

    @Test
    fun concurrent401ReusesFreshToken(): Unit =
        runBlocking {
            val remote = StubUpstream()
            var tokenRequests = 0
            val refreshed = CompletableDeferred<Unit>()
            val secondEntered = CompletableDeferred<Unit>()
            remote.tokenHandler = {
                tokenRequests++
                if (tokenRequests > 1) refreshed.complete(Unit)
                token(if (tokenRequests == 1) "old" else "new")
            }
            val service = IgdbService(remote, RateLimiter { true }, config)
            service.query("/v1/games/details", input)
            var oldCalls = 0
            remote.igdbHandler = { accessToken ->
                if (accessToken == "old") {
                    oldCalls++
                    if (oldCalls == 1) {
                        secondEntered.await()
                    } else {
                        secondEntered.complete(Unit)
                        refreshed.await()
                    }
                    response(401)
                } else {
                    response(200, "[]")
                }
            }
            withTimeout(2000) {
                List(2) { async { service.query("/v1/games/details", input) } }.awaitAll()
            }
            assertEquals(2, tokenRequests)
        }

    @Test
    fun tokenMustBeStringAndShortTokensExpire(): Unit =
        runBlocking {
            val remote = StubUpstream()
            remote.tokenHandler = { response(200, """{"access_token":null,"expires_in":3600}""") }
            assertFailsWith<IllegalStateException> {
                IgdbService(remote, RateLimiter { true }, config).query("/v1/games/details", input)
            }
            var calls = 0
            remote.tokenHandler = {
                calls++
                response(200, """{"access_token":"short","expires_in":60}""")
            }
            val service = IgdbService(remote, RateLimiter { true }, config)
            repeat(2) { service.query("/v1/games/details", input) }
            assertEquals(2, calls)
        }

    @Test
    fun queriesAndValidationMatchContract() {
        val service = IgdbService(StubUpstream(), RateLimiter { true }, config)

        fun query(
            path: String,
            json: String,
        ) = service.buildQuery(path, Json.parseToJsonElement(json) as JsonObject)
        assertEquals(
            "/v4/games" to (
                "fields name,platforms.name,cover.url;" +
                    "where first_release_date > 4102444800 & hypes > 0 & version_parent = null;" +
                    "sort hypes desc;limit 50;"
            ),
            query("/v1/games/anticipated", """{"currentTimestamp":4102444800,"pageSize":50}"""),
        )
        assertEquals(
            "/v4/games" to (
                "fields name,platforms.name,cover.url;where id = (42, 43) & age_ratings != null " +
                    "& age_ratings.rating_category != 7 & age_ratings.rating_category != 26 " +
                    "& age_ratings.rating_category != 38;sort hypes desc;limit 20;"
            ),
            query("/v1/games/popular", """{"ids":[42,43],"pageSize":20}"""),
        )
        for (json in listOf("""{"id":"42"}""", """{"id":2147483648}""", """{"id":1.5}""")) {
            assertNull(query("/v1/games/details", json))
        }
        assertNull(query("/v1/games/popular", """{"ids":[],"pageSize":20}"""))
        assertNull(query("/v1/games/popularity", """{"type":33,"pageSize":20}"""))
    }

    private fun response(
        status: Int,
        body: String = "",
    ) = UpstreamResponse(
        HttpStatusCode.fromValue(status),
        headersOf("Content-Type", "application/json"),
        body.encodeToByteArray(),
    )

    private fun token(value: String) = response(200, """{"access_token":"$value","expires_in":3600}""")

    private class StubUpstream : Upstream {
        var tokenHandler: suspend () -> UpstreamResponse = { error("Unexpected token request") }
        var igdbHandler: suspend (String) -> UpstreamResponse = {
            UpstreamResponse(HttpStatusCode.OK, headersOf("Content-Type", "application/json"), "[]".encodeToByteArray())
        }

        override suspend fun token(
            clientId: String,
            clientSecret: String,
        ) = tokenHandler()

        override suspend fun igdb(
            path: String,
            query: String,
            clientId: String,
            token: String,
        ) = igdbHandler(token)

        override suspend fun giveaway(url: String): UpstreamResponse = error("Unexpected giveaway request")

        override suspend fun image(url: String): UpstreamResponse = error("Unexpected image request")
    }
}
