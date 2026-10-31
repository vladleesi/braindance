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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PopularNowTest {
    private val now = 1_800_000_000L
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)

    private class MutableClock(
        var nowMillis: Long,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(nowMillis)
    }

    private fun game(
        id: Int,
        released: Long? = 1_500_000_000,
        cover: Boolean = true,
        gameType: Int? = 0,
        versionParent: Int? = null,
        status: Int? = null,
        themes: List<Int> = emptyList(),
        ratingCategories: List<Int> = emptyList(),
        collections: List<Int> = emptyList(),
        name: String = "Game $id",
    ) = IgdbGame(
        id = id,
        name = name,
        cover = if (cover) IgdbCover("//images.igdb.com/igdb/image/upload/t_thumb/co$id.jpg") else null,
        gameType = gameType,
        versionParent = versionParent,
        gameStatus = status,
        firstReleaseDate = released,
        themes = themes,
        collections = collections,
        ageRatings = ratingCategories.map { IgdbAgeRating(it) },
    )

    @Test
    fun fusionFavorsAgreementWhileKeepingSingleSourceLeaders() {
        val fused =
            PopularNowRanking.fuse(
                mapOf(
                    PopularitySignal.IGDB_PLAYING to listOf(10, 20, 30) + (100..140),
                    PopularitySignal.STEAM_PEAK_PLAYERS to listOf(50, 20, 10) + (200..240),
                    PopularitySignal.TWITCH_HOURS_WATCHED to listOf(60, 10, 20, 20) + (300..340),
                    PopularitySignal.IGDB_VISITS to listOf(10, 20) + (400..440),
                ),
            )
        // Games listed by every source lead; duplicate entries within one source count once.
        assertEquals(listOf(10, 20), fused.take(2))
        // A leader on a single credible source still outranks the long tail of every list.
        assertTrue(fused.indexOf(50) < 10 && fused.indexOf(60) < 10, "$fused")
        // Twitch alone does not outrank a game ranked well by actual play.
        assertTrue(fused.indexOf(30) < fused.indexOf(300))
    }

    @Test
    fun fusionIsDeterministicAndToleratesMissingSignals() {
        val signals =
            mapOf(
                PopularitySignal.TWITCH_HOURS_WATCHED to listOf(3, 1, 2),
                PopularitySignal.STEAM_PEAK_PLAYERS to listOf(2, 3, 1),
            )
        val expected = PopularNowRanking.fuse(signals)
        assertEquals(expected, PopularNowRanking.fuse(signals.entries.reversed().associate { it.key to it.value }))
        assertEquals(emptyList(), PopularNowRanking.fuse(emptyMap()))
        val tied =
            PopularNowRanking.rankedGameIds(listOf(PopularityPrimitive(9, 5.0), PopularityPrimitive(4, 5.0)))
        assertEquals(listOf(4, 9), tied)
    }

    @Test
    fun selectionRestoresFusedOrderAndFiltersIneligibleGames() {
        val games =
            listOf(
                game(1),
                game(2, released = now + 1000),
                game(3, released = null),
                game(4, released = null, status = 4),
                game(5, gameType = 1),
                game(6, versionParent = 1),
                game(7, cover = false),
                game(8, themes = listOf(42)),
                game(9, ratingCategories = listOf(7)),
                game(10, status = 6),
                game(11, name = "Game 1"),
                game(12, collections = listOf(77)),
                game(13, collections = listOf(77)),
                game(14, collections = listOf(77)),
                game(15, gameType = 8, ratingCategories = listOf(1)),
            ).shuffled(kotlin.random.Random(1)).associateBy { it.id }
        val order = listOf(15, 14, 13, 12, 99) + (1..11)
        val selected = PopularNowRanking.select(order, games, now)
        // Remakes, early access, and up to two games per collection stay; 99 was deleted upstream.
        assertEquals(listOf(15, 14, 13, 1, 4), selected.map { it.id })
        assertEquals(listOf(15, 14), PopularNowRanking.select(order, games, now, limit = 2).map { it.id })
    }

    /** Serves canned IGDB data: one multiquery for discovery and one games query for hydration. */
    private class StubIgdb(
        var typeNames: Map<Int, String> =
            mapOf(1 to "Visits", 3 to "Playing", 5 to "24hr Peak Players", 34 to "24hr Hours Watched"),
        var calculatedAt: Long = 1_800_000_000L,
        var primitives: Map<Int, List<Int>> =
            mapOf(3 to listOf(1, 2, 3), 5 to listOf(2, 1), 34 to listOf(4, 2), 1 to listOf(1, 3)),
    ) : Upstream {
        val discoveryCalls = AtomicInteger()
        val hydrationCalls = AtomicInteger()
        val hydrationBodies = mutableListOf<String>()
        var failure: HttpStatusCode? = null
        var gate: CompletableDeferred<Unit>? = null
        private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

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
            failure?.let { return UpstreamResponse(it, headersOf(), byteArrayOf()) }
            val body =
                if (path == RecommendationService.MULTIQUERY_PATH) {
                    discoveryCalls.incrementAndGet()
                    val types = typeNames.entries.joinToString(",") { """{"id":${it.key},"name":"${it.value}"}""" }
                    val lists =
                        PopularitySignal.entries.joinToString(",") { signal ->
                            val rows =
                                primitives[signal.typeId].orEmpty().withIndex().joinToString(",") { (index, gameId) ->
                                    """{"game_id":$gameId,"value":${100 - index},"calculated_at":$calculatedAt}"""
                                }
                            """{"name":"${signal.queryName}","result":[$rows]}"""
                        }
                    """[{"name":"types","result":[$types]},$lists]"""
                } else {
                    hydrationCalls.incrementAndGet()
                    synchronized(hydrationBodies) { hydrationBodies += query }
                    // IGDB does not keep the requested ID order; return games in reverse.
                    (4 downTo 1).joinToString(",", "[", "]") { id ->
                        val cover = """{"id":$id,"url":"//images.igdb.com/t_thumb/co$id.jpg"}"""
                        """{"id":$id,"name":"Game $id","cover":$cover,"first_release_date":1500000000,"game_type":0}"""
                    }
                }
            return UpstreamResponse(HttpStatusCode.OK, jsonHeaders, body.encodeToByteArray())
        }

        override suspend fun giveaway(url: String) = error("unused")

        override suspend fun image(url: String) = error("unused")
    }

    private fun service(
        stub: StubIgdb,
        clock: Clock,
    ) = PopularNowService(IgdbService(stub, RateLimiter { true }, config), clock, log = {})

    @Test
    fun cacheMissUsesTwoRequestsAndHitsUseNone(): Unit =
        runBlocking {
            val stub = StubIgdb()
            val clock = MutableClock(now * 1000)
            val service = service(stub, clock)
            val first = assertIs<ResponseCache.LoadResult.Hit>(service.popularNow())
            val ids =
                Json
                    .parseToJsonElement(first.response.body.decodeToString())
                    .jsonArray
                    .map {
                        it.jsonObject
                            .getValue("id")
                            .jsonPrimitive.int
                    }
            // Fused order from all four signals, restored after the reversed hydration response.
            assertEquals(listOf(2, 1, 3, 4), ids)
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
            assertContains(stub.hydrationBodies.single(), "where id = (2,1,3,4);limit 4;")
            assertFalse(stub.hydrationBodies.single().contains("platforms"))
            service.popularNow()
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
        }

    @Test
    fun concurrentMissesShareOneComputation(): Unit =
        runBlocking {
            val stub = StubIgdb().apply { gate = CompletableDeferred() }
            val service = service(stub, MutableClock(now * 1000))
            val callers = List(10) { async(start = CoroutineStart.UNDISPATCHED) { service.popularNow() } }
            checkNotNull(stub.gate).complete(Unit)
            assertTrue(callers.awaitAll().all { it is ResponseCache.LoadResult.Hit })
            assertEquals(1 to 1, stub.discoveryCalls.get() to stub.hydrationCalls.get())
        }

    @Test
    fun signalsWithChangedMeaningOrOldDataAreDropped(): Unit =
        runBlocking {
            // Each game leads exactly one signal. Type 3 is no longer "Playing", so its leader is not a candidate.
            val primitives = mapOf(3 to listOf(3), 5 to listOf(1), 34 to listOf(2), 1 to listOf(4))
            val clock = MutableClock(now * 1000)

            suspend fun feed(typeNames: Map<Int, String>): List<Int> {
                val result = service(StubIgdb(typeNames = typeNames, primitives = primitives), clock).popularNow()
                val body = assertIs<ResponseCache.LoadResult.Hit>(result).response.body.decodeToString()
                return Json.parseToJsonElement(body).jsonArray.map {
                    it.jsonObject
                        .getValue("id")
                        .jsonPrimitive.int
                }
            }
            assertEquals(listOf(3, 1, 2, 4), feed(StubIgdb().typeNames))
            assertEquals(listOf(1, 2, 4), feed(mapOf(1 to "Visits", 3 to "Want to Play", 5 to "Peak", 34 to "Watched")))

            val empty = StubIgdb(primitives = emptyMap())
            assertEquals(ResponseCache.LoadResult.Uncached(502), service(empty, clock).popularNow())
            val outdated = StubIgdb(calculatedAt = now - 8 * 24 * 60 * 60)
            assertEquals(ResponseCache.LoadResult.Uncached(502), service(outdated, clock).popularNow())
        }

    @Test
    fun failuresServeBoundedStaleResults(): Unit =
        runBlocking {
            val stub = StubIgdb()
            val clock = MutableClock(now * 1000)
            val service = service(stub, clock)
            val fresh = assertIs<ResponseCache.LoadResult.Hit>(service.popularNow())
            stub.failure = HttpStatusCode.TooManyRequests
            clock.nowMillis += 31 * 60 * 1000L
            val stale = assertIs<ResponseCache.LoadResult.Hit>(service.popularNow())
            assertTrue(stale.stale)
            assertEquals(fresh.response.body.decodeToString(), stale.response.body.decodeToString())
            clock.nowMillis += 25 * 60 * 60 * 1000L
            assertEquals(ResponseCache.LoadResult.Uncached(429), service.popularNow())
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
                )
            }
            val client = createClient { defaultRequest { header(HttpHeaders.Host, "localhost") } }
            val response = client.get("https://localhost/v1/games/popular-now")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(POPULAR_NOW_CACHE_CONTROL, response.headers[HttpHeaders.CacheControl])
            val first =
                Json
                    .parseToJsonElement(response.bodyAsText())
                    .jsonArray
                    .first()
                    .jsonObject
            assertEquals(setOf("id", "name", "cover"), first.keys)
            assertEquals(setOf("url"), first.getValue("cover").jsonObject.keys)
            for (query in listOf("pageSize=20", "type=34", "limit=5")) {
                val invalid = client.get("https://localhost/v1/games/popular-now?$query")
                assertEquals(HttpStatusCode.BadRequest, invalid.status, query)
            }
            stub.failure = HttpStatusCode.TooManyRequests
            clock.nowMillis += 31 * 60 * 1000L
            val stale = client.get("https://localhost/v1/games/popular-now")
            assertEquals(HttpStatusCode.OK, stale.status)
            assertEquals(PRIVATE_CACHE_CONTROL, stale.headers[HttpHeaders.CacheControl])
        }
}
