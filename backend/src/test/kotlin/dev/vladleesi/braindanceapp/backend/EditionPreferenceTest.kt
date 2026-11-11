// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixtures mirror the live IGDB records checked on 2026-11-11: Darksiders II (1269) has only a Wii U eShop storefront
 * link and lists Darksiders II: Deathinitive Edition (20048), a remaster whose `parent_game` is 1269, in `remasters`.
 */
class EditionPreferenceTest {
    private val now = 1_800_000_000L
    private val json = Json { explicitNulls = false }

    private fun site(
        type: Int,
        url: String,
    ) = IgdbWebsite(type, url)

    private val wiiU = site(24, "https://www.nintendo.com/games/detail/darksiders-ii-wii-u/")
    private val steam = site(13, "https://store.steampowered.com/app/388410")
    private val gog = site(17, "https://www.gog.com/game/darksiders_ii_deathinitive_edition")
    private val switch = site(24, "https://www.nintendo.com/games/detail/darksiders-ii-deathinitive-edition-switch/")
    private val wikipedia = site(3, "https://en.wikipedia.org/wiki/Darksiders_II")

    private val darksidersII =
        IgdbGame(
            id = 1269,
            name = "Darksiders II",
            gameType = 0,
            collections = listOf(280),
            franchises = listOf(885),
            firstReleaseDate = 1_344_902_400,
            totalRatingCount = 315,
            remasters = listOf(20048),
            websites = listOf(wikipedia, wiiU, site(1, "https://www.thqnordic.com/games/darksiders-2")),
        )
    private val deathinitive =
        IgdbGame(
            id = 20048,
            name = "Darksiders II: Deathinitive Edition",
            gameType = 9,
            parentGame = 1269,
            collections = listOf(280),
            franchises = listOf(885),
            firstReleaseDate = 1_445_904_000,
            totalRatingCount = 102,
            websites = listOf(wikipedia, steam, gog, switch),
        )

    private fun original(
        id: Int,
        websites: List<IgdbWebsite> = listOf(wiiU),
        status: Int? = null,
        remasters: List<Int> = emptyList(),
        expanded: List<Int> = emptyList(),
    ) = IgdbGame(
        id = id,
        name = "Original $id",
        gameType = 0,
        gameStatus = status,
        firstReleaseDate = now - 4000L * 86_400,
        remasters = remasters,
        expandedGames = expanded,
        websites = websites,
    )

    private fun successor(
        id: Int,
        parent: Int,
        gameType: Int? = 9,
        websites: List<IgdbWebsite> = listOf(steam),
        status: Int? = null,
        releasedDaysAgo: Long = 1000,
        ratings: Int? = 10,
        versionParent: Int? = null,
        name: String = "Successor $id",
    ) = IgdbGame(
        id = id,
        name = name,
        gameType = gameType,
        gameStatus = status,
        parentGame = parent,
        versionParent = versionParent,
        firstReleaseDate = now - releasedDaysAgo * 86_400,
        totalRatingCount = ratings,
        websites = websites,
    )

    /** The named multiquery results IGDB returns for [EditionPreference.query]. */
    private fun results(
        originals: List<IgdbGame>,
        successors: List<IgdbGame>,
    ): Map<String, JsonArray> =
        mapOf(
            "originals" to JsonArray(originals.map(json::encodeToJsonElement)),
            "successors" to JsonArray(successors.map(json::encodeToJsonElement)),
        )

    private fun resolve(
        originals: List<IgdbGame>,
        successors: List<IgdbGame>,
        accept: (IgdbGame) -> Boolean = { true },
    ) = EditionPreference.replacements(originals, results(originals, successors), now, accept)

    @Test
    fun darksidersIIPrefersTheDeathinitiveEdition() {
        assertEquals(StoreAvailability.WITHDRAWN, EditionPreference.availability(darksidersII))
        assertEquals(StoreAvailability.LISTED, EditionPreference.availability(deathinitive))
        val query = checkNotNull(EditionPreference.query(listOf(darksidersII), "cover.url"))
        assertContains(query, "query games \"originals\" {fields game_status,websites.type,websites.url;")
        assertContains(query, "where id = (1269);limit 1;};")
        assertContains(query, "where id = (20048) & version_parent = null & game_type = (9,10);limit 1;};")
        assertEquals(mapOf(1269 to deathinitive), resolve(listOf(darksidersII), listOf(deathinitive)))
    }

    @Test
    fun explicitDelistingIsEnoughWithoutStorefrontLinks() {
        // Metro: Last Light is marked delisted; its remaster Redux is sold.
        val delisted = original(539, websites = emptyList(), status = 8, remasters = listOf(50199))
        val redux = successor(50199, parent = 539)
        assertEquals(mapOf(539 to redux), resolve(listOf(delisted), listOf(redux)))
    }

