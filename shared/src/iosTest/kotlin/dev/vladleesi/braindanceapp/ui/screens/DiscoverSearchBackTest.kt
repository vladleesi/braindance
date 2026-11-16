// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.NavigationState
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DiscoverSearchBackTest {
    @Test
    fun backLeavesSearchBeforeItLeavesTheScreen() =
        runComposeUiTest {
            val dispatcher = NavigationEventDispatcher()
            val owner =
                object : NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = dispatcher
                }
            val back = DirectNavigationEventInput().also(dispatcher::addInput)
            val navigator =
                Navigator(
                    NavigationState(
                        startRoute = BottomBarRoute.HomeRoute,
                        topLevelRoute = mutableStateOf(BottomBarRoute.DiscoverRoute),
                        backStacks = BottomBarRoute.routes.associateWith { NavBackStack<NavKey>(it) },
                    ),
                )
            var screenBacks = 0
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
                        DiscoverScreen(Modifier)
                    }
                }
            }

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
}
