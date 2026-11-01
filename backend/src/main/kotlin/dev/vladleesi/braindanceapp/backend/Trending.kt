// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ln1p
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
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

/**
 * A ranked feed entry. [score] and [attention] stay internal; [platform] is set only for a new platform release.
 * [established] marks the capped tier of games without recent dated activity.
 */
internal data class TrendingGame(
    val id: Int,
    val name: String,
    val coverUrl: String,
    val reason: TrendingReason?,
    val platform: String?,
    val score: Double,
    val attention: Double,
    val established: Boolean = false,
)

/**
 * Game IDs to hydrate after discovery, in three disjoint pools: [attentionIds], the strongest attention; [tailIds],
 * the remaining listed games, which IGDB narrows to recent releases; and [showcaseIds], recent event games.
 */
internal data class HydrationPlan(
    val attentionIds: List<Int>,
    val tailIds: List<Int>,
    val showcaseIds: List<Int>,
)

/**
 * How many unique games survived each selection stage, for diagnostics. [displayable] games have a name and cover
 * and are listable; [unreleased] displayable games are left for Most Anticipated; [weak] released ones lacked the
 * interest their tier requires. [repeated] counts eligible games skipped for variety, and [establishedCapped] those
 * the established cap held back, before the feed filled.
 */
internal data class TrendingFunnel(
    val hydrated: Int,
    val displayable: Int,
    val unreleased: Int,
    val fresh: Int,
    val established: Int,
    val weak: Int,
    val repeated: Int,
    val establishedCapped: Int,
    val results: Int,
)

/**
 * Ranking parameters. [TrendingRanking.DEFAULT_TUNING] serves the feed; the opt-in live evaluation compares
 * alternatives on the same candidates. [rankK] sets how quickly a rank's score falls: (K + 1) / (K + rank).
 * [establishedFactor] scales established games' attention, and [minEstablishedAttention] applies before scaling.
 */
internal data class TrendingTuning(
    val weights: Map<PopularitySignal, Double>,
    val rankK: Double,
    val establishedFactor: Double,
    val minEstablishedAttention: Double,
    val maxEstablished: Int,
    val resultCount: Int = TrendingRanking.FEED_GAMES,
)

internal data class TrendingSelection(
    val games: List<TrendingGame>,
    val funnel: TrendingFunnel,
)

/**
 * Popular Right Now ranking from one snapshot of current IGDB data. IGDB popularity primitives hold only their latest
 * values, so growth over time cannot be measured without stored history; the feed ranks current discovery interest
 * and boosts verified recent activity instead. Only released games, including early access, are eligible; upcoming
 * games belong to Most Anticipated. Eligibility and ranking are separate:
 *
 * - A fresh game has dated recent activity: a launch, a first release on a new platform, leaving early access, or
 *   an appearance at a gaming event. It needs measurable interest: a listing in any valid popularity signal, or at
 *   least [MIN_HYPES] IGDB hypes (follows before release).
 * - An established game has no such activity and needs attention of [ESTABLISHED_MIN_ATTENTION]; at most
 *   [MAX_ESTABLISHED] appear in each [RESULT_COUNT] entries, so habitual audiences cannot turn the feed into a
 *   leaderboard.
 *
 * Established games rank by attention × [ESTABLISHED_FACTOR], so a habitual audience counts half as much as the same
 * attention on a game with recent activity.
 *
 * Eligible games rank by interest × (1 + [ACTIVITY_BOOST] × activity strength). Interest is attention, weighted
 * toward IGDB discovery signals, plus a small hype share for fresh games. Activity strength is 1.0 just after a launch
 * and decays to 0 over weeks, so recent activity raises a game by at most 75% rather than gating it.
 */
internal object TrendingRanking {
    /** One page of the feed, and the window the established cap applies to. */
    const val RESULT_COUNT = 20

    /** Ranked games kept per computation, for paginated reads. */
    const val FEED_GAMES = 100
    const val SIGNAL_DEPTH = 200

    /** Strongest-attention games hydrated with full metadata; fresh games beyond it come from the other pools. */
    const val HYDRATION_COUNT = 100

