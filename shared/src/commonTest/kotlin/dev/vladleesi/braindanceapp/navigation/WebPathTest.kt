// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class WebPathTest {
    private val tabs: Set<NavKey> = BottomBarRoute.routes.toSet()
    private val home = BottomBarRoute.HomeRoute

    @Test
    fun everyRouteRoundTripsThroughItsPath() {
        val routes = tabs + listOf(GameDetailsRoute(42), GiveawayDetailsRoute(7))
        for (route in routes) {
            assertEquals(route, webPathToRoute(route.toWebPath()), route.toWebPath())
        }
        assertEquals("/game/42", GameDetailsRoute(42).toWebPath())
        assertEquals("/", home.toWebPath())
    }

    @Test
    fun unknownOrInvalidAddressesAreRejected() {
        for (path in listOf("/game/0", "/game/-1", "/game/abc", "/game/", "/game/42/x", "/giveaway/1e3", "/unknown")) {
            assertNull(webPathToRoute(path), path)
        }
        assertEquals(home, webPathToRoute(null))
        assertEquals(home, webPathToRoute(""))
    }

    @Test
    fun aReloadKeepsTheRestoredNavigation() {
        val restored = NavigationSnapshot(NavigationSnapshot.VERSION, home, listOf(listOf(home, GameDetailsRoute(1))))
        assertSame(restored, applyDeepLink(restored, GameDetailsRoute(1), home, tabs))
        assertSame(restored, applyDeepLink(restored, null, home, tabs))
    }

    @Test
    fun aLinkedDetailScreenOpensOnTopOfTheCurrentTab() {
        val restored = NavigationSnapshot(NavigationSnapshot.VERSION, home, listOf(listOf(home)))
        val linked = applyDeepLink(restored, GiveawayDetailsRoute(9), home, tabs)
        assertEquals(listOf(home, GiveawayDetailsRoute(9)), linked?.backStack(home))

        val fresh = applyDeepLink(null, GameDetailsRoute(5), home, tabs)
        assertEquals(home, fresh?.topLevelRoute)
        assertEquals(listOf(home, GameDetailsRoute(5)), fresh?.backStack(home))
    }

    @Test
    fun aLinkedTabOpensThatTab() {
        val linked = applyDeepLink(null, BottomBarRoute.ProfileRoute, home, tabs)
        assertEquals(BottomBarRoute.ProfileRoute, linked?.topLevelRoute)
        assertEquals(listOf(BottomBarRoute.ProfileRoute), linked?.backStack(BottomBarRoute.ProfileRoute))
    }
}
