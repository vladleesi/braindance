// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.clients

import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.data.api.defaultConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest

object BackendClient {
    /**
     * Builds the backend client. [engine] is null in the app, which selects the platform engine; tests pass a mock
     * engine and a [baseUrl] of their own.
     */
    fun build(
        debugHttpLogging: Boolean,
        baseUrl: String = BuildKonfig.BACKEND_BASE_URL,
        engine: HttpClientEngine? = null,
    ): HttpClient {
        val config: HttpClientConfig<*>.() -> Unit = {
            defaultConfig(debugHttpLogging)
            defaultRequestConfig(baseUrl)
        }
        return if (engine == null) HttpClient(config) else HttpClient(engine, config)
    }
}

/**
 * Whether [baseUrl] may be the backend's: HTTPS, or plain HTTP to this computer for an API run locally, since such
 * traffic never leaves it. Any other HTTP address is refused.
 */
internal fun isAllowedBackendUrl(baseUrl: String): Boolean =
    baseUrl.startsWith("https://") || LocalHttpUrl.matches(baseUrl)

private val LocalHttpUrl = Regex("""^http://(localhost|127\.0\.0\.1)(:\d{1,5})?(/.*)?$""")

private fun HttpClientConfig<*>.defaultRequestConfig(baseUrl: String) {
    val normalizedBaseUrl = baseUrl.trimEnd('/')
    require(isAllowedBackendUrl(normalizedBaseUrl)) {
        "Set BACKEND_BASE_URL to the HTTPS backend URL, or to an HTTP localhost URL for an API run locally."
    }
    defaultRequest {
        url("$normalizedBaseUrl/")
    }
}