    /** Recent releases with hypes, listed tail releases, and event games fetched per computation. */
    const val RECENT_LIMIT = 50
    const val TAIL_LIMIT = 40
    const val SHOWCASE_LIMIT = 50

    /** Event game IDs sent to IGDB, most recently featured first. */
    const val MAX_SHOWCASE_IDS = 300

    /** IGDB hypes that show meaningful interest in a fresh game no popularity signal lists. */
    const val MIN_HYPES = 20

    /** With every signal valid: about rank 60 on Visits, Want to Play, and Twitch together. */
    const val ESTABLISHED_MIN_ATTENTION = 0.3
    const val MAX_ESTABLISHED = 6
    const val ESTABLISHED_FACTOR = 0.5

    /** Multiplier headroom for the strongest recent activity: a launch this week scores 1.75 × its interest. */
    const val ACTIVITY_BOOST = 0.75

    // Hypes add at most this much interest, below a mid-ranked popularity listing, because they measure interest
    // before release rather than now. They saturate logarithmically at POPULAR_HYPES.
    private const val HYPE_SHARE = 0.1
    private const val POPULAR_HYPES = 200.0

    // Attention weights favor discovery: IGDB page visits and Want to Play list additions measure games people are
    // looking into now, including upcoming ones. Twitch hours watched follows launches and showcase streams as well
    // as established audiences; Playing additions and Steam peak players mostly measure installed audiences, which
    // evergreen multiplayer games dominate. IGDB attributes its homepage "Popular right now" section to PopScore
    // without a published formula; these weights matched it best among the variants the opt-in live evaluation
    // compares. Shares are renormalized over valid signals.
    @Suppress("MagicNumber")
    private val signalWeights =
        mapOf(
            PopularitySignal.IGDB_VISITS to 0.40,
            PopularitySignal.IGDB_WANT_TO_PLAY to 0.20,
            PopularitySignal.TWITCH_HOURS_WATCHED to 0.20,
            PopularitySignal.IGDB_PLAYING to 0.10,
            PopularitySignal.STEAM_PEAK_PLAYERS to 0.10,
        )

    /** Signals this feed reads, in multiquery order. */
    val signals: List<PopularitySignal> = signalWeights.keys.toList()

    // A rank's score is (K + 1) / (K + rank): 1.0 at the top, 0.5 at rank 61, and about 0.23 at rank 200. A flatter
    // curve keeps games well inside several lists competitive with a few games at the very top of one.
    private const val RANK_K = 60.0

    val DEFAULT_TUNING =
        TrendingTuning(
            signalWeights,
            RANK_K,
            ESTABLISHED_FACTOR,
            ESTABLISHED_MIN_ATTENTION,
            MAX_ESTABLISHED,
        )

    // Attention seen by only one signal counts at this share when other signals exist. Agreement between independent
    // signals is more credible, but a mild discount keeps single-source discoveries competitive. It orders candidates;
    // it does not decide fresh eligibility.
    private const val SINGLE_SIGNAL_CONFIDENCE = 0.75

    const val ESTABLISHED = 0.2

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

    /** Releases this recent still raise relevance, so recent and tail pools look this far back. */
    val RECENT_WINDOW = launch.goneDays.days

    /** A platform release this soon after the first release is part of the launch, not a new platform. */
    private const val PORT_MIN_GAP_DAYS = 30.0
    private const val SECONDS_PER_DAY = 86_400.0

    // IGDB release_date_statuses names. Other statuses, such as alpha, beta, or cancelled, are not releases.
    private const val FULL_RELEASE_STATUS = "full release"
    private const val EARLY_ACCESS_STATUS = "early access"

    /** Weighted rank attention per game. */
    fun attention(
        rankedGameIds: Map<PopularitySignal, List<Int>>,
        tuning: TrendingTuning = DEFAULT_TUNING,
    ): Map<Int, Attention> {
        val weights = tuning.weights
        val listed = rankedGameIds.filterKeys { (weights[it] ?: 0.0) > 0.0 }
        val totalWeight = listed.keys.sumOf { weights.getValue(it) }
        if (totalWeight == 0.0) return emptyMap()
        val weighted = mutableMapOf<Int, Double>()
        val signalCounts = mutableMapOf<Int, Int>()
        for ((signal, gameIds) in listed) {
            val weight = weights.getValue(signal)
            gameIds.distinct().forEachIndexed { index, gameId ->
                weighted[gameId] = (weighted[gameId] ?: 0.0) + weight * (tuning.rankK + 1) / (tuning.rankK + index + 1)
                signalCounts[gameId] = (signalCounts[gameId] ?: 0) + 1
            }
        }
        val agreement = minOf(2, listed.size)
        return weighted.mapValues { (gameId, value) ->
            val signals = signalCounts.getValue(gameId)
            val confidence = if (signals >= agreement) 1.0 else SINGLE_SIGNAL_CONFIDENCE
            Attention(value / totalWeight * confidence, signals)
        }
    }

