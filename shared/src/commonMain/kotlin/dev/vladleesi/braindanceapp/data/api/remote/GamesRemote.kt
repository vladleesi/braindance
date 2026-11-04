// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.remote

import dev.vladleesi.braindanceapp.data.config.ApiConfig
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class GamesRemote(
    private val backendHttpClient: HttpClient,
) {
    suspend fun gameDetails(id: Int): List<GameItem> =
        backendHttpClient
            .get(ApiConfig.Endpoints.GAME_DETAILS) {
                parameter("id", id)
            }.body()

    /** The backend ranks recommendations; the default limit keeps one shared edge-cache URL per game. */
    suspend fun recommendations(id: Int): List<SimilarGame> =
        backendHttpClient
            .get(ApiConfig.Endpoints.GAME_RECOMMENDATIONS) {
                parameter("id", id)
            }.body()

    suspend fun mostAnticipated(pageSize: Int): List<GameItem> =
        backendHttpClient
            .get(ApiConfig.Endpoints.MOST_ANTICIPATED) {
                parameter("pageSize", pageSize)
            }.body()

    /** The backend ranks and hydrates the whole feed, so one request returns the finished cards. */
    suspend fun trendingNow(): List<TrendingGameItem> = backendHttpClient.get(ApiConfig.Endpoints.TRENDING).body()
}
