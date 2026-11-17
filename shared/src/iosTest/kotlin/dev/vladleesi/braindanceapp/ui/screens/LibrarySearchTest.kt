// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.NavigationState
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LibrarySearchTest {
    @Test
    fun librarysSearchButtonStartsAGameSearch() =
        runComposeUiTest {
            val navigator =
                Navigator(
                    NavigationState(
                        startRoute = BottomBarRoute.HomeRoute,
                        topLevelRoute = mutableStateOf(BottomBarRoute.LibraryRoute),
                        backStacks = BottomBarRoute.routes.associateWith { NavBackStack<NavKey>(it) },
                    ),
                )
            setContent {
                CompositionLocalProvider(LocalNavigator provides navigator) {
                    BraindanceTheme { LibraryScreen(Modifier) }
                }
            }

            onNodeWithContentDescription("Search games").assertIsEnabled().performClick()

            assertEquals(BottomBarRoute.DiscoverRoute, navigator.state.topLevelRoute)
            assertTrue(navigator.isSearchFocusRequested.value)
        }
}
