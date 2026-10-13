// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.Serializable

@Serializable
data class SimilarGame(
    val cover: Cover?,
    val id: Int?,
    val name: String?,
)
