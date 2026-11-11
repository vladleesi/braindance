// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/** An IGDB website. Only edition lookups expand websites; [type] is an IGDB `website_types` ID. */
@Serializable
internal data class IgdbWebsite(
    val type: Int? = null,
    val url: String? = null,
)

/** What a game's IGDB record says about whether it is sold now. */
internal enum class StoreAvailability {
    /** At least one storefront link outside storefronts that have closed. */
    LISTED,

    /** IGDB marks the game delisted or offline, or every storefront link points to a closed storefront. */
    WITHDRAWN,

    /** No storefront links. A missing link does not show that a game is delisted. */
    UNKNOWN,
}

/**
 * Prefers a currently sold remaster or expanded edition over an original that IGDB shows is no longer sold. Only
 * recommendations use it: Popular Right Now shows whichever edition people are paying attention to, and game detail
 * pages keep every original.
 *
 * IGDB relationships, as checked against the live API: every remaster (`game_type` 9) and expanded game (10) has a
 * `parent_game`, and the original's `remasters` and `expanded_games` lists hold exactly those children. Both keep the
 * original's content. Remakes rebuild a game and ports are separate platform releases, so they are related games rather
 * than replacements; sequels, DLC, bundles, and franchise or collection members have no such link. Version entries
 * (`version_parent`) are editions that recommendations already exclude. `external_games` keeps entries for delisted
 * products and `websites.trusted` describes moderation, so neither shows availability; title similarity alone proves
 * nothing.
 *
 * An original is replaced only when it is [StoreAvailability.WITHDRAWN] and a verified successor is
 * [StoreAvailability.LISTED]. A verified successor is listed in the original's `remasters` or `expanded_games`, has the
 * matching game type and a `parent_game` pointing back, is no version entry, and is released and not cancelled,
 * rumored, delisted, or offline. Among several, more storefronts, then more ratings, then the lower ID wins; release
 * dates do not decide. Anything uncertain keeps the original.
 */
internal object EditionPreference {
    // IGDB game_types.
    private const val REMASTER_TYPE = 9
    private const val EXPANDED_GAME_TYPE = 10

    // IGDB game_statuses.
    private const val OFFLINE_STATUS = 5
    private const val CANCELLED_STATUS = 6
    private const val RUMORED_STATUS = 7
    private const val DELISTED_STATUS = 8
    private val withdrawnStatuses = setOf(OFFLINE_STATUS, DELISTED_STATUS)
    private val unavailableStatuses = withdrawnStatuses + setOf(CANCELLED_STATUS, RUMORED_STATUS)

    /** IGDB website_types of storefronts: app stores, Steam, itch.io, Epic, GOG, consoles, Meta, and GameJolt. */
    @Suppress("MagicNumber")
    private val storefrontTypes = setOf(10, 11, 12, 13, 15, 16, 17, 22, 23, 24, 25, 26)

    /**
     * Product pages of storefronts that no longer sell: the Xbox 360 Marketplace, and the Wii U and 3DS eShops, whose
     * nintendo.com pages end in the platform. Detection reads stored URLs only and never fetches them.
     */
    private val closedStorefront =
        Regex(
            "^https?://(marketplace\\.xbox\\.com/|([a-z0-9-]+\\.)*nintendo\\.[a-z.]+/.*-(wii-u|3ds)/?$)",
            RegexOption.IGNORE_CASE,
        )

    /** Bounds one lookup: originals checked and successors fetched. */
    const val MAX_ORIGINALS = 30
    private const val MAX_SUCCESSORS = 60

    private const val ORIGINALS_QUERY = "originals"
    private const val SUCCESSORS_QUERY = "successors"
    private val requiredSuccessorFields =
        listOf(
            "name",
            "game_type",
            "game_status",
            "version_parent",
            "parent_game",
            "first_release_date",
            "total_rating_count",
            "websites.type",
            "websites.url",
        )

    fun successorIds(game: IgdbGame): List<Int> = (game.remasters + game.expandedGames).distinct()

