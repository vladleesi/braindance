// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.vladleesi.braindanceapp.data.api.remote.GamesRemote
import dev.vladleesi.braindanceapp.data.repository.GameSearchRepo
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.NavigationState
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.testing.mockBackendClient
import dev.vladleesi.braindanceapp.testing.respondJson
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.viewmodels.GameSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DiscoverSearchBackTest {
    private val dispatcher = NavigationEventDispatcher()
    private val back = DirectNavigationEventInput().also(dispatcher::addInput)
    private var screenBacks = 0

    // A page of games released in 2020, so the year labels identify result cards and the grid can scroll.
    private val viewModel =
        GameSearchViewModel(
            GameSearchRepo(
                GamesRemote(
                    mockBackendClient(Dispatchers.Default) {
                        respondJson(
                            (1..20).joinToString(",", "[", "]") { id ->
                                val name = if (id == 1) "Sample Hades" else "Sample Game $id"
                                """{"id":$id,"name":"$name","first_release_date":1600000000}"""
                            },
                        )
                    },
                ),
            ),
        )

    private fun ComposeUiTest.setDiscover() {
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        val navigator =
            Navigator(
                NavigationState(
                    startRoute = BottomBarRoute.HomeRoute,
                    topLevelRoute = mutableStateOf(BottomBarRoute.DiscoverRoute),
                    backStacks = BottomBarRoute.routes.associateWith { NavBackStack<NavKey>(it) },
                ),
            )
        setContent {
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides owner,
                LocalNavigator provides navigator,
            ) {
                BraindanceTheme {
                    // Stands in for NavDisplay, which handles back for the screen.
                    NavigationBackHandler(
                        state = rememberNavigationEventState(NavigationEventInfo.None),
                        onBackCompleted = { screenBacks++ },
                    )
                    DiscoverScreen(Modifier, viewModel)
                }
            }
        }
    }

    @Test
    fun backLeavesSearchBeforeItLeavesTheScreen() =
        runComposeUiTest {
            setDiscover()

            onNode(hasSetTextAction()).performTextInput("Hades")
            waitForIdle()
            back.backCompleted()
            waitForIdle()

            assertEquals(0, screenBacks)
            onNodeWithText("Search games").assertIsDisplayed()
            onNodeWithText("Discover").assertIsDisplayed()

            back.backCompleted()
            waitForIdle()

            assertEquals(1, screenBacks)
        }

    @Test
    fun resultsReplaceDiscoverUntilTheSearchIsCleared() =
        runComposeUiTest {
            setDiscover()
            onNodeWithText("What should I play next?").assertIsDisplayed()

            onNode(hasSetTextAction()).performTextInput("Hades")
            waitUntil(timeoutMillis = RESULT_TIMEOUT_MILLIS) {
                onAllNodesWithText("2020").fetchSemanticsNodes().isNotEmpty()
            }
            // Without covers, each tile shows the title as well as the line under it.
            assertEquals(2, onAllNodesWithText("Sample Hades", useUnmergedTree = true).fetchSemanticsNodes().size)
            assertEquals(0, onAllNodesWithText("What should I play next?").fetchSemanticsNodes().size)

            onNodeWithContentDescription("Clear search").performClick()
            waitForIdle()

            onNodeWithText("What should I play next?").assertIsDisplayed()
            assertEquals(0, onAllNodesWithText("2020").fetchSemanticsNodes().size)
        }

    @Test
    fun clearingASubmittedSearchStartsOverInsteadOfLeavingIt() =
        runComposeUiTest {
            setDiscover()
            val field = onNode(hasSetTextAction())
            field.performTextInput("Hades")
            field.performImeAction()
            waitForIdle()
            field.assertIsNotFocused()

            onNodeWithContentDescription("Clear search").performClick()
            waitForIdle()

            // Clear keeps the user in search, ready to type. Only back (or the chevron) leaves it.
            field.assertIsFocused()
            // Only the typed text: the node's text also holds the placeholder.
            field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            onNodeWithContentDescription("Close search").assertIsDisplayed()

            back.backCompleted()
            waitForIdle()
            field.assertIsNotFocused()
            assertEquals(0, screenBacks)
        }

    @Test
    fun aTouchScrollThroughResultsPutsTheKeyboardAwayAndKeepsTheSearch() =
        runComposeUiTest {
            setDiscover()
            val field = onNode(hasSetTextAction())
            field.performTextInput("Hades")
            waitUntil(timeoutMillis = RESULT_TIMEOUT_MILLIS) {
                onAllNodesWithText("2020").fetchSemanticsNodes().isNotEmpty()
            }
            field.assertIsFocused()

            onAllNodesWithText("Sample Hades")[0].performTouchInput { swipeUp() }
            waitForIdle()

            field.assertIsNotFocused()
            field.assertTextEquals("Hades")
        }

    private companion object {
        const val RESULT_TIMEOUT_MILLIS = 5_000L
    }
}
