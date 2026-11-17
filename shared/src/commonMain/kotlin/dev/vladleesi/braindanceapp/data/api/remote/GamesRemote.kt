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

    /**
     * Page [page] (1-based) of [pageSize] Most Anticipated games. The first page leaves `page` out, so Home and the
     * full list share one edge-cache URL.
     */
    suspend fun mostAnticipated(
        pageSize: Int,
        page: Int = 1,
    ): List<GameItem> =
        backendHttpClient
            .get(ApiConfig.Endpoints.MOST_ANTICIPATED) {
                if (page > 1) parameter("page", page)
                parameter("pageSize", pageSize)
            }.body()

    /**
     * One page of search results for a [query] in the API's canonical form, best match first. Every parameter is sent
     * explicitly, in a fixed order, so equal searches share one edge-cache URL.
     */
    suspend fun search(
        query: String,
        page: Int,
        pageSize: Int,
    ): List<GameItem> =
        backendHttpClient
            .get(ApiConfig.Endpoints.GAME_SEARCH) {
                parameter("q", query)
                parameter("page", page)
                parameter("pageSize", pageSize)
            }.body()

    /**
     * Page [page] (1-based) of the ranked feed, at the API's default page size. The backend ranks and hydrates the
     * whole feed, so one request returns the finished cards. The first page sends no parameters, so Home and the full
     * list share one edge-cache URL.
     */
    suspend fun trendingNow(page: Int = 1): List<TrendingGameItem> =
        backendHttpClient
            .get(ApiConfig.Endpoints.TRENDING) {
                if (page > 1) parameter("page", page)
            }.body()
}