    /**
     * One multiquery that reads the availability of [originals] with successors and fetches those successors with
     * [successorFields] as well. Returns null when no original has a successor, so callers send no request.
     */
    fun query(
        originals: List<IgdbGame>,
        successorFields: String,
    ): String? {
        val linked = linkedOriginals(originals)
        if (linked.isEmpty()) return null
        val originalIds = linked.map { it.id }.sorted()
        val successorIds =
            linked
                .flatMap(::successorIds)
                .distinct()
                .sorted()
                .take(MAX_SUCCESSORS)
        val fields = (successorFields.split(',') + requiredSuccessorFields).distinct().joinToString(",")
        return "query games \"$ORIGINALS_QUERY\" {fields game_status,websites.type,websites.url;" +
            "where id = (${originalIds.joinToString(",")});limit ${originalIds.size};};" +
            "query games \"$SUCCESSORS_QUERY\" {fields $fields;" +
            "where id = (${successorIds.joinToString(",")}) & version_parent = null & " +
            "game_type = ($REMASTER_TYPE,$EXPANDED_GAME_TYPE);limit ${successorIds.size};};"
    }

    /**
     * Maps each withdrawn original among [originals] to its preferred listed successor in the [query] [results].
     * [accept] applies the caller's own eligibility rules to a successor. Originals missing from the results are kept.
     * Throws on malformed payloads.
     */
    fun replacements(
        originals: List<IgdbGame>,
        results: Map<String, JsonArray>,
        nowSeconds: Long,
        accept: (IgdbGame) -> Boolean,
    ): Map<Int, IgdbGame> {
        val availability = RecommendationQueries.parseGames(results[ORIGINALS_QUERY]).associateBy { it.id }
        val successors = RecommendationQueries.parseGames(results[SUCCESSORS_QUERY]).associateBy { it.id }
        val preference =
            compareByDescending<IgdbGame> { listedStorefronts(it) }
                .thenByDescending { it.totalRatingCount ?: 0 }
                .thenBy { it.id }
        return linkedOriginals(originals)
            .mapNotNull { original ->
                val fetched = availability[original.id] ?: return@mapNotNull null
                if (availability(fetched) != StoreAvailability.WITHDRAWN) return@mapNotNull null
                successorIds(original)
                    .mapNotNull(successors::get)
                    .filter { isVerifiedSuccessor(original, it, nowSeconds) && accept(it) }
                    .minWithOrNull(preference)
                    ?.let { original.id to it }
            }.toMap()
    }

    fun availability(game: IgdbGame): StoreAvailability {
        if (game.gameStatus in withdrawnStatuses) return StoreAvailability.WITHDRAWN
        val storeUrls = game.websites.filter { it.type in storefrontTypes }.mapNotNull { it.url?.trim() }
        return when {
            storeUrls.none(String::isNotEmpty) -> StoreAvailability.UNKNOWN
            storeUrls.all { closedStorefront.containsMatchIn(it) } -> StoreAvailability.WITHDRAWN
            else -> StoreAvailability.LISTED
        }
    }

    private fun linkedOriginals(originals: List<IgdbGame>) =
        originals
            .filter { successorIds(it).isNotEmpty() }
            .distinctBy { it.id }
            .take(MAX_ORIGINALS)

    private fun isVerifiedSuccessor(
        original: IgdbGame,
        successor: IgdbGame,
        nowSeconds: Long,
    ): Boolean {
        val listedAs =
            when (successor.gameType) {
                REMASTER_TYPE -> original.remasters
                EXPANDED_GAME_TYPE -> original.expandedGames
                else -> emptyList()
            }
        return successor.id in listedAs &&
            successor.id != original.id &&
            successor.parentGame == original.id &&
            successor.versionParent == null &&
            !successor.name.isNullOrBlank() &&
            successor.gameStatus !in unavailableStatuses &&
            FeedEligibility.isReleased(successor, nowSeconds) &&
            availability(successor) == StoreAvailability.LISTED
    }

    private fun listedStorefronts(game: IgdbGame): Int =
        game.websites
            .filter { website ->
                website.type in storefrontTypes &&
                    !website.url.isNullOrBlank() &&
                    !closedStorefront.containsMatchIn(website.url.trim())
            }.mapNotNull { it.type }
            .distinct()
            .size
}
