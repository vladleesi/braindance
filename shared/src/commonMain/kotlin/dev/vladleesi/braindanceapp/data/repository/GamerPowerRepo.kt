// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import io.ktor.client.call.body

class GamerPowerRepo(
    private val gamerPowerRemote: GamerPowerRemote,
) {
    suspend fun giveaways(pageSize: Int): List<GiveawayResponse>? {
        val result = gamerPowerRemote.giveaways()
        // TODO: Add cache and local pagination
        return result.body<List<GiveawayResponse>?>()?.take(pageSize)
    }

    suspend fun giveaway(id: Int): GiveawayResponse? {
        val result = gamerPowerRemote.giveaway(id)
        return result.body<GiveawayResponse?>()
    }
}
