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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun tappingTheCurrentTabLeavesAScreenOpenedOnIt() {
        val navigator = navigator()
        navigator.navigate(discover)
        navigator.navigate(ProfileRoute)

        navigator.selectTab(discover)

        assertEquals(listOf(discover), navigator.stack(discover))
        assertEquals(discover, navigator.state.topLevelRoute)
    }

    @Test
    fun tappingDiscoverOnItsRootScreenFocusesSearch() {
        val navigator = navigator()
        navigator.navigate(discover)

        navigator.selectTab(discover)

        assertEquals(listOf(discover), navigator.stack(discover))
        assertTrue(navigator.isSearchFocusRequested.value)
    }

    @Test
    fun switchingToDiscoverDoesNotFocusSearch() {
        val navigator = navigator()

        navigator.selectTab(discover)
        navigator.navigate(home)
        navigator.navigate(discover)

        assertEquals(discover, navigator.state.topLevelRoute)
        assertFalse(navigator.isSearchFocusRequested.value)
    }

    @Test
    fun aSearchActionOpensDiscoversRootScreenFromAnyTabAndFocusesSearch() {
        val navigator = navigator()
        navigator.navigate(discover)
        navigator.navigate(GameDetailsRoute(1))
        navigator.navigate(home)
        navigator.navigate(GameDetailsRoute(2))

        navigator.openSearch()

        assertEquals(discover, navigator.state.topLevelRoute)
        assertEquals(listOf(discover), navigator.stack(discover))
        // The tab the search started from keeps its screen, so Back from Discover returns to Home as it was.
        assertEquals(listOf(home, GameDetailsRoute(2)), navigator.stack(home))
        assertTrue(navigator.isSearchFocusRequested.value)
    }

    @Test
    fun aRepeatedSearchActionAsksForFocusAgainOnceTheFieldTookIt() {
        val navigator = navigator()
        navigator.openSearch()
        navigator.onSearchFocused()
        assertFalse(navigator.isSearchFocusRequested.value)

        navigator.openSearch()

        assertEquals(listOf(discover), navigator.stack(discover))
        assertTrue(navigator.isSearchFocusRequested.value)
    }

    @Test
    fun backFromASearchStartedOnAnotherTabReturnsToTheStartTab() {
        val navigator = navigator()
        navigator.openSearch()

        navigator.goBack()

        assertEquals(home, navigator.state.topLevelRoute)
    }

    @Test
    fun tappingAnotherTabKeepsItsStack() {
        val navigator = navigator()
        navigator.navigate(GameDetailsRoute(1))
        navigator.navigate(discover)

        navigator.selectTab(home)

        assertEquals(home, navigator.state.topLevelRoute)
        assertEquals(listOf(home, GameDetailsRoute(1)), navigator.stack(home))
    }

    @Test
    fun switchingVersionsReplacesTheScreenSoBackLeavesTheGame() {
        val navigator = navigator()
        navigator.navigate(GameDetailsRoute(75))

        navigator.replace(GameDetailsRoute(45180))
        navigator.replace(GameDetailsRoute(202174))
        navigator.replace(GameDetailsRoute(75))

        assertEquals(listOf(home, GameDetailsRoute(75)), navigator.stack(home))
        navigator.replace(GameDetailsRoute(75))
        assertEquals(listOf(home, GameDetailsRoute(75)), navigator.stack(home), "the shown version is kept")
        navigator.goBack()
        assertEquals(listOf<NavKey>(home), navigator.stack(home))
    }

    @Test
    fun replacingWithTheScreenUnderneathReturnsToIt() {
        val navigator = navigator()
        navigator.navigate(GameDetailsRoute(1))
        navigator.navigate(GameDetailsRoute(2))

        navigator.replace(GameDetailsRoute(1))

        assertEquals(listOf(home, GameDetailsRoute(1)), navigator.stack(home))
    }

    @Test
    fun aTabsRootAndTabsAreNeverReplaced() {
        val navigator = navigator()
        navigator.replace(GameDetailsRoute(1))
        assertEquals(listOf(home, GameDetailsRoute(1)), navigator.stack(home), "opens on top of the root")

        navigator.replace(discover)
        assertEquals(discover, navigator.state.topLevelRoute)
        assertEquals(listOf(home, GameDetailsRoute(1)), navigator.stack(home))
    }

    @Test
    fun openingTheScreenUnderneathReturnsToItInsteadOfStackingACopy() {
        val navigator = navigator()
        navigator.navigate(GameDetailsRoute(1942))
        navigator.open(GameDetailsRoute(12503))

        navigator.open(GameDetailsRoute(1942))
        assertEquals(listOf(home, GameDetailsRoute(1942)), navigator.stack(home))

        navigator.open(GameDetailsRoute(12503))
        navigator.open(GameDetailsRoute(13166))
        assertEquals(
            listOf(home, GameDetailsRoute(1942), GameDetailsRoute(12503), GameDetailsRoute(13166)),
            navigator.stack(home),
            "other screens open on top",
        )
    }

    @Test
    fun aVersionSwitchKeepsTheScreenAndItsAddressFollowsTheVersion() {
        val navigator = navigator()
        val page = GameDetailsRoute(75)
        navigator.navigate(page)

        navigator.replace(page.withVersion(45180))

        val shown = navigator.stack(home).last() as GameDetailsRoute
        assertEquals(45180, shown.id)
        assertEquals(page.contentKey, shown.contentKey, "one screen: no transition, same state and ViewModel")
        assertEquals("/games/45180", shown.toWebPath())
        assertTrue(shown.isSameScreen(GameDetailsRoute(45180)))
        assertFalse(shown.isSameScreen(page))

        // A link to the version already shown does not open it again.
        navigator.openLink(GameDetailsRoute(45180))
        assertEquals(2, navigator.stack(home).size)
    }
}
