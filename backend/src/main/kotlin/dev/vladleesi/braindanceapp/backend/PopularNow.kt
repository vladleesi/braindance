// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * IGDB popularity primitives combined into the Popular Right Now feed. Type IDs are IGDB data, not documented
 * constants, so every computation also reads `/popularity_types` and drops a signal whose name no longer contains
 * [typeNameKeyword]. Each [weight] is a relative share; together they sum to 1.0. Weights favor actual play over
 * interest, keep Steam and Twitch equal, and use no sales or wishlist signal, which would duplicate Steam or measure
 * anticipation. [queryName] labels the signal's result in the IGDB multiquery.
 */
internal enum class PopularitySignal(
    val typeId: Int,
    val weight: Double,
    val queryName: String,
    private val typeNameKeyword: String,
) {
    IGDB_PLAYING(typeId = 3, weight = 0.30, queryName = "playing", typeNameKeyword = "playing"),
    STEAM_PEAK_PLAYERS(typeId = 5, weight = 0.25, queryName = "steam", typeNameKeyword = "peak"),
    TWITCH_HOURS_WATCHED(typeId = 34, weight = 0.25, queryName = "twitch", typeNameKeyword = "watched"),
    IGDB_VISITS(typeId = 1, weight = 0.20, queryName = "visits", typeNameKeyword = "visits"),
    ;

    fun matchesTypeName(typeName: String) = typeName.contains(typeNameKeyword, ignoreCase = true)
}

@Serializable
internal data class PopularityPrimitive(
    @SerialName("game_id") val gameId: Int,
    val value: Double,
    @SerialName("calculated_at") val calculatedAt: Long? = null,
)

internal object PopularNowRanking {
    const val RESULT_COUNT = 20
    const val SIGNAL_DEPTH = 50
    const val HYDRATION_COUNT = 60

    // Weighted reciprocal rank fusion: weight / (RRF_K + rank). A smaller constant than the common 60 keeps a game at
    // the top of one credible source competitive with games that appear lower in several sources.
    private const val RRF_K = 20.0
    private const val MAX_PER_COLLECTION = 2

    // IGDB game_status values.
    private const val EARLY_ACCESS_STATUS = 4
    private const val CANCELLED_STATUS = 6
    private const val RUMORED_STATUS = 7
    private val cancelledOrRumored = setOf(CANCELLED_STATUS, RUMORED_STATUS)

    // IGDB age_rating_categories IDs for adult-only ratings, as excluded by the previous Popular Right Now query.
    @Suppress("MagicNumber")
    private val adultRatingCategories = setOf(7, 26, 38)

