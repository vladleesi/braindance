// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.measureTime

class TrendingTest {
    private val now = 1_800_000_000L
    private val day = 86_400L
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)

    private class MutableClock(
        var nowMillis: Long,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }

    private fun release(
        daysAgo: Long,
        platform: Int,
        abbreviation: String? = "P$platform",
        status: String? = null,
    ) = IgdbReleaseDate(now - daysAgo * day, IgdbPlatform(platform, abbreviation), status?.let(::IgdbReleaseStatus))

    private fun game(
        id: Int,
        releasedDaysAgo: Long? = 2000,
        releases: List<IgdbReleaseDate> = emptyList(),
        gameType: Int? = 0,
        status: Int? = null,
        themes: List<Int> = emptyList(),
        collections: List<Int> = emptyList(),
        cover: Boolean = true,
        name: String = "Game $id",
    ) = IgdbGame(
        id = id,
        name = name,
        cover = if (cover) IgdbCover("//images.igdb.com/igdb/image/upload/t_thumb/co$id.jpg") else null,
        gameType = gameType,
        gameStatus = status,
        firstReleaseDate = releasedDaysAgo?.let { now - it * day },
        themes = themes,
        collections = collections,
        releaseDates = releases,
    )

    private fun activity(
        game: IgdbGame,
        showcaseDaysAgo: Long? = null,
    ) = TrendingRanking.recentActivity(game, showcaseDaysAgo?.let { it * day }, now)

    /** Attention for games listed at the given rank (1-based) by every signal. */
    private fun everywhere(vararg ranks: Pair<Int, Int>): Map<PopularitySignal, List<Int>> {
        val ordered = ranks.sortedBy { it.second }
        return PopularitySignal.entries.associateWith { signal ->
            val list = MutableList(ordered.maxOf { it.second }) { 10_000 + it + signal.ordinal * 1000 }
            ordered.forEach { (gameId, rank) -> list[rank - 1] = gameId }
            list
        }
    }

    @Test
    fun attentionRewardsAgreementAndDiscountsSingleSourceAttention() {
        val attention =
            TrendingRanking.attention(
                mapOf(
                    PopularitySignal.IGDB_VISITS to listOf(1, 2, 3, 3),
                    PopularitySignal.IGDB_PLAYING to listOf(1, 2),
                    PopularitySignal.TWITCH_HOURS_WATCHED to listOf(4, 1),
                    PopularitySignal.STEAM_PEAK_PLAYERS to listOf(1),
                ),
            )
        assertEquals(1.0, attention.getValue(1).score.coerceAtMost(1.0), 0.05)
        assertEquals(4, attention.getValue(1).signals)
        // Twitch alone leads with game 4, but a single source counts less than agreement on lower ranks.
        assertTrue(attention.getValue(4).score < attention.getValue(2).score)
        // Duplicate rows in one signal count once.
        assertEquals(1, attention.getValue(3).signals)

        // Missing signals renormalize, and agreement is relative to the signals that remain.
        val twitchOnly = TrendingRanking.attention(mapOf(PopularitySignal.TWITCH_HOURS_WATCHED to listOf(7)))
        assertEquals(1.0, twitchOnly.getValue(7).score, 1e-9)
        assertEquals(emptyMap(), TrendingRanking.attention(emptyMap()))
    }

    @Test
    fun recentActivityUsesOnlyDatedEvidence() {
        val launch = activity(game(1, releasedDaysAgo = 3))
        assertEquals(TrendingReason.NEW_RELEASE to 1.0, launch.reason to launch.relevance)
        val olderLaunch = activity(game(1, releasedDaysAgo = 45))
        assertNull(olderLaunch.reason, "a 45-day-old launch is no longer labeled new")
        assertTrue(olderLaunch.relevance in TrendingRanking.ESTABLISHED..0.9)

        val established = activity(game(2))
        assertEquals(null to TrendingRanking.ESTABLISHED, established.reason to established.relevance)
        val upcoming = activity(game(3, releasedDaysAgo = -30))
        assertNull(upcoming.reason)
        assertTrue(upcoming.relevance > TrendingRanking.ESTABLISHED)

        assertEquals(TrendingReason.SHOWCASED, activity(game(2), showcaseDaysAgo = 2).reason)
        assertEquals(TrendingReason.SHOWCASED, activity(game(3, releasedDaysAgo = -30), showcaseDaysAgo = 2).reason)
        val oldShowcase = activity(game(2), showcaseDaysAgo = 25)
        assertEquals(null to TrendingRanking.ESTABLISHED, oldShowcase.reason to oldShowcase.relevance)
    }

    @Test
    fun platformActivityRequiresAFirstPlayableReleaseAfterLaunch() {
        val port = activity(game(1, releases = listOf(release(2000, 6), release(5, 167, "PS5"))))
        assertEquals(
            Triple(TrendingReason.NEW_PLATFORM, "PS5", 0.8),
            Triple(port.reason, port.platform, port.relevance),
        )

        // Another region on a platform that already had the game, a future date, and a launch-window port do not count.
        val regional = game(2, releases = listOf(release(2000, 6), release(900, 167), release(3, 167)))
        assertNull(activity(regional).reason)
        assertNull(activity(game(3, releases = listOf(release(2000, 6), release(-5, 167)))).reason)
        val launchPort = activity(game(4, releasedDaysAgo = 20, releases = listOf(release(20, 6), release(5, 167))))
        assertEquals(TrendingReason.NEW_RELEASE, launchPort.reason)

        // A beta on the platform does not make it available; its later full release does.
        val afterBeta =
            game(5, releases = listOf(release(2000, 6), release(300, 167, status = "Beta"), release(4, 167)))
        assertEquals(TrendingReason.NEW_PLATFORM, activity(afterBeta).reason)
        val betaOnly = game(6, releases = listOf(release(2000, 6), release(4, 167, status = "Beta")))
        assertNull(activity(betaOnly).reason)

        val noAbbreviation = activity(game(7, releases = listOf(release(2000, 6), release(5, 167, abbreviation = " "))))
        assertEquals(TrendingReason.NEW_PLATFORM to null, noAbbreviation.reason to noAbbreviation.platform)
        val stalePort = activity(game(8, releases = listOf(release(2000, 6), release(25, 167))))
        assertNull(stalePort.platform, "the platform is only reported while the reason is shown")
    }

    @Test
    fun leavingEarlyAccessIsAFullRelease() {
        val releases = listOf(release(400, 6, status = "Early Access"), release(6, 6, status = "Full Release"))
        val left = activity(game(1, releasedDaysAgo = 400, releases = releases))
        assertEquals(TrendingReason.FULL_RELEASE, left.reason)
        // A full release without an earlier early access entry is just another release record.
        val fullReleases = listOf(release(2000, 6, status = "Full Release"), release(6, 6, status = "Full Release"))
        val plain = game(2, releases = fullReleases)
        assertNull(activity(plain).reason)
    }

    @Test
    fun showcaseAgesKeepRecentEventsAndTheirLatestAppearance() {
        val events =
            listOf(
                IgdbEvent(1, startTime = now - 3 * day, endTime = now - 2 * day, games = listOf(10, 11)),
                IgdbEvent(2, startTime = now - day, endTime = now + day, games = listOf(11)),
                IgdbEvent(3, startTime = now - 30 * day, endTime = now - 29 * day, games = listOf(12)),
                IgdbEvent(4, startTime = now + day, games = listOf(13)),
                IgdbEvent(5, startTime = null, games = listOf(14)),
            )
        assertEquals(mapOf(10 to 2 * day, 11 to 0L), TrendingRanking.showcaseAges(events, now))
    }

    @Test
    fun rankingPrefersFreshActivityOverStableAudiencesWithoutFilling() {
        val games =
            listOf(
                game(1),
                game(2, releasedDaysAgo = 5),
                game(3),
                game(4, releasedDaysAgo = 2),
                game(5, releases = listOf(release(2000, 6), release(3, 167))),
                game(6),
            ).associateBy { it.id }
        // 1 is the largest established game; 3 is a smaller established one; 6 is established and showcased.
        val attention = TrendingRanking.attention(everywhere(1 to 1, 2 to 8, 3 to 10, 4 to 60, 5 to 12, 6 to 15))
        val ranked = TrendingRanking.rank(attention, games, mapOf(6 to day), now)
        assertEquals(listOf(2, 6, 5, 4, 1), ranked.map { it.id })
        assertEquals(
            listOf(TrendingReason.NEW_RELEASE, TrendingReason.SHOWCASED, TrendingReason.NEW_PLATFORM),
            ranked.take(3).map { it.reason },
        )
        // The top established game stays only below every fresh game; a smaller one does not fill the feed.
        assertNull(ranked.last().reason)
        assertTrue(ranked.none { it.id == 3 })
        assertTrue(ranked.all { it.score >= TrendingRanking.MIN_SCORE })
        val reversed = games.entries.reversed().associate { it.toPair() }
        assertEquals(ranked, TrendingRanking.rank(attention, reversed, mapOf(6 to day), now))
    }

    @Test
    fun rankingFiltersUnsuitableEntriesAndKeepsVariety() {
        val games =
            listOf(
                game(1, releasedDaysAgo = 1, gameType = 1),
                game(2, releasedDaysAgo = 1, status = 6),
                game(3, releasedDaysAgo = 1, themes = listOf(RecommendationQueries.EROTIC_THEME)),
                game(4, releasedDaysAgo = 1, cover = false),
                game(5, releasedDaysAgo = 1, collections = listOf(9)),
                game(6, releasedDaysAgo = 1, collections = listOf(9)),
                game(7, releasedDaysAgo = 1, collections = listOf(9)),
                game(8, releasedDaysAgo = 1, name = "Game 5"),
                game(9, releasedDaysAgo = 1, gameType = 8),
                game(10, releasedDaysAgo = -10, status = 7),
            ).associateBy { it.id }
        val attention = TrendingRanking.attention(everywhere(*(1..10).map { it to it }.toTypedArray()))
        val ranked = TrendingRanking.rank(attention, games, emptyMap(), now)
        // DLC, cancelled, erotic, coverless, rumored, a third collection entry, and a duplicate title are skipped.
        assertEquals(listOf(5, 6, 9), ranked.map { it.id })
        assertEquals(listOf(5), TrendingRanking.rank(attention, games, emptyMap(), now, limit = 1).map { it.id })
    }

    @Test
    fun hydrationKeepsTheStrongestAttentionWithinItsBound() {
        val ids = (1..300).toList()
        val attention = TrendingRanking.attention(PopularitySignal.entries.associateWith { ids })
        val order = TrendingRanking.hydrationOrder(attention)
        assertEquals(ids.take(TrendingRanking.HYDRATION_COUNT), order)
    }

    @Test
    fun responseContainsOnlyCardFieldsAndVerifiedReasons() {
        val body =
            trendingJson(
                listOf(
                    TrendingGame(1, "A \"quoted\" name", "//c/1.jpg", TrendingReason.NEW_PLATFORM, "PS5", 0.9, 0.9),
                    TrendingGame(2, "B", "//c/2.jpg", null, null, 0.5, 0.9),
                ),
            )
        val items = Json.parseToJsonElement(body).jsonArray.map { it.jsonObject }
        assertEquals(setOf("id", "name", "cover", "reason", "platform"), items[0].keys)
        assertEquals("new_platform", items[0].getValue("reason").jsonPrimitive.content)
        assertEquals("A \"quoted\" name", items[0].getValue("name").jsonPrimitive.content)
        assertEquals(setOf("id", "name", "cover"), items[1].keys)
    }

    /** Serves one multiquery with popularity lists and events, then the requested games in reverse ID order. */
    private inner class StubIgdb(
        var games: List<IgdbGame> = defaultGames(),
        var events: String? = """[{"id":1,"start_time":${now - 2 * day},"end_time":${now - day},"games":[3]}]""",
        var primitives: Map<Int, List<Int>> =
            PopularitySignal.entries.associate { it.typeId to listOf(1, 2, 3, 4) },
    ) : Upstream {
        val discoveryCalls = AtomicInteger()
        val hydrationCalls = AtomicInteger()
        val bodies = mutableListOf<String>()
        var failure: HttpStatusCode? = null
        var gate: CompletableDeferred<Unit>? = null
        private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
        private val json = Json { explicitNulls = false }

        override suspend fun token(
            clientId: String,
            clientSecret: String,
        ) = UpstreamResponse(
            HttpStatusCode.OK,
            jsonHeaders,
            """{"access_token":"t","expires_in":3600}""".encodeToByteArray(),
        )

        override suspend fun igdb(
            path: String,
            query: String,
            clientId: String,
            token: String,
        ): UpstreamResponse {
            gate?.await()
            synchronized(bodies) { bodies += query }
            failure?.let { return UpstreamResponse(it, headersOf(), byteArrayOf()) }
            val body =
                if (path == RecommendationService.MULTIQUERY_PATH) {
                    discoveryCalls.incrementAndGet()
                    val types =
                        mapOf(1 to "Visits", 3 to "Playing", 5 to "24hr Peak Players", 34 to "24hr Hours Watched")
                            .entries
                            .joinToString(",") { """{"id":${it.key},"name":"${it.value}"}""" }
                    val lists =
                        PopularitySignal.entries.joinToString(",") { signal ->
                            val rows =
                                primitives[signal.typeId].orEmpty().withIndex().joinToString(",") { (index, gameId) ->
                                    """{"game_id":$gameId,"value":${100 - index},"calculated_at":$now}"""
                                }
                            """{"name":"${signal.queryName}","result":[$rows]}"""
                        }
                    val eventResult = events?.let { """,{"name":"events","result":$it}""" }.orEmpty()
                    """[{"name":"types","result":[$types]},$lists$eventResult]"""
                } else {
                    hydrationCalls.incrementAndGet()
                    val requested =
                        Regex("""where id = \(([0-9,]+)\)""")
                            .find(query)
                            ?.groupValues
                            ?.get(1)
                            .orEmpty()
                            .split(',')
                            .mapNotNull(String::toIntOrNull)
                            .toSet()
                    val elements =
                        games
                            .filter { it.id in requested }
                            .sortedByDescending { it.id }
                            .map { json.encodeToJsonElement(it) as JsonObject }
                    JsonArray(elements).toString()
                }
            return UpstreamResponse(HttpStatusCode.OK, jsonHeaders, body.encodeToByteArray())
        }

        override suspend fun giveaway(url: String) = error("unused")

        override suspend fun image(url: String) = error("unused")
    }

    /** 1 is established, 2 is new, 3 is showcased, and 4 is a recent port. */
    private fun defaultGames() =
        listOf(
            game(1),
            game(2, releasedDaysAgo = 4),
            game(3),
            game(4, releases = listOf(release(2000, 6), release(2, 167, "PS5"))),
        )

    private fun service(
        stub: StubIgdb,
        clock: Clock,
    ) = TrendingService(IgdbService(stub, RateLimiter { true }, config), clock, log = {})

    private fun ids(result: ResponseCache.LoadResult): List<Int> =
        Json
            .parseToJsonElement(assertIs<ResponseCache.LoadResult.Hit>(result).response.body.decodeToString())
            .jsonArray
            .map {
                it.jsonObject
                    .getValue("id")
                    .jsonPrimitive.int
            }

    @Test
    fun cacheMissUsesTwoRequestsAndHitsUseNone(): Unit =
        runBlocking {
            val stub = StubIgdb()
            val service = service(stub, MutableClock(now * 1000))
            // Hydration returns games in reverse; the feed follows the ranking.
            assertEquals(listOf(2, 3, 4, 1), ids(service.trending()))
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
            val (discovery, hydration) = stub.bodies
            assertContains(discovery, "query events \"events\" {fields start_time,end_time,games;")
            assertContains(discovery, "where popularity_type = 34;sort value desc;limit 100;")
            assertContains(hydration, "release_dates.platform.abbreviation,release_dates.status.name;")
            assertContains(hydration, "where id = (1,2,3,4);limit 4;")
            service.trending()
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
        }

    @Test
    fun concurrentMissesShareOneComputation(): Unit =
        runBlocking {
            val stub = StubIgdb().apply { gate = CompletableDeferred() }
            val service = service(stub, MutableClock(now * 1000))
            val callers = List(10) { async(start = CoroutineStart.UNDISPATCHED) { service.trending() } }
            checkNotNull(stub.gate).complete(Unit)
            assertTrue(callers.awaitAll().all { it is ResponseCache.LoadResult.Hit })
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
        }

    @Test
    fun missingOrInvalidEventsOnlyRemoveShowcaseReasons(): Unit =
        runBlocking {
            val clock = MutableClock(now * 1000)
            for (events in listOf(null, """[{"id":"not a number"}]""")) {
                val feed = ids(service(StubIgdb(events = events), clock).trending())
                assertEquals(listOf(2, 4), feed.take(2), events)
                assertTrue(3 !in feed.take(2))
            }
        }

    @Test
    fun weakOrMissingDataFailsInsteadOfPublishingAnEmptyFeed(): Unit =
        runBlocking {
            val clock = MutableClock(now * 1000)
            val noSignals = StubIgdb(primitives = emptyMap())
            assertEquals(ResponseCache.LoadResult.Uncached(502), service(noSignals, clock).trending())
            assertEquals(1, noSignals.discoveryCalls.get() + noSignals.hydrationCalls.get())
            // Only established games with modest attention: nothing qualifies, so nothing is cached.
            val weak =
                StubIgdb(primitives = PopularitySignal.entries.associate { it.typeId to (101..150).toList() + 1 })
            assertEquals(ResponseCache.LoadResult.Uncached(502), service(weak, clock).trending())
        }

    @Test
    fun failuresServeBoundedStaleResults(): Unit =
        runBlocking {
            val stub = StubIgdb()
            val clock = MutableClock(now * 1000)
            val service = service(stub, clock)
            val fresh = assertIs<ResponseCache.LoadResult.Hit>(service.trending())
            stub.failure = HttpStatusCode.TooManyRequests
            clock.nowMillis += 31 * 60 * 1000L
            val stale = assertIs<ResponseCache.LoadResult.Hit>(service.trending())
            assertTrue(stale.stale)
            assertEquals(fresh.response.body.decodeToString(), stale.response.body.decodeToString())
            clock.nowMillis += 25 * 60 * 60 * 1000L
            assertEquals(ResponseCache.LoadResult.Uncached(429), service.trending())
        }

    @Test
    fun endpointReturnsMinimalPublicFeed() =
        testApplication {
            val publicConfig = config.copy(publicBaseUrl = "https://localhost", publicCacheEnabled = true)
            val stub = StubIgdb()
            val igdb = IgdbService(stub, RateLimiter { true }, publicConfig)
            val clock = MutableClock(now * 1000)
            application {
                api(
                    publicConfig,
                    igdb,
                    GiveawayService(stub),
                    RecommendationService(igdb, log = {}),
                    PopularNowService(igdb, clock, log = {}),
                    TrendingService(igdb, clock, log = {}),
                )
            }
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
            val response = client.get("https://localhost/v1/games/trending")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(HOME_FEED_CACHE_CONTROL, response.headers[HttpHeaders.CacheControl])
            val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray.map { it.jsonObject }
            assertEquals(setOf("id", "name", "cover", "reason"), items.first().keys)
            assertEquals(
                "new_release",
                items
                    .first()
                    .getValue("reason")
                    .jsonPrimitive.content,
            )
            val port = items.first { it.getValue("id").jsonPrimitive.int == 4 }
            assertEquals("PS5", port.getValue("platform").jsonPrimitive.content)
            for (query in listOf("pageSize=20", "limit=5", "id=1")) {
                val invalid = client.get("https://localhost/v1/games/trending?$query")
                assertEquals(HttpStatusCode.BadRequest, invalid.status, query)
            }
            stub.failure = HttpStatusCode.ServiceUnavailable
            clock.nowMillis += 31 * 60 * 1000L
            val stale = client.get("https://localhost/v1/games/trending")
            assertEquals(HttpStatusCode.OK, stale.status)
            assertEquals(PRIVATE_CACHE_CONTROL, stale.headers[HttpHeaders.CacheControl])
            clock.nowMillis += 25 * 60 * 60 * 1000L
            assertEquals(HttpStatusCode.BadGateway, client.get("https://localhost/v1/games/trending").status)
        }

    /**
     * Opt-in: `BRAINDANCE_BENCHMARK=1 ./gradlew :backend:test --tests '*TrendingTest*' --rerun -i`. Measures scoring
     * and selection for a full-size candidate pool with a zero-latency fake; it does not include IGDB latency.
     */
    @Test
    fun syntheticRankingBenchmark(): Unit =
        runBlocking {
            assumeTrue(System.getenv("BRAINDANCE_BENCHMARK") == "1")
            val random = Random(7)
            val signals =
                PopularitySignal.entries.associateWith {
                    (1..1000).shuffled(random).take(TrendingRanking.SIGNAL_DEPTH)
                }
            val games =
                (1..1000).associateWith { id ->
                    val releases =
                        List(random.nextInt(1, 12)) { release(random.nextLong(0, 3000), random.nextInt(1, 8)) }
                    game(id, releasedDaysAgo = random.nextLong(-200, 3000), releases = releases)
                }
            val events = listOf(IgdbEvent(1, now - 2 * day, now - day, (1..1000).shuffled(random).take(40)))
            repeat(200) { rankOnce(signals, games, events) }
            val samples = List(1000) { measureTime { rankOnce(signals, games, events) } }.sorted()
            val trending = service(StubIgdb(), MutableClock(now * 1000))
            trending.trending()
            val cached = List(1000) { measureTime { trending.trending() } }.sorted()
            println(
                "ranking p50 ${samples[500]} p95 ${samples[950]}; cached feed p50 ${cached[500]} p95 ${cached[950]}",
            )
        }

    private fun rankOnce(
        signals: Map<PopularitySignal, List<Int>>,
        games: Map<Int, IgdbGame>,
        events: List<IgdbEvent>,
    ): List<TrendingGame> {
        val attention = TrendingRanking.attention(signals)
        val hydrated = TrendingRanking.hydrationOrder(attention).associateWith { games.getValue(it) }
        return TrendingRanking.rank(attention, hydrated, TrendingRanking.showcaseAges(events, now), now)
    }
}
