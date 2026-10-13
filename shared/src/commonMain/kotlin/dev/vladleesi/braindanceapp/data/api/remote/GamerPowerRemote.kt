// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.remote

import dev.vladleesi.braindanceapp.data.api.KtorClientManager
import dev.vladleesi.braindanceapp.data.config.ApiConfig
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class GamerPowerRemote(
    ktorClientManager: KtorClientManager,
) {
    private val backendHttpClient = ktorClientManager.backendHttpClient

    suspend fun giveaways(): HttpResponse = backendHttpClient.get(ApiConfig.Endpoints.GIVEAWAYS)

    suspend fun giveaway(id: Int): HttpResponse = backendHttpClient.get("${ApiConfig.Endpoints.GIVEAWAYS}/$id")
}