    /**
     * Splits candidates not already fetched in [recentIds] into hydration pools. Relevance is unknown before
     * hydration, so the strongest attention is hydrated in full, while listed tail games and event games are
     * fetched only when IGDB filters show they can be fresh. Event games featured most recently come first.
     */
    fun hydrationPlan(
        attention: Map<Int, Attention>,
        recentIds: Set<Int>,
        showcaseAges: Map<Int, Long>,
    ): HydrationPlan {
        val byAttention =
            attention.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Attention>> { it.value.score }.thenBy { it.key })
                .map { it.key }
                .filter { it !in recentIds }
        val strongest = byAttention.take(HYDRATION_COUNT)
        val strongestIds = strongest.toSet()
        val showcased =
            showcaseAges.entries
                .sortedWith(compareBy<Map.Entry<Int, Long>> { it.value }.thenBy { it.key })
                .map { it.key }
                .filter { it !in recentIds && it !in strongestIds }
                .take(MAX_SHOWCASE_IDS)
        val showcasedIds = showcased.toSet()
        return HydrationPlan(strongest, byAttention.drop(HYDRATION_COUNT).filter { it !in showcasedIds }, showcased)
    }

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
        tuning: TrendingTuning = DEFAULT_TUNING,
    ): List<TrendingGame> = select(attention, games, showcaseAges, nowSeconds, tuning).games

    /**
     * Ranks displayable, eligible games strongest first. Ties prefer more attention, then the lower ID. The feed is
     * shorter rather than filled with games that meet no eligibility rule.
     */
    fun select(
        attention: Map<Int, Attention>,
        games: Map<Int, IgdbGame>,
        showcaseAges: Map<Int, Long>,
        nowSeconds: Long,
        tuning: TrendingTuning = DEFAULT_TUNING,
    ): TrendingSelection {
        var displayable = 0
        var unreleased = 0
        val eligible = mutableListOf<TrendingGame>()
        for (game in games.values) {
            val name = game.name?.takeIf(String::isNotBlank) ?: continue
            val coverUrl = game.cover?.url?.takeIf(String::isNotBlank) ?: continue
            if (!FeedEligibility.isListable(game)) continue
            displayable++
            if (!FeedEligibility.isReleased(game, nowSeconds)) {
                unreleased++
                continue
            }
            val activity = recentActivity(game, showcaseAges[game.id], nowSeconds)
            val gameAttention = attention[game.id]
            val (score, established) = score(game, gameAttention, activity, tuning) ?: continue
            val attentionScore = gameAttention?.score ?: 0.0
            eligible +=
                TrendingGame(
                    game.id,
                    name,
                    coverUrl,
                    activity.reason,
                    activity.platform,
                    score,
                    attentionScore,
                    established,
                )
        }
        val ordered =
            eligible.sortedWith(
                compareByDescending<TrendingGame> { it.score }
                    .thenByDescending { it.attention }
                    .thenBy { it.id },
            )
        val fill = fill(ordered, games, tuning)
        val established = eligible.count { it.established }
        val funnel =
            TrendingFunnel(
                hydrated = games.size,
                displayable = displayable,
                unreleased = unreleased,
                fresh = eligible.size - established,
                established = established,
                weak = displayable - unreleased - eligible.size,
                repeated = fill.repeated,
                establishedCapped = fill.establishedCapped,
                results = fill.feed.size,
            )
        return TrendingSelection(fill.feed, funnel)
    }

    private class Fill(
        val feed: List<TrendingGame>,
        val repeated: Int,
        val establishedCapped: Int,
    )

    /**
     * Takes [ordered] games until the feed is full, skipping repeats for variety. Each [RESULT_COUNT] entries admit
     * [TrendingTuning.maxEstablished] more established games; one held back by the cap keeps its place in line and
     * joins as soon as a later page admits it.
     */
    private fun fill(
        ordered: List<TrendingGame>,
        games: Map<Int, IgdbGame>,
        tuning: TrendingTuning,
    ): Fill {
        val variety = FeedVariety()
        val feed = mutableListOf<TrendingGame>()
        val held = ArrayDeque<TrendingGame>()
        var repeated = 0
        var establishedCapped = 0
        var establishedCount = 0

        fun admitsEstablished() = establishedCount < tuning.maxEstablished * (feed.size / RESULT_COUNT + 1)

        fun add(entry: TrendingGame) {
            if (!variety.accept(checkNotNull(games[entry.id]), entry.name)) {
                repeated++
                return
            }
            if (entry.established) establishedCount++
            feed += entry
        }

        fun releaseHeld() {
            while (held.isNotEmpty() && feed.size < tuning.resultCount && admitsEstablished()) add(held.removeFirst())
        }
        for (entry in ordered) {
            releaseHeld()
            if (feed.size == tuning.resultCount) break
            if (entry.established && !admitsEstablished()) {
                held += entry
                establishedCapped++
            } else {
                add(entry)
            }
        }
        releaseHeld()
        return Fill(feed, repeated, establishedCapped)
    }

    /** The game's score and whether it is established, or null when it lacks the interest its tier requires. */
    private fun score(
        game: IgdbGame,
        attention: Attention?,
        activity: RecentActivity,
        tuning: TrendingTuning,
    ): Pair<Double, Boolean>? {
        val attentionScore = attention?.score ?: 0.0
        val strength = ((activity.relevance - ESTABLISHED) / (1.0 - ESTABLISHED)).coerceIn(0.0, 1.0)
        val fresh = strength > 0.0
        val score =
            if (fresh) {
                val hypes = game.hypes ?: 0
                if (attention == null && hypes < MIN_HYPES) return null
                (attentionScore + HYPE_SHARE * hypeShare(hypes)) * (1.0 + ACTIVITY_BOOST * strength)
            } else {
                if (attentionScore < tuning.minEstablishedAttention) return null
                attentionScore * tuning.establishedFactor
            }
        return score to !fresh
    }

    private fun hypeShare(hypes: Int) = minOf(1.0, ln1p(hypes.coerceAtLeast(0).toDouble()) / ln1p(POPULAR_HYPES))

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
        val released = FeedEligibility.isReleased(game, nowSeconds)
        val options = mutableListOf(RecentActivity(ESTABLISHED, reason = null))
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
 * Builds the Popular Right Now feed with two IGDB multiqueries per cache miss. The first reads popularity lists, their
 * type names, recent events, and recent releases with hypes; the second hydrates the released strongest-attention
 * candidates, listed tail games released recently, and released event games. Up to [TrendingRanking.FEED_GAMES]
 * ranked games are cached as one serialized feed, and pages are cut from it; shared edge caching keeps pages across
 * instances.
 */
