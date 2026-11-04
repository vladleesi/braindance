// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.safeCall

class HomeRepo(
    private val gamesRemote: GamesRemote,
) {
    suspend fun mostAnticipated(pageSize: Int): Outcome<List<GameItem>> =
        safeCall { gamesRemote.mostAnticipated(pageSize = pageSize) }

    suspend fun trendingNow(): Outcome<List<TrendingGameItem>> = safeCall { gamesRemote.trendingNow() }
}
