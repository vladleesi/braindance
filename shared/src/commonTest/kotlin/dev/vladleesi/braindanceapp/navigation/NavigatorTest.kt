// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    private val home = BottomBarRoute.HomeRoute
    private val discover = BottomBarRoute.DiscoverRoute

    private fun navigator() =
        Navigator(
            NavigationState(
                startRoute = home,
                topLevelRoute = mutableStateOf(home),
                backStacks = BottomBarRoute.routes.associateWith { NavBackStack<NavKey>(it) },
            ),
        )

    private fun Navigator.stack(tab: NavKey) = state.backStacks.getValue(tab).toList()

    @Test
    fun profileOpenedFromAnotherTabMovesThere() {
        val navigator = navigator()
        navigator.navigate(GameDetailsRoute(1))
        navigator.navigate(ProfileRoute)
        navigator.navigate(discover)

        navigator.navigate(ProfileRoute)

        assertEquals(listOf(home, GameDetailsRoute(1)), navigator.stack(home))
        assertEquals(listOf(discover, ProfileRoute), navigator.stack(discover))
    }

    @Test
    fun profileIsNeverStackedOnItself() {
        val navigator = navigator()
        navigator.navigate(ProfileRoute)

        navigator.navigate(ProfileRoute)

        assertEquals(listOf(home, ProfileRoute), navigator.stack(home))
    }

    @Test
    fun otherScreensCanStillOpenAgain() {
        val navigator = navigator()
        navigator.navigate(GameDetailsRoute(1))
        navigator.navigate(GameDetailsRoute(1))

        assertEquals(listOf(home, GameDetailsRoute(1), GameDetailsRoute(1)), navigator.stack(home))
    }
}
