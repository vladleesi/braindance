// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameItem(
    val cover: Cover?,
    @SerialName("first_release_date")
    val firstReleaseDate: Long?,
    val genres: List<Genre>?,
    val id: Int?,
    val name: String?,
    val platforms: List<Platform>?,
    @SerialName("release_dates")
    val releaseDates: List<ReleaseDate>?,
    val screenshots: List<Screenshot>?,
    @SerialName("similar_games")
    val similarGames: List<SimilarGame>?,
    val websites: List<Websites>?,
    val summary: String?,
    val storyline: String?,
    val videos: List<Video>?,
)
