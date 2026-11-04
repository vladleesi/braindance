// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.safeCall

class GameDetailsRepo(
    private val gamesRemote: GamesRemote,
) {
    /** The backend answers an unknown ID with an empty list, which is [DataError.NotFound], not an empty success. */
    suspend fun gameDetails(gameId: Int): Outcome<GameItem> =
        when (val outcome = safeCall { gamesRemote.gameDetails(gameId).firstOrNull() }) {
            is Outcome.Success -> outcome.value?.let { Outcome.Success(it) } ?: Outcome.Failure(DataError.NotFound)
            is Outcome.Failure -> outcome
        }

    suspend fun recommendations(gameId: Int): Outcome<List<SimilarGame>> =
        safeCall { gamesRemote.recommendations(gameId) }
}
