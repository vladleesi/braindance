// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.map
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.safeCall
import kotlin.concurrent.Volatile

class GamerPowerRepo(
    private val gamerPowerRemote: GamerPowerRemote,
) {
    // The giveaways the last successful list load returned, by ID. List items carry every field the details screen
    // shows, so a giveaway opened from the list needs no second request. It holds at most one page, has no time limit
    // (it is as fresh as the cards on screen), and each successful list load replaces it.
    @Volatile
    private var listedGiveaways: Map<Int, GiveawayResponse> = emptyMap()

    /**
     * The first [pageSize] giveaways; the backend returns the whole list in one response. A successful load also
     * replaces what [listedGiveaway] returns with these giveaways.
     */
    suspend fun giveaways(pageSize: Int): Outcome<List<GiveawayResponse>> {
        val outcome = safeCall { gamerPowerRemote.giveaways() }.map { it.take(pageSize) }
        if (outcome is Outcome.Success) {
            listedGiveaways = outcome.value.mapNotNull { item -> item.id?.let { id -> id to item } }.toMap()
        }
        return outcome
    }

    /** Giveaway [id] as the last successful list load returned it, or null when that list did not include it. */
    fun listedGiveaway(id: Int): GiveawayResponse? = listedGiveaways[id]

    /** An empty answer for [id] is [DataError.NotFound], like the backend's 404. */
    suspend fun giveaway(id: Int): Outcome<GiveawayResponse> =
        when (val outcome = safeCall { gamerPowerRemote.giveaway(id) }) {
            is Outcome.Success -> outcome.value?.let { Outcome.Success(it) } ?: Outcome.Failure(DataError.NotFound)
            is Outcome.Failure -> outcome
        }
}
