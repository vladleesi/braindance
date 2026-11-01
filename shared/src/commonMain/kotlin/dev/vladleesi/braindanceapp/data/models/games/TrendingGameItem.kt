// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.Serializable

/**
 * A Trending Now card from `/v1/games/trending`. [reason] is an optional verified cause, such as `new_release`;
 * [platform] is a short platform name sent only with `new_platform`. Unknown reasons are ignored by the UI.
 */
@Serializable
data class TrendingGameItem(
    val id: Int,
    val name: String? = null,
    val cover: Cover? = null,
    val reason: String? = null,
    val platform: String? = null,
)
