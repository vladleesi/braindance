// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.request.PopularityPrimitives
import io.ktor.client.call.body

class HomeRepo(
    private val gamesRemote: GamesRemote,
    private val primitivesRepo: PopularityPrimitivesRepo,
) {
    suspend fun mostAnticipated(pageSize: Int): List<GameItem>? {
        val response = gamesRemote.mostAnticipated(pageSize = pageSize)
        return response.body()
    }

    suspend fun popularRightNow(pageSize: Int): List<GameItem>? {
        val popularityResponses =
            primitivesRepo.popularityPrimitives(
                type = PopularityPrimitives.HOURS_WATCHED_24H,
                pageSize = pageSize + pageSize, // Extra results to compensate for filtered adult-only games
            )

        if (popularityResponses.isNullOrEmpty()) {
            return null
        }

        return gamesRemote
            .popularGames(
                ids = popularityResponses.map { it.gameId },
                pageSize = pageSize,
            ).body()
    }
}
