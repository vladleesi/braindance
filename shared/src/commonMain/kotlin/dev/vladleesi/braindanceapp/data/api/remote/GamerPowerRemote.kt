// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.remote

import dev.vladleesi.braindanceapp.data.config.ApiConfig
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class GamerPowerRemote(
    private val backendHttpClient: HttpClient,
) {
    suspend fun giveaways(): List<GiveawayResponse> = backendHttpClient.get(ApiConfig.Endpoints.GIVEAWAYS).body()

    /** Null when the backend answers with an empty body for this ID. */
    suspend fun giveaway(id: Int): GiveawayResponse? =
        backendHttpClient.get("${ApiConfig.Endpoints.GIVEAWAYS}/$id").body()
}
