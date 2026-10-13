// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.request

enum class PopularityPrimitives(
    val type: Int,
) {
    VISITS(1),
    WANT_TO_PLAY(2),
    PLAYING(3),
    PLAYED(4),
    PEAK_PLAYERS_24H(5),
    POSITIVE_REVIEWS(6),
    NEGATIVE_REVIEWS(7),
    TOTAL_REVIEWS(8),
    GLOBAL_TOP_SELLERS(9),
    MOST_WISHLISTED_UPCOMING(10),
    HOURS_WATCHED_24H(34),
}
