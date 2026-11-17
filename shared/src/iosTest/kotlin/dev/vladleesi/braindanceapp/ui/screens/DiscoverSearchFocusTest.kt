// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
import kotlin.test.assertFalse

/** Search actions (Navigator.openSearch) focus Discover's field; reaching Discover any other way does not. */
@OptIn(ExperimentalTestApi::class)
class DiscoverSearchFocusTest {
    private val navigator =
        Navigator(
            NavigationState(
                startRoute = BottomBarRoute.HomeRoute,
                topLevelRoute = mutableStateOf(BottomBarRoute.DiscoverRoute),
                backStacks = BottomBarRoute.routes.associateWith { NavBackStack<NavKey>(it) },
            ),
        )
    private val viewModel =
        GameSearchViewModel(GameSearchRepo(GamesRemote(mockBackendClient(Dispatchers.Default) { respondJson("[]") })))
    private var focusManager: FocusManager? = null

    private fun ComposeUiTest.setDiscover() {
        setContent {
            focusManager = LocalFocusManager.current
            CompositionLocalProvider(LocalNavigator provides navigator) {
                BraindanceTheme {
                    DiscoverScreen(Modifier, viewModel)
                }
            }
        }
    }

    @Test
    fun openingDiscoverLeavesTheFieldAlone() =
        runComposeUiTest {
            setDiscover()

            onNode(hasSetTextAction()).assertIsNotFocused()
        }

    @Test
    fun aSearchActionOnVisibleDiscoverFocusesTheFieldOnce() =
        runComposeUiTest {
            setDiscover()

            runOnIdle { navigator.openSearch() }
            waitForIdle()

            onNode(hasSetTextAction()).assertIsFocused()
            assertFalse(navigator.isSearchFocusRequested.value)
        }

    @Test
    fun aSearchActionFromAnotherScreenFocusesTheFieldWhenDiscoverAppears() =
        runComposeUiTest {
            navigator.openSearch()

            setDiscover()
            waitForIdle()

            onNode(hasSetTextAction()).assertIsFocused()
            assertFalse(navigator.isSearchFocusRequested.value)
        }

    @Test
    fun aRepeatedSearchActionFocusesTheFieldAgain() =
        runComposeUiTest {
            setDiscover()
            runOnIdle { navigator.openSearch() }
            waitForIdle()
            runOnIdle { focusManager?.clearFocus() }
            waitForIdle()
            onNode(hasSetTextAction()).assertIsNotFocused()

            runOnIdle { navigator.openSearch() }
            waitForIdle()

            onNode(hasSetTextAction()).assertIsFocused()
        }
}
