// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import dev.vladleesi.braindanceapp.logger.BLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** The backend answers within its 4.5-second budget, so each attempt gives up after five seconds. */
internal const val REQUEST_TIMEOUT_MILLIS = 5_000L

/** Retries after the first attempt; more would multiply backend load during an outage. */
internal const val MAX_RETRIES = 2
private const val MAX_RETRY_DELAY_MILLIS = 2_000L
private val retriedStatuses =
    setOf(HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable, HttpStatusCode.GatewayTimeout)

fun HttpClientConfig<*>.defaultConfig(debugHttpLogging: Boolean) {
    expectSuccess = true
    // Retry must be installed before the timeout, so each attempt gets its own timeout and a timed-out attempt can be
    // retried. A cold backend answers its first request with 504 after about 4.5 seconds; the retry then joins the
    // refresh it started.
    retryConfig()
    timeoutConfig()
    loggerConfig(debugHttpLogging)
    jsonConfig()
    errorHandlerConfig()
}

// Only the request timeout: it bounds each attempt on every engine, and the browser engine supports no other.
private fun HttpClientConfig<*>.timeoutConfig() {
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}

// Retries only gateway failures and transport errors, including timeouts. Client errors, including 429, and
// cancellation are final.
private fun HttpClientConfig<*>.retryConfig() {
    install(HttpRequestRetry) {
        retryIf(MAX_RETRIES) { _, response -> response.status in retriedStatuses }
        retryOnExceptionIf(MAX_RETRIES) { _, cause -> cause.isRetriableTransportFailure() }
        exponentialDelay(maxDelayMs = MAX_RETRY_DELAY_MILLIS)
    }
}

/**
 * A timed-out attempt reaches the retry plugin as a [CancellationException] whose cause chain holds the
 * [HttpRequestTimeoutException], so the chain is checked before excluding cancellation of the caller itself.
 */
private fun Throwable.isRetriableTransportFailure(): Boolean =
    when {
        generateSequence(this) { it.cause }.any { it is HttpRequestTimeoutException } -> true
        this is CancellationException || this is ResponseException -> false
        else -> true
    }

private fun HttpClientConfig<*>.loggerConfig(debugHttpLogging: Boolean) {
    if (!debugHttpLogging) return

    install(Logging) {
        level = LogLevel.INFO
        logger =
            object : Logger {
                override fun log(message: String) {
                    BLogger.debug(tag = "HTTP Client", message = message)
                }
            }
    }
}

/** JSON settings for backend responses; shared with tests so they decode exactly like the app. */
internal val backendJson =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
        // Set default value to expected values
        isLenient = true
    }

private fun HttpClientConfig<*>.jsonConfig() {
    install(ContentNegotiation) {
        json(json = backendJson)
    }
}

@Suppress("ForbiddenComment")
private fun HttpClientConfig<*>.errorHandlerConfig() {
    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            // TODO: Handle it inside coroutines
            throw cause
        }
    }
}
