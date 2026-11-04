// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data

import dev.vladleesi.braindanceapp.data.api.REQUEST_TIMEOUT_MILLIS
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Repositories turn backend answers into typed outcomes, so callers never see Ktor responses or exceptions. */
class RepositoryOutcomeTest {
    private val dispatcher = StandardTestDispatcher()

    private fun homeRepo(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HomeRepo(GamesRemote(mockBackendClient(dispatcher, handler)))

    private suspend fun trendingError(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        assertIs<Outcome.Failure>(homeRepo(handler).trendingNow()).error

    @Test
    fun successfulAnswersAreTypedValues() =
        runTest(dispatcher) {
            val outcome = homeRepo { respondJson("""[{"id":7,"name":"A"}]""") }.trendingNow()
            assertEquals(listOf(7), assertIs<Outcome.Success<List<TrendingGameItem>>>(outcome).value.map { it.id })
        }

    @Test
    fun statusesMapToDataErrors() =
        runTest(dispatcher) {
            assertEquals(DataError.NotFound, trendingError { respondJson("{}", HttpStatusCode.NotFound) })
            assertEquals(DataError.RateLimited, trendingError { respondJson("{}", HttpStatusCode.TooManyRequests) })
            // 503 is retried by the client first; the error describes the final answer.
            assertEquals(DataError.Server, trendingError { respondJson("{}", HttpStatusCode.ServiceUnavailable) })
            assertEquals(DataError.Server, trendingError { respondJson("{}", HttpStatusCode.InternalServerError) })
            assertEquals(DataError.Unknown, trendingError { respondJson("{}", HttpStatusCode.BadRequest) })
        }

    @Test
    fun transportFailuresMapToDataErrors() =
        runTest(dispatcher) {
            assertEquals(
                DataError.Timeout,
                trendingError {
                    delay(REQUEST_TIMEOUT_MILLIS * 2)
                    respondJson("[]")
                },
            )
            assertEquals(DataError.Network, trendingError { throw IOException("connection refused") })
            assertEquals(DataError.Unknown, trendingError { respondJson("""{"not":"a list"}""") })
        }

    @Test
    fun emptyAnswersForOneItemAreNotFound() =
        runTest(dispatcher) {
            val client =
                mockBackendClient(dispatcher) {
                    respondJson(
                        if (it.url.encodedPath ==
                            "/v1/games/details"
                        ) {
                            "[]"
                        } else {
                            "null"
                        },
                    )
                }
            assertEquals(
                DataError.NotFound,
                assertIs<Outcome.Failure>(GameDetailsRepo(GamesRemote(client)).gameDetails(1)).error,
            )
            assertEquals(
                DataError.NotFound,
                assertIs<Outcome.Failure>(GamerPowerRepo(GamerPowerRemote(client)).giveaway(1)).error,
            )
        }

    @Test
    fun giveawaysAreCutToThePageSize() =
        runTest(dispatcher) {
            val client = mockBackendClient(dispatcher) { respondJson("""[{"id":1},{"id":2},{"id":3}]""") }
            val outcome = GamerPowerRepo(GamerPowerRemote(client)).giveaways(pageSize = 2)
            assertEquals(listOf(1, 2), assertIs<Outcome.Success<List<GiveawayResponse>>>(outcome).value.map { it.id })
        }
}
