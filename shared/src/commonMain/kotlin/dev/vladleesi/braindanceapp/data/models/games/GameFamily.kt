// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The entries IGDB lists separately for one game, as game details send them. Variants (editions and updates) belong
 * on the game's page; extras, other versions, and bundles have pages of their own.
 */
@Serializable
data class GameFamily(
    /** The game a DLC, remaster, or port belongs to. */
    val parent: FamilyGame? = null,
    val variants: List<FamilyGame>? = null,
    val extras: List<FamilyGame>? = null,
    @SerialName("other_versions")
    val otherVersions: List<FamilyGame>? = null,
    @SerialName("included_in")
    val includedIn: List<FamilyGame>? = null,
    val contents: List<FamilyGame>? = null,
)

/** A card in a [GameFamily] list. */
@Serializable
data class FamilyGame(
    val id: Int?,
    val name: String?,
    val kind: GameKind? = null,
    /** A short name on the game's page, such as "Hearts of Stone"; absent when the name is already short. */
    val label: String? = null,
    val cover: Cover? = null,
    @SerialName("first_release_date")
    val firstReleaseDate: Long? = null,
)
