// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.repository

import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.map
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.safeCall

/**
 * Search text in the API's canonical form: lowercase, with whitespace runs collapsed to one space and no space at
 * either end. Double quotes, backslashes, and control characters count as whitespace. Equal searches therefore share
 * one request and one cached response, and the API caches only this form publicly.
 */
object SearchQuery {
    /** The API rejects shorter searches; one character matches too many games to be useful. */
    const val MIN_LENGTH = 2

    /** The API rejects longer searches, so longer text is cut to this length. */
    const val MAX_LENGTH = 64

    /** [text] in canonical form, or null when it is too short to search or has no letter or digit. */
    fun normalize(text: String): String? {
        val normalized =
            buildString {
                var pendingSpace = false
                for (char in text.lowercase()) {
                    if (separatesWords(char)) {
                        pendingSpace = isNotEmpty()
                    } else {
                        if (pendingSpace) append(' ')
                        pendingSpace = false
                        append(char)
                    }
                }
            }
        val limited = normalized.cutTo(MAX_LENGTH).trimEnd()
        return limited.takeIf { it.length >= MIN_LENGTH && it.any(Char::isLetterOrDigit) }
    }

    private fun separatesWords(char: Char): Boolean =
        when (char) {
            '"', '\\' -> true
            else -> char.isWhitespace() || char.isISOControl()
        }

    // Never splits a surrogate pair, which would leave half a character.
    private fun String.cutTo(maxLength: Int): String {
        if (length <= maxLength) return this
        val end = if (this[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
        return substring(0, end)
    }
}

/** One page of search results; [isLast] when no further page exists. */
data class SearchPage(
    val games: List<GameItem>,
    val isLast: Boolean,
)

/** Game search through the API, which ranks results and pages through them. It keeps no state. */
class GameSearchRepo(
    private val gamesRemote: GamesRemote,
) {
    /** Page [page] (1-based) of results for a [query] from [SearchQuery.normalize]. */
    suspend fun search(
        query: String,
        page: Int,
    ): Outcome<SearchPage> =
        safeCall { gamesRemote.search(query = query, page = page, pageSize = PAGE_SIZE) }.map { games ->
            // A short page is the last one, and the API serves at most MAX_RESULTS results per search.
            SearchPage(games = games, isLast = games.size < PAGE_SIZE || page * PAGE_SIZE >= MAX_RESULTS)
        }

    companion object {
        /** The API's default page size, so pages of every client share edge-cache entries. */
        const val PAGE_SIZE = 20

        /** The most results the API serves for one search; a page must start within them. */
        const val MAX_RESULTS = 200
    }
}
