// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class BackendClientTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun timeoutAndRetryPluginsAreInstalled() {
        val client = mockBackendClient(dispatcher) { respondJson("[]") }
        assertNotNull(client.pluginOrNull(HttpTimeout))
        assertNotNull(client.pluginOrNull(HttpRequestRetry))
    }

    @Test
    fun gatewayFailuresAreRetriedUntilSuccess() =
        runTest(dispatcher) {
            var calls = 0
            val client =
                mockBackendClient(dispatcher) {
                    calls++
                    if (calls < 3) respondJson("{}", HttpStatusCode.ServiceUnavailable) else respondJson("""["ok"]""")
                }

            assertEquals("""["ok"]""", client.get("v1/games/trending").bodyAsText())
            assertEquals(3, calls)
        }

    @Test
    fun retriesStopAfterTheLimit() =
        runTest(dispatcher) {
            var calls = 0
            val client =
                mockBackendClient(dispatcher) {
                    calls++
                    respondJson("{}", HttpStatusCode.BadGateway)
                }

            assertFailsWith<Exception> { client.get("v1/games/trending").bodyAsText() }
            assertEquals(1 + MAX_RETRIES, calls)
        }

    @Test
    fun clientErrorsAreNotRetried() =
        runTest(dispatcher) {
            for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.TooManyRequests)) {
                var calls = 0
                val client =
                    mockBackendClient(dispatcher) {
                        calls++
                        respondJson("{}", status)
                    }

                assertFailsWith<ClientRequestException> { client.get("v1/games/details?id=1") }
                assertEquals(1, calls, "status $status")
            }
        }

    @Test
    fun slowResponsesTimeOut() =
        runTest(dispatcher) {
            var calls = 0
            val client =
                mockBackendClient(dispatcher) {
                    calls++
                    delay(REQUEST_TIMEOUT_MILLIS * 2)
                    respondJson("[]")
                }

            assertFailsWith<HttpRequestTimeoutException> { client.get("v1/games/trending") }
            // A timeout is a transport failure, so it is retried like one.
            assertEquals(1 + MAX_RETRIES, calls)
        }

    @Test
    fun cancelledRequestsAreNotRetried() =
        runTest(dispatcher) {
            var calls = 0
            val client =
                mockBackendClient(dispatcher) {
                    calls++
                    delay(REQUEST_TIMEOUT_MILLIS / 2)
                    respondJson("[]")
                }

            val request = launch { client.get("v1/games/trending") }
            advanceTimeBy(REQUEST_TIMEOUT_MILLIS / 4)
            request.cancel()
            advanceUntilIdle()

            assertEquals(1, calls)
        }
}
