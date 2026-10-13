// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class KtorUpstreamTest {
    @Test
    fun concurrentFirstRequestsShareOneClient(): Unit =
        runBlocking {
            val creations = AtomicInteger()
            var client: HttpClient? = null
            val upstream =
                KtorUpstream {
                    creations.incrementAndGet()
                    HttpClient(
                        MockEngine {
                            respond("[]", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                        },
                    ).also { client = it }
                }
            try {
                assertEquals(0, creations.get())
                val responses =
                    List(20) {
                        async(Dispatchers.Default) { upstream.giveaway("https://www.gamerpower.com/api/giveaways") }
                    }.awaitAll()
                assertEquals(1, creations.get())
                responses.forEach { assertEquals("[]", it.body.decodeToString()) }
            } finally {
                upstream.close()
                upstream.close()
            }
            val clientJob = checkNotNull(requireNotNull(client).coroutineContext[Job])
            clientJob.join()
            assertFalse(clientJob.isActive)
        }

    @Test
    fun shutdownBeforeFirstRequestDoesNotCreateClient(): Unit =
        runBlocking {
            val upstream = KtorUpstream { error("Client must not be created") }
            upstream.close()
            upstream.close()
            assertFailsWith<IllegalStateException> {
                upstream.giveaway("https://www.gamerpower.com/api/giveaways")
            }
        }

    @Test
    fun suppliedClientClosesWithoutRequests(): Unit =
        runBlocking {
            val client = HttpClient(MockEngine { respond("[]") })
            val clientJob = checkNotNull(client.coroutineContext[Job])
            KtorUpstream(client).close()
            clientJob.join()
            assertFalse(clientJob.isActive)
        }

    @Test
    fun healthAndRejectedRequestsDoNotCreateClient() =
        testApplication {
            val config = BackendConfig("id", "secret", null, emptySet(), 8080)
            KtorUpstream { error("Client must not be created") }.use { upstream ->
                application {
                    api(config, IgdbService(upstream, RateLimiter { true }, config), GiveawayService(upstream))
                }
                assertEquals(HttpStatusCode.OK, client.get("/health").status)
                assertEquals(HttpStatusCode.NotFound, client.get("/missing").status)
                assertEquals(HttpStatusCode.UnsupportedMediaType, client.post("/v1/games/details").status)
                assertEquals(
                    HttpStatusCode.BadRequest,
                    client.post("/v1/games/details") { header(HttpHeaders.ContentType, "application/json") }.status,
                )
                assertEquals(HttpStatusCode.BadRequest, client.get("/v1/giveaways/image").status)
            }
        }
}
