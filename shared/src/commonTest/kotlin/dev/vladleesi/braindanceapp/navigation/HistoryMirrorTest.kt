// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryMirrorTest {
    /** Session history as a browser keeps it: entries and the current position. */
    private class FakeBrowser : BrowserLocation {
        data class Entry(
            val path: String,
            val index: Int,
            val snapshot: String,
        )

        val entries = mutableListOf(Entry("/", -1, ""))
        var position = 0
        var backCalls = 0

        val current get() = entries[position]

        override fun read() = current.path

        override fun currentIndex() = current.index.takeIf { it >= 0 }

        override fun push(
            path: String,
            index: Int,
            snapshotJson: String,
        ) {
            while (entries.size > position + 1) entries.removeAt(entries.lastIndex)
            entries.add(Entry(path, index, snapshotJson))
            position += 1
        }

        override fun replace(
            path: String,
            index: Int,
            snapshotJson: String,
        ) {
            entries[position] = Entry(path, index, snapshotJson)
        }

        override fun back() {
            backCalls += 1
            position -= 1
        }

        override fun observe(onChange: (path: String?, index: Int?, snapshotJson: String?) -> Unit): () -> Unit = {}

        /** The Back button: the browser moves, then the page receives popstate. */
        fun pressBack(
            mirror: HistoryMirror,
            appSnapshot: String,
        ): String? {
            position -= 1
            return mirror.onBrowserMoved(current.index, current.snapshot, appSnapshot)
        }
    }

    @Test
    fun backFromAnotherTabReturnsToTheOpenedGameAndThenHome() {
        val browser = FakeBrowser()
        val mirror = HistoryMirror(browser)
        mirror.start("home", "/")
        mirror.onNavigated("home>game", "/game/1")
        mirror.onNavigated("profile|home>game", "/profile")
        assertEquals(listOf("/", "/game/1", "/profile"), browser.entries.map { it.path })

        // Back restores the stored state; applying it must not add an entry.
        val first = browser.pressBack(mirror, appSnapshot = "profile|home>game")
        assertEquals("home>game", first)
        mirror.onNavigated("home>game", "/game/1")

        val second = browser.pressBack(mirror, appSnapshot = "home>game")
        assertEquals("home", second)
        mirror.onNavigated("home", "/")

        assertEquals(3, browser.entries.size, "Back never adds history entries")
        assertEquals(0, browser.position, "the next Back leaves the site, after Home")
    }

    @Test
    fun anInAppBackStepMovesTheBrowserBackToo() {
        val browser = FakeBrowser()
        val mirror = HistoryMirror(browser)
        mirror.start("home", "/")
        mirror.onNavigated("home>game", "/game/1")

        mirror.onNavigated("home", "/")

        assertEquals(1, browser.backCalls)
        assertEquals(0, browser.position)
        assertNull(mirror.onBrowserMoved(0, "home", currentSnapshotJson = "home"), "already showing that entry")
    }

    @Test
    fun aNewNavigationAfterGoingBackDropsTheForwardEntries() {
        val browser = FakeBrowser()
        val mirror = HistoryMirror(browser)
        mirror.start("home", "/")
        mirror.onNavigated("home>game", "/game/1")
        browser.pressBack(mirror, appSnapshot = "home>game")
        mirror.onNavigated("home", "/")

        mirror.onNavigated("search", "/search")

        assertEquals(listOf("/", "/search"), browser.entries.map { it.path })
    }

    @Test
    fun restoreAppliesEveryTabAndTheSelectedTab() {
        val tabs: List<NavKey> = BottomBarRoute.routes
        val state =
            NavigationState(
                startRoute = BottomBarRoute.HomeRoute,
                topLevelRoute = mutableStateOf(BottomBarRoute.LibraryRoute),
                backStacks = tabs.associateWith { NavBackStack(it) },
            )

        state.restore(
            NavigationSnapshot(
                NavigationSnapshot.VERSION,
                BottomBarRoute.HomeRoute,
                listOf(listOf(BottomBarRoute.HomeRoute, GameDetailsRoute(1))),
            ),
        )

        assertEquals(BottomBarRoute.HomeRoute, state.topLevelRoute)
        assertEquals(
            listOf(BottomBarRoute.HomeRoute, GameDetailsRoute(1)),
            state.backStacks.getValue(BottomBarRoute.HomeRoute).toList(),
        )
        assertEquals(
            listOf<NavKey>(BottomBarRoute.LibraryRoute),
            state.backStacks.getValue(BottomBarRoute.LibraryRoute).toList(),
        )
    }
}