    /**
     * Fuses ranked game IDs per signal into one ordering. A game's score sums `weight / (k + rank)` over the signals
     * that list it; ties prefer more signals, then a better best rank, then the lower ID, so output is deterministic.
     */
    fun fuse(rankedGameIds: Map<PopularitySignal, List<Int>>): List<Int> {
        data class Score(
            var value: Double = 0.0,
            var signals: Int = 0,
            var bestRank: Int = Int.MAX_VALUE,
        )
        val scores = mutableMapOf<Int, Score>()
        for ((signal, gameIds) in rankedGameIds) {
            gameIds.distinct().forEachIndexed { index, gameId ->
                val rank = index + 1
                val score = scores.getOrPut(gameId, ::Score)
                score.value += signal.weight / (RRF_K + rank)
                score.signals++
                score.bestRank = minOf(score.bestRank, rank)
            }
        }
        return scores.entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, Score>> { it.value.value }
                    .thenByDescending { it.value.signals }
                    .thenBy { it.value.bestRank }
                    .thenBy { it.key },
            ).map { it.key }
    }

    /** Orders primitives by value, highest first, with the game ID breaking ties so equal values stay stable. */
    fun rankedGameIds(primitives: List<PopularityPrimitive>): List<Int> =
        primitives
            .sortedWith(compareByDescending<PopularityPrimitive> { it.value }.thenBy { it.gameId })
            .map { it.gameId }

    /**
     * Keeps the fused order, which IGDB hydration does not preserve, and returns released, displayable main games.
     * Missing entries (deleted IDs), editions, DLC, cancelled or unreleased games, covers, and adult content are
     * skipped; duplicate titles and more than two games from one collection are skipped for variety.
     */
    fun select(
        fusedGameIds: List<Int>,
        games: Map<Int, IgdbGame>,
        nowSeconds: Long,
        limit: Int = RESULT_COUNT,
    ): List<Recommendation> {
        val seenTitles = mutableSetOf<String>()
        val collectionCounts = mutableMapOf<Int, Int>()
        val selected = mutableListOf<Recommendation>()
        for (gameId in fusedGameIds) {
            if (selected.size == limit) break
            val game = games[gameId] ?: continue
            val name = game.name?.takeIf(String::isNotBlank) ?: continue
            val coverUrl = game.cover?.url?.takeIf(String::isNotBlank) ?: continue
            if (!isEligible(game, nowSeconds)) continue
            if (game.collections.any { (collectionCounts[it] ?: 0) >= MAX_PER_COLLECTION }) continue
            if (!seenTitles.add(RecommendationRanking.normalizedTitle(name))) continue
            game.collections.forEach { collectionCounts[it] = (collectionCounts[it] ?: 0) + 1 }
            selected += Recommendation(game.id, name, coverUrl, score = 0.0)
        }
        return selected
    }

    private fun isEligible(
        game: IgdbGame,
        nowSeconds: Long,
    ): Boolean {
        val released = game.firstReleaseDate?.let { it <= nowSeconds } == true || game.gameStatus == EARLY_ACCESS_STATUS
        return released &&
            game.versionParent == null &&
            (game.gameType == null || game.gameType in RecommendationQueries.recommendableGameTypes) &&
            game.gameStatus !in cancelledOrRumored &&
            RecommendationQueries.EROTIC_THEME !in game.themes &&
            game.ageRatings.none { it.ratingCategory in adultRatingCategories }
    }
}

/**
 * Builds the Popular Right Now feed with two IGDB requests per cache miss: one multiquery for the popularity lists and
 * their type names, then one hydration query for the best fused candidates. The serialized feed is cached in process;
 * shared edge caching keeps it across instances.
 */
