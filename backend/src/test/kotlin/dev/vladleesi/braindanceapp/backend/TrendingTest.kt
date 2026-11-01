// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
        hypes: Int? = null,
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
        hypes = hypes,
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
        assertEquals(null to TrendingRanking.ESTABLISHED, upcoming.reason to upcoming.relevance)

        assertEquals(TrendingReason.SHOWCASED, activity(game(2), showcaseDaysAgo = 2).reason)
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
    fun recentActivityBoostsRankingWithoutGatingEstablishedInterest() {
        val games =
            listOf(
                game(1),
                game(2, releasedDaysAgo = 5),
                game(3),
                game(4, releasedDaysAgo = 2),
                game(5, releases = listOf(release(2000, 6), release(3, 167))),
                game(6),
                game(7),
                game(8),
            ).associateBy { it.id }
        // 1 leads every signal without recent activity; 6 is showcased; 8 is established and far down every list.
        val attention =
            TrendingRanking.attention(
                everywhere(1 to 1, 2 to 8, 3 to 10, 4 to 60, 5 to 12, 6 to 15, 7 to 70, 8 to 160),
            )
        val ranked = TrendingRanking.rank(attention, games, mapOf(6 to day), now)
        // Fresh games outrank the overall leader, which still appears ahead of a weaker established game: activity
        // and habitual audiences weigh rankings, while only weak interest removes a game.
        assertEquals(listOf(2, 6, 5, 4, 1, 3, 7), ranked.map { it.id })
        assertEquals(
            listOf(
                TrendingReason.NEW_RELEASE,
                TrendingReason.SHOWCASED,
                TrendingReason.NEW_PLATFORM,
                TrendingReason.NEW_RELEASE,
            ),
            ranked.take(4).map { it.reason },
        )
        assertEquals(listOf(1, 3, 7), ranked.filter { it.established }.map { it.id })
        assertTrue(ranked.filter { it.established }.all { it.reason == null })
        assertTrue(ranked.none { it.id == 8 }, "established games need meaningful attention")
        // The boost is bounded: the strongest activity at most multiplies interest by 1 + ACTIVITY_BOOST.
        val launch = ranked.first()
        assertEquals(launch.attention * (1 + TrendingRanking.ACTIVITY_BOOST), launch.score, 1e-9)
        val reversed = games.entries.reversed().associate { it.toPair() }
        assertEquals(ranked, TrendingRanking.rank(attention, reversed, mapOf(6 to day), now))
    }

    @Test
    fun richCandidatePoolFillsTheFeedWithFreshGames() {
        // 10 established leaders, 25 fresh games below them, and 5 fresh games only one signal lists, deep in it.
        val established = (1..10).map { game(it) }
        val launches = (11..35).map { game(it, releasedDaysAgo = it % 20L) }
        val singleSignal = (36..40).map { game(it, releasedDaysAgo = 3) }
        val games = (established + launches + singleSignal).associateBy { it.id }
        val ranks = (1..10).map { it to it } + (11..35).map { it to it + 10 }
        val signals =
            everywhere(*ranks.toTypedArray()) +
                (PopularitySignal.TWITCH_HOURS_WATCHED to (1000..1149).toList() + (36..40))
        val page = TrendingRanking.DEFAULT_TUNING.copy(resultCount = TrendingRanking.RESULT_COUNT)
        val selection = TrendingRanking.select(TrendingRanking.attention(signals), games, emptyMap(), now, page)
        assertEquals(TrendingRanking.RESULT_COUNT, selection.games.size)
        // The established leaders qualify, but launches with comparable attention rank above them.
        assertTrue(selection.games.all { !it.established && it.reason == TrendingReason.NEW_RELEASE })
        assertEquals(40, selection.funnel.displayable)
        assertEquals(30 to 10, selection.funnel.fresh to selection.funnel.established)
        // Ordering is deterministic and strongest first.
        val scores = selection.games.map { it.score }
        assertEquals(scores.sortedDescending(), scores)
    }

    @Test
    fun unreleasedGamesAreLeftForMostAnticipated() {
        val games =
            listOf(
                game(1, releasedDaysAgo = -30, hypes = 900),
                game(2, releasedDaysAgo = null, hypes = 400),
                game(3, releasedDaysAgo = null, status = 4),
                game(4, releasedDaysAgo = 3),
            ).associateBy { it.id }
        val attention = TrendingRanking.attention(everywhere(1 to 1, 2 to 2, 3 to 3, 4 to 40))
        // 1 is dated upcoming and showcased, 2 is undated, and 3 is in early access without a date.
        val selection = TrendingRanking.select(attention, games, mapOf(1 to day), now)
        assertEquals(listOf(4, 3), selection.games.map { it.id })
        assertEquals(2, selection.funnel.unreleased)
    }

    @Test
    fun eachPageAdmitsItsOwnEstablishedShare() {
        // 30 broadly popular established games, and 30 launches only one signal lists, deep in it.
        val established = (1..30).map { game(it) }
        val launches = (31..60).map { game(it, releasedDaysAgo = 3) }
        val games = (established + launches).associateBy { it.id }
        val signals =
            everywhere(*(1..30).map { it to it }.toTypedArray()) +
                (PopularitySignal.STEAM_PEAK_PLAYERS to (1000..1099).toList() + (31..60))
        val feed = TrendingRanking.rank(TrendingRanking.attention(signals), games, emptyMap(), now)
        val pages = feed.chunked(TrendingRanking.RESULT_COUNT)
        // Every page holds at most MAX_ESTABLISHED leaders; held-back leaders keep their order on later pages.
        assertEquals(listOf(6, 6, 6), pages.map { page -> page.count { it.established } })
        assertEquals((1..18).toList(), feed.filter { it.established }.map { it.id })
        assertEquals(60 - 30 + 18, feed.size)
    }

    @Test
    fun establishedGamesAreCappedAndNeedMeaningfulAttention() {
        val games = (1..10).map { game(it) }.associateBy { it.id }
        val ranks = (1..9).map { it to it * 5 } + (10 to 190)
        val attention = TrendingRanking.attention(everywhere(*ranks.toTypedArray()))
        val selection = TrendingRanking.select(attention, games, emptyMap(), now)
        // Only MAX_ESTABLISHED appear, and nothing fills the remaining slots; rank 190 everywhere is too weak.
        assertEquals((1..TrendingRanking.MAX_ESTABLISHED).toList(), selection.games.map { it.id })
        assertTrue(selection.games.all { it.established && it.reason == null })
        assertEquals(3 to 1, selection.funnel.establishedCapped to selection.funnel.weak)
    }

    @Test
    fun freshGamesNeedListingsOrHypes() {
        val games =
            listOf(
                game(1, releasedDaysAgo = 3, hypes = 40),
                game(2, releasedDaysAgo = 3, hypes = TrendingRanking.MIN_HYPES - 1),
                game(3, releasedDaysAgo = 3),
                game(4, hypes = 500),
                game(5, releasedDaysAgo = 3, hypes = 0),
                game(6, hypes = 25),
            ).associateBy { it.id }
        // Only game 5 is listed, deep in one signal of two.
        val signals =
            mapOf(
                PopularitySignal.IGDB_VISITS to (1000..1189).toList() + 5,
                PopularitySignal.IGDB_PLAYING to (2000..2009).toList(),
            )
        val selection = TrendingRanking.select(TrendingRanking.attention(signals), games, mapOf(6 to day), now)
        // An unlisted launch with hypes, a deep single-signal listing, and a hyped showcased game qualify. An obscure
        // launch and an established game with only old hypes do not.
        assertEquals(setOf(1, 5, 6), selection.games.map { it.id }.toSet())
        assertEquals(TrendingReason.SHOWCASED, selection.games.single { it.id == 6 }.reason)
        assertEquals(3, selection.funnel.weak)
    }

    @Test
    fun incompleteMetadataIsRankedWithWhatIgdbProvides() {
        val games =
            listOf(
                // No type, release dates, or hypes: the first release date alone shows a launch.
                IgdbGame(1, "Untyped", IgdbCover("//c/1.jpg"), firstReleaseDate = now - 2 * day),
                // Early access without any dates is released but has no dated activity.
                IgdbGame(2, "Undated", IgdbCover("//c/2.jpg"), gameStatus = 4),
                IgdbGame(3, "No cover", null, firstReleaseDate = now - 2 * day),
                IgdbGame(4, " ", IgdbCover("//c/4.jpg"), firstReleaseDate = now - 2 * day),
            ).associateBy { it.id }
        val attention = TrendingRanking.attention(everywhere(1 to 30, 2 to 2, 3 to 1, 4 to 3))
        val selection = TrendingRanking.select(attention, games, emptyMap(), now)
        assertEquals(listOf(1, 2), selection.games.map { it.id })
        assertEquals(TrendingReason.NEW_RELEASE to false, selection.games[0].reason to selection.games[0].established)
        assertEquals(null to true, selection.games[1].reason to selection.games[1].established)
        assertEquals(2, selection.funnel.displayable)
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
        val single = TrendingRanking.DEFAULT_TUNING.copy(resultCount = 1)
        assertEquals(listOf(5), TrendingRanking.rank(attention, games, emptyMap(), now, single).map { it.id })
    }

    @Test
    fun hydrationPlanBoundsPoolsWithoutOverlap() {
        val ids = (1..300).toList()
        val attention = TrendingRanking.attention(PopularitySignal.entries.associateWith { ids })
        // 5 was fetched as a recent release; 150 and 2000 were featured at events, 2000 most recently.
        val plan = TrendingRanking.hydrationPlan(attention, setOf(5), mapOf(150 to day, 2000 to 0L, 3 to day))
        assertEquals((1..101).filter { it != 5 }, plan.attentionIds)
        assertEquals(TrendingRanking.HYDRATION_COUNT, plan.attentionIds.size)
        assertEquals(listOf(2000, 150), plan.showcaseIds)
        assertEquals((102..300).filter { it != 150 }, plan.tailIds)
        val showcases = (1..1000).associate { 5000 + it to it.toLong() }
        val bounded = TrendingRanking.hydrationPlan(emptyMap(), emptySet(), showcases)
        assertEquals((5001..5000 + TrendingRanking.MAX_SHOWCASE_IDS).toList(), bounded.showcaseIds)
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
        var recent: List<IgdbGame> = emptyList(),
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
            assertEquals(RecommendationService.MULTIQUERY_PATH, path)
            val body =
                if (query.contains("query popularity_primitives")) {
                    discoveryCalls.incrementAndGet()
                    val types =
                        mapOf(
                            1 to "Visits",
                            2 to "Want to Play",
                            3 to "Playing",
                            5 to "24hr Peak Players",
                            34 to "24hr Hours Watched",
                        ).entries
                            .joinToString(",") { """{"id":${it.key},"name":"${it.value}"}""" }
                    val lists =
                        PopularitySignal.entries.joinToString(",") { signal ->
                            val rows =
                                primitives[signal.typeId].orEmpty().withIndex().joinToString(",") { (index, gameId) ->
                                    """{"game_id":$gameId,"value":${1000 - index},"calculated_at":$now}"""
                                }
                            """{"name":"${signal.queryName}","result":[$rows]}"""
                        }
                    val eventResult = events?.let { """,{"name":"events","result":$it}""" }.orEmpty()
                    val recentResult = """,{"name":"recent","result":${encode(recent)}}"""
                    """[{"name":"types","result":[$types]},$lists$eventResult$recentResult]"""
                } else {
                    hydrationCalls.incrementAndGet()
                    // Each named games query returns the requested games; the tail query keeps only recent releases,
                    // as IGDB's date filter would. Games come back in reverse ID order: ranking must not rely on it.
                    Regex("""query games "(\w+)" \{[^}]*?where id = \(([0-9,]+)\)""")
                        .findAll(query)
                        .joinToString(",", "[", "]") { match ->
                            val (name, idList) = match.destructured
                            val requested = idList.split(',').map(String::toInt).toSet()
                            val found =
                                games.filter { game ->
                                    game.id in requested &&
                                        (name != "tail" || (game.firstReleaseDate ?: 0) in now - 60 * day..now)
                                }
                            """{"name":"$name","result":${encode(found)}}"""
                        }
                }
            return UpstreamResponse(HttpStatusCode.OK, jsonHeaders, body.encodeToByteArray())
        }

        private fun encode(found: List<IgdbGame>) =
            JsonArray(found.sortedByDescending { it.id }.map { json.encodeToJsonElement(it) as JsonObject }).toString()

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

    private fun pastFeedWindow(clock: MutableClock) {
        clock.nowMillis += TrendingService.feedTimeLeft(clock.nowMillis).inWholeMilliseconds + 60_000
    }

    /** Pages expire at the end of the current feed window, and browsers keep them for at most 20 minutes. */
    private fun assertFeedWindowCaching(headers: Headers) {
        val cacheControl = checkNotNull(headers[HttpHeaders.CacheControl])
        val match = checkNotNull(Regex("""public, max-age=(\d+), s-maxage=(\d+)""").matchEntire(cacheControl))
        val (browser, edge) = match.destructured.toList().map(String::toInt)
        assertTrue(edge in 1..21_600 && browser == minOf(1200, edge), cacheControl)
        assertEquals("public, max-age=$edge, stale-if-error=86400", headers[CLOUDFLARE_CACHE_CONTROL_HEADER])
    }

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
            assertContains(discovery, "where popularity_type = 34;sort value desc;limit 200;")
            assertContains(discovery, "query games \"recent\" {fields name,cover.url,")
            assertContains(discovery, "& hypes >= 20 & version_parent = null & cover != null")
            assertContains(hydration, "release_dates.platform.abbreviation,release_dates.status.name;")
            assertContains(hydration, "query games \"attention\" {")
            assertContains(hydration, "where id = (1,2,3,4) & ${FeedEligibility.releasedFilter(now)};limit 4;};")
            // Pools without IDs are omitted rather than sent empty.
            assertTrue("\"tail\"" !in hydration && "\"showcased\"" !in hydration)
            service.trending()
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
        }

    @Test
    fun releaseAndEventCandidatesOutsideTopListsStillUseTwoRequests(): Unit =
        runBlocking {
            val listed = (1..130).toList()
            val games =
                listOf(
                    game(1),
                    // A tail listing released recently, and one released long ago, which IGDB filters out.
                    game(125, releasedDaysAgo = 6),
                    game(126),
                    // Event games no popularity signal lists: one with interest, one obscure.
                    game(501, hypes = 60),
                    game(502, hypes = 0),
                )
            val stub =
                StubIgdb(
                    games = games,
                    events = """[{"id":1,"start_time":${now - 2 * day},"end_time":${now - day},"games":[501,502,1]}]""",
                    primitives = PopularitySignal.entries.associate { it.typeId to listed },
                    // A recent release absent from every popularity list.
                    recent = listOf(game(900, releasedDaysAgo = 4, hypes = 80)),
                )
            val feed = ids(service(stub, MutableClock(now * 1000)).trending())
            assertEquals(setOf(1, 125, 501, 900), feed.toSet())
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
            val hydration = stub.bodies.last()
            assertContains(hydration, "query games \"tail\" {")
            assertContains(hydration, "query games \"showcased\" {")
            assertContains(hydration, "where id = (501,502) & version_parent = null")
            assertTrue("900" !in hydration, "recent releases arrive with discovery and are not hydrated again")
        }

    @Test
    fun feedsRefreshWithCurrentDataWhenTheirWindowEnds(): Unit =
        runBlocking {
            val stub = StubIgdb()
            val clock = MutableClock(now * 1000)
            val service = service(stub, clock)
            assertEquals(listOf(2, 3, 4, 1), ids(service.trending()))
            stub.recent = listOf(game(900, releasedDaysAgo = 1, hypes = 200))
            clock.nowMillis += TrendingService.feedTimeLeft(clock.nowMillis).inWholeMilliseconds - 60_000
            assertEquals(listOf(2, 3, 4, 1), ids(service.trending()), "one feed serves its whole window")
            clock.nowMillis += 60_000
            assertContains(ids(service.trending()), 900)
            assertEquals(2 to 2, stub.discoveryCalls.get() to stub.hydrationCalls.get())
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
    fun slowRefreshOutlivesTheResponseBudgetAndFillsTheCache(): Unit =
        runBlocking {
            val stub = StubIgdb().apply { gate = CompletableDeferred() }
            val igdb = IgdbService(stub, RateLimiter { true }, config)
            val service = TrendingService(igdb, MutableClock(now * 1000), log = {}, responseBudget = 50.milliseconds)
            // A cold refresh outlasts the budget: callers time out, and a retry joins the running refresh.
            repeat(2) { assertEquals(ResponseCache.LoadResult.Uncached(504), service.trending()) }
            checkNotNull(stub.gate).complete(Unit)
            val served =
                withTimeout(5.seconds) {
                    var result = service.trending()
                    while (result !is ResponseCache.LoadResult.Hit) result = service.trending()
                    result
                }
            assertEquals(listOf(2, 3, 4, 1), ids(served))
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
            service.close()
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
            // Only an established game far down every list: nothing qualifies, so nothing is cached.
            val weak =
                StubIgdb(primitives = PopularitySignal.entries.associate { it.typeId to (101..290).toList() + 1 })
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
            pastFeedWindow(clock)
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
                    TrendingService(igdb, clock, log = {}),
                )
            }
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
            val response = client.get("https://localhost/v1/games/trending")
            assertEquals(HttpStatusCode.OK, response.status)
            assertFeedWindowCaching(response.headers)
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
            // Pages are cut from the same cached feed: no further IGDB requests.
            val calls = stub.discoveryCalls.get() to stub.hydrationCalls.get()

            suspend fun ids(query: String) =
                Json
                    .parseToJsonElement(client.get("https://localhost/v1/games/trending?$query").bodyAsText())
                    .jsonArray
                    .map {
                        it.jsonObject
                            .getValue("id")
                            .jsonPrimitive.int
                    }
            assertEquals(listOf(2, 3, 4, 1), ids("page=1&pageSize=20"))
            assertEquals(listOf(4, 1), ids("page=2&pageSize=2"))
            assertEquals(emptyList(), ids("page=5&pageSize=20"))
            assertEquals(calls, stub.discoveryCalls.get() to stub.hydrationCalls.get())
            val paged = client.get("https://localhost/v1/games/trending?page=2&pageSize=2")
            assertFeedWindowCaching(paged.headers)
            val invalidQueries =
                listOf(
                    "pageSize=0",
                    "pageSize=51",
                    "page=0",
                    "page=01",
                    "page=+2",
                    "page=6",
                    "page=1&page=2",
                    "limit=5",
                    "id=1",
                )
            for (query in invalidQueries) {
                val invalid = client.get("https://localhost/v1/games/trending?$query")
                assertEquals(HttpStatusCode.BadRequest, invalid.status, query)
            }
            // The legacy Popular Right Now endpoint is gone.
            assertEquals(HttpStatusCode.NotFound, client.get("https://localhost/v1/games/popular-now").status)
            stub.failure = HttpStatusCode.ServiceUnavailable
            pastFeedWindow(clock)
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
        val showcases = TrendingRanking.showcaseAges(events, now)
        val plan = TrendingRanking.hydrationPlan(attention, emptySet(), showcases)
        // IGDB returns at most the pool limits, as a full-size live hydration would.
        val hydratedIds =
            plan.attentionIds + plan.tailIds.take(TrendingRanking.TAIL_LIMIT) +
                plan.showcaseIds.take(TrendingRanking.SHOWCASE_LIMIT)
        val hydrated = hydratedIds.associateWith { games.getValue(it) }
        return TrendingRanking.rank(attention, hydrated, showcases, now)
    }
}
