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
    val websites: List<Websites>?,
    val summary: String?,
    val storyline: String?,
    val videos: List<Video>?,
    /** What the entry is, such as a game, an edition, or an update. Only game details send it and the fields below. */
    val kind: GameKind? = null,
    /** The game whose page shows this entry: an edition's or update's game, otherwise the entry itself. */
    @SerialName("canonical_id")
    val canonicalId: Int? = null,
    /** The details of the [canonicalId] game, sent only when that is not this entry. */
    val canonical: GameItem? = null,
    /** The [canonicalId] game's related entries; absent when the API could not load them. */
    val family: GameFamily? = null,
)