internal class PopularNowService(
    private val igdb: IgdbService,
    private val clock: Clock = Clock.systemUTC(),
    private val log: (String) -> Unit = ::println,
) {
    private val cache =
        ResponseCache(
            CACHE_TTL.inWholeMilliseconds,
            CACHE_BYTES,
            CACHE_BYTES.toInt(),
            clock,
            staleIfErrorMillis = STALE_IF_ERROR.inWholeMilliseconds,
        )
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun popularNow(): ResponseCache.LoadResult = cache.getOrLoad(CACHE_KEY) { load() }

    private suspend fun load(): ResponseCache.LoadResult {
        val metrics = LoadMetrics()
        val result =
            withTimeoutOrNull(TOTAL_BUDGET) {
                val discovery =
                    execute(RecommendationService.MULTIQUERY_PATH, discoveryQuery())
                metrics.igdbRequests++
                if (discovery.status != OK) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(discovery.status)
                val signals = validSignals(RecommendationQueries.parseMultiquery(checkNotNull(discovery.body)))
                metrics.signals = signals.keys
                if (signals.isEmpty()) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(BAD_GATEWAY)
                val candidates = PopularNowRanking.fuse(signals).take(PopularNowRanking.HYDRATION_COUNT)
                metrics.candidates = candidates.size
                val games =
                    if (candidates.isEmpty()) {
                        emptyMap()
                    } else {
                        val hydration =
                            execute(GAMES_PATH, hydrationQuery(candidates))
                        metrics.igdbRequests++
                        if (hydration.status != OK) {
                            return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(hydration.status)
                        }
                        val parsed = json.parseToJsonElement(checkNotNull(hydration.body).decodeToString()).jsonArray
                        RecommendationQueries.parseGames(parsed).associateBy { it.id }
                    }
                val nowSeconds = clock.millis().milliseconds.inWholeSeconds
                val feed = PopularNowRanking.select(candidates, games, nowSeconds)
                metrics.results = feed.size
                ResponseCache.LoadResult.Hit(CachedResponse(gameCardsJson(feed).encodeToByteArray()))
            } ?: ResponseCache.LoadResult.Uncached(GATEWAY_TIMEOUT)
        val outcome = if (result is ResponseCache.LoadResult.Uncached) "failed_${result.status}" else "ok"
        log(metrics.event(outcome))
        return result
    }

    /** Waits briefly for an IGDB rate-limit slot instead of failing at once during a burst. */
    private suspend fun execute(
        path: String,
        body: String,
    ) = igdb.execute(path, body, SLOT_WAIT.inWholeMilliseconds)

    /** Keeps signals whose IGDB type name still matches and whose newest value was calculated recently. */
    private fun validSignals(results: Map<String, JsonArray>): Map<PopularitySignal, List<Int>> {
        val typeNames =
            results["types"].orEmpty().associate {
                val type = it.jsonObject
                type
                    .getValue("id")
                    .jsonPrimitive.content
                    .toInt() to type["name"]?.jsonPrimitive?.content.orEmpty()
            }
        val oldestAccepted = (clock.millis().milliseconds - MAX_SIGNAL_AGE).inWholeSeconds
        return PopularitySignal.entries
            .mapNotNull { signal ->
                val primitives =
                    results[signal.queryName].orEmpty().map { json.decodeFromJsonElement<PopularityPrimitive>(it) }
                val newest = primitives.mapNotNull { it.calculatedAt }.maxOrNull()
                val valid =
                    signal.matchesTypeName(typeNames[signal.typeId].orEmpty()) &&
                        primitives.isNotEmpty() &&
                        newest != null &&
                        newest >= oldestAccepted
                if (valid) signal to PopularNowRanking.rankedGameIds(primitives) else null
            }.toMap()
    }

    /** One structured log line per cache miss; it contains no request or credential data. */
    private class LoadMetrics {
        private val started = System.nanoTime()
        var igdbRequests = 0
        var signals: Set<PopularitySignal> = emptySet()
        var candidates = 0
        var results = 0

        fun event(outcome: String) =
            "{\"event\":\"popular_now\",\"outcome\":\"$outcome\",\"igdb_requests\":$igdbRequests," +
                "\"signals\":\"${signals.joinToString(",") { it.queryName }}\",\"candidates\":$candidates," +
                "\"results\":$results,\"duration_ms\":${(System.nanoTime() - started).nanoseconds.inWholeMilliseconds}}"
    }

    companion object {
        private const val CACHE_KEY = "popular-now"
        private const val GAMES_PATH = "/v4/games"
        private val CACHE_TTL = 30.minutes
        private val STALE_IF_ERROR = 24.hours
        private const val CACHE_BYTES = 256L * 1024
        private val MAX_SIGNAL_AGE = 7.days
        private val TOTAL_BUDGET = 4.5.seconds
        private val SLOT_WAIT = 1.seconds
        private const val OK = 200
        private const val BAD_GATEWAY = 502
        private const val GATEWAY_TIMEOUT = 504

        fun discoveryQuery(): String =
            buildString {
                val typeIds = PopularitySignal.entries.joinToString(",") { it.typeId.toString() }
                append("query popularity_types \"types\" {fields name;where id = ($typeIds);limit 10;};")
                for (signal in PopularitySignal.entries) {
                    append(
                        "query popularity_primitives \"${signal.queryName}\" {fields game_id,value,calculated_at;" +
                            "where popularity_type = ${signal.typeId};sort value desc;" +
                            "limit ${PopularNowRanking.SIGNAL_DEPTH};};",
                    )
                }
            }

        fun hydrationQuery(gameIds: List<Int>): String =
            "fields name,cover.url,game_type,game_status,version_parent,first_release_date,themes,collections," +
                "age_ratings.rating_category;where id = (${gameIds.joinToString(",")});limit ${gameIds.size};"
    }
}
