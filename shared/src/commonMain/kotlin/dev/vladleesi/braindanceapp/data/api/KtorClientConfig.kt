// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import dev.vladleesi.braindanceapp.logger.BLogger
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.retry
import io.ktor.client.plugins.timeout
import io.ktor.client.request.request
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIMEOUT_SECONDS = 5000L
private const val MAX_RETRIES = 5

fun HttpClientConfig<*>.defaultConfig(debugHttpLogging: Boolean) {
    expectSuccess = true
    engineConfig()
    loggerConfig(debugHttpLogging)
    jsonConfig()
    errorHandlerConfig()
}

private fun HttpClientConfig<*>.engineConfig() {
    engine {
        request {
            timeout {
                requestTimeoutMillis = TIMEOUT_SECONDS
                socketTimeoutMillis = TIMEOUT_SECONDS
            }
            retry {
                retryOnExceptionOrServerErrors(maxRetries = MAX_RETRIES)
            }
        }
    }
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

private fun HttpClientConfig<*>.jsonConfig() {
    install(ContentNegotiation) {
        json(
            json =
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    explicitNulls = false
                    // Set default value to expected values
                    isLenient = true
                },
        )
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
