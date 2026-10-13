// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.remote

import dev.vladleesi.braindanceapp.data.api.KtorClientManager
import dev.vladleesi.braindanceapp.data.config.ApiConfig
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse

class PopularityPrimitivesRemote(
    ktorClientManager: KtorClientManager,
) {
    private val backendHttpClient = ktorClientManager.backendHttpClient

    suspend fun popularityPrimitives(
        type: Int,
        pageSize: Int,
    ): HttpResponse =
        backendHttpClient.get(ApiConfig.Endpoints.POPULARITY_PRIMITIVES) {
            parameter("type", type)
            parameter("pageSize", pageSize)
        }
}
