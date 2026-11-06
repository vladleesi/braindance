// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class WebPathTest {
    private val tabs: Set<NavKey> = BottomBarRoute.routes.toSet()
    private val home = BottomBarRoute.HomeRoute

    @Test
    fun everyRouteRoundTripsThroughItsPath() {
        val routes = tabs + listOf(ProfileRoute, GameDetailsRoute(42), GiveawayDetailsRoute(7))
        for (route in routes) {
            assertEquals(route, webPathToRoute(route.toWebPath()), route.toWebPath())
        }
        assertEquals(
            listOf("/", "/discover", "/releases", "/news", "/library", "/profile"),
            (BottomBarRoute.routes + ProfileRoute).map { it.toWebPath() },
        )
        assertEquals("/game/42", GameDetailsRoute(42).toWebPath())
        assertEquals("/giveaway/7", GiveawayDetailsRoute(7).toWebPath())
    }

    @Test
    fun unknownOrInvalidAddressesAreRejected() {
        for (path in listOf(
            "/search",
            "/collections",
            "/game/0",
            "/game/-1",
            "/game/abc",
            "/game/",
            "/game/42/x",
            "/giveaway/1e3",
            "/unknown",
        )) {
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
        val linked = applyDeepLink(null, BottomBarRoute.LibraryRoute, home, tabs)
        assertEquals(BottomBarRoute.LibraryRoute, linked?.topLevelRoute)
        assertEquals(listOf(BottomBarRoute.LibraryRoute), linked?.backStack(BottomBarRoute.LibraryRoute))
    }

    @Test
    fun aLinkedProfileOpensOnTopOfTheCurrentTab() {
        val restored =
            NavigationSnapshot(
                NavigationSnapshot.VERSION,
                BottomBarRoute.NewsRoute,
                listOf(listOf(home), listOf(BottomBarRoute.NewsRoute)),
            )
        val linked = applyDeepLink(restored, webPathToRoute("/profile"), home, tabs)
        assertEquals(BottomBarRoute.NewsRoute, linked?.topLevelRoute)
        assertEquals(listOf(BottomBarRoute.NewsRoute, ProfileRoute), linked?.backStack(BottomBarRoute.NewsRoute))
    }

    @Test
    fun aLinkedProfileOpenInAnotherTabMovesToTheCurrentTab() {
        val restored =
            NavigationSnapshot(
                NavigationSnapshot.VERSION,
                BottomBarRoute.NewsRoute,
                listOf(listOf(home, ProfileRoute), listOf(BottomBarRoute.NewsRoute)),
            )
        val linked = applyDeepLink(restored, ProfileRoute, home, tabs)
        assertEquals(listOf<NavKey>(home), linked?.backStack(home))
        assertEquals(listOf(BottomBarRoute.NewsRoute, ProfileRoute), linked?.backStack(BottomBarRoute.NewsRoute))
    }

    @Test
    fun aTabAddressOpensThatTabAtItsRoot() {
        val restored = NavigationSnapshot(NavigationSnapshot.VERSION, home, listOf(listOf(home, GameDetailsRoute(1))))
        val linked = applyDeepLink(restored, home, home, tabs)
        assertEquals(listOf<NavKey>(home), linked?.backStack(home))
    }
}
