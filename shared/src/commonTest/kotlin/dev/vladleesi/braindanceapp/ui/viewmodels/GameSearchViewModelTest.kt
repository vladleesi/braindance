// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GameSearchRepo
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class GameSearchViewModelTest : ViewModelTest() {
    /** The search requests the mock backend received, as `query:page`. */
    private val requests = mutableListOf<String>()

    private fun viewModel(
        handler: suspend MockRequestHandleScope.(query: String, page: Int) -> HttpResponseData =
            { query, page -> respondJson(page(query, page, count = 20)) },
    ): GameSearchViewModel {
        val client =
            mockBackendClient(dispatcher) { request: HttpRequestData ->
                assertEquals("/v1/games/search", request.url.encodedPath)
                // A fixed parameter order keeps one edge-cache URL per page.
                val names =
                    request.url.encodedQuery
                        .split('&')
                        .map { it.substringBefore('=') }
                assertEquals(listOf("q", "page", "pageSize"), names)
                val parameters = request.url.parameters
                assertEquals("20", parameters["pageSize"])
                val query = checkNotNull(parameters["q"])
                val page = checkNotNull(parameters["page"]).toInt()
                requests += "$query:$page"
                this.handler(query, page)
            }
        return GameSearchViewModel(GameSearchRepo(GamesRemote(client)), dispatcher.scheduler.timeSource)
    }

    /** A card whose ID encodes the query's length, the page, and its index, so pages never share an ID by accident. */
    private fun card(
        query: String,
        page: Int,
        index: Int,
    ): String {
        val id = query.length * 100_000 + page * 1_000 + index
        return """{"id":$id,"name":"$query $page.$index","first_release_date":1500000000}"""
    }

    private fun page(
        query: String,
        page: Int,
        count: Int,
    ): String = (0 until count).joinToString(",", "[", "]") { index -> card(query, page, index) }

    private fun SearchState.titles(): List<String> = assertIs<SearchState.Results>(this).games.map { it.title }

    private fun TestScope.typeAndWait(
        viewModel: GameSearchViewModel,
        text: String,
    ) {
        viewModel.onQueryChange(text)
        advanceUntilIdle()
    }

    @Test
    fun typingWaitsForAPauseAndSearchesOnlyTheLastText() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            for (text in listOf("ze", "zel", "zeld", "zelda")) {
                viewModel.onQueryChange(text)
                advanceTimeBy(GameSearchViewModel.DEBOUNCE / 2)
            }
            assertEquals(SearchState.Loading("zelda"), viewModel.state.value)
            assertEquals(emptyList(), requests, "no request while typing")

            advanceUntilIdle()
            assertEquals(listOf("zelda:1"), requests)
            assertEquals(
                20,
                viewModel.state.value
                    .titles()
                    .size,
            )
        }

    @Test
    fun textThatNormalizesToTheSameQuerySendsNothing() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            typeAndWait(viewModel, "Zelda")

            typeAndWait(viewModel, " zelda ")
            typeAndWait(viewModel, "ZELDA")

            assertEquals(listOf("zelda:1"), requests)
        }

    @Test
    fun textTooShortToSearchRestoresDiscoverWithoutARequest() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            typeAndWait(viewModel, "hades")

            typeAndWait(viewModel, "h")
            assertEquals(SearchState.Idle, viewModel.state.value)
            typeAndWait(viewModel, "")
            assertEquals(SearchState.Idle, viewModel.state.value)

            assertEquals(listOf("hades:1"), requests)
        }

    @Test
    fun clearingDuringARequestCancelsIt() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { query, page ->
                    delay(1_000)
                    respondJson(page(query, page, count = 3))
                }
            viewModel.onQueryChange("doom")
            advanceTimeBy(GameSearchViewModel.DEBOUNCE + 100.milliseconds)
            runCurrent()

            viewModel.onQueryChange("")
            advanceUntilIdle()

            assertEquals(SearchState.Idle, viewModel.state.value)
        }

    @Test
    fun aSlowOlderResponseNeverReplacesANewerOne() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { query, page ->
                    if (query == "doom") delay(5_000)
                    respondJson(page(query, page, count = 1))
                }
            viewModel.onSearch("doom")
            runCurrent()
            viewModel.onSearch("hades")
            advanceUntilIdle()

            assertEquals(listOf("hades 1.0"), viewModel.state.value.titles())
        }

    @Test
    fun theSearchActionSkipsThePendingPause() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onQueryChange("portal")
            runCurrent()

            viewModel.onSearch("portal")
            runCurrent()

            assertEquals(listOf("portal:1"), requests)
            assertIs<SearchState.Results>(viewModel.state.value)
            advanceUntilIdle()
            assertEquals(listOf("portal:1"), requests, "the debounced request is not sent as well")
        }

    @Test
    fun aRecentSearchShowsAtOnceUntilItExpires() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            typeAndWait(viewModel, "doom")
            viewModel.loadMore()
            advanceUntilIdle()
            typeAndWait(viewModel, "doom 2")

            viewModel.onQueryChange("doom")
            val restored = assertIs<SearchState.Results>(viewModel.state.value)
            assertEquals(40, restored.games.size, "both loaded pages, with no loading state in between")
            assertEquals(MoreResults.Available, restored.more)
            assertEquals(listOf("doom:1", "doom:2", "doom 2:1"), requests)

            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals("doom:3", requests.last(), "paging continues where it ended")

            typeAndWait(viewModel, "hades")
            advanceTimeBy(GameSearchViewModel.CACHE_LIFETIME + 1.minutes)
            viewModel.onQueryChange("doom")
            assertEquals(SearchState.Loading("doom"), viewModel.state.value)
            advanceUntilIdle()
            assertEquals("doom:1", requests.last())
        }

    @Test
    fun pagesAppendWithoutDuplicatesUntilAShortPage() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { query, page ->
                    when (page) {
                        1 -> respondJson(page(query, page, count = 20))
                        // Repeats the last card of page 1 and ends with a short page.
                        else -> {
                            val cards = listOf(card(query, 1, 19)) + List(5) { card(query, page, it) }
                            respondJson(cards.joinToString(",", "[", "]"))
                        }
                    }
                }
            typeAndWait(viewModel, "mario")

            viewModel.loadMore()
            viewModel.loadMore()
            assertEquals(MoreResults.Loading, assertIs<SearchState.Results>(viewModel.state.value).more)
            advanceUntilIdle()

            val results = assertIs<SearchState.Results>(viewModel.state.value)
            assertEquals(25, results.games.size)
            assertEquals(
                results.games.size,
                results.games
                    .map { it.id }
                    .toSet()
                    .size,
            )
            assertEquals(MoreResults.None, results.more)
            assertEquals(listOf("mario:1", "mario:2"), requests, "one request per page")

            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(2, requests.size, "nothing loads after the last page")
        }

    @Test
    fun pagingStopsAtTheApiResultLimit() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            typeAndWait(viewModel, "star")

            repeat(15) {
                viewModel.loadMore()
                advanceUntilIdle()
            }

            val results = assertIs<SearchState.Results>(viewModel.state.value)
            assertEquals(GameSearchRepo.MAX_RESULTS, results.games.size)
            assertEquals(MoreResults.None, results.more)
            assertEquals("star:10", requests.last())
        }

    @Test
    fun aNewQueryCancelsPaging() =
        runTest(dispatcher) {
            val viewModel =
                viewModel { query, page ->
                    if (page == 2) delay(5_000)
                    respondJson(page(query, page, count = 20))
                }
            typeAndWait(viewModel, "mario")
            viewModel.loadMore()
            runCurrent()

            viewModel.onSearch("zelda")
            advanceUntilIdle()

            val results = assertIs<SearchState.Results>(viewModel.state.value)
            assertEquals("zelda", results.query)
            assertEquals(20, results.games.size)
            assertEquals(MoreResults.Available, results.more)
        }

    @Test
    fun aFailedPageKeepsLoadedResultsAndLoadsAgainOnlyOnRetry() =
        runTest(dispatcher) {
            var failPages = true
            val viewModel =
                viewModel { query, page ->
                    if (page == 2 && failPages) {
                        respondJson("{}", HttpStatusCode.TooManyRequests)
                    } else {
                        respondJson(page(query, page, count = 20))
                    }
                }
            typeAndWait(viewModel, "zelda")

            viewModel.loadMore()
            advanceUntilIdle()
            val failed = assertIs<SearchState.Results>(viewModel.state.value)
            assertEquals(20, failed.games.size)
            assertEquals(MoreResults.Failed(DataError.RateLimited), failed.more)

            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(listOf("zelda:1", "zelda:2"), requests, "scrolling does not repeat a failed page")

            failPages = false
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(
                40,
                viewModel.state.value
                    .titles()
                    .size,
            )
        }

    @Test
    fun emptyAndFailedSearchesHaveTheirOwnStates() =
        runTest(dispatcher) {
            var available = false
            val viewModel =
                viewModel { query, page ->
                    when {
                        query == "zzqx" -> respondJson("[]")
                        !available -> respondJson("{}", HttpStatusCode.ServiceUnavailable)
                        else -> respondJson(page(query, page, count = 2))
                    }
                }
            typeAndWait(viewModel, "zzqx")
            assertEquals(SearchState.Empty("zzqx"), viewModel.state.value)

            typeAndWait(viewModel, "celeste")
            assertEquals(SearchState.Error("celeste", DataError.Server), viewModel.state.value)

            available = true
            viewModel.retry()
            assertEquals(SearchState.Loading("celeste"), viewModel.state.value)
            advanceUntilIdle()
            assertEquals(
                2,
                viewModel.state.value
                    .titles()
                    .size,
            )
        }

    @Test
    fun theSearchActionRetriesAFailedSearch() =
        runTest(dispatcher) {
            var available = false
            val viewModel =
                viewModel { query, page ->
                    if (available) {
                        respondJson(page(query, page, count = 1))
                    } else {
                        respondJson("{}", HttpStatusCode.BadGateway)
                    }
                }
            typeAndWait(viewModel, "celeste")
            assertIs<SearchState.Error>(viewModel.state.value)

            available = true
            viewModel.onSearch("celeste")
            advanceUntilIdle()

            assertEquals(listOf("celeste 1.0"), viewModel.state.value.titles())
        }

    @Test
    fun cardsShowTheReleaseYearAndSkipGamesWithoutAnId() =
        runTest(dispatcher) {
            val body =
                """[{"id":7,"name":"Doom","first_release_date":742780800},{"name":"No ID"},""" +
                    """{"id":8,"name":"Doom"}]"""
            val viewModel = viewModel { _, _ -> respondJson(body) }
            typeAndWait(viewModel, "doom")

            val results = assertIs<SearchState.Results>(viewModel.state.value)
            assertEquals(listOf(7, 8), results.games.map { it.id })
            assertEquals(GameCardLabel.ReleaseYear(1993), results.games.first().label)
            assertEquals(null, results.games.last().label)
            assertEquals(MoreResults.None, results.more)
        }
}
