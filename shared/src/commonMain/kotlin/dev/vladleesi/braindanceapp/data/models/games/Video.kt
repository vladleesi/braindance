// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Video(
    val id: Int?,
    @SerialName("video_id")
    val videoId: String?,
)
