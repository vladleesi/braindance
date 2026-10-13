// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber")

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.utils.io.readAvailable
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val JSON_CONTENT_TYPE = "application/json"

class BodyTooLarge : Exception()

/** Contains only a bounded body; the network response is closed before returning to a service. */
data class UpstreamResponse(
    val status: HttpStatusCode,
    val headers: Headers,
    val body: ByteArray,
)

suspend fun HttpResponse.readLimited(limit: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    val channel = bodyAsChannel()
    while (true) {
        val count = channel.readAvailable(buffer)
        if (count == -1) break
        if (output.size() + count > limit) throw BodyTooLarge()
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

interface Upstream {
    suspend fun token(
        clientId: String,
        clientSecret: String,
    ): UpstreamResponse

    suspend fun igdb(
        path: String,
        query: String,
        clientId: String,
        token: String,
    ): UpstreamResponse

    suspend fun giveaway(url: String): UpstreamResponse

    suspend fun image(url: String): UpstreamResponse
}

class KtorUpstream(
    createClient: () -> HttpClient,
) : Upstream,
    AutoCloseable {
    constructor(client: HttpClient) : this({ client }) {
        clientDelegate.value
    }

    private val clientLock = Any()
    private var closed = false
    private val clientDelegate =
        lazy(clientLock) {
            check(!closed) { "Upstream is closed" }
            createClient()
        }
    private val client by clientDelegate
    private val measuredOperations = ConcurrentHashMap.newKeySet<String>()

    override fun close() {
        synchronized(clientLock) {
            if (!closed) {
                closed = true
                if (clientDelegate.isInitialized()) client.close()
            }
        }
    }

    override suspend fun token(
        clientId: String,
        clientSecret: String,
    ) = measured("twitch_token") {
        client
            .preparePost("https://id.twitch.tv/oauth2/token") {
                header(HttpHeaders.ContentType, "application/x-www-form-urlencoded")
                timeout { requestTimeoutMillis = 5000 }
                setBody(
                    "client_id=${clientId.encodeURLParameter()}&client_secret=" +
                        "${clientSecret.encodeURLParameter()}&grant_type=client_credentials",
                )
            }.bounded(2048, null)
    }

    override suspend fun igdb(
        path: String,
        query: String,
        clientId: String,
        token: String,
    ) = measured("igdb") {
        client
            .preparePost("https://api.igdb.com$path") {
                header("Client-ID", clientId)
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.ContentType, "text/plain")
                timeout { requestTimeoutMillis = 8000 }
                setBody(query)
            }.bounded(1024 * 1024, JSON_CONTENT_TYPE)
    }

    override suspend fun giveaway(url: String) =
        measured("giveaway") {
            client
                .prepareGet(url) {
                    header(HttpHeaders.Accept, JSON_CONTENT_TYPE)
                    timeout { requestTimeoutMillis = 8000 }
                }.bounded(2 * 1024 * 1024, JSON_CONTENT_TYPE)
        }

    override suspend fun image(url: String) =
        measured("image") {
            client
                .prepareGet(url) {
                    header(HttpHeaders.Accept, "image/*")
                    timeout { requestTimeoutMillis = 8000 }
                }.bounded(5 * 1024 * 1024, "image/")
        }

    private suspend fun measured(
        operation: String,
        block: suspend () -> UpstreamResponse,
    ): UpstreamResponse {
        if (!measuredOperations.add(operation)) return block()
        // Resolve the shared client first, keeping its initialization out of the upstream duration.
        client
        val started = System.nanoTime()
        var status: Int? = null
        try {
            return block().also { status = it.status.value }
        } finally {
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            println(
                "{\"event\":\"backend_first_upstream\",\"operation\":\"$operation\"," +
                    "\"duration_ms\":$duration,\"status\":$status}",
            )
        }
    }
}

private suspend fun HttpStatement.bounded(
    limit: Int,
    expectedType: String?,
): UpstreamResponse =
    execute { response ->
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
        val acceptedType =
            when (expectedType) {
                null -> true
                "image/" -> contentType.startsWith(expectedType)
                else -> contentType.contains(expectedType)
            }
        val body =
            if (response.status.value in 200..299 && acceptedType) {
                if ((response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0) > limit) throw BodyTooLarge()
                response.readLimited(limit)
            } else {
                byteArrayOf()
            }
        UpstreamResponse(response.status, response.headers, body)
    }
