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

private fun HttpClientConfig<*>.defaultRequestConfig(baseUrl: String) {
    val normalizedBaseUrl = baseUrl.trimEnd('/')
    require(normalizedBaseUrl.startsWith("https://")) { "Set BACKEND_BASE_URL to the HTTPS backend URL." }
    defaultRequest {
        url("$normalizedBaseUrl/")
    }
}
