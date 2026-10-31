// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.remote

import dev.vladleesi.braindanceapp.data.api.KtorClientManager
import dev.vladleesi.braindanceapp.data.config.ApiConfig
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse

class GamesRemote(
    ktorClientManager: KtorClientManager,
) {
    private val backendHttpClient = ktorClientManager.backendHttpClient

    suspend fun gameDetails(id: Int): HttpResponse =
        backendHttpClient.get(ApiConfig.Endpoints.GAME_DETAILS) {
            parameter("id", id)
        }

    /** The backend ranks recommendations; the default limit keeps one shared edge-cache URL per game. */
    suspend fun recommendations(id: Int): HttpResponse =
        backendHttpClient.get(ApiConfig.Endpoints.GAME_RECOMMENDATIONS) {
            parameter("id", id)
        }

    suspend fun mostAnticipated(pageSize: Int): HttpResponse =
        backendHttpClient.get(ApiConfig.Endpoints.MOST_ANTICIPATED) {
            parameter("pageSize", pageSize)
        }

    suspend fun popularGames(
        ids: List<Int>,
        pageSize: Int,
    ): HttpResponse =
        backendHttpClient.get(ApiConfig.Endpoints.POPULAR_GAMES) {
            parameter("ids", ids.joinToString(","))
            parameter("pageSize", pageSize)
        }
}
