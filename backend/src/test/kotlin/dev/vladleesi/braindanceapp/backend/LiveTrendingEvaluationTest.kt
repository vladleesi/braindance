// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.Instant
import java.util.Locale
import kotlin.test.Test
import kotlin.time.measureTimedValue

/**
 * Opt-in Trending Now report on live IGDB data (about 10 IGDB requests):
 *
 * ```sh
 * BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
 *   ./gradlew :backend:test --tests '*LiveTrendingEvaluationTest*' --rerun -i
 * ```
 *
 * It prints popularity type names, recent IGDB events, the candidate pools and per-stage survivor counts, the feed
 * with each entry's tier, attention, hypes, and reason, and request sizes and latency. It shows where each game of the
 * latest [homepageSnapshots] entry ranks or why it is excluded, then reranks the same candidate pool with alternative
 * [TrendingTuning] values and compares each variant, and the ranking before this change, with that snapshot: recall,
 * unreleased, recent, older, and indie entries. Finally it times a cold and a warm service call. It asserts nothing
 * about ranking quality, which needs human review.
 */
class LiveTrendingEvaluationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun printTrendingReport() {
        val clientId = System.getenv("TWITCH_CLIENT_ID")
        val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")
        assumeTrue(
            System.getenv("BRAINDANCE_LIVE_EVAL") == "1" && !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank(),
            "Set BRAINDANCE_LIVE_EVAL=1 and Twitch credentials",
        )
        val config = BackendConfig(clientId, clientSecret, null, emptySet(), 8080)
        val upstream = KtorUpstream(HttpClient(CIO) { upstreamDefaults() })
        val igdb = IgdbService(upstream, InMemoryRateLimiter(), config)
        try {
            runBlocking {
                val nowSeconds = Instant.now().epochSecond
                val types = igdb.execute("/v4/popularity_types", "fields name,updated_at;sort id asc;limit 50;", 2000)
                println("popularity_types: ${types.body?.decodeToString()}")
                val eventNames =
                    igdb.execute(
                        "/v4/events",
                        "fields name,start_time,games;where start_time >= ${nowSeconds - 30 * 86_400};" +
                            "sort start_time desc;limit 50;",
                        2000,
                    )
                println("events (30 days): ${eventNames.body?.decodeToString()?.take(3000)}")

                val (discovery, discoveryTime) =
                    measureTimedValue {
                        igdb.execute(
                            RecommendationService.MULTIQUERY_PATH,
                            TrendingService.discoveryQuery(nowSeconds),
                            2000,
                        )
                    }
                val discoveryBody = checkNotNull(discovery.body) { "discovery failed: ${discovery.status}" }
                val results = RecommendationQueries.parseMultiquery(discoveryBody)
                val signals = validPopularitySignals(results, System.currentTimeMillis(), TrendingRanking.signals)
                println("discovery: ${discoveryBody.size} bytes in $discoveryTime; valid signals ${signals.keys}")
                val events = results["events"].orEmpty().map { json.decodeFromJsonElement<IgdbEvent>(it) }
                val recent =
                    RecommendationQueries
                        .parseGames(results[TrendingService.RECENT_QUERY_NAME])
                        .associateBy { it.id }
                val attention = TrendingRanking.attention(signals)
                val showcases = TrendingRanking.showcaseAges(events, nowSeconds)
                val plan = TrendingRanking.hydrationPlan(attention, recent.keys, showcases)
                println(
                    "pools: listed ${attention.size} (per signal ${signals.mapValues { it.value.size }}), " +
                        "events ${events.size} with ${showcases.size} games, recent releases ${recent.size}; " +
                        "hydrate attention ${plan.attentionIds.size}, tail ${plan.tailIds.size}, " +
                        "showcase ${plan.showcaseIds.size}",
                )

                val (hydration, hydrationTime) =
                    measureTimedValue {
                        igdb.execute(
                            RecommendationService.MULTIQUERY_PATH,
                            TrendingService.hydrationQuery(plan, nowSeconds),
                            2000,
                        )
                    }
                val hydrationBody = checkNotNull(hydration.body) { "hydration failed: ${hydration.status}" }
                val hydrated = RecommendationQueries.parseMultiquery(hydrationBody)
                println(
                    "hydration: ${hydrationBody.size} bytes in $hydrationTime; returned " +
                        TrendingService.HYDRATION_QUERY_NAMES.associateWith { hydrated[it]?.size ?: 0 },
                )
                val games =
                    recent +
                        TrendingService.HYDRATION_QUERY_NAMES
                            .flatMap { RecommendationQueries.parseGames(hydrated[it]) }
                            .associateBy { it.id }
                val (selection, rankingTime) =
                    measureTimedValue { TrendingRanking.select(attention, games, showcases, nowSeconds) }
                val payload = trendingJson(selection.games.take(TrendingRanking.RESULT_COUNT)).encodeToByteArray().size
                println("ranking: $rankingTime; payload $payload bytes; funnel ${selection.funnel}")
                println("first page of ${selection.games.size} ranked games:")
                printFeed(selection.games.take(TrendingRanking.RESULT_COUNT), attention, games, nowSeconds)

                printComparisons(igdb, signals, games, showcases, nowSeconds)
                printServiceTimings(igdb)
            }
        } finally {
            upstream.close()
        }
    }

    /** Reranks the production candidate pool with each weighting and compares it, and the legacy feed, with IGDB. */
    private suspend fun printComparisons(
        igdb: IgdbService,
        signals: Map<PopularitySignal, List<Int>>,
        games: Map<Int, IgdbGame>,
        showcases: Map<Int, Long>,
        nowSeconds: Long,
    ) {
        val indie = indieGameIds(igdb, games.keys)
        val snapshot = homepageSnapshots.last()
        val unbounded = TrendingRanking.DEFAULT_TUNING.copy(resultCount = UNBOUNDED, maxEstablished = UNBOUNDED)
        val fullRanking =
            TrendingRanking.select(TrendingRanking.attention(signals), games, showcases, nowSeconds, unbounded).games
        printSnapshotCoverage(snapshot, games, signals, fullRanking, nowSeconds)
        val legacy = legacyFeed(igdb, signals, showcases, nowSeconds)
        val legacyGames = legacy.associate { it.id to it.game }
        val legacyEntries = legacy.map { it.id to it.name }
        println(compare("legacy (0.9.0)", legacyEntries, legacyGames, snapshot, nowSeconds, indie))
        for ((label, tuning) in tuningVariants) {
            val variantAttention = TrendingRanking.attention(signals, tuning)
            val firstPage = tuning.copy(resultCount = TrendingRanking.RESULT_COUNT)
            val variant = TrendingRanking.select(variantAttention, games, showcases, nowSeconds, firstPage)
            println(compare(label, variant.games.map { it.id to it.name }, games, snapshot, nowSeconds, indie))
            println("    ${variant.games.joinToString(" | ") { it.name }}")
        }
    }

    private fun printFeed(
        feed: List<TrendingGame>,
        attention: Map<Int, Attention>,
        games: Map<Int, IgdbGame>,
        nowSeconds: Long,
    ) {
        feed.forEachIndexed { index, game ->
            val released = games[game.id]?.let { FeedEligibility.isReleased(it, nowSeconds) } ?: false
            println(
                String.format(
                    Locale.ROOT,
                    "%2d. %-45s %-11s %-8s score %.3f attention %.3f (%d signals) hypes %4d reason %s %s",
                    index + 1,
                    game.name,
                    if (game.established) "established" else "fresh",
                    if (released) "released" else "upcoming",
                    game.score,
                    game.attention,
                    attention[game.id]?.signals ?: 0,
                    games[game.id]?.hypes ?: 0,
                    game.reason?.wireName ?: "-",
                    game.platform.orEmpty(),
                ),
            )
        }
    }

    /** Where each homepage game sits in this snapshot's signals, and whether discovery hydrated it. */
    private fun printSnapshotCoverage(
        snapshot: HomepageSnapshot,
        games: Map<Int, IgdbGame>,
        signals: Map<PopularitySignal, List<Int>>,
        fullRanking: List<TrendingGame>,
        nowSeconds: Long,
    ) {
        val byTitle = games.values.associateBy { RecommendationRanking.normalizedTitle(it.name.orEmpty()) }
        println("homepage snapshot ${snapshot.date} (manual fixture, ${snapshot.titles.size} games):")
        for (title in snapshot.titles) {
            val game = byTitle[RecommendationRanking.normalizedTitle(title)]
            val ranks =
                game?.let { found ->
                    signals.entries.joinToString(" ") { (signal, ids) ->
                        "${signal.queryName}=${ids.indexOf(found.id).takeIf { it >= 0 }?.plus(1) ?: "-"}"
                    }
                }
            val position = game?.let { found -> fullRanking.indexOfFirst { it.id == found.id } } ?: -1
            val placement =
                when {
                    game == null -> ""
                    position >= 0 ->
                        String.format(
                            Locale.ROOT,
                            "; uncapped rank %d score %.3f",
                            position + 1,
                            fullRanking[position].score,
                        )
                    else -> "; excluded: ${exclusion(game, nowSeconds)}"
                }
            println("    $title: ${if (game == null) "not hydrated" else "id ${game.id} $ranks$placement"}")
        }
    }

    private fun exclusion(
        game: IgdbGame,
        nowSeconds: Long,
    ): String =
        when {
            game.cover?.url.isNullOrBlank() -> "no cover"
            !FeedEligibility.isReleased(game, nowSeconds) -> "unreleased"
            !FeedEligibility.isListable(game) ->
                "not listable (type ${game.gameType}, status ${game.gameStatus}, parent ${game.versionParent})"
            else -> "weak (hypes ${game.hypes})"
        }

    /** One compact line of benchmark metrics for a feed of `(id, name)` entries. */
    @Suppress("LongParameterList")
    private fun compare(
        label: String,
        feed: List<Pair<Int, String>>,
        games: Map<Int, IgdbGame>,
        snapshot: HomepageSnapshot,
        nowSeconds: Long,
        indie: Set<Int>,
    ): String {
        val expected = snapshot.titles.map(RecommendationRanking::normalizedTitle).toSet()
        val hits = feed.count { RecommendationRanking.normalizedTitle(it.second) in expected }
        val released = feed.mapNotNull { games[it.first]?.firstReleaseDate }
        // Unreleased includes upcoming games IGDB lists without a date.
        val upcoming = feed.mapNotNull { games[it.first] }.count { !FeedEligibility.isReleased(it, nowSeconds) }
        val recentReleases = released.count { it in nowSeconds - RECENT_SECONDS..nowSeconds }
        val evergreen = released.count { it < nowSeconds - EVERGREEN_SECONDS }
        return String.format(
            Locale.ROOT,
            "%-30s results %2d, homepage recall %2d/%d, unreleased %2d, released within 60 days %2d, " +
                "released over 2 years ago %2d, indie %2d",
            label,
            feed.size,
            hits,
            expected.size,
            upcoming,
            recentReleases,
            evergreen,
            feed.count { it.first in indie },
        )
    }

    /** IDs of games IGDB tags with the Indie genre, from one extra request. */
    private suspend fun indieGameIds(
        igdb: IgdbService,
        gameIds: Set<Int>,
    ): Set<Int> {
        if (gameIds.isEmpty()) return emptySet()
        val response =
            igdb.execute(
                "/v4/games",
                "fields id;where id = (${gameIds.joinToString(",")}) & genres = ($INDIE_GENRE);limit 500;",
                2000,
            )
        val body = response.body ?: return emptySet()
        return json
            .parseToJsonElement(body.decodeToString())
            .jsonArray
            .map {
                it.jsonObject
                    .getValue("id")
                    .jsonPrimitive.content
                    .toInt()
            }.toSet()
    }

    /**
     * The ranking before this change on the same snapshot: the top 100 of the four original signals with their
     * original weights, the 120 strongest games hydrated, and `attention × relevance >= 0.15`. One extra request.
     */
    private suspend fun legacyFeed(
        igdb: IgdbService,
        signals: Map<PopularitySignal, List<Int>>,
        showcases: Map<Int, Long>,
        nowSeconds: Long,
    ): List<LegacyEntry> {
        val legacySignals = signals.filterKeys { it in legacyWeights }.mapValues { it.value.take(LEGACY_DEPTH) }
        val legacyTuning = TrendingRanking.DEFAULT_TUNING.copy(weights = legacyWeights)
        val attention = TrendingRanking.attention(legacySignals, legacyTuning)
        val legacyAttention =
            attention.mapValues { (_, value) ->
                // The original single-signal discount was 0.6 rather than the current 0.75.
                if (value.signals >= minOf(2, legacySignals.size)) value else value.copy(score = value.score * 0.8)
            }
        val candidates =
            legacyAttention.entries
                .sortedWith(compareByDescending<Map.Entry<Int, Attention>> { it.value.score }.thenBy { it.key })
                .take(LEGACY_HYDRATION)
                .map { it.key }
        val response =
            igdb.execute(
                "/v4/games",
                "fields name,cover.url,game_type,game_status,version_parent,first_release_date,themes,collections," +
                    "age_ratings.rating_category,release_dates.date,release_dates.platform.abbreviation," +
                    "release_dates.status.name;where id = (${candidates.joinToString(",")});limit ${candidates.size};",
                2000,
            )
        val body = checkNotNull(response.body) { "legacy hydration failed: ${response.status}" }
        val games = RecommendationQueries.parseGames(json.parseToJsonElement(body.decodeToString()).jsonArray)
        val scored =
            games.mapNotNull { game ->
                val name = game.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                if (game.cover?.url.isNullOrBlank() || !FeedEligibility.isListable(game)) return@mapNotNull null
                val activity = TrendingRanking.recentActivity(game, showcases[game.id], nowSeconds)
                val score = legacyAttention.getValue(game.id).score * activity.relevance
                if (score < LEGACY_MIN_SCORE) null else LegacyEntry(game, name, score)
            }
        val variety = FeedVariety()
        val feed =
            scored
                .sortedWith(compareByDescending<LegacyEntry> { it.score }.thenBy { it.id })
                .filter { variety.accept(it.game, it.name) }
                .take(TrendingRanking.RESULT_COUNT)
        println("legacy: ${feed.joinToString(" | ") { it.name }}")
        return feed
    }

    private class LegacyEntry(
        val game: IgdbGame,
        val name: String,
        val score: Double,
    ) {
        val id get() = game.id
    }

    /** Cold and warm service calls; the cold call logs the production metrics line with per-stage counts. */
    private suspend fun printServiceTimings(igdb: IgdbService) {
        val service = TrendingService(igdb)
        val (cold, coldTime) = measureTimedValue { service.trending() }
        val (_, warmTime) = measureTimedValue { service.trending() }
        val size = (cold as? ResponseCache.LoadResult.Hit)?.response?.body?.size
        println("service: cold $coldTime (${size ?: cold} bytes), warm $warmTime")
    }

    /** Titles shown in IGDB's homepage "Popular right now" section, copied by hand from a browser. */
    private data class HomepageSnapshot(
        val date: String,
        val titles: List<String>,
    )

    private companion object {
        const val UNBOUNDED = 10_000
        const val LEGACY_DEPTH = 100
        const val LEGACY_HYDRATION = 120
        const val LEGACY_MIN_SCORE = 0.15
        const val INDIE_GENRE = 32
        const val RECENT_SECONDS = 60L * 86_400
        const val EVERGREEN_SECONDS = 730L * 86_400

        val legacyWeights =
            mapOf(
                PopularitySignal.IGDB_VISITS to 0.30,
                PopularitySignal.IGDB_PLAYING to 0.25,
                PopularitySignal.TWITCH_HOURS_WATCHED to 0.25,
                PopularitySignal.STEAM_PEAK_PLAYERS to 0.20,
            )

        /** Alternatives reranked over the production candidate pool; the first is the production tuning. */
        val tuningVariants: List<Pair<String, TrendingTuning>> =
            run {
                val production = TrendingRanking.DEFAULT_TUNING
                val proposal =
                    mapOf(
                        PopularitySignal.IGDB_VISITS to 0.45,
                        PopularitySignal.IGDB_WANT_TO_PLAY to 0.25,
                        PopularitySignal.IGDB_PLAYING to 0.15,
                        PopularitySignal.TWITCH_HOURS_WATCHED to 0.075,
                        PopularitySignal.STEAM_PEAK_PLAYERS to 0.075,
                    )
                listOf(
                    "production" to production,
                    "45/25/15/7.5/7.5 weights" to production.copy(weights = proposal),
                    "0.9.0 weights" to production.copy(weights = legacyWeights),
                    "K20, floor 0.15" to production.copy(rankK = 20.0, minEstablishedAttention = 0.15),
                    "established x1.0" to production.copy(establishedFactor = 1.0),
                    "cap 3" to production.copy(maxEstablished = 3),
                )
            }

        /**
         * Manual evaluation fixture, not production data: one entry per observed homepage state, newest last. Add a
         * snapshot when evaluating on another day; compare variants across snapshots, not on one alone.
         */
        val homepageSnapshots =
            listOf(
                HomepageSnapshot(
                    "2026-11-01",
                    listOf(
                        "Call of Duty: Modern Warfare 4",
                        "Phantom Blade Zero",
                        "Valheim",
                        "Scam With Your Friends",
                        "Nomad Drive",
                        "Enshrouded",
                        "The Blood of Dawnwalker",
                        "Marvel's Wolverine",
                        "Control Resonant",
                        "TCG Card Shop Simulator",
                        "Gears of War: E-Day",
                        "Castlevania: Belmont's Curse",
                    ),
                ),
            )
    }
}