    @Test
    fun oldGamesWithoutStorefrontsOrSuccessorsAreKept() {
        // No storefront link says nothing about availability.
        val unlisted = original(1, websites = listOf(wikipedia), remasters = listOf(2))
        assertEquals(StoreAvailability.UNKNOWN, EditionPreference.availability(unlisted))
        assertEquals(emptyMap(), resolve(listOf(unlisted), listOf(successor(2, parent = 1))))
        // Old age and a closed storefront alone send no lookup when nothing replaces the game.
        assertNull(EditionPreference.query(listOf(original(3), original(4, websites = emptyList())), "cover.url"))
    }

    @Test
    fun availableOriginalStaysEligibleBesideItsRemaster() {
        // Darksiders (1270) is still sold on Steam and Xbox next to Darksiders: Warmastered Edition (25081).
        val darksiders =
            original(
                1270,
                websites =
                    listOf(
                        site(13, "https://store.steampowered.com/app/50620"),
                        site(22, "https://www.xbox.com/en-us/games/store/darksiders/BTXC0RGTT3XN"),
                    ),
                remasters = listOf(25081),
            )
        assertEquals(StoreAvailability.LISTED, EditionPreference.availability(darksiders))
        assertEquals(emptyMap(), resolve(listOf(darksiders), listOf(successor(25081, parent = 1270))))
        // One current link among closed ones is enough to keep the original.
        val mixed = original(5, websites = listOf(wiiU, steam), remasters = listOf(6))
        assertEquals(emptyMap(), resolve(listOf(mixed), listOf(successor(6, parent = 5))))
    }

    @Test
    fun successorWithoutVerifiedAvailabilityIsNotForced() {
        val unverified =
            listOf(
                // Final Doom's remaster has no storefront links in IGDB.
                successor(11, parent = 10, websites = emptyList()),
                successor(12, parent = 10, websites = listOf(site(24, "https://www.nintendo.com/games/detail/x-3ds/"))),
                successor(13, parent = 10, status = 8),
                successor(14, parent = 10, status = 6),
                successor(15, parent = 10, releasedDaysAgo = -30),
            )
        val withdrawn = original(10, status = 8, remasters = unverified.map { it.id })
        assertEquals(emptyMap(), resolve(listOf(withdrawn), unverified))
        // The caller's own eligibility rules apply as well.
        val listed = successor(16, parent = 10)
        assertEquals(
            emptyMap(),
            resolve(listOf(withdrawn.copy(remasters = listOf(16))), listOf(listed)) { false },
        )
    }

    @Test
    fun severalSuccessorsPreferVerifiedAvailabilityOverRecency() {
        val newest = successor(21, parent = 20, releasedDaysAgo = 10, ratings = 500)
        val widelySold = successor(22, parent = 20, gameType = 10, websites = listOf(steam, gog, switch), ratings = 5)
        // Not a verified successor: a version entry, and an entry whose parent is another game.
        val version = successor(23, 20, gameType = 10, websites = listOf(steam, gog, switch, wiiU), versionParent = 20)
        val strayParent = successor(24, parent = 99, websites = listOf(steam, gog, switch, site(16, "https://e.com/p")))
        val original = original(20, remasters = listOf(21, 24), expanded = listOf(22, 23))
        val successors = listOf(newest, widelySold, version, strayParent)
        assertEquals(mapOf(20 to widelySold), resolve(listOf(original), successors))
        // Equal storefronts fall back to rating count, then the lower ID; never to the release date.
        val older = successor(31, parent = 30, releasedDaysAgo = 3000, ratings = 40)
        val recent = successor(32, parent = 30, releasedDaysAgo = 5, ratings = 40)
        assertEquals(
            mapOf(30 to older),
            resolve(listOf(original(30, remasters = listOf(32, 31))), listOf(recent, older)),
        )
    }

    @Test
    fun similarNamesAndOtherRelationsNeverReplace() {
        // An edition-like title without the explicit link is not a successor.
        val lookalike = successor(41, parent = 1269, name = "Darksiders II: Deathinitive Edition")
        assertEquals(emptyMap(), resolve(listOf(darksidersII.copy(remasters = emptyList())), listOf(lookalike)))
        // A listed entry whose type is not remaster or expanded game (sequel, remake, DLC, bundle) is rejected.
        val mismatched = listOf(0, 1, 3, 8, null).mapIndexed { index, type -> successor(50 + index, 40, type) }
        val original = original(40, remasters = mismatched.map { it.id })
        assertEquals(emptyMap(), resolve(listOf(original), mismatched))
        // A remaster listed under expanded games, or the other way round, is inconsistent and kept out.
        val swapped = original(60, expanded = listOf(61))
        assertEquals(emptyMap(), resolve(listOf(swapped), listOf(successor(61, parent = 60, gameType = 9))))
    }

