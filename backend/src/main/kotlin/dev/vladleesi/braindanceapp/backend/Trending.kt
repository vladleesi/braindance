// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.time.Clock
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/** Why a game is trending. Every reason comes from a dated IGDB record; [wireName] is the API value. */
internal enum class TrendingReason(
    val wireName: String,
) {
    NEW_RELEASE("new_release"),
    FULL_RELEASE("full_release"),
    NEW_PLATFORM("new_platform"),
    SHOWCASED("showcased"),
}

/** An IGDB gaming event, such as a showcase or convention, with the games it featured. Times are Unix seconds. */
@Serializable
internal data class IgdbEvent(
    val id: Int,
    @SerialName("start_time") val startTime: Long? = null,
    @SerialName("end_time") val endTime: Long? = null,
    val games: List<Int> = emptyList(),
)

/** A game's current attention: the weighted share of the maximum signal score and how many signals list it. */
internal data class Attention(
    val score: Double,
    val signals: Int,
)

/** A ranked feed entry. [score] and [attention] stay internal; [platform] is set only for a new platform release. */
internal data class TrendingGame(
    val id: Int,
    val name: String,
    val coverUrl: String,
    val reason: TrendingReason?,
    val platform: String?,
    val score: Double,
    val attention: Double,
)

/**
 * Trending Now ranking from one snapshot of current IGDB data. IGDB popularity primitives hold only their latest
 * values, so growth over time cannot be measured without stored history; instead, a game's current attention is
 * multiplied by the relevance of its most recent dated activity. A launch, a first release on a new platform, leaving
 * early access, or appearing at a gaming event keeps relevance high for a few weeks; an established game without such
 * activity keeps [ESTABLISHED] relevance and needs near-top attention on most signals to qualify.
 */
internal object TrendingRanking {
    const val RESULT_COUNT = 20
    const val SIGNAL_DEPTH = 100
    const val HYDRATION_COUNT = 120

    /** Minimum attention × relevance. The feed is shorter rather than filled with weak candidates. */
    const val MIN_SCORE = 0.15

    // Attention weights. Visits and Playing list additions favor games people are discovering now; Steam peak players
    // weigh least because established multiplayer games dominate it. Shares are renormalized over valid signals.
    @Suppress("MagicNumber")
    private val signalWeights =
        mapOf(
            PopularitySignal.IGDB_VISITS to 0.30,
            PopularitySignal.IGDB_PLAYING to 0.25,
            PopularitySignal.TWITCH_HOURS_WATCHED to 0.25,
            PopularitySignal.STEAM_PEAK_PLAYERS to 0.20,
        )

    // A rank's score is (K + 1) / (K + rank): 1.0 at the top, 0.5 at rank 21, and about 0.17 at rank 100.
    private const val RANK_K = 20.0

    // Attention seen by only one signal, such as streaming-only games, counts at this share when other signals exist.
    private const val SINGLE_SIGNAL_CONFIDENCE = 0.6

    const val ESTABLISHED = 0.2
    private const val UPCOMING = 0.35

    /** Relevance of a dated activity: [peak] until [fullDays], then linear decay to [ESTABLISHED] at [goneDays]. */
    private class Activity(
        val reason: TrendingReason,
        val peak: Double,
        val fullDays: Double,
        val goneDays: Double,
        val labelDays: Double,
    ) {
        fun relevance(ageDays: Double): Double =
            when {
                ageDays <= fullDays -> peak
                ageDays >= goneDays -> ESTABLISHED
                else -> peak - (peak - ESTABLISHED) * (ageDays - fullDays) / (goneDays - fullDays)
            }
    }

    // Relevance curves per activity: peak, full-relevance days, days until established, and days the label is shown.
    @Suppress("MagicNumber")
    private val launch = Activity(TrendingReason.NEW_RELEASE, 1.0, 14.0, 60.0, labelDays = 30.0)

    @Suppress("MagicNumber")
    private val fullRelease = Activity(TrendingReason.FULL_RELEASE, 0.9, 7.0, 30.0, labelDays = 21.0)

    @Suppress("MagicNumber")
    private val newPlatform = Activity(TrendingReason.NEW_PLATFORM, 0.8, 7.0, 30.0, labelDays = 21.0)

    @Suppress("MagicNumber")
    private val showcase = Activity(TrendingReason.SHOWCASED, 0.9, 3.0, 21.0, labelDays = 14.0)

    /** Events starting this long ago or more no longer affect relevance. */
    val EVENT_WINDOW = 21.days

    /** A platform release this soon after the first release is part of the launch, not a new platform. */
    private const val PORT_MIN_GAP_DAYS = 30.0
    private const val SECONDS_PER_DAY = 86_400.0

