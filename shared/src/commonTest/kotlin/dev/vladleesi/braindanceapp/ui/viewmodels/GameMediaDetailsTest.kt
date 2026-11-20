// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.testing.ViewModelTest
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.ui.components.media.GameMedia
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The details page's media gallery, from the media the details response already carries. */
class GameMediaDetailsTest : ViewModelTest() {
    private val thumbs = "//images.igdb.com/igdb/image/upload/t_thumb"

    private val gameMedia =
        """"videos":[{"id":1,"video_id":"bbbbbbbbbbb"},{"id":2,"video_id":"not a video"},""" +
            """{"id":3,"video_id":"aaaaaaaaaaa"}],""" +
            """"screenshots":[{"id":4,"url":"$thumbs/sc2.jpg"},{"id":5,"url":null},""" +
            """{"id":6,"animated":true,"url":"$thumbs/sc1.jpg"}]"""

    private suspend fun TestScope.details(response: String): GameDetails {
        var detailsRequests = 0
        val client =
            mockBackendClient(dispatcher) { request ->
                when (request.url.encodedPath) {
                    "/v1/games/details" -> {
                        detailsRequests++
                        respondJson(response)
                    }
                    else -> respondJson("{}", HttpStatusCode.InternalServerError)
                }
            }
        val viewModel = GameDetailsViewModel(gameId = 42, gameDetailsRepo = GameDetailsRepo(GamesRemote(client)))
        viewModel.load()
        advanceUntilIdle()
        // The gallery comes with the details: no request of its own.
        assertEquals(1, detailsRequests)
        return assertIs<GameDetailsState.Success>(viewModel.gameDetailsState.value).gameDetails
    }

    @Test
    fun theGalleryShowsValidVideosAndThenScreenshotsInTheApiOrder() =
        runTest(dispatcher) {
            val media = details("""[{"id":42,"name":"Game",$gameMedia}]""").media

            assertEquals(
                listOf(
                    GameMedia.YouTubeVideo("bbbbbbbbbbb"),
                    GameMedia.YouTubeVideo("aaaaaaaaaaa"),
                    GameMedia.Screenshot("https:$thumbs/sc2.jpg"),
                    GameMedia.Screenshot("https:$thumbs/sc1.jpg"),
                ),
                media.items,
            )
        }

    @Test
    fun aGameWithoutMediaHasAnEmptyGallery() =
        runTest(dispatcher) {
            assertTrue(details("""[{"id":42,"name":"Game"}]""").media.isEmpty)
            assertTrue(details("""[{"id":42,"name":"Game","videos":[],"screenshots":[{"url":""}]}]""").media.isEmpty)
        }

    @Test
    fun anEditionWithoutMediaShowsItsGamesAndOneWithMediaShowsOnlyItsOwn() =
        runTest(dispatcher) {
            val game = """{"id":1942,"name":"Game","kind":"game",$gameMedia}"""

            val withoutMedia =
                details("""[{"id":42,"name":"Edition","kind":"edition","canonical_id":1942,"canonical":$game}]""")
            assertEquals(4, withoutMedia.media.items.size)

            val withMedia =
                details(
                    """[{"id":42,"name":"Edition","kind":"edition","canonical_id":1942,""" +
                        """"videos":[{"video_id":"ccccccccccc"}],"canonical":$game}]""",
                )
            assertEquals(listOf(GameMedia.YouTubeVideo("ccccccccccc")), withMedia.media.items)
        }
}