    @Test
    fun missingOrMalformedResultsKeepOriginals() {
        assertEquals(emptyMap(), EditionPreference.replacements(listOf(darksidersII), emptyMap(), now) { true })
        val noSuccessors = mapOf("originals" to JsonArray(listOf(json.encodeToJsonElement(darksidersII))))
        assertEquals(emptyMap(), EditionPreference.replacements(listOf(darksidersII), noSuccessors, now) { true })
        val malformed =
            mapOf(
                "originals" to
                    JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "id" to JsonPrimitive(1269),
                                    "websites" to JsonArray(listOf(JsonPrimitive(760924))),
                                ),
                            ),
                        ),
                    ),
            )
        // Callers catch parse failures and keep their ranked originals.
        assertFails { EditionPreference.replacements(listOf(darksidersII), malformed, now) { true } }
    }

    @Test
    fun lookupsStayBounded() {
        val originals = (1..100).map { original(it, remasters = listOf(1000 + it, 2000 + it)) }
        val query = checkNotNull(EditionPreference.query(originals, "name,cover.url"))
        assertContains(query, "limit ${EditionPreference.MAX_ORIGINALS};};")
        assertContains(query, "limit 60;};")
        // Caller fields are merged without repeating required ones.
        assertEquals(1, Regex("\\bname\\b").findAll(query.substringAfter("\"successors\"")).count())
    }

    @Test
    fun recommendationsPutTheSuccessorInTheOriginalsPlaceWithoutDuplicates() {
        fun entry(
            id: Int,
            score: Double,
        ) = Recommendation(id, "Game $id", null, score)
        val successor = successor(20048, parent = 1269, name = "Darksiders II: Deathinitive Edition")
        val ranked = listOf(entry(1269, 0.9), entry(2, 0.8), entry(20048, 0.7)) + (3..60).map { entry(it, 0.5) }
        val preferred = RecommendationRanking.preferEditions(ranked, mapOf(1269 to successor))
        assertEquals(Recommendation(20048, "Darksiders II: Deathinitive Edition", null, 0.9), preferred.first())
        assertEquals(listOf(20048, 2, 3), preferred.take(3).map { it.id })
        assertEquals(preferred.size, preferred.distinctBy { it.id }.size)
        // A spare ranked entry fills the place the duplicate left.
        assertEquals(RecommendationRanking.MAX_RESULTS, preferred.size)
        assertEquals(
            ranked.take(RecommendationRanking.MAX_RESULTS),
            RecommendationRanking.preferEditions(ranked, emptyMap()),
        )
    }

    // Service flow: the source recommends Darksiders II; the edition lookup is the third IGDB request.

    private val metadata =
        IgdbGame(
            id = 0,
            genres = listOf(12, 25),
            themes = listOf(17, 38),
            keywords = listOf(500, 501),
            playerPerspectives = listOf(2),
            firstReleaseDate = 1_340_000_000,
        )

    private fun catalog(successorDiscoverable: Boolean): List<IgdbGame> {
        val source = metadata.copy(id = 1, name = "Source Quest", similarGames = listOf(1269, 2), totalRatingCount = 90)
        val other = metadata.copy(id = 2, name = "Other Quest", totalRatingCount = 50)
        val original =
            darksidersII.copy(
                genres = metadata.genres,
                themes = metadata.themes,
                keywords = metadata.keywords,
                playerPerspectives = metadata.playerPerspectives,
                collections = emptyList(),
                franchises = emptyList(),
            )
        // Without ratings the successor matches no discovery search, so only the edition lookup finds it.
        val remaster =
            original.copy(
                id = 20048,
                name = deathinitive.name,
                gameType = 9,
                parentGame = 1269,
                remasters = emptyList(),
                totalRatingCount = if (successorDiscoverable) 102 else null,
                websites = deathinitive.websites,
                cover = IgdbCover("//images.igdb.com/igdb/image/upload/t_thumb/co20048.jpg"),
            )
        return listOf(source, other, original, remaster)
    }

    private fun service(igdb: FakeIgdb) =
        RecommendationService(
            IgdbService(igdb, { true }, BackendConfig("id", "secret", null, emptySet(), 8080)),
            log = {},
        )

    @Test
    fun recommendationsPreferTheSoldRemasterWithOneBoundedLookup() =
        runBlocking {
            for (discoverable in listOf(false, true)) {
                val igdb = FakeIgdb(catalog(discoverable))
                service(igdb).use { service ->
                    val result = service.recommendations(1)
                    val ids = result.games.map { it.id }
                    assertEquals(RecommendationResult.Kind.COMPLETE, result.kind)
                    assertTrue(20048 in ids && 1269 !in ids && 2 in ids, "$discoverable: $ids")
                    assertEquals(ids.distinct(), ids)
                    assertEquals(3, igdb.igdbRequests.get())
                    assertContains(igdb.bodies.last(), "query games \"originals\"")
                }
            }
        }

    @Test
    fun failedEditionLookupKeepsTheRankedOriginals() =
        runBlocking {
            val logs = mutableListOf<String>()
            val igdb = FakeIgdb(catalog(successorDiscoverable = false))
            igdb.failures = mapOf(2 to HttpStatusCode.ServiceUnavailable)
            RecommendationService(
                IgdbService(igdb, { true }, BackendConfig("id", "secret", null, emptySet(), 8080)),
                log = { logs += it },
            ).use { service ->
                val result = service.recommendations(1)
                assertEquals(RecommendationResult.Kind.COMPLETE, result.kind)
                assertEquals(listOf(2, 1269), result.games.map { it.id }.sorted())
                assertContains(logs.single(), "\"edition_failure\":")
            }
        }
}
