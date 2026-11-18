// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Game relations as the details screen presents them, from responses shaped like the API's for The Witcher 3. */
class GameFamilyDetailsTest : ViewModelTest() {
    private val witcherFamily =
        """
        "family":{
          "variants":[
            {"id":22439,"name":"The Witcher 3: Wild Hunt - Game of the Year Edition","kind":"edition",
             "label":"Game of the Year Edition","first_release_date":1472428800},
            {"id":119402,"name":"The Witcher 3: Wild Hunt - Complete Edition","kind":"edition",
             "label":"Complete Edition"},
            {"id":230102,"name":"The Witcher 3: Wild Hunt - Free Next-Gen Update","kind":"update",
             "label":"Free Next-Gen Update","first_release_date":1670889600}
          ],
          "extras":[
            {"id":12503,"name":"The Witcher 3: Wild Hunt - Hearts of Stone","kind":"expansion",
             "label":"Hearts of Stone","first_release_date":1444608000},
            {"id":13166,"name":"The Witcher 3: Wild Hunt - Blood and Wine","kind":"expansion",
             "label":"Blood and Wine","first_release_date":1464566400}
          ],
          "other_versions":[{"id":99001,"name":"The Witcher 3: Wild Hunt","kind":"port"}],
          "included_in":[{"id":141472,"name":"The Witcher 3: Wild Hunt + Dark Souls III","kind":"bundle",
                          "first_release_date":1500000000}],
          "contents":[]
        }
        """.trimIndent()

    private val witcher = """{"id":1942,"name":"The Witcher 3: Wild Hunt","kind":"game","summary":"The game."}"""

    /** An expansion's response: its own page, whose family names The Witcher 3 as its parent. */
    private fun expansion(
        id: Int,
        name: String,
    ) = """[{"id":$id,"name":"$name","kind":"expansion","canonical_id":$id,""" +
        """"family":{"parent":{"id":1942,"name":"The Witcher 3: Wild Hunt","kind":"game"},""" +
        """"variants":[],"extras":[],"other_versions":[],"contents":[],"included_in":[""" +
        """{"id":22439,"name":"The Witcher 3: Wild Hunt - Game of the Year Edition","kind":"edition"},""" +
        """{"id":119402,"name":"The Witcher 3: Wild Hunt - Complete Edition","kind":"edition"}]}}]"""

