// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Int?,
    val name: String?,
)
