// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import kotlin.test.Test
import kotlin.test.assertNotNull

class ImageHttpClientTest {
    @Test
    fun imageDownloadsHaveTimeoutsAndRetries() {
        val client = imageHttpClient(MockEngine { respondOk() })
        assertNotNull(client.pluginOrNull(HttpTimeout))
        assertNotNull(client.pluginOrNull(HttpRequestRetry))
        client.close()
    }
}
