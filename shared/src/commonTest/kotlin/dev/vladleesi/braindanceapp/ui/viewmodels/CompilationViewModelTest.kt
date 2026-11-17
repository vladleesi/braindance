// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompilationViewModelTest : ViewModelTest() {
    /** The requests the mock backend received, as path and query. */
    private val requests = mutableListOf<String>()

    private fun homeRepo(handler: suspend MockRequestHandleScope.(page: Int) -> HttpResponseData): HomeRepo {
        val client =
            mockBackendClient(dispatcher) { request: HttpRequestData ->
                requests += request.url.encodedPath + "?" + request.url.encodedQuery
                handler(request.url.parameters["page"]?.toInt() ?: 1)
            }
        return HomeRepo(GamesRemote(client))
    }

    /** [count] cards of page [page]; IDs encode the page, so pages never share an ID by accident. */
    private fun games(
        page: Int,
        count: Int,
        firstIndex: Int = 0,
    ): String =
        (firstIndex until firstIndex + count).joinToString(",", "[", "]") { index ->
            """{"id":${page * 1000 + index},"name":"Game $page.$index","reason":"showcased"}"""
        }

    private fun PagedListState<*>.loaded() = assertIs<PagedListState.Loaded<*>>(this)

    @Test
    fun theFirstPageSharesHomesRequestAndLaterPagesFollowAsTheListScrolls() =
        runTest(dispatcher) {
            val viewModel = PopularGamesViewModel(homeRepo { page -> respondJson(games(page, count = 20)) })

            viewModel.load()
            assertEquals(PagedListState.Loading, viewModel.state.value)
            advanceUntilIdle()
            assertEquals(
                20,
                viewModel.state.value
                    .loaded()
                    .items.size,
            )
            assertEquals(
                MoreResults.Available,
                viewModel.state.value
                    .loaded()
                    .more,
            )

            viewModel.loadMore()
            assertEquals(
                MoreResults.Loading,
                viewModel.state.value
                    .loaded()
                    .more,
            )
            // A second request for the same page is never sent while one runs.
            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(listOf("/v1/games/trending?", "/v1/games/trending?page=2"), requests)
            val loaded = viewModel.state.value.loaded()
            assertEquals(40, loaded.items.size)
            assertEquals(MoreResults.Available, loaded.more)
        }

    @Test
    fun aShortPageEndsTheList() =
        runTest(dispatcher) {
            val viewModel =
                PopularGamesViewModel(homeRepo { page -> respondJson(games(page, count = if (page == 1) 20 else 5)) })

            viewModel.load()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(
                MoreResults.None,
                viewModel.state.value
                    .loaded()
                    .more,
            )
            assertEquals(
                25,
                viewModel.state.value
                    .loaded()
                    .items.size,
            )
            assertEquals(2, requests.size)
        }

    @Test
    fun theListStopsAtTheMostGamesTheApiServes() =
        runTest(dispatcher) {
            val viewModel = PopularGamesViewModel(homeRepo { page -> respondJson(games(page, count = 20)) })

            viewModel.load()
            advanceUntilIdle()
            repeat(10) {
                viewModel.loadMore()
                advanceUntilIdle()
            }

            assertEquals(HomeRepo.POPULAR_MAX_GAMES / HomeRepo.POPULAR_PAGE_SIZE, requests.size)
            assertEquals(
                MoreResults.None,
                viewModel.state.value
                    .loaded()
                    .more,
            )
        }

    @Test
    fun gamesRepeatedOnALaterPageAppearOnceAtTheirFirstPosition() =
        runTest(dispatcher) {
            // The ranking moved between requests: page 2 repeats the last five games of page 1.
            val viewModel =
                PopularGamesViewModel(
                    homeRepo { page ->
                        respondJson(if (page == 1) games(1, count = 20) else games(1, count = 20, firstIndex = 15))
                    },
                )

            viewModel.load()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            val ids =
                viewModel.state.value
                    .loaded()
                    .items
                    .map { assertIs<GameCardModel>(it).id }
            assertEquals(ids.distinct(), ids)
            assertEquals(35, ids.size)
        }

    @Test
    fun aFailedPageKeepsTheLoadedGamesAndLoadsAgainOnlyOnRetry() =
        runTest(dispatcher) {
            var failPage2 = true
            val viewModel =
                PopularGamesViewModel(
                    homeRepo { page ->
                        if (page == 2 && failPage2) {
                            respondJson("", HttpStatusCode.NotFound)
                        } else {
                            respondJson(games(page, count = 20))
                        }
                    },
                )
            viewModel.load()
            advanceUntilIdle()

            viewModel.loadMore()
            advanceUntilIdle()
            val failed = viewModel.state.value.loaded()
            assertEquals(20, failed.items.size)
            assertEquals(MoreResults.Failed(DataError.NotFound), failed.more)

            // Scrolling near the end again does not repeat the failed request.
            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(2, requests.size)

            failPage2 = false
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(
                40,
                viewModel.state.value
                    .loaded()
                    .items.size,
            )
        }

    @Test
    fun aFailedFirstPageShowsTheErrorUntilRetried() =
        runTest(dispatcher) {
            var fail = true
            val viewModel =
                PopularGamesViewModel(
                    homeRepo { page ->
                        if (fail) respondJson("", HttpStatusCode.TooManyRequests) else respondJson(games(page, 3))
                    },
                )

            viewModel.load()
            advanceUntilIdle()
            assertEquals(PagedListState.Error(DataError.RateLimited), viewModel.state.value)

            fail = false
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(
                MoreResults.None,
                viewModel.state.value
                    .loaded()
                    .more,
            )
        }

    @Test
    fun anEmptyFeedIsEmptyAndLoadingTwiceSendsOneRequest() =
        runTest(dispatcher) {
            val viewModel = AnticipatedGamesViewModel(homeRepo { respondJson("[]") })

            viewModel.load()
            viewModel.load()
            advanceUntilIdle()

            assertEquals(PagedListState.Empty, viewModel.state.value)
            assertEquals(listOf("/v1/games/anticipated?pageSize=20"), requests)
        }

    @Test
    fun anticipatedPagesAfterTheFirstNameTheirPage() =
        runTest(dispatcher) {
            val viewModel = AnticipatedGamesViewModel(homeRepo { page -> respondJson(games(page, count = 20)) })

            viewModel.load()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(
                listOf("/v1/games/anticipated?pageSize=20", "/v1/games/anticipated?page=2&pageSize=20"),
                requests,
            )
        }

    @Test
    fun aRefreshWinsOverASlowerPage() =
        runTest(dispatcher) {
            val page2 = CompletableDeferred<Unit>()
            val viewModel =
                PopularGamesViewModel(
                    homeRepo { page ->
                        if (page == 2) page2.await()
                        respondJson(games(page, count = 20))
                    },
                )
            viewModel.load()
            advanceUntilIdle()
            viewModel.loadMore()
            runCurrent()

            viewModel.refresh()
            page2.complete(Unit)
            advanceUntilIdle()

            val loaded = viewModel.state.value.loaded()
            assertEquals(20, loaded.items.size)
            assertEquals(MoreResults.Available, loaded.more)
        }

    @Test
    fun giveawaysAreOnePageAndOpenWithoutAnotherRequest() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) { request ->
                    requests += request.url.encodedPath
                    respondJson((1..30).joinToString(",", "[", "]") { id -> """{"id":$id,"title":"Giveaway $id"}""" })
                }
            val repo = GamerPowerRepo(GamerPowerRemote(client))
            val viewModel = GiveawaysListViewModel(repo)

            viewModel.load()
            advanceUntilIdle()

            val loaded = viewModel.state.value.loaded()
            assertEquals(30, loaded.items.size)
            assertEquals(MoreResults.None, loaded.more)
            assertTrue(repo.listedGiveaway(30) != null)
            assertNull(repo.listedGiveaway(31))
            assertEquals(listOf("/v1/giveaways"), requests)
        }
}
