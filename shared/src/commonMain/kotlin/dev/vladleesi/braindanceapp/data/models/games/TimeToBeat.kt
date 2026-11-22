// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.Serializable

@Serializable
data class TimeToBeat(
    val hastily: Long? = null,
    val normally: Long? = null,
    val completely: Long? = null,
    val count: Int,
)
