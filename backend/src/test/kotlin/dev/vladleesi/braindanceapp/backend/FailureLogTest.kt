// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FailureLogTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private class StubUpstream(
        val token: () -> UpstreamResponse,
        val igdb: () -> UpstreamResponse,
    ) : Upstream {
        override suspend fun token(
            clientId: String,
            clientSecret: String,
        ) = token()

        override suspend fun igdb(
            path: String,
            query: String,
            clientId: String,
            token: String,
        ) = igdb()

        override suspend fun giveaway(url: String) = error("unused")

        override suspend fun image(url: String) = error("unused")
    }

    private fun validToken() =
        UpstreamResponse(
            HttpStatusCode.OK,
            jsonHeaders,
            """{"access_token":"t","expires_in":3600}""".encodeToByteArray(),
        )

    /** Requests game details through the real API and returns the response status and the written log lines. */
    private fun detailsRequest(
        upstream: Upstream,
        rateLimiter: RateLimiter = RateLimiter { true },
    ): Pair<HttpStatusCode, List<JsonObject>> {
        val lines = Collections.synchronizedList(mutableListOf<String>())
        val failures = FailureLog(log = lines::add)
        var status = HttpStatusCode.OK
        testApplication {
            application {
                val igdb = IgdbService(upstream, rateLimiter, config, failures = failures)
                api(
                    config,
                    igdb,
                    GiveawayService(upstream),
                    RecommendationService(igdb),
                    TrendingService(igdb),
                    failures,
                )
            }
            status = client.get("/v1/games/details?id=4242").status
        }
        return status to lines.map { Json.parseToJsonElement(it).jsonObject }
    }

    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

    @Test
    fun unexpectedExceptionsAreLoggedOnceWithoutTheQuery() {
        val (status, lines) =
            detailsRequest(
                StubUpstream(token = ::validToken, igdb = { throw IllegalStateException("boom at ?id=4242") }),
            )

        assertEquals(HttpStatusCode.BadGateway, status)
        val line = lines.single()
        assertEquals("request_failed", line.text("event"))
        assertEquals("/v1/games/details", line.text("route"))
        assertEquals("IllegalStateException", line.text("error"))
        assertFalse(line.toString().contains("4242"), "the log must not contain the query or exception message")
    }

    @Test
    fun tokenFailuresNameTheirReasonAndUpstreamStatus() {
        val (status, lines) =
            detailsRequest(
                StubUpstream(
                    token = { UpstreamResponse(HttpStatusCode.BadRequest, jsonHeaders, byteArrayOf()) },
                    igdb = { error("unused") },
                ),
            )

        assertEquals(HttpStatusCode.BadGateway, status)
        val line = lines.single()
        assertEquals("TokenUnavailable", line.text("error"))
        assertEquals("request_failed", line.text("reason"))
        assertEquals("400", line.text("upstream_status"))
    }

    @Test
    fun igdbErrorStatusesAreLogged() {
        val (status, lines) =
            detailsRequest(
                StubUpstream(
                    token = ::validToken,
                    igdb = { UpstreamResponse(HttpStatusCode.InternalServerError, jsonHeaders, byteArrayOf()) },
                ),
            )

        assertEquals(HttpStatusCode.BadGateway, status)
        val line = lines.single()
        assertEquals("upstream_failed", line.text("event"))
        assertEquals("igdb", line.text("operation"))
        assertEquals("500", line.text("upstream_status"))
    }

    @Test
    fun limiterRejectionsAreLogged() {
        val (status, lines) =
            detailsRequest(StubUpstream(token = ::validToken, igdb = { error("unused") }), RateLimiter { false })

        assertEquals(HttpStatusCode.TooManyRequests, status)
        assertEquals("limiter_rejected", lines.single().text("event"))
    }

    @Test
    fun repeatedFailuresAreSuppressedWithinTheIntervalAndCounted() {
        val lines = mutableListOf<String>()
        var now = 0L
        val failures = FailureLog(log = lines::add, nanoTime = { now }, interval = 1_000)

        repeat(5) { failures.limiterRejected("igdb") }
        failures.upstreamFailed("igdb", 503)
        now += 1_000
        failures.limiterRejected("igdb")

        assertEquals(3, lines.size, "one line per event and subject per interval")
        val last = Json.parseToJsonElement(lines.last()).jsonObject
        assertEquals("limiter_rejected", last.text("event"))
        assertEquals("4", last.text("suppressed"))
    }
}
