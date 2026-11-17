// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.viewmodels.PopularGamesViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import kotlinx.coroutines.Dispatchers
import kotlin.concurrent.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CompilationScreenTest {
    @Test
    fun seeAllNamesItsSectionAndOpensItsList() =
        runComposeUiTest {
            var opened = 0
            setContent {
                BraindanceTheme {
                    CarouselSection(
                        title = "Popular Right Now",
                        state = SectionState.Success(listOf(GameCardModel(1, "Sample game", ""))),
                        onRetry = {},
                        skeleton = {},
                        onSeeAll = { opened++ },
                    ) { games ->
                        items(games, key = { it.id }) { game ->
                            GameCard(
                                game,
                                onClick = {},
                                showTitle = true,
                                modifier = Modifier.width(Dimens.gameCardWidth),
                            )
                        }
                    }
                }
            }

            onNodeWithContentDescription("See all Popular Right Now").performClick()

            assertEquals(1, opened)
        }

    @Test
    fun theListLoadsTheNextPageNearItsEndKeepsTheLoadedGamesAndGoesBack() =
        runComposeUiTest {
            // The pages the mock backend served, in order; the engine runs off the main thread.
            val pages = AtomicReference(emptyList<Int>())
            val viewModel =
                PopularGamesViewModel(
                    HomeRepo(
                        GamesRemote(
                            mockBackendClient(Dispatchers.Default) { request ->
                                val page = request.url.parameters["page"]?.toInt() ?: 1
                                do {
                                    val served = pages.value
                                } while (!pages.compareAndSet(served, served + page))
                                respondJson(
                                    (0 until PAGE_SIZE).joinToString(",", "[", "]") { index ->
                                        """{"id":${page * 1000 + index},"name":"Game $page.$index"}"""
                                    },
                                )
                            },
                        ),
                    ),
                )
            var backs = 0
            setContent {
                BraindanceTheme {
                    GameCompilation(
                        compilation = Compilation.Popular,
                        title = "Popular Right Now",
                        viewModel = viewModel,
                        onBack = { backs++ },
                        onGameClick = {},
                        modifier = Modifier,
                    )
                }
            }
            waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
                onAllNodesWithText("Game 1.0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(PAGE_SIZE - 1)
            waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
                onAllNodesWithText("Game 2.0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }

            // Each page is requested once, in order; a tall window may already have asked for the one after.
            val served = pages.value
            assertEquals(served.distinct(), served)
            assertEquals(listOf(1, 2), served.take(2))
            // The first page stays loaded above the second.
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            onAllNodesWithText("Game 1.0", useUnmergedTree = true)[0].assertExists()
            onNodeWithContentDescription("Button: Back").performClick()
            assertEquals(1, backs)
        }

    private companion object {
        const val PAGE_SIZE = HomeRepo.POPULAR_PAGE_SIZE
        const val TIMEOUT_MILLIS = 5_000L
    }
}
