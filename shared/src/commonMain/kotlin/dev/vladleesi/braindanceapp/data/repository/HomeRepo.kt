// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import io.ktor.client.call.body

class HomeRepo(
    private val gamesRemote: GamesRemote,
) {
    suspend fun mostAnticipated(pageSize: Int): List<GameItem>? {
        val response = gamesRemote.mostAnticipated(pageSize = pageSize)
        return response.body()
    }

    suspend fun popularRightNow(): List<GameItem> = gamesRemote.popularNow().body()
}
