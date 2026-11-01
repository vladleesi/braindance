// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * IGDB [PopScore](https://api-docs.igdb.com/#popscore) primitives read by the Popular Right Now feed. Type IDs are
 * IGDB data, not documented constants, so every computation also reads `/popularity_types` and drops a signal whose
 * name no longer contains [typeNameKeyword]. [queryName] labels the signal's result in the IGDB multiquery.
 */
internal enum class PopularitySignal(
    val typeId: Int,
    val queryName: String,
    private val typeNameKeyword: String,
) {
    IGDB_PLAYING(typeId = 3, queryName = "playing", typeNameKeyword = "playing"),
    STEAM_PEAK_PLAYERS(typeId = 5, queryName = "steam", typeNameKeyword = "peak"),
    TWITCH_HOURS_WATCHED(typeId = 34, queryName = "twitch", typeNameKeyword = "watched"),
    IGDB_VISITS(typeId = 1, queryName = "visits", typeNameKeyword = "visits"),
    IGDB_WANT_TO_PLAY(typeId = 2, queryName = "want_to_play", typeNameKeyword = "want to play"),
    ;

    fun matchesTypeName(typeName: String) = typeName.contains(typeNameKeyword, ignoreCase = true)
}

@Serializable
internal data class PopularityPrimitive(
    @SerialName("game_id") val gameId: Int,
    val value: Double,
    @SerialName("calculated_at") val calculatedAt: Long? = null,
)

/** Which hydrated IGDB games a home feed may show. */
internal object FeedEligibility {
    // IGDB game_status values.
    private const val EARLY_ACCESS_STATUS = 4
    private const val CANCELLED_STATUS = 6
    private const val RUMORED_STATUS = 7
    private val cancelledOrRumored = setOf(CANCELLED_STATUS, RUMORED_STATUS)

    // IGDB age_rating_categories ID for ESRB AO, the only adults-only rating. Regional mature ratings such as GRAC 19+
    // (26) and ACB R 18+ (38) are routine for mainstream mature games, including ones ESRB rates T or M, so they do
    // not exclude a game; the erotic theme excludes sexual content.
    private const val ADULTS_ONLY_RATING_CATEGORY = 7

    /** A first release date that has passed, or early access, which IGDB may list without a date. */
    fun isReleased(
        game: IgdbGame,
        nowSeconds: Long,
    ): Boolean = game.firstReleaseDate?.let { it <= nowSeconds } == true || game.gameStatus == EARLY_ACCESS_STATUS

    /** Main games, standalone expansions, remakes, and remasters that are not cancelled, rumored, or adult-only. */
    fun isListable(game: IgdbGame): Boolean =
        game.versionParent == null &&
            (game.gameType == null || game.gameType in RecommendationQueries.recommendableGameTypes) &&
            game.gameStatus !in cancelledOrRumored &&
            RecommendationQueries.EROTIC_THEME !in game.themes &&
            game.ageRatings.none { it.ratingCategory == ADULTS_ONLY_RATING_CATEGORY }

    /** IGDB filter clause for games [isReleased] accepts as of [nowSeconds]. */
    fun releasedFilter(nowSeconds: Long) = "(first_release_date <= $nowSeconds | game_status = $EARLY_ACCESS_STATUS)"
}

/**
 * Keeps a home feed varied in feed order: a repeated normalized title is skipped, and so is a game from a collection
 * that already has [maxPerCollection] entries.
 */
internal class FeedVariety(
    private val maxPerCollection: Int = 2,
) {
    private val seenTitles = mutableSetOf<String>()
    private val collectionCounts = mutableMapOf<Int, Int>()

    fun accept(
        game: IgdbGame,
        name: String,
    ): Boolean {
        if (game.collections.any { (collectionCounts[it] ?: 0) >= maxPerCollection }) return false
        if (!seenTitles.add(RecommendationRanking.normalizedTitle(name))) return false
        game.collections.forEach { collectionCounts[it] = (collectionCounts[it] ?: 0) + 1 }
        return true
    }
}

/** Multiquery parts for the popularity type names and the top [depth] games of each of [signals]. */
internal fun popularitySignalQueries(
    depth: Int,
    signals: List<PopularitySignal>,
): String =
    buildString {
        val typeIds = signals.joinToString(",") { it.typeId.toString() }
        append("query popularity_types \"types\" {fields name;where id = ($typeIds);limit 10;};")
        for (signal in signals) {
            append(
                "query popularity_primitives \"${signal.queryName}\" {fields game_id,value,calculated_at;" +
                    "where popularity_type = ${signal.typeId};sort value desc;limit $depth;};",
            )
        }
    }

private val MAX_SIGNAL_AGE = 7.days
private val primitiveJson = Json { ignoreUnknownKeys = true }

/**
 * Ranked game IDs for each of [signals] in multiquery [results] whose IGDB type name still matches and whose newest
 * value was calculated within seven days of [nowMillis]. Invalid or outdated signals are omitted rather than guessed.
 * Each list orders games by value, highest first; the game ID breaks ties so equal values stay stable.
 */
internal fun validPopularitySignals(
    results: Map<String, JsonArray>,
    nowMillis: Long,
    signals: List<PopularitySignal>,
): Map<PopularitySignal, List<Int>> {
    val typeNames =
        results["types"].orEmpty().associate {
            val type = it.jsonObject
            type
                .getValue("id")
                .jsonPrimitive.content
                .toInt() to type["name"]?.jsonPrimitive?.content.orEmpty()
        }
    val oldestAccepted = (nowMillis.milliseconds - MAX_SIGNAL_AGE).inWholeSeconds
    return signals
        .mapNotNull { signal ->
            val primitives =
                results[signal.queryName].orEmpty().map {
                    primitiveJson.decodeFromJsonElement<PopularityPrimitive>(it)
                }
            val newest = primitives.mapNotNull { it.calculatedAt }.maxOrNull()
            val valid =
                signal.matchesTypeName(typeNames[signal.typeId].orEmpty()) &&
                    primitives.isNotEmpty() &&
                    newest != null &&
                    newest >= oldestAccepted
            val ranked =
                primitives
                    .sortedWith(compareByDescending<PopularityPrimitive> { it.value }.thenBy { it.gameId })
                    .map { it.gameId }
            if (valid) signal to ranked else null
        }.toMap()
}
