// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.fixtures

import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.api.clients.BackendClient
import dev.vladleesi.braindanceapp.data.api.remote.GamerPowerRemote
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.imageHttpClient
import dev.vladleesi.braindanceapp.utils.largeCoverUrlAtOrNull
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import dev.vladleesi.braindanceapp.utils.toSafeExternalUrl
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** Fixture builds serve every screen offline from sample data that follows the API contract. */
class ApiFixtureEngineTest {
    private val dispatcher = StandardTestDispatcher()
    private val client =
        BackendClient.build(
            debugHttpLogging = false,
            baseUrl = API_FIXTURES_BASE_URL,
            engine = createApiFixtureEngine(dispatcher),
        )
    private val homeRepo = HomeRepo(GamesRemote(client))
    private val detailsRepo = GameDetailsRepo(GamesRemote(client))
    private val giveawaysRepo = GamerPowerRepo(GamerPowerRemote(client))

    @Test
    fun everyListedGameOpensWithRecommendations() =
        runTest(dispatcher) {
            val trendingIds = homeRepo.trendingNow().successValue().map { it.id }
            val anticipatedIds = homeRepo.mostAnticipated(pageSize = ALL).successValue().mapNotNull { it.id }
            assertTrue(trendingIds.isNotEmpty() && anticipatedIds.isNotEmpty())
            for (id in (trendingIds + anticipatedIds).distinct()) {
                assertEquals(id, detailsRepo.gameDetails(id).successValue().id)
                val recommendations = detailsRepo.recommendations(id).successValue()
                assertTrue(recommendations.isNotEmpty() && recommendations.none { it.id == id })
                recommendations.forEach { game ->
                    val recommendedId = game.id ?: fail("Recommendation without an ID")
                    detailsRepo.gameDetails(recommendedId).successValue()
                }
            }
        }

    @Test
    fun anticipatedFeedHonoursPageSize() =
        runTest(dispatcher) {
            assertEquals(3, homeRepo.mostAnticipated(pageSize = 3).successValue().size)
        }

    @Test
    fun everyListedGiveawayOpens() =
        runTest(dispatcher) {
            val giveaways = giveawaysRepo.giveaways(pageSize = ALL).successValue()
            assertTrue(giveaways.isNotEmpty())
            giveaways.forEach { giveaway ->
                val id = giveaway.id ?: fail("Giveaway without an ID")
                assertEquals(giveaway, giveawaysRepo.giveaway(id).successValue())
            }
        }

    @Test
    fun unknownItemsAreNotFound() =
        runTest(dispatcher) {
            assertEquals(DataError.NotFound, detailsRepo.gameDetails(UNKNOWN_ID).failureError())
            assertEquals(DataError.NotFound, giveawaysRepo.giveaway(UNKNOWN_ID).failureError())
            assertEquals(emptyList(), detailsRepo.recommendations(UNKNOWN_ID).successValue())
        }

    @Test
    fun invalidRequestsAreRejected() =
        runTest(dispatcher) {
            suspend fun status(path: String) = client.get(path) { expectSuccess = false }.status
            assertEquals(HttpStatusCode.BadRequest, status("v1/games/details"))
            assertEquals(
                HttpStatusCode.BadRequest,
                client
                    .get("v1/games/details") {
                        expectSuccess = false
                        parameter("id", "-1")
                    }.status,
            )
            assertEquals(HttpStatusCode.BadRequest, status("v1/giveaways/abc"))
            assertEquals(HttpStatusCode.NotFound, status("v1/unknown"))
            assertEquals(HttpStatusCode.MethodNotAllowed, client.post("v1/giveaways") { expectSuccess = false }.status)
        }

    @Test
    fun everyImageIsServedOffline() =
        runTest(dispatcher) {
            val imageClient = imageHttpClient(engine = createApiFixtureEngine(dispatcher))
            val imageUrls = (coverUrls() + giveawayImageUrls()).distinct()
            assertTrue(imageUrls.isNotEmpty())
            imageUrls.forEach { url ->
                assertEquals(API_FIXTURES_HOST, Url(url).host, url)
                val response = imageClient.get(url)
                assertEquals(HttpStatusCode.OK, response.status, url)
                assertEquals(ContentType.Image.PNG, response.contentType())
                assertTrue(response.bodyAsBytes().take(PNG_SIGNATURE.size) == PNG_SIGNATURE, url)
            }
        }

    @Test
    fun otherHostsNeverReachTheNetwork() =
        runTest(dispatcher) {
            val imageClient = imageHttpClient(engine = createApiFixtureEngine(dispatcher))
            val realCover = "https://images.igdb.com/igdb/image/upload/t_720p/cover-101.webp"
            assertEquals(HttpStatusCode.NotFound, imageClient.get(realCover).status)
        }

    @Test
    fun externalLinksAreSafeAndPointAtTheReservedExampleDomain() =
        runTest(dispatcher) {
            val gameLinks =
                homeRepo.trendingNow().successValue().flatMap { game ->
                    detailsRepo
                        .gameDetails(game.id)
                        .successValue()
                        .websites
                        .orEmpty()
                        .mapNotNull { it.url }
                }
            val giveawayLinks =
                giveawaysRepo.giveaways(pageSize = ALL).successValue().flatMap { giveaway ->
                    listOfNotNull(giveaway.openGiveawayUrl, giveaway.gamerpowerUrl, giveaway.openGiveaway)
                }
            assertTrue(gameLinks.isNotEmpty() && giveawayLinks.isNotEmpty())
            (gameLinks + giveawayLinks).forEach { link ->
                assertEquals(link, link.toSafeExternalUrl())
                assertEquals("example.com", Url(link).host, link)
            }
        }

    // Covers as the app requests them: the card size, plus the smaller sizes it can substitute.
    private suspend fun coverUrls(): List<String> {
        val anticipated = homeRepo.mostAnticipated(pageSize = ALL).successValue().mapNotNull { it.cover?.url }
        val trending = homeRepo.trendingNow().successValue().mapNotNull { it.cover?.url }
        return (anticipated + trending).flatMap { url ->
            val cardUrl = url.toCoverUrl(CoverSize.P_720)
            listOf(cardUrl) +
                listOf(CoverSize.COVER_SMALL_2X, CoverSize.COVER_BIG).mapNotNull(cardUrl::largeCoverUrlAtOrNull)
        }
    }

    private suspend fun giveawayImageUrls(): List<String> =
        giveawaysRepo.giveaways(pageSize = ALL).successValue().flatMap { listOfNotNull(it.image, it.thumbnail) }

    private fun <T> Outcome<T>.successValue(): T =
        when (this) {
            is Outcome.Success -> value
            is Outcome.Failure -> fail("Expected success, got $error")
        }

    private fun Outcome<*>.failureError(): DataError =
        when (this) {
            is Outcome.Success -> fail("Expected a failure, got $value")
            is Outcome.Failure -> error
        }

    private companion object {
        const val ALL = 1_000
        const val UNKNOWN_ID = 999_999
        val PNG_SIGNATURE = listOf<Byte>(-119, 80, 78, 71, 13, 10, 26, 10)
    }
}
