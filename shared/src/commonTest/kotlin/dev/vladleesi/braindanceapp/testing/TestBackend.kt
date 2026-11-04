// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.testing

import dev.vladleesi.braindanceapp.data.api.clients.BackendClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher

const val TEST_BACKEND_URL = "https://backend.test"

/** Builds the production backend client around a mock engine that runs on [dispatcher]. */
fun mockBackendClient(
    dispatcher: CoroutineDispatcher,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient {
    val engine =
        MockEngine(
            MockEngineConfig().apply {
                this.dispatcher = dispatcher
                addHandler(handler)
            },
        )
    return BackendClient.build(debugHttpLogging = false, baseUrl = TEST_BACKEND_URL, engine = engine)
}

fun MockRequestHandleScope.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData = respond(json, status, headersOf(HttpHeaders.ContentType, "application/json"))
