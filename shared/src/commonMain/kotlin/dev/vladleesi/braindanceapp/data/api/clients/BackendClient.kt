// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.clients

import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.data.api.defaultConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.defaultRequest

object BackendClient {
    fun build(debugHttpLogging: Boolean) =
        HttpClient {
            defaultConfig(debugHttpLogging)
            defaultRequestConfig()
        }
}

private fun HttpClientConfig<*>.defaultRequestConfig() {
    val baseUrl = BuildKonfig.BACKEND_BASE_URL.trimEnd('/')
    require(baseUrl.startsWith("https://")) { "Set BACKEND_BASE_URL to the HTTPS backend URL." }
    defaultRequest {
        url("$baseUrl/")
    }
}