internal class TrendingService(
    private val igdb: IgdbService,
    private val clock: Clock = Clock.systemUTC(),
    private val log: (String) -> Unit = ::println,
    private val responseBudget: Duration = RESPONSE_BUDGET,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refresh = AtomicReference<Deferred<ResponseCache.LoadResult>?>()
    private val cache =
        ResponseCache(
            FEED_WINDOW.inWholeMilliseconds,
            CACHE_BYTES,
            CACHE_BYTES.toInt(),
            clock,
            staleIfErrorMillis = STALE_IF_ERROR.inWholeMilliseconds,
            expiresAt = ::feedWindowEnd,
        )
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var pages: FeedPages? = null

    /** Page [page] (1-based) of [pageSize] games; past the end it is empty. Callers validate both bounds. */
    suspend fun trending(
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
    ): ResponseCache.LoadResult =
        when (val result = cache.getOrLoad(CACHE_KEY) { awaitRefresh() }) {
            is ResponseCache.LoadResult.Hit -> result.copy(response = pagesOf(result.response).page(page, pageSize))
            is ResponseCache.LoadResult.Uncached -> result
        }

    override fun close() {
        scope.cancel()
    }

    /**
     * Waits up to [responseBudget], just under the clients' timeout, for the shared feed refresh. A cold instance can
     * need longer; the refresh then keeps running, bounded by [LOAD_LIMIT] and upstream timeouts, and stores its feed,
     * so a retry joins it or reads the cache instead of starting over.
     */
    private suspend fun awaitRefresh(): ResponseCache.LoadResult {
        val running = refresh.get()?.takeUnless { it.isCompleted } ?: startRefresh()
        return withTimeoutOrNull(responseBudget) { running.await() }
            ?: ResponseCache.LoadResult.Uncached(GATEWAY_TIMEOUT)
    }

    private fun startRefresh(): Deferred<ResponseCache.LoadResult> {
        val started =
            scope.async(start = CoroutineStart.LAZY) {
                load().also { if (it is ResponseCache.LoadResult.Hit) cache.store(CACHE_KEY, it.response) }
            }
        while (true) {
            val current = refresh.get()
            if (current != null && !current.isCompleted) {
                started.cancel()
                return current
            }
            if (refresh.compareAndSet(current, started)) {
                started.start()
                return started
            }
        }
    }

    private fun pagesOf(feed: CachedResponse): FeedPages =
        pages?.takeIf { it.feed === feed } ?: FeedPages(feed, json).also { pages = it }

    /** Page bodies cut from one cached feed, built once per page; replaced when the cached feed changes. */
    private class FeedPages(
        val feed: CachedResponse,
        private val json: Json,
    ) {
        private val entries by lazy { json.parseToJsonElement(feed.body.decodeToString()).jsonArray }
        private val bodies = ConcurrentHashMap<Pair<Int, Int>, CachedResponse>()

        fun page(
            page: Int,
            pageSize: Int,
        ): CachedResponse =
            bodies.computeIfAbsent(page to pageSize) {
                val start = (page - 1).toLong() * pageSize
                val slice = if (start >= entries.size) emptyList() else entries.drop(start.toInt()).take(pageSize)
                CachedResponse(JsonArray(slice).toString().encodeToByteArray())
            }
    }

    private suspend fun load(): ResponseCache.LoadResult {
        val metrics = LoadMetrics()
        val result =
            withTimeoutOrNull(LOAD_LIMIT) {
                val nowSeconds = clock.millis() / MILLIS_PER_SECOND
                val discovery = execute(discoveryQuery(nowSeconds))
                metrics.igdbRequests++
                if (discovery.status != OK) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(discovery.status)
                val discoveryBody = checkNotNull(discovery.body)
                metrics.discoveryBytes = discoveryBody.size
                val results = RecommendationQueries.parseMultiquery(discoveryBody)
                val signals = validPopularitySignals(results, clock.millis(), TrendingRanking.signals)
                metrics.signals = signals.keys
                if (signals.isEmpty()) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(BAD_GATEWAY)
                val attention = TrendingRanking.attention(signals)
                val showcases = TrendingRanking.showcaseAges(events(results, metrics), nowSeconds)
                val recent = recentReleases(results, metrics)
                metrics.listed = attention.size
                metrics.eventGames = showcases.size
                val plan = TrendingRanking.hydrationPlan(attention, recent.keys, showcases)
                metrics.plan = plan
                metrics.cutoffAttention = plan.attentionIds.lastOrNull()?.let { attention.getValue(it).score } ?: 0.0
                val hydrationQuery = hydrationQuery(plan, nowSeconds)
                val hydrated =
                    if (hydrationQuery.isEmpty()) {
                        emptyMap()
                    } else {
                        val hydration = execute(hydrationQuery)
                        metrics.igdbRequests++
                        if (hydration.status != OK) {
                            return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(hydration.status)
                        }
                        val hydrationBody = checkNotNull(hydration.body)
                        metrics.hydrationBytes = hydrationBody.size
                        RecommendationQueries.parseMultiquery(hydrationBody)
                    }
                val games =
                    recent +
                        HYDRATION_QUERY_NAMES
                            .flatMap { RecommendationQueries.parseGames(hydrated[it]) }
                            .associateBy { it.id }
                val selection = TrendingRanking.select(attention, games, showcases, nowSeconds)
                metrics.funnel = selection.funnel
                metrics.reasons = selection.games.count { it.reason != null }
                // An empty feed after valid signals indicates incomplete upstream data; keep serving the previous one.
                if (selection.games.isEmpty()) return@withTimeoutOrNull ResponseCache.LoadResult.Uncached(BAD_GATEWAY)
                ResponseCache.LoadResult.Hit(CachedResponse(trendingJson(selection.games).encodeToByteArray()))
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

    /** Recent releases only add candidates, so malformed rows are logged and ignored like events. */
    private fun recentReleases(
        results: Map<String, JsonArray>,
        metrics: LoadMetrics,
    ): Map<Int, IgdbGame> {
        val rows = results[RECENT_QUERY_NAME]
        if (rows == null) {
            metrics.recent = "missing"
            return emptyMap()
        }
        return runCatching { RecommendationQueries.parseGames(rows).associateBy { it.id } }
            .onSuccess { metrics.recent = it.size.toString() }
            .onFailure { metrics.recent = "invalid" }
            .getOrDefault(emptyMap())
    }

    /** Waits briefly for an IGDB rate-limit slot instead of failing at once during a burst. */
    private suspend fun execute(body: String) =
        igdb.execute(RecommendationService.MULTIQUERY_PATH, body, SLOT_WAIT.inWholeMilliseconds)

    /**
     * One structured log line per cache miss with candidate counts per stage; it contains no request or credential
     * data.
     */
    private class LoadMetrics {
        private val started = System.nanoTime()
        var igdbRequests = 0
        var signals: Set<PopularitySignal> = emptySet()
        var events = "none"
        var recent = "none"
        var listed = 0
        var eventGames = 0
        var plan: HydrationPlan? = null
        var cutoffAttention = 0.0
        var discoveryBytes = 0
        var hydrationBytes = 0
        var funnel: TrendingFunnel? = null
        var reasons = 0

        fun event(outcome: String): String {
            val fields =
                linkedMapOf<String, Any>(
                    "outcome" to "\"$outcome\"",
                    "igdb_requests" to igdbRequests,
                    "signals" to "\"${signals.joinToString(",") { it.queryName }}\"",
                    "events" to "\"$events\"",
                    "recent" to "\"$recent\"",
                    "listed" to listed,
                    "event_games" to eventGames,
                    "hydrate_attention" to (plan?.attentionIds?.size ?: 0),
                    "hydrate_tail" to (plan?.tailIds?.size ?: 0),
                    "hydrate_showcase" to (plan?.showcaseIds?.size ?: 0),
                    "cutoff_attention" to String.format(Locale.ROOT, "%.3f", cutoffAttention),
                )
            funnel?.let {
                fields["hydrated"] = it.hydrated
                fields["displayable"] = it.displayable
                fields["unreleased"] = it.unreleased
                fields["fresh"] = it.fresh
                fields["established"] = it.established
                fields["weak"] = it.weak
                fields["repeated"] = it.repeated
                fields["established_capped"] = it.establishedCapped
                fields["results"] = it.results
            }
            fields["reasons"] = reasons
            fields["discovery_bytes"] = discoveryBytes
            fields["hydration_bytes"] = hydrationBytes
            fields["duration_ms"] = (System.nanoTime() - started).nanoseconds.inWholeMilliseconds
            return fields.entries.joinToString(",", "{\"event\":\"trending\",", "}") { "\"${it.key}\":${it.value}" }
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = TrendingRanking.RESULT_COUNT
        const val MAX_PAGE_SIZE = 50
        private const val CACHE_KEY = "trending"
        private const val MILLIS_PER_SECOND = 1000
        private const val EVENT_LIMIT = 50

        // IGDB refreshes popularity daily. One ranked feed serves each fixed UTC window, in process and at the edge,
        // so its pages stay consistent; feeds of different instances in one window can still differ slightly.
        private val FEED_WINDOW = 6.hours

        private fun feedWindowEnd(nowMillis: Long): Long {
            val window = FEED_WINDOW.inWholeMilliseconds
            return (nowMillis / window + 1) * window
        }

        /** Time until the current feed window ends, rounded up to whole seconds. */
        fun feedTimeLeft(nowMillis: Long): Duration =
            ((feedWindowEnd(nowMillis) - nowMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).seconds

        private val STALE_IF_ERROR = 24.hours
        private const val CACHE_BYTES = 256L * 1024
        private val RESPONSE_BUDGET = 4.5.seconds
        private val LOAD_LIMIT = 20.seconds
        private val SLOT_WAIT = 1.seconds
        private const val OK = 200
        private const val BAD_GATEWAY = 502
        private const val GATEWAY_TIMEOUT = 504

        const val RECENT_QUERY_NAME = "recent"
        private const val ATTENTION_QUERY_NAME = "attention"
        private const val TAIL_QUERY_NAME = "tail"
        private const val SHOWCASE_QUERY_NAME = "showcased"
        val HYDRATION_QUERY_NAMES = listOf(ATTENTION_QUERY_NAME, TAIL_QUERY_NAME, SHOWCASE_QUERY_NAME)

        private const val GAME_FIELDS =
            "name,cover.url,game_type,game_status,version_parent,first_release_date,hypes,themes,collections," +
                "age_ratings.rating_category,release_dates.date,release_dates.platform.abbreviation," +
                "release_dates.status.name"

        // Upstream narrowing for the supplementary pools. Ranking still applies every eligibility rule, so the
        // null-safe clauses only reduce payload; they match the anticipated query's proven syntax.
        private val listableFilter =
            "version_parent = null & cover != null & ${RecommendationQueries.gameTypeFilter}" +
                " & (game_status = null | game_status != 6)" +
                " & (themes = null | themes != (${RecommendationQueries.EROTIC_THEME}))"

        /** Released within [TrendingRanking.RECENT_WINDOW] of [nowSeconds]. */
        private fun recentlyReleased(nowSeconds: Long) =
            "first_release_date >= ${nowSeconds - TrendingRanking.RECENT_WINDOW.inWholeSeconds} & " +
                "first_release_date <= $nowSeconds"

        fun discoveryQuery(nowSeconds: Long): String =
            popularitySignalQueries(TrendingRanking.SIGNAL_DEPTH, TrendingRanking.signals) +
                "query events \"events\" {fields start_time,end_time,games;" +
                "where start_time >= ${nowSeconds - TrendingRanking.EVENT_WINDOW.inWholeSeconds} & " +
                "start_time <= $nowSeconds;sort start_time desc;limit $EVENT_LIMIT;};" +
                "query games \"$RECENT_QUERY_NAME\" {fields $GAME_FIELDS;where ${recentlyReleased(nowSeconds)} & " +
                "hypes >= ${TrendingRanking.MIN_HYPES} & $listableFilter;sort hypes desc;" +
                "limit ${TrendingRanking.RECENT_LIMIT};};"

        fun hydrationQuery(
            plan: HydrationPlan,
            nowSeconds: Long,
        ): String =
            buildString {
                if (plan.attentionIds.isNotEmpty()) {
                    append(
                        "query games \"$ATTENTION_QUERY_NAME\" {fields $GAME_FIELDS;" +
                            "where id = (${plan.attentionIds.joinToString(",")}) & " +
                            "${FeedEligibility.releasedFilter(nowSeconds)};limit ${plan.attentionIds.size};};",
                    )
                }
                if (plan.tailIds.isNotEmpty()) {
                    append(
                        "query games \"$TAIL_QUERY_NAME\" {fields $GAME_FIELDS;" +
                            "where id = (${plan.tailIds.joinToString(",")}) & ${recentlyReleased(nowSeconds)} & " +
                            "$listableFilter;sort hypes desc;limit ${TrendingRanking.TAIL_LIMIT};};",
                    )
                }
                if (plan.showcaseIds.isNotEmpty()) {
                    append(
                        "query games \"$SHOWCASE_QUERY_NAME\" {fields $GAME_FIELDS;" +
                            "where id = (${plan.showcaseIds.joinToString(",")}) & $listableFilter & " +
                            "${FeedEligibility.releasedFilter(nowSeconds)};sort hypes desc;" +
                            "limit ${TrendingRanking.SHOWCASE_LIMIT};};",
                    )
                }
            }
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
