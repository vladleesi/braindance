// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
            assertEquals("https://images.igdb.com/igdb/image/upload/t_720p/co1.jpg", details.coverImageUrl)
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
