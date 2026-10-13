// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.api.remote.PopularityPrimitivesRemote
import dev.vladleesi.braindanceapp.data.models.popularity.PopularityResponse
import dev.vladleesi.braindanceapp.data.models.request.PopularityPrimitives
import io.ktor.client.call.body

class PopularityPrimitivesRepo(
    private val remote: PopularityPrimitivesRemote,
) {
    suspend fun popularityPrimitives(
        type: PopularityPrimitives,
        pageSize: Int,
    ) = remote
        .popularityPrimitives(type = type.type, pageSize = pageSize)
        .body<List<PopularityResponse>?>()
}
