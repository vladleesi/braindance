// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import io.ktor.client.call.body

class GameDetailsRepo(
    private val gamesRemote: GamesRemote,
) {
    suspend fun gameDetails(gameId: Int): List<GameItem>? {
        val response = gamesRemote.gameDetails(gameId)
        return response.body()
    }
}