    // IGDB release_date_statuses names. Other statuses, such as alpha, beta, or cancelled, are not releases.
    private const val FULL_RELEASE_STATUS = "full release"
    private const val EARLY_ACCESS_STATUS = "early access"

    fun attention(rankedGameIds: Map<PopularitySignal, List<Int>>): Map<Int, Attention> {
        val totalWeight = rankedGameIds.keys.sumOf { signalWeights.getValue(it) }
        if (totalWeight == 0.0) return emptyMap()
        val weighted = mutableMapOf<Int, Double>()
        val signalCounts = mutableMapOf<Int, Int>()
        for ((signal, gameIds) in rankedGameIds) {
            val weight = signalWeights.getValue(signal)
            gameIds.distinct().forEachIndexed { index, gameId ->
                weighted[gameId] = (weighted[gameId] ?: 0.0) + weight * (RANK_K + 1) / (RANK_K + index + 1)
                signalCounts[gameId] = (signalCounts[gameId] ?: 0) + 1
            }
        }
        val agreement = minOf(2, rankedGameIds.size)
        return weighted.mapValues { (gameId, value) ->
            val signals = signalCounts.getValue(gameId)
            val confidence = if (signals >= agreement) 1.0 else SINGLE_SIGNAL_CONFIDENCE
            Attention(value / totalWeight * confidence, signals)
        }
    }

    /**
     * Candidates to hydrate, strongest attention first. Relevance never exceeds 1.0, so a game left out scores at most
     * the attention of the last hydrated game.
     */
    fun hydrationOrder(attention: Map<Int, Attention>): List<Int> =
        attention.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Attention>> { it.value.score }.thenBy { it.key })
            .take(HYDRATION_COUNT)
            .map { it.key }

    /** Seconds since each game last appeared at an event that started within [EVENT_WINDOW]; ongoing events are 0. */
    fun showcaseAges(
        events: List<IgdbEvent>,
        nowSeconds: Long,
    ): Map<Int, Long> {
        val oldestStart = nowSeconds - EVENT_WINDOW.inWholeSeconds
        val ages = mutableMapOf<Int, Long>()
        for (event in events) {
            val start = event.startTime ?: continue
            if (start > nowSeconds || start < oldestStart) continue
            val age = (nowSeconds - (event.endTime ?: start)).coerceAtLeast(0)
            event.games.forEach { gameId -> ages[gameId] = minOf(age, ages[gameId] ?: Long.MAX_VALUE) }
        }
        return ages
    }

    fun rank(
        attention: Map<Int, Attention>,
        games: Map<Int, IgdbGame>,
        showcaseAges: Map<Int, Long>,
        nowSeconds: Long,
        limit: Int = RESULT_COUNT,
    ): List<TrendingGame> {
        val scored =
            games.values.mapNotNull { game ->
                val gameAttention = attention[game.id]?.score ?: return@mapNotNull null
                val name = game.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val coverUrl = game.cover?.url?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                if (!PopularNowRanking.isListable(game)) return@mapNotNull null
                val activity = recentActivity(game, showcaseAges[game.id], nowSeconds)
                val score = gameAttention * activity.relevance
                if (score < MIN_SCORE) return@mapNotNull null
                TrendingGame(game.id, name, coverUrl, activity.reason, activity.platform, score, gameAttention)
            }
        val variety = FeedVariety()
        return scored
            .sortedWith(
                compareByDescending<TrendingGame> { it.score }
                    .thenByDescending { it.attention }
                    .thenBy { it.id },
            ).filter { variety.accept(checkNotNull(games[it.id]), it.name) }
            .take(limit)
    }

    /** The relevance of a game's strongest recent activity, with its reason only while the label is still accurate. */
    internal data class RecentActivity(
        val relevance: Double,
        val reason: TrendingReason?,
        val platform: String? = null,
    )

    internal fun recentActivity(
        game: IgdbGame,
        showcaseAgeSeconds: Long?,
        nowSeconds: Long,
    ): RecentActivity {
        val released = PopularNowRanking.isReleased(game, nowSeconds)
        val options = mutableListOf(RecentActivity(if (released) ESTABLISHED else UPCOMING, reason = null))
        val firstRelease = game.firstReleaseDate
        if (released && firstRelease != null) {
            options += labeled(launch, ageDays(firstRelease, nowSeconds))
        }
        showcaseAgeSeconds?.let { options += labeled(showcase, it / SECONDS_PER_DAY) }
        if (firstRelease != null) options += platformActivity(game, firstRelease, nowSeconds)
        // Highest relevance wins; on ties the list order prefers the launch over later activity.
        val strongest =
            compareBy<IndexedValue<RecentActivity>> { it.value.relevance }.thenByDescending { it.index }
        return options.withIndex().maxWith(strongest).value
    }

    /** New platform launches and early access exits, from the dated releases on each platform. */
    private fun platformActivity(
        game: IgdbGame,
        firstRelease: Long,
        nowSeconds: Long,
    ): List<RecentActivity> {
        val oldestRecent = nowSeconds - (newPlatform.goneDays * SECONDS_PER_DAY).toLong()
        val activities = mutableListOf<RecentActivity>()
        val byPlatform =
            game.releaseDates
                .filter { it.platform != null && it.date != null && it.date <= nowSeconds }
                .groupBy { checkNotNull(it.platform).id }
        for (releases in byPlatform.values) {
            val ordered = releases.sortedBy { it.date }
            // Alpha and beta entries do not make a platform available; the first playable release does.
            val first = ordered.firstOrNull { it.status.isAvailable() }
            val firstDate = first?.date
            if (firstDate != null && firstDate >= oldestRecent) {
                val isPort = (firstDate - firstRelease) / SECONDS_PER_DAY >= PORT_MIN_GAP_DAYS
                if (isPort) {
                    val activity = labeled(newPlatform, ageDays(firstDate, nowSeconds))
                    val platform = first.platform?.abbreviation?.takeIf(String::isNotBlank)
                    activities += activity.copy(platform = platform.takeIf { activity.reason != null })
                }
            }
            val full = ordered.firstOrNull { it.status.hasName(FULL_RELEASE_STATUS) } ?: continue
            val fullDate = checkNotNull(full.date)
            val leftEarlyAccess =
                ordered.any { checkNotNull(it.date) < fullDate && it.status.hasName(EARLY_ACCESS_STATUS) }
            if (fullDate >= oldestRecent && leftEarlyAccess) {
                activities += labeled(fullRelease, ageDays(fullDate, nowSeconds))
            }
        }
        return activities
    }

    private fun labeled(
        activity: Activity,
        ageDays: Double,
    ) = RecentActivity(activity.relevance(ageDays), activity.reason.takeIf { ageDays <= activity.labelDays })

    private fun ageDays(
        timestamp: Long,
        nowSeconds: Long,
    ) = (nowSeconds - timestamp).coerceAtLeast(0) / SECONDS_PER_DAY

    /** Undated legacy entries have no status; they count as releases like full and early access releases. */
    private fun IgdbReleaseStatus?.isAvailable() =
        this?.name == null || hasName(FULL_RELEASE_STATUS) || hasName(EARLY_ACCESS_STATUS)

    private fun IgdbReleaseStatus?.hasName(statusName: String) = this?.name.equals(statusName, ignoreCase = true)
}

