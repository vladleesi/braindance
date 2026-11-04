// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.map
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.safeCall

class GamerPowerRepo(
    private val gamerPowerRemote: GamerPowerRemote,
) {
    /** The first [pageSize] giveaways; the backend returns the whole list in one response. */
    suspend fun giveaways(pageSize: Int): Outcome<List<GiveawayResponse>> =
        safeCall { gamerPowerRemote.giveaways() }.map { it.take(pageSize) }

    /** An empty answer for [id] is [DataError.NotFound], like the backend's 404. */
    suspend fun giveaway(id: Int): Outcome<GiveawayResponse> =
        when (val outcome = safeCall { gamerPowerRemote.giveaway(id) }) {
            is Outcome.Success -> outcome.value?.let { Outcome.Success(it) } ?: Outcome.Failure(DataError.NotFound)
            is Outcome.Failure -> outcome
        }
}
