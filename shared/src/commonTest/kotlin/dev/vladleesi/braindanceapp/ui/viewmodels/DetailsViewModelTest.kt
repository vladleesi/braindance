// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_complete
import dev.vladleesi.braindanceapp.resources.about_developer
import dev.vladleesi.braindanceapp.resources.about_developer_publisher
import dev.vladleesi.braindanceapp.resources.about_extra
import dev.vladleesi.braindanceapp.resources.about_main
import dev.vladleesi.braindanceapp.resources.about_publisher
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetailsViewModelTest : ViewModelTest() {
    @Test
    fun gameDetailsAndRecommendationsLoadIndependently() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) { request ->
                    when (request.url.encodedPath) {
                        "/v1/games/details" -> respondJson("""[{"id":42,"name":"Game"}]""")
                        else -> respondJson("{}", HttpStatusCode.InternalServerError)
                    }
                }
            val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))

            viewModel.load()
            advanceUntilIdle()

            assertEquals("Game", assertIs<GameDetailsState.Success>(viewModel.gameDetailsState.value).gameDetails.name)
            assertEquals(RecommendationsState.Unavailable, viewModel.recommendations.value)
        }

    @Test
    fun gameDetailsCoverUsesTheCardImageSize() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) { request ->
                    when (request.url.encodedPath) {
                        "/v1/games/details" ->
                            respondJson(
                                """[{"id":42,"name":"Game",""" +
                                    """"cover":{"url":"//images.igdb.com/igdb/image/upload/t_thumb/co1.jpg"}}]""",
                            )
                        else -> respondJson("{}", HttpStatusCode.InternalServerError)
                    }
                }
            val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))

            viewModel.load()
            advanceUntilIdle()

            val details = assertIs<GameDetailsState.Success>(viewModel.gameDetailsState.value).gameDetails
            assertEquals("https://images.igdb.com/igdb/image/upload/t_720p/co1.webp", details.coverImageUrl)
        }

    @Test
    fun timeToBeatScalesAgainstTheLongestEstimateBeforeRounding() =
        runTest(dispatcher) {
            val details =
                loadDetails(""""time_to_beat":{"hastily":5400,"normally":10800,"completely":8640,"count":1}""")

            assertEquals(
                GamePlayTimes(
                    listOf(
                        PlayTime(Res.string.about_main, hours = 2, relativeLength = 0.5f),
                        PlayTime(Res.string.about_extra, hours = 3, relativeLength = 1f),
                        PlayTime(Res.string.about_complete, hours = 2, relativeLength = 0.8f),
                    ),
                    sampleCount = 1,
                ),
                details.playTimes,
            )
            assertTrue(details.facts.isEmpty)
        }

    @Test
    fun timeToBeatOmitsInvalidEstimatesAndRequiresAtLeastOneDuration() =
        runTest(dispatcher) {
            val partial = loadDetails(""""time_to_beat":{"hastily":-7200,"normally":0,"completely":3600,"count":0}""")
            val empty = loadDetails(""""time_to_beat":{"hastily":-1,"normally":0,"completely":null,"count":3}""")

            assertEquals(GamePlayTimes(listOf(PlayTime(Res.string.about_complete, 1, 1f))), partial.playTimes)
            assertNull(empty.playTimes)
        }

    @Test
    fun aboutCombinesIdenticalCompanyCreditsAndKeepsTags() =
        runTest(dispatcher) {
            val details =
                loadDetails(
                    """
                    "developers":[{"id":1,"name":"Studio"},{"id":2,"name":"Partner"}],
                    "publishers":[{"id":2,"name":"Partner"},{"id":1,"name":"Studio"}],
                    "game_modes":[{"id":3,"name":"Single player"}],
                    "player_perspectives":[{"id":4,"name":"Third person"}],
                    "themes":[{"id":5,"name":"Fantasy"}]
                    """.trimIndent(),
                )

            assertEquals(listOf(GameFact(Res.string.about_developer_publisher, "Studio, Partner")), details.facts.rows)
            assertEquals(listOf("Single player", "Third person", "Fantasy"), details.facts.tags)
        }

    @Test
    fun aboutKeepsDistinctCompaniesEvenWhenTheirNamesMatch() =
        runTest(dispatcher) {
            val details =
                loadDetails(""""developers":[{"id":1,"name":"Studio"}],"publishers":[{"id":2,"name":"Studio"}]""")

            assertEquals(
                listOf(GameFact(Res.string.about_developer, "Studio"), GameFact(Res.string.about_publisher, "Studio")),
                details.facts.rows,
            )
            assertNull(details.playTimes)
        }

    private suspend fun loadDetails(fields: String): GameDetails {
        val client =
            mockBackendClient(dispatcher) { request ->
                when (request.url.encodedPath) {
                    "/v1/games/details" -> respondJson("""[{"id":42,$fields}]""")
                    else -> respondJson("[]")
                }
            }
        val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))
        viewModel.load()
        return assertIs<GameDetailsState.Success>(
            viewModel.gameDetailsState.first { it !is GameDetailsState.Loading },
        ).gameDetails
    }

    @Test
    fun theBackdropIsTheImageTheApiChoseAtBothSizes() =
        runTest(dispatcher) {
            val thumbs = "//images.igdb.com/igdb/image/upload/t_thumb"
            val client =
                mockBackendClient(dispatcher) { request ->
                    when (request.url.encodedPath) {
                        "/v1/games/details" ->
                            respondJson(
                                """[{"id":42,"name":"Game","backdrop":{"url":"$thumbs/ar1.jpg"},""" +
                                    """"screenshots":[{"id":3,"url":"$thumbs/sc1.jpg"}]}]""",
                            )
                        else -> respondJson("{}", HttpStatusCode.InternalServerError)
                    }
                }
            val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))

            viewModel.load()
            advanceUntilIdle()

            val details = assertIs<GameDetailsState.Success>(viewModel.gameDetailsState.value).gameDetails
            assertEquals(
                BackdropImage(
                    compactUrl = "https://images.igdb.com/igdb/image/upload/t_screenshot_med/ar1.webp",
                    wideUrl = "https://images.igdb.com/igdb/image/upload/t_screenshot_big/ar1.webp",
                    hdUrl = "https://images.igdb.com/igdb/image/upload/t_720p/ar1.webp",
                    fullHdUrl = "https://images.igdb.com/igdb/image/upload/t_1080p/ar1.webp",
                ),
                details.backdrop,
            )
        }

    @Test
    fun withoutTheApisBackdropTheClientPicksNone() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) { request ->
                    when (request.url.encodedPath) {
                        "/v1/games/details" ->
                            respondJson(
                                """[{"id":42,"name":"Game","screenshots":[""" +
                                    """{"id":3,"url":"//images.igdb.com/igdb/image/upload/t_thumb/sc1.jpg"}]}]""",
                            )
                        else -> respondJson("{}", HttpStatusCode.InternalServerError)
                    }
                }
            val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))

            viewModel.load()
            advanceUntilIdle()

            assertNull(assertIs<GameDetailsState.Success>(viewModel.gameDetailsState.value).gameDetails.backdrop)
        }

    @Test
    fun aGameDetailsRefreshWinsOverASlowerFirstLoad() =
        runTest(dispatcher) {
            var detailCalls = 0
            val client =
                mockBackendClient(dispatcher) { request ->
                    if (request.url.encodedPath != "/v1/games/details") return@mockBackendClient respondJson("[]")
                    detailCalls++
                    if (detailCalls == 1) {
                        delay(3_000)
                        respondJson("""[{"id":42,"name":"Old"}]""")
                    } else {
                        respondJson("""[{"id":42,"name":"New"}]""")
                    }
                }
            val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))

            viewModel.load()
            runCurrent()
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("New", assertIs<GameDetailsState.Success>(viewModel.gameDetailsState.value).gameDetails.name)
        }

    @Test
    fun giveawayLoadsAndFailsExplicitly() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) { request ->
                    if (request.url.encodedPath == "/v1/giveaways/7") {
                        respondJson("""{"id":7,"title":"Free game"}""")
                    } else {
                        respondJson("{}", HttpStatusCode.InternalServerError)
                    }
                }
            val repo = GamerPowerRepo(GamerPowerRemote(client))

            val found = GiveawayDetailsViewModel(giveawayId = 7, gamerPowerRepo = repo)
            val failing = GiveawayDetailsViewModel(giveawayId = 8, gamerPowerRepo = repo)
            found.load()
            failing.load()
            advanceUntilIdle()

            val giveaway = assertIs<GiveawayDetailsState.Success>(found.giveawayDetailsState.value).giveawayDetails
            assertEquals("Free game", giveaway.title)
            assertIs<GiveawayDetailsState.Error>(failing.giveawayDetailsState.value)
        }

    @Test
    fun aListedGiveawayOpensWithoutARequestUntilRefreshed() =
        runTest(dispatcher) {
            val requests = mutableListOf<String>()
            val client =
                mockBackendClient(dispatcher) { request ->
                    requests += request.url.encodedPath
                    when (request.url.encodedPath) {
                        "/v1/giveaways" -> respondJson("""[{"id":7,"title":"Listed"},{"title":"No ID"}]""")
                        "/v1/giveaways/7" -> respondJson("""{"id":7,"title":"Fetched"}""")
                        "/v1/giveaways/8" -> respondJson("""{"id":8,"title":"Direct link"}""")
                        else -> respondJson("{}", HttpStatusCode.InternalServerError)
                    }
                }
            val repo = GamerPowerRepo(GamerPowerRemote(client))
            repo.giveaways(pageSize = 20)
            requests.clear()

            fun GiveawayDetailsViewModel.title() =
                assertIs<GiveawayDetailsState.Success>(giveawayDetailsState.value).giveawayDetails.title

            val listed = GiveawayDetailsViewModel(giveawayId = 7, gamerPowerRepo = repo)
            val unlisted = GiveawayDetailsViewModel(giveawayId = 8, gamerPowerRepo = repo)
            // The screen's first frame reads this before load() runs, so a listed giveaway never shows Loading.
            assertEquals("Listed", listed.title())
            assertEquals(GiveawayDetailsState.Loading, unlisted.giveawayDetailsState.value)
            listed.load()
            unlisted.load()
            advanceUntilIdle()

            assertEquals("Listed", listed.title())
            assertEquals("Direct link", unlisted.title())
            assertEquals(listOf("/v1/giveaways/8"), requests)

            listed.refresh()
            advanceUntilIdle()

            assertEquals("Fetched", listed.title())
        }

    @Test
    fun giveawayCancellationIsNotReportedAsAnError() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) {
                    delay(1_000)
                    respondJson("""{"id":7}""")
                }
            val viewModel =
                GiveawayDetailsViewModel(giveawayId = 7, gamerPowerRepo = GamerPowerRepo(GamerPowerRemote(client)))

            viewModel.load()
            runCurrent()
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()

            assertEquals(GiveawayDetailsState.Loading, viewModel.giveawayDetailsState.value)
        }

    @Test
    fun anEmptyDetailsAnswerIsNotFoundAndRecommendationsAreUnique() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) { request ->
                    when (request.url.encodedPath) {
                        "/v1/games/details" -> respondJson("[]")
                        else -> respondJson("""[{"id":5},{"name":"No ID"},{"id":5},{"id":6}]""")
                    }
                }
            val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))

            viewModel.load()
            advanceUntilIdle()

            assertEquals(GameDetailsState.NotFound, viewModel.gameDetailsState.value)
            val cards = assertIs<RecommendationsState.Success>(viewModel.recommendations.value).games
            assertEquals(listOf(5, 6), cards.map { it.id })
        }

    @Test
    fun aNullGiveawayIsNotFound() =
        runTest(dispatcher) {
            val client = mockBackendClient(dispatcher) { respondJson("null") }
            val viewModel =
                GiveawayDetailsViewModel(giveawayId = 7, gamerPowerRepo = GamerPowerRepo(GamerPowerRemote(client)))

            viewModel.load()
            advanceUntilIdle()

            assertEquals(GiveawayDetailsState.NotFound, viewModel.giveawayDetailsState.value)
        }
}
