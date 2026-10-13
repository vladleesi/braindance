// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.popularity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PopularityResponse(
    val id: Int,
    @SerialName("game_id")
    val gameId: Int,
)
