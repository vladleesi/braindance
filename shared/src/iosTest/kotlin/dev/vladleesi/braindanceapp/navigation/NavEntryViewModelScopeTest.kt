// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

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
        lateinit var navigator: Navigator
    }

    private fun NavKey.screenName() =
        (this as? GameDetailsRoute)?.let { "game:${it.id}" } ?: this::class.simpleName.orEmpty()

    @Composable
    private fun App(recorder: Recorder) {
        val host = remember { HostOwner() }
        CompositionLocalProvider(
            LocalLifecycleOwner provides host,
            LocalViewModelStoreOwner provides host,
            LocalSavedStateRegistryOwner provides host,
        ) {
            val state = rememberNavigationState(BottomBarRoute.HomeRoute, BottomBarRoute.routes.toSet())
            val navigator = remember { Navigator(state).also { recorder.navigator = it } }
            NavDisplay(
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
                            BasicText(screen.name)
                        }
                    },
                onBack = { navigator.goBack() },
            )
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
            runOnIdle { recorder.navigator.navigate(BottomBarRoute.SearchRoute) }
            waitForIdle()
            runOnIdle { recorder.navigator.navigate(GameDetailsRoute(1)) }
            waitForIdle()
            val games = recorder.created.filter { it.name == "game:1" }
            assertEquals(2, games.size)
            assertNotSame(games[0], games[1])

            // Popping the game in Search must not clear the one still open in Home.
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
}
