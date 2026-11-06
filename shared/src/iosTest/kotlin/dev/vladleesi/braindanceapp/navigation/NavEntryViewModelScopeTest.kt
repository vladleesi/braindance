// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.ui.components.NavigationScaffold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Drives the app's own navigation (state, navigator, per-tab entry decorators, NavDisplay) with screens that each
 * create a ViewModel, and records when those ViewModels are cleared.
 */
@OptIn(ExperimentalTestApi::class)
class NavEntryViewModelScopeTest {
    private class ScreenViewModel(
        val name: String,
        private val cleared: MutableList<String>,
    ) : ViewModel() {
        override fun onCleared() {
            cleared += name
        }
    }

    /** The host owners that an Activity or a ComposeUIViewController would otherwise provide. */
    private class HostOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

        init {
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }
    }

    private class Recorder {
        val created = mutableListOf<ScreenViewModel>()
        val cleared = mutableListOf<String>()
        val scrollStates = mutableMapOf<String, ScrollState>()
        var width by mutableStateOf(599)
        lateinit var navigator: Navigator
    }

    private fun NavKey.screenName() =
        (this as? GameDetailsRoute)?.let { "game:${it.id}" } ?: this::class.simpleName.orEmpty()

    @Composable
    private fun App(recorder: Recorder) {
        val host = remember { HostOwner() }
        val windowInfo =
            remember {
                object : WindowInfo {
                    override val isWindowFocused = true
                    override val containerSize get() = IntSize(recorder.width, 844)
                }
            }
        CompositionLocalProvider(
            LocalLifecycleOwner provides host,
            LocalViewModelStoreOwner provides host,
            LocalSavedStateRegistryOwner provides host,
            LocalDensity provides Density(1f),
            LocalWindowInfo provides windowInfo,
        ) {
            val state = rememberNavigationState(BottomBarRoute.HomeRoute, BottomBarRoute.routes.toSet())
            val navigator = remember { Navigator(state).also { recorder.navigator = it } }
            CompositionLocalProvider(LocalNavigator provides navigator) {
                NavigationScaffold { modifier ->
                    NavDisplay(
                        modifier = modifier,
                        entries =
                            state.toEntries { key ->
                                NavEntry(key) {
                                    val screen =
                                        viewModel {
                                            ScreenViewModel(
                                                key.screenName(),
                                                recorder.cleared,
                                            ).also(recorder.created::add)
                                        }
                                    val scrollState = rememberScrollState()
                                    recorder.scrollStates[screen.name] = scrollState
                                    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                                        repeat(100) { BasicText("${screen.name}:$it") }
                                    }
                                }
                            },
                        onBack = { navigator.goBack() },
                    )
                }
            }
        }
    }

    @Test
    fun poppingAScreenClearsItsViewModel() =
        runComposeUiTest {
            val recorder = Recorder()
            setContent { App(recorder) }

            runOnIdle { recorder.navigator.navigate(GameDetailsRoute(1)) }
            waitForIdle()
            assertEquals(listOf("HomeRoute", "game:1"), recorder.created.map { it.name })

            runOnIdle { recorder.navigator.goBack() }
            waitForIdle()
            assertEquals(listOf("game:1"), recorder.cleared, "the popped screen's ViewModel is cleared, Home's is kept")
        }

    @Test
    fun theSameScreenInTwoTabsHasItsOwnViewModel() =
        runComposeUiTest {
            val recorder = Recorder()
            setContent { App(recorder) }

            runOnIdle { recorder.navigator.navigate(GameDetailsRoute(1)) }
            waitForIdle()
            runOnIdle { recorder.navigator.navigate(BottomBarRoute.DiscoverRoute) }
            waitForIdle()
            runOnIdle { recorder.navigator.navigate(GameDetailsRoute(1)) }
            waitForIdle()
            val games = recorder.created.filter { it.name == "game:1" }
            assertEquals(2, games.size)
            assertNotSame(games[0], games[1])

            // Popping the game in Discover must not clear the one still open in Home.
            runOnIdle { recorder.navigator.goBack() }
            waitForIdle()
            assertEquals(listOf("game:1"), recorder.cleared)
            runOnIdle { recorder.navigator.navigate(BottomBarRoute.HomeRoute) }
            waitForIdle()
            assertEquals(
                2,
                recorder.created.count { it.name == "game:1" },
                "Home's game ViewModel was reused, not recreated",
            )
        }

    @Test
    fun resizingAcrossBreakpointKeepsEntriesAndScrollPositions() =
        runComposeUiTest {
            val recorder = Recorder()
            setContent { App(recorder) }
            onAllNodes(isSelectable()).assertCountEquals(5)
            val homeBottomBounds = onNodeWithText("Home").fetchSemanticsNode().boundsInRoot
            val newsBottomBounds = onNodeWithText("News").fetchSemanticsNode().boundsInRoot
            assertEquals(homeBottomBounds.top, newsBottomBounds.top)

            runOnIdle { recorder.navigator.navigate(GameDetailsRoute(1)) }
            waitForIdle()
            val detailViewModel = recorder.created.last()
            val detailScroll = recorder.scrollStates.getValue("game:1")
            runOnIdle { detailScroll.dispatchRawDelta(100f) }
            assertEquals(100, detailScroll.value)
            for (width in listOf(600, 1200, 599, 600)) {
                runOnIdle { recorder.width = width }
                waitForIdle()
                onAllNodes(isSelectable()).assertCountEquals(5)
                onNodeWithText("Home").assertIsSelected()
                assertSame(detailScroll, recorder.scrollStates.getValue("game:1"))
                assertEquals(100, detailScroll.value)
                val homeBounds = onNodeWithText("Home").fetchSemanticsNode().boundsInRoot
                val newsBounds = onNodeWithText("News").fetchSemanticsNode().boundsInRoot
                if (width >= 600) {
                    assertEquals(homeBounds.left, newsBounds.left)
                    assertTrue(homeBounds.bottom <= newsBounds.top)
                } else {
                    assertEquals(homeBounds.top, newsBounds.top)
                    assertTrue(homeBounds.right <= newsBounds.left)
                }
            }

            onNodeWithText("News").performClick()
            runOnIdle { recorder.width = 599 }
            onNodeWithText("News").assertIsSelected()
            runOnIdle { recorder.width = 600 }
            onNodeWithText("News").assertIsSelected()
            onNodeWithText("Home").performClick()
            waitForIdle()
            assertSame(detailViewModel, recorder.created.single { it.name == "game:1" })
            assertEquals(100, recorder.scrollStates.getValue("game:1").value)
            assertEquals(
                listOf(BottomBarRoute.HomeRoute, GameDetailsRoute(1)),
                recorder.navigator.state.backStacks[BottomBarRoute.HomeRoute]
                    ?.toList(),
            )
            assertTrue(recorder.cleared.isEmpty())

            runOnIdle { recorder.navigator.goBack() }
            waitForIdle()
            assertEquals(listOf("game:1"), recorder.cleared)
        }

    @Test
    fun bothNavigationLayoutsKeepTabReselectionBehavior() =
        runComposeUiTest {
            val recorder = Recorder()
            setContent { App(recorder) }
            for (width in listOf(599, 600)) {
                runOnIdle {
                    recorder.width = width
                    recorder.navigator.navigate(BottomBarRoute.HomeRoute)
                    recorder.navigator.navigate(GameDetailsRoute(1))
                }
                waitForIdle()
                onNodeWithText("Home").performClick()
                runOnIdle {
                    assertEquals(
                        listOf(BottomBarRoute.HomeRoute),
                        recorder.navigator.state.backStacks[BottomBarRoute.HomeRoute]
                            ?.toList(),
                    )
                }

                onNodeWithText("Discover").performClick()
                runOnIdle {
                    recorder.navigator.setResult(BottomBarRoute.DiscoverRoute.RESULT_KEY, false)
                    recorder.navigator.navigate(GameDetailsRoute(2))
                }
                waitForIdle()
                onNodeWithText("Discover").performClick()
                runOnIdle {
                    assertEquals(
                        GameDetailsRoute(2),
                        recorder.navigator.state.backStacks[BottomBarRoute.DiscoverRoute]
                            ?.last(),
                    )
                    assertEquals(
                        true,
                        runBlocking { recorder.navigator.resultsFlow(BottomBarRoute.DiscoverRoute.RESULT_KEY).first() },
                    )
                }
            }
        }
}
