// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.giveaways

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiveawayResponse(
    val id: Int?,
    val title: String?,
    val worth: String?,
    val thumbnail: String?,
    val image: String?,
    val description: String?,
    val instructions: String?,
    @SerialName("open_giveaway_url")
    val openGiveawayUrl: String?,
    @SerialName("published_date")
    val publishedDate: String?,
    val type: String?,
    val platforms: String?,
    @SerialName("end_date")
    val endDate: String?,
    val users: Int?,
    val status: String?,
    @SerialName("gamerpower_url")
    val gamerpowerUrl: String?,
    @SerialName("open_giveaway")
    val openGiveaway: String?,
)
