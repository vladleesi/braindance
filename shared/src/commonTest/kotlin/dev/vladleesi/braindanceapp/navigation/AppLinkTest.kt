// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.routes.CompilationRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinkTest {
    private val home = BottomBarRoute.HomeRoute
    private val tabs: Set<NavKey> = BottomBarRoute.routes.toSet()

    @Test
    fun linksToTheWebAppOpenTheSameScreenAsInABrowser() {
        assertEquals(GameDetailsRoute(42), appLinkRoute("https://app.braindancegg.com/games/42"))
        assertEquals(GameDetailsRoute(42), appLinkRoute("https://app.braindancegg.com/game/42"))
        assertEquals(GiveawayDetailsRoute(7), appLinkRoute("https://app.braindancegg.com/giveaway/7"))
        assertEquals(CompilationRoute(Compilation.Popular), appLinkRoute("https://app.braindancegg.com/popular"))
        assertEquals(BottomBarRoute.DiscoverRoute, appLinkRoute("https://app.braindancegg.com/discover"))
        assertEquals(home, appLinkRoute("https://app.braindancegg.com/"))
        assertEquals(home, appLinkRoute("https://app.braindancegg.com"))
    }

    @Test
    fun queriesFragmentsAndLetterCaseOfTheOriginDoNotMatter() {
        assertEquals(GameDetailsRoute(42), appLinkRoute("https://app.braindancegg.com/games/42?utm_source=share"))
        assertEquals(GameDetailsRoute(42), appLinkRoute("https://app.braindancegg.com/games/42#media"))
        assertEquals(GameDetailsRoute(42), appLinkRoute("HTTPS://App.BraindanceGG.com/games/42"))
        assertEquals(home, appLinkRoute("https://app.braindancegg.com?ref=landing"))
    }

    @Test
    fun linksToOtherSitesOrInsecureOnesAreIgnored() {
        for (url in listOf(
            "http://app.braindancegg.com/games/42",
            "braindance://games/42",
            "https://braindancegg.com/games/42",
            "https://app.braindancegg.com.example.com/games/42",
            "https://app.braindancegg.com:8443/games/42",
            "https://app.braindancegg.com@example.com/games/42",
            "https://user@app.braindancegg.com/games/42",
            "https://example.com/https://app.braindancegg.com/games/42",
            "javascript:alert(1)",
            "",
        )) {
            assertNull(appLinkRoute(url), url)
        }
        assertNull(appLinkRoute(null))
    }

    @Test
    fun invalidOrOutdatedPathsOpenNothing() {
        for (path in listOf("/games/0", "/games/abc", "/games/%34%32", "/GAMES/42", "/games/42/", "/unknown")) {
            assertNull(appLinkRoute("https://app.braindancegg.com$path"), path)
        }
        assertNull(appLinkRoute("https://app.braindancegg.com/games/" + "1".repeat(3000)))
    }

    @Test
    fun aLaunchLinkOpensOnTopOfTheStartScreenSoBackReturnsHome() {
        val snapshot = applyDeepLink(null, appLinkRoute("https://app.braindancegg.com/games/42"), home, tabs)
        assertEquals(home, snapshot?.topLevelRoute)
        assertEquals(listOf(home, GameDetailsRoute(42)), snapshot?.backStack(home))
    }

    @Test
    fun aLaunchWithoutAUsableLinkStartsNormally() {
        assertNull(applyDeepLink(null, appLinkRoute("https://example.com/games/42"), home, tabs))
    }

    @Test
    fun aLinkWhileRunningOpensOnTopOfTheCurrentTabOnce() {
        val navigator = navigator()
        navigator.navigate(BottomBarRoute.LibraryRoute)

        navigator.openLink(GameDetailsRoute(42))
        navigator.openLink(GameDetailsRoute(42))

        assertEquals(BottomBarRoute.LibraryRoute, navigator.state.topLevelRoute)
        assertEquals(
            listOf(BottomBarRoute.LibraryRoute, GameDetailsRoute(42)),
            navigator.state.backStacks
                .getValue(BottomBarRoute.LibraryRoute)
                .toList(),
        )
    }

    @Test
    fun aTabLinkWhileRunningSwitchesToThatTab() {
        val navigator = navigator()

        navigator.openLink(BottomBarRoute.NewsRoute)

        assertEquals(BottomBarRoute.NewsRoute, navigator.state.topLevelRoute)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun theInboxKeepsTheNewestLinkUntilTheAppReadsIt() =
        runTest(UnconfinedTestDispatcher()) {
            val inbox = AppLinkInbox()
            inbox.open("https://app.braindancegg.com/games/1")
            inbox.open("https://app.braindancegg.com/games/2")

            assertEquals("https://app.braindancegg.com/games/2", inbox.received.first())

            val received = mutableListOf<String>()
            val collecting = launch { inbox.received.collect(received::add) }
            inbox.open("https://app.braindancegg.com/games/3")
            inbox.open("https://app.braindancegg.com/games/4")
            collecting.cancel()
            assertEquals(
                listOf("https://app.braindancegg.com/games/3", "https://app.braindancegg.com/games/4"),
                received,
            )
        }

    private fun navigator() =
        Navigator(
            NavigationState(
                startRoute = home,
                topLevelRoute = mutableStateOf(home),
                backStacks = BottomBarRoute.routes.associateWith { NavBackStack<NavKey>(it) },
            ),
        )
}