    /** A ViewModel whose API answers details with [details] for the requested ID. */
    private fun TestScope.viewModel(
        gameId: Int,
        details: (id: String) -> String,
        requestedRecommendations: MutableList<String>,
    ): GameDetailsViewModel {
        val client =
            mockBackendClient(dispatcher) { request ->
                val id = request.url.parameters["id"].orEmpty()
                when (request.url.encodedPath) {
                    "/v1/games/details" -> respondJson(details(id))
                    "/v1/games/recommendations" -> {
                        requestedRecommendations += id
                        respondJson("""[{"id":${id}0,"name":"Similar to $id"}]""")
                    }
                    else -> respondJson("{}", HttpStatusCode.NotFound)
                }
            }
        return GameDetailsViewModel(gameId = gameId, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))
    }

    private fun TestScope.load(
        gameId: Int,
        detailsJson: String,
        requestedRecommendations: MutableList<String> = mutableListOf(),
    ): Pair<GameDetailsViewModel, GameDetails> {
        val viewModel = viewModel(gameId, { detailsJson }, requestedRecommendations)
        viewModel.load()
        advanceUntilIdle()
        val state = viewModel.gameDetailsState.value
        check(state is GameDetailsState.Success) { "Not loaded: $state" }
        return viewModel to state.gameDetails
    }

    @Test
    fun bothWitcherExpansionsLinkTheirBaseGame() =
        runTest(dispatcher) {
            for ((id, name) in listOf(12503 to "Hearts of Stone", 13166 to "Blood and Wine")) {
                val (_, details) = load(id, expansion(id, "The Witcher 3: Wild Hunt - $name"))
                assertEquals(
                    GameOrigin(GameKind.EXPANSION, OriginRelation.FOR, 1942, "The Witcher 3: Wild Hunt"),
                    details.relations.origin,
                    name,
                )
                // An expansion is a game of its own: no version selector and no selected variant.
                assertNull(details.relations.versions, name)
                assertNull(details.selectedVariant, name)
                // The editions that include it are listed with what they are.
                val bundles = details.relations.related.single()
                assertEquals(RelatedCategory.INCLUDED_IN, bundles.category)
                assertEquals(listOf(GameKind.EDITION, GameKind.EDITION), bundles.games.map { it.kind })
            }
        }

    @Test
    fun theBaseGameSelectsItselfAmongItsVersionsAndGroupsRelatedReleases() =
        runTest(dispatcher) {
            val requested = mutableListOf<String>()
            val (_, details) =
                load(
                    gameId = 1942,
                    detailsJson = """[${witcher.dropLast(1)},"canonical_id":1942,$witcherFamily}]""",
                    requestedRecommendations = requested,
                )

            assertNull(details.relations.origin)
            val versions = details.relations.versions ?: error("No versions")
            assertEquals(1942, versions.selectedId)
            assertEquals(listOf(1942, 22439, 119402, 230102), versions.options.map { it.id })
            assertEquals(
                listOf(null, "Game of the Year Edition", "Complete Edition", "Free Next-Gen Update"),
                versions.options.map { it.label },
            )
            assertEquals(GameKind.GAME, versions.selected?.kind)

            // Separate releases, in tab order; editions are versions, never related rows.
            val related = details.relations.related
            assertEquals(
                listOf(RelatedCategory.EXTRAS, RelatedCategory.OTHER_VERSIONS, RelatedCategory.INCLUDED_IN),
                related.map { it.category },
                "empty kinds have no tab",
            )
            val extras = related.first().games
            assertEquals(listOf("Hearts of Stone", "Blood and Wine"), extras.map { it.title })
            assertEquals(listOf(2015, 2016), extras.map { it.year })
            assertEquals("The Witcher 3: Wild Hunt", related[1].games.single().title, "without a label, the full name")
            assertEquals(listOf("1942"), requested, "a game's own page needs no second request")
        }

    @Test
    fun aDirectLinkToAnUpdateShowsItsGameWithTheUpdateSelected() =
        runTest(dispatcher) {
            val requested = mutableListOf<String>()
            val (viewModel, details) =
                load(
                    gameId = 230102,
                    detailsJson =
                        """[{"id":230102,"name":"The Witcher 3: Wild Hunt - Free Next-Gen Update",""" +
                            """"summary":"The free update.","first_release_date":1670889600,"kind":"update",""" +
                            """"canonical_id":1942,"canonical":$witcher,$witcherFamily}]""",
                    requestedRecommendations = requested,
                )

            // The page shows the update as a release of its game, selected in the version selector and nowhere else.
            assertEquals(1942, details.id)
            assertEquals("The Witcher 3: Wild Hunt - Free Next-Gen Update", details.name)
            assertEquals("The free update.", details.summary)
            val versions = details.relations.versions ?: error("No versions")
            assertEquals(230102, versions.selectedId)
            assertEquals(GameKind.UPDATE, versions.selected?.kind)
            assertEquals(1, versions.options.count { it.id == 230102 }, "the selection is not repeated")
            assertNull(details.relations.origin, "a version is not presented as content for its game")
            assertEquals(230102, details.releaseId)

            // Similar games follow the game the page shows.
            assertEquals(listOf("230102", "1942"), requested)
            val similar = viewModel.recommendations.value
            assertEquals(listOf(19420), (similar as RecommendationsState.Success).games.map { it.id })
        }

    @Test
    fun anEditionShowsItsOwnReleaseDataAndTheGamesForWhatItLacks() =
        runTest(dispatcher) {
            // Shaped like IGDB's Mass Effect 3 and its N7 Collector's Edition: their own cover, platforms, and genres.
            val game =
                """{"id":75,"name":"Mass Effect 3","kind":"game","summary":"The game.","storyline":"The story.",""" +
                    """"cover":{"id":1,"url":"//images.igdb.com/igdb/image/upload/t_thumb/co1x7q.jpg"},""" +
                    """"first_release_date":1330992000,"platforms":[{"id":6,"name":"PC (Microsoft Windows)"}],""" +
                    """"genres":[{"id":12,"name":"Role-playing (RPG)"}],""" +
                    """"screenshots":[{"id":1,"url":"//images.igdb.com/igdb/image/upload/t_thumb/game.jpg"}],""" +
                    """"websites":[{"id":1,"url":"https://store.steampowered.com/app/1238020","type":13}]}"""
            val family =
                """"family":{"variants":[{"id":45180,"name":"Mass Effect 3: N7 Collector's Edition",""" +
                    """"kind":"edition","label":"N7 Collector's Edition"}]}"""
            val (_, edition) =
                load(
                    gameId = 45180,
                    detailsJson =
                        """[{"id":45180,"name":"Mass Effect 3: N7 Collector's Edition","kind":"edition",""" +
                            """"cover":{"id":2,"url":"//images.igdb.com/igdb/image/upload/t_thumb/co6bxq.jpg"},""" +
                            """"first_release_date":1330992000,""" +
                            """"platforms":[{"id":49,"name":"Xbox One"},{"id":9,"name":"PlayStation 3"}],""" +
                            """"genres":[{"id":5,"name":"Shooter"}],""" +
                            """"websites":[{"id":2,"url":"https://masseffect.fandom.com/wiki/N7","type":2}],""" +
                            """"canonical_id":75,"canonical":$game,$family}]""",
                )

            assertEquals(75, edition.id, "relations and similar games follow the game")
            assertEquals("Mass Effect 3: N7 Collector's Edition", edition.name)
            assertTrue("co6bxq" in edition.coverImageUrl, edition.coverImageUrl)
            assertEquals(listOf(ParentPlatformType.Xbox, ParentPlatformType.PlayStation), edition.platforms)
            assertEquals(listOf("Shooter"), edition.genres.map { it.name })
            // What IGDB lacks for the edition is its game's: no store sells it, so the game's stores are shown.
            assertEquals(listOf("The game.", "The story."), listOf(edition.summary, edition.storyline))
            assertEquals(listOf("https://store.steampowered.com/app/1238020"), edition.stores.map { it.url })
            assertTrue(
                edition.backdrop
                    ?.compactUrl
                    .orEmpty()
                    .contains("game"),
                "${edition.backdrop}",
            )
            assertEquals(45180, edition.relations.versions?.selectedId)
        }

    @Test
    fun switchingVersionsUpdatesThePageInPlace() =
        runTest(dispatcher) {
            val requested = mutableListOf<String>()
            val update =
                """[{"id":230102,"name":"The Witcher 3: Wild Hunt - Free Next-Gen Update","kind":"update",""" +
                    """"canonical_id":1942,"canonical":$witcher,$witcherFamily}]"""
            val game = """[${witcher.dropLast(1)},"canonical_id":1942,$witcherFamily}]"""
            val viewModel = viewModel(1942, { id -> if (id == "230102") update else game }, requested)
            viewModel.load()
            advanceUntilIdle()

            viewModel.showVersion(230102)
            // At once, before the update loads: the page stays, with the update selected and its details loading.
            val switchingState = viewModel.gameDetailsState.value as GameDetailsState.Success
            assertTrue(switchingState.isSwitchingVersion)
            val switching = switchingState.gameDetails
            assertEquals("The Witcher 3: Wild Hunt", switching.name)
            assertEquals(230102, switching.relations.versions?.selectedId)

            advanceUntilIdle()
            val shownState = viewModel.gameDetailsState.value as GameDetailsState.Success
            assertFalse(shownState.isSwitchingVersion)
            val shown = shownState.gameDetails
            assertEquals("The Witcher 3: Wild Hunt - Free Next-Gen Update", shown.name)
            assertEquals(230102, shown.relations.versions?.selectedId)

            viewModel.showVersion(1942)
            advanceUntilIdle()
            val back = (viewModel.gameDetailsState.value as GameDetailsState.Success).gameDetails
            assertEquals("The Witcher 3: Wild Hunt", back.name)
            assertEquals(1942, back.relations.versions?.selectedId)
            assertEquals(listOf("1942"), requested, "similar games of one game load once")
        }

    @Test
    fun aSelectedVersionTheListLacksIsStillShown() =
        runTest(dispatcher) {
            // An edition of an edition resolves to the game, but the API lists only the game's direct editions.
            val (_, details) =
                load(
                    gameId = 372654,
                    detailsJson =
                        """[{"id":372654,"name":"Complete Edition: 10th Anniversary Edition","kind":"edition",""" +
                            """"canonical_id":1942,"canonical":$witcher,$witcherFamily}]""",
                )
            val versions = details.relations.versions ?: error("No versions")
            assertEquals(372654, versions.selectedId)
            assertEquals("Complete Edition: 10th Anniversary Edition", versions.selected?.label)
        }

    @Test
    fun remastersDlcAndUnknownKindsNameTheirRelation() =
        runTest(dispatcher) {
            val expected =
                listOf(
                    "remaster" to OriginRelation.OF,
                    "dlc" to OriginRelation.FOR,
                    "a_kind_added_later" to OriginRelation.PART_OF,
                )
            for ((kind, relation) in expected) {
                val (_, details) =
                    load(
                        gameId = 5,
                        detailsJson =
                            """[{"id":5,"name":"Entry","kind":"$kind","canonical_id":5,""" +
                                """"family":{"parent":{"id":1,"name":"Game","kind":"game"},"variants":[]}}]""",
                    )
                assertEquals(relation, details.relations.origin?.relation, kind)
                assertEquals(emptyList(), details.relations.related, kind)
            }
        }

    @Test
    fun gamesWithoutRelationsOrWithoutAFamilyShowNone() =
        runTest(dispatcher) {
            val (_, alone) =
                load(
                    gameId = 7,
                    detailsJson =
                        """[{"id":7,"name":"Game","kind":"game","canonical_id":7,"family":{"variants":[],""" +
                            """"extras":[],"other_versions":[],"included_in":[],"contents":[]}}]""",
                )
            assertEquals(GameRelations(), alone.relations)

            // The API could not load the update's game, so it sent the update alone, as before families existed.
            val (_, update) =
                load(230102, """[{"id":230102,"name":"Free Next-Gen Update","kind":"update","summary":"S"}]""")
            assertEquals(230102, update.id)
            assertNull(update.selectedVariant)
            assertEquals(GameRelations(), update.relations)

            // Incomplete cards (no ID or name) and links to the page itself are left out.
            val (_, incomplete) =
                load(
                    gameId = 8,
                    detailsJson =
                        """[{"id":8,"name":"Game","kind":"game","canonical_id":8,"family":{""" +
                            """"parent":{"id":8,"name":"Game"},"variants":[{"id":null,"name":"X"}],""" +
                            """"extras":[{"id":9,"name":""},{"id":8,"name":"Game"}]}}]""",
                )
            assertEquals(GameRelations(), incomplete.relations)
        }
}