/**
 * Builds the Trending Now feed with two IGDB requests per cache miss: one multiquery for popularity lists, their type
 * names, and recent events, then one hydration query for the candidates with the strongest attention. Only the
 * serialized feed is cached; shared edge caching keeps it across instances.
 */
internal class TrendingService(
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

    suspend fun trending(): ResponseCache.LoadResult = cache.getOrLoad(CACHE_KEY) { load() }

    private suspend fun load(): ResponseCache.LoadResult {
        val metrics = LoadMetrics()
        val result =
            withTimeoutOrNull(TOTAL_BUDGET) {
                val nowSeconds = clock.millis() / MILLIS_PER_SECOND
                val discovery = execute(RecommendationService.MULTIQUERY_PATH, discoveryQuery(nowSeconds))
                metrics.igdbRequests++
                if (discovery.status != OK) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(discovery.status)
                val results = RecommendationQueries.parseMultiquery(checkNotNull(discovery.body))
                val signals = validPopularitySignals(results, clock.millis())
                metrics.signals = signals.keys
                if (signals.isEmpty()) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(BAD_GATEWAY)
                val attention = TrendingRanking.attention(signals)
                val showcases = TrendingRanking.showcaseAges(events(results, metrics), nowSeconds)
                val candidates = TrendingRanking.hydrationOrder(attention)
                metrics.candidates = candidates.size
                metrics.cutoffAttention = candidates.lastOrNull()?.let { attention.getValue(it).score } ?: 0.0
                val hydration = execute(GAMES_PATH, hydrationQuery(candidates))
                metrics.igdbRequests++
                if (hydration.status != OK) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(hydration.status)
                val parsed = json.parseToJsonElement(checkNotNull(hydration.body).decodeToString()).jsonArray
                val games = RecommendationQueries.parseGames(parsed).associateBy { it.id }
                val feed = TrendingRanking.rank(attention, games, showcases, nowSeconds)
                metrics.results = feed.size
                metrics.reasons = feed.count { it.reason != null }
                // An empty feed after valid signals indicates incomplete upstream data; keep serving the previous one.
                if (feed.isEmpty()) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(BAD_GATEWAY)
                ResponseCache.LoadResult.Hit(CachedResponse(trendingJson(feed).encodeToByteArray()))
            } ?: ResponseCache.LoadResult.Uncached(GATEWAY_TIMEOUT)
        val outcome = if (result is ResponseCache.LoadResult.Uncached) "failed_${result.status}" else "ok"
        log(metrics.event(outcome))
        return result
    }

    /** Events only add reasons, so malformed event data is logged and ignored instead of failing the feed. */
    private fun events(
        results: Map<String, JsonArray>,
        metrics: LoadMetrics,
    ): List<IgdbEvent> {
        val rows = results["events"]
        if (rows == null) {
            metrics.events = "missing"
            return emptyList()
        }
        return runCatching { rows.map { json.decodeFromJsonElement<IgdbEvent>(it) } }
            .onSuccess { metrics.events = it.size.toString() }
            .onFailure { metrics.events = "invalid" }
            .getOrDefault(emptyList())
    }

    /** Waits briefly for an IGDB rate-limit slot instead of failing at once during a burst. */
    private suspend fun execute(
        path: String,
        body: String,
    ) = igdb.execute(path, body, SLOT_WAIT.inWholeMilliseconds)

    /** One structured log line per cache miss; it contains no request or credential data. */
    private class LoadMetrics {
        private val started = System.nanoTime()
        var igdbRequests = 0
        var signals: Set<PopularitySignal> = emptySet()
        var events = "none"
        var candidates = 0
        var cutoffAttention = 0.0
        var results = 0
        var reasons = 0

        fun event(outcome: String) =
            "{\"event\":\"trending\",\"outcome\":\"$outcome\",\"igdb_requests\":$igdbRequests," +
                "\"signals\":\"${signals.joinToString(",") { it.queryName }}\",\"events\":\"$events\"," +
                "\"candidates\":$candidates,\"cutoff_attention\":${String.format(
                    Locale.ROOT,
                    "%.3f",
                    cutoffAttention,
                )}," +
                "\"results\":$results,\"reasons\":$reasons," +
                "\"duration_ms\":${(System.nanoTime() - started).nanoseconds.inWholeMilliseconds}}"
    }

    companion object {
        private const val CACHE_KEY = "trending"
        private const val GAMES_PATH = "/v4/games"
        private const val MILLIS_PER_SECOND = 1000
        private const val EVENT_LIMIT = 50
        private val CACHE_TTL = 30.minutes
        private val STALE_IF_ERROR = 24.hours
        private const val CACHE_BYTES = 256L * 1024
        private val TOTAL_BUDGET = 4.5.seconds
        private val SLOT_WAIT = 1.seconds
        private const val OK = 200
        private const val BAD_GATEWAY = 502
        private const val GATEWAY_TIMEOUT = 504

        fun discoveryQuery(nowSeconds: Long): String =
            popularitySignalQueries(TrendingRanking.SIGNAL_DEPTH) +
                "query events \"events\" {fields start_time,end_time,games;" +
                "where start_time >= ${nowSeconds - TrendingRanking.EVENT_WINDOW.inWholeSeconds} & " +
                "start_time <= $nowSeconds;sort start_time desc;limit $EVENT_LIMIT;};"

        fun hydrationQuery(gameIds: List<Int>): String =
            "fields name,cover.url,game_type,game_status,version_parent,first_release_date,themes,collections," +
                "age_ratings.rating_category,release_dates.date,release_dates.platform.abbreviation," +
                "release_dates.status.name;where id = (${gameIds.joinToString(",")});limit ${gameIds.size};"
    }
}

/** Compact cards with an optional verified reason: `{"id","name","cover":{"url"},"reason"?,"platform"?}`. */
internal fun trendingJson(games: List<TrendingGame>): String =
    JsonArray(
        games.map { game ->
            JsonObject(
                buildMap {
                    put("id", JsonPrimitive(game.id))
                    put("name", JsonPrimitive(game.name))
                    put("cover", JsonObject(mapOf("url" to JsonPrimitive(game.coverUrl))))
                    game.reason?.let { put("reason", JsonPrimitive(it.wireName)) }
                    game.platform?.let { put("platform", JsonPrimitive(it)) }
                },
            )
        },
    ).toString()
