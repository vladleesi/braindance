// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.map
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.safeCall

/** One page of a feed; [isLast] when no further page exists. */
data class FeedPage<T>(
    val items: List<T>,
    val isLast: Boolean,
)

/** The game feeds Home shows, and their full lists page by page. It keeps no state. */
class HomeRepo(
    private val gamesRemote: GamesRemote,
) {
    suspend fun mostAnticipated(pageSize: Int): Outcome<List<GameItem>> =
        safeCall { gamesRemote.mostAnticipated(pageSize = pageSize) }

    suspend fun trendingNow(): Outcome<List<TrendingGameItem>> = safeCall { gamesRemote.trendingNow() }

    /** Page [page] (1-based) of Popular Right Now; the first page is the one Home shows. */
    suspend fun popularPage(page: Int): Outcome<FeedPage<TrendingGameItem>> =
        safeCall { gamesRemote.trendingNow(page = page) }.map { games ->
            FeedPage(games, isLast = isLastPage(games.size, page, POPULAR_PAGE_SIZE, POPULAR_MAX_GAMES))
        }

    /** Page [page] (1-based) of Most Anticipated; the first page is the one Home shows. */
    suspend fun anticipatedPage(page: Int): Outcome<FeedPage<GameItem>> =
        safeCall { gamesRemote.mostAnticipated(pageSize = ANTICIPATED_PAGE_SIZE, page = page) }.map { games ->
            FeedPage(games, isLast = isLastPage(games.size, page, ANTICIPATED_PAGE_SIZE, ANTICIPATED_MAX_GAMES))
        }

    // A short page is the last one, and no page may start past the most games the API serves for the feed.
    private fun isLastPage(
        size: Int,
        page: Int,
        pageSize: Int,
        maxGames: Int,
    ) = size < pageSize || page * pageSize >= maxGames

    companion object {
        /** The API's default page size for Popular Right Now, so the first page shares Home's request. */
        const val POPULAR_PAGE_SIZE = 20

        /** The most games the API ranks for Popular Right Now. */
        const val POPULAR_MAX_GAMES = 100

        /** Home's page size for Most Anticipated, so the first page of the full list shares Home's request. */
        const val ANTICIPATED_PAGE_SIZE = 20

        /** The most games the API serves for Most Anticipated; a page must start within them. */
        const val ANTICIPATED_MAX_GAMES = 200
    }
}
