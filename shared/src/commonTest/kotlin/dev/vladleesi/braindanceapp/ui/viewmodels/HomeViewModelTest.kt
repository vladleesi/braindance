// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardModel
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HomeViewModelTest : ViewModelTest() {
    private fun viewModel(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HomeViewModel {
        val client = mockBackendClient(dispatcher, handler)
        return HomeViewModel(HomeRepo(GamesRemote(client)), GamerPowerRepo(GamerPowerRemote(client)))
    }

    private fun SectionState<MiniGameCardModel>.cardTitles() =
        assertIs<SectionState.Success<MiniGameCardModel>>(this).items.map { it.title }

    @Test
    fun loadFillsEverySection() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { request ->
                    when (request.url.encodedPath) {
                        "/v1/giveaways" -> respondJson("""[{"id":9,"title":"Free"}]""")
                        else -> respondJson("""[{"id":1,"name":"A"}]""")
                    }
                }

            viewModel.load()
            advanceUntilIdle()

            assertEquals(listOf("A"), viewModel.trendingNow.value.cardTitles())
            assertEquals(listOf("A"), viewModel.mostAnticipated.value.cardTitles())
            assertIs<SectionState.Success<*>>(viewModel.giveaways.value)
        }

    @Test
    fun failedSectionShowsErrorWithoutAffectingOthers() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { request ->
                    if (request.url.encodedPath == "/v1/games/trending") {
                        respondJson("{}", HttpStatusCode.InternalServerError)
                    } else {
                        respondJson("[]")
                    }
                }

            viewModel.load()
            advanceUntilIdle()

            assertIs<SectionState.Error>(viewModel.trendingNow.value)
            assertIs<SectionState.Success<*>>(viewModel.mostAnticipated.value)
        }

    @Test
    fun cancellationIsNotReportedAsAnError() =
        runTest(dispatcher) {
            val viewModel =
                viewModel {
                    delay(1_000)
                    respondJson("[]")
                }

            viewModel.load()
            runCurrent()
            viewModel.viewModelScope.cancel()
            advanceUntilIdle()

            assertEquals(SectionState.Loading, viewModel.trendingNow.value)
            assertEquals(SectionState.Loading, viewModel.mostAnticipated.value)
            assertEquals(SectionState.Loading, viewModel.giveaways.value)
        }

    @Test
    fun aSecondRefreshWinsOverASlowerFirstLoad() =
        runTest(dispatcher) {
            var trendingCalls = 0
            val viewModel =
                viewModel { request ->
                    if (request.url.encodedPath != "/v1/games/trending") return@viewModel respondJson("[]")
                    trendingCalls++
                    if (trendingCalls == 1) {
                        delay(3_000)
                        respondJson("""[{"id":1,"name":"Old"}]""")
                    } else {
                        respondJson("""[{"id":2,"name":"New"}]""")
                    }
                }

            viewModel.load()
            runCurrent()
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("New"), viewModel.trendingNow.value.cardTitles())
            assertEquals(2, trendingCalls)
        }

    @Test
    fun refreshingLastsUntilEverySectionFinishes() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { request ->
                    if (request.url.encodedPath == "/v1/giveaways") delay(2_000)
                    respondJson("[]")
                }

            viewModel.refresh()
            runCurrent()
            assertTrue(viewModel.isRefreshing.value)

            advanceTimeBy(1_000)
            assertTrue(viewModel.isRefreshing.value, "giveaways are still loading")

            advanceUntilIdle()
            assertFalse(viewModel.isRefreshing.value)
        }

    @Test
    fun cardsWithoutIdsAndDuplicateIdsAreDropped() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { request ->
                    when (request.url.encodedPath) {
                        "/v1/games/trending" -> respondJson("""[{"id":1,"name":"A"},{"id":1,"name":"A"},{"id":2}]""")
                        "/v1/games/anticipated" -> respondJson("""[{"name":"No ID"},{"id":3},{"id":3},{"id":4}]""")
                        else -> respondJson("""[{"title":"No ID"},{"id":9},{"id":9},{"id":10}]""")
                    }
                }

            viewModel.load()
            advanceUntilIdle()

            fun SectionState<*>.ids() =
                assertIs<SectionState.Success<*>>(this).items.map {
                    when (it) {
                        is MiniGameCardModel -> it.id
                        is GiveawayItemModel -> it.id
                        else -> error("unexpected $it")
                    }
                }
            assertEquals(listOf(1, 2), viewModel.trendingNow.value.ids())
            assertEquals(listOf(3, 4), viewModel.mostAnticipated.value.ids())
            assertEquals(listOf(9, 10), viewModel.giveaways.value.ids())
        }
}
