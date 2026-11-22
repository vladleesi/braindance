// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamingEvent(
    val id: Int,
    val name: String,
    @SerialName("start_time")
    val startTime: Long,
    @SerialName("end_time")
    val endTime: Long? = null,
    val logo: Cover? = null,
    @SerialName("live_stream_url")
    val liveStreamUrl: String? = null,
    @SerialName("game_count")
    val gameCount: Int? = null,
)
