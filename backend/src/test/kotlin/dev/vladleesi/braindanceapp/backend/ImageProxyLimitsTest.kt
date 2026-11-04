// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Memory and upstream bounds of the giveaway image proxy. */
class ImageProxyLimitsTest {
    private class MutableClock(
        var nowMillis: Long = 0,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }

    /** Image upstream that counts calls and concurrent fetches; fetches wait for [gate] when it is set. */
    private class ImageUpstream(
        var status: HttpStatusCode = HttpStatusCode.OK,
        var gate: CompletableDeferred<Unit>? = null,
    ) : Upstream {
        val calls = AtomicInteger()
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()

        override suspend fun image(url: String): UpstreamResponse {
            calls.incrementAndGet()
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            try {
                gate?.await()
                return UpstreamResponse(status, headersOf(HttpHeaders.ContentType, "image/jpeg"), ByteArray(16))
            } finally {
                inFlight.decrementAndGet()
            }
        }

        override suspend fun token(
            clientId: String,
            clientSecret: String,
        ) = error("unused")

        override suspend fun igdb(
            path: String,
            query: String,
            clientId: String,
            token: String,
        ) = error("unused")

        override suspend fun giveaway(url: String) = error("unused")
    }

    private fun imageUrl(index: Int) = "https://www.gamerpower.com/offers/$index/a.jpg"

    @Test
    fun concurrentFetchesAreBoundedAndTheRestWaitInsteadOfFailing(): Unit =
        runBlocking {
            val gate = CompletableDeferred<Unit>()
            val upstream = ImageUpstream(gate = gate)
            val service = GiveawayService(upstream, maxConcurrentImageFetches = 4, imagePermitWait = 10.seconds)

            val requests = (1..10).map { index -> async { service.image(imageUrl(index)).first.status } }
            withTimeout(5.seconds) { while (upstream.inFlight.get() < 4) yield() }
            repeat(20) { yield() }
            assertEquals(4, upstream.inFlight.get(), "only four fetches may buffer at once")

            gate.complete(Unit)
            assertEquals(List(10) { 200 }, requests.awaitAll())
            assertEquals(4, upstream.maxInFlight.get())
            assertEquals(10, upstream.calls.get())
        }

    @Test
    fun aFetchThatCannotGetAPermitInTimeIsBusy(): Unit =
        runBlocking {
            val gate = CompletableDeferred<Unit>()
            val upstream = ImageUpstream(gate = gate)
            val service = GiveawayService(upstream, maxConcurrentImageFetches = 1, imagePermitWait = 50.milliseconds)

            val holder = async { service.image(imageUrl(1)) }
            withTimeout(5.seconds) { while (upstream.inFlight.get() < 1) yield() }

            assertEquals(503, service.image(imageUrl(2)).first.status)
            // A busy proxy says nothing about the link, so the next attempt fetches it.
            gate.complete(Unit)
            holder.await()
            assertEquals(200, service.image(imageUrl(2)).first.status)
        }

    @Test
    fun failedImagesAreNotFetchedAgainForAMinute(): Unit =
        runBlocking {
            val upstream = ImageUpstream(status = HttpStatusCode.NotFound)
            val clock = MutableClock()
            val service = GiveawayService(upstream, clock)

            assertEquals(502, service.image(imageUrl(1)).first.status)
            assertEquals(502, service.image(imageUrl(1)).first.status)
            assertEquals(1, upstream.calls.get())

            clock.nowMillis += 60_001
            upstream.status = HttpStatusCode.OK
            assertEquals(200, service.image(imageUrl(1)).first.status)
            assertEquals(2, upstream.calls.get())
        }

    @Test
    fun imagesAboveTheCapAreRejectedBeforeReading(): Unit =
        runBlocking {
            val upstream =
                object : Upstream by ImageUpstream() {
                    override suspend fun image(url: String) =
                        UpstreamResponse(
                            HttpStatusCode.OK,
                            headersOf(
                                HttpHeaders.ContentType to listOf("image/jpeg"),
                                HttpHeaders.ContentLength to listOf((MAX_IMAGE_BYTES + 1).toString()),
                            ),
                            ByteArray(16),
                        )
                }
            assertEquals(502, GiveawayService(upstream).image(imageUrl(1)).first.status)
        }
}
