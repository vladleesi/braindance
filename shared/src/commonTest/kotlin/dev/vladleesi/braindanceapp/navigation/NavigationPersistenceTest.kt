// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigationPersistenceTest {
    private val tabs = BottomBarRoute.routes
    private val tabSet = tabs.toSet()

    private val stacks: Map<NavKey, List<NavKey>> =
        tabs.associateWith { listOf(it) } +
            mapOf(
                BottomBarRoute.HomeRoute to
                    listOf(BottomBarRoute.HomeRoute, GameDetailsRoute(1942), GameDetailsRoute(1020)),
                BottomBarRoute.NewsRoute to listOf(BottomBarRoute.NewsRoute, GiveawayDetailsRoute(7)),
            )

    @Test
    fun restoresSelectedTabAndEveryBackStack() {
        val snapshotJson = encodeNavigationSnapshot(BottomBarRoute.NewsRoute, tabs, stacks)
        val snapshot = decodeNavigationSnapshot(snapshotJson, tabSet)

        assertEquals(BottomBarRoute.NewsRoute, snapshot?.topLevelRoute)
        for (tab in tabs) {
            assertEquals(stacks.getValue(tab), snapshot?.backStack(tab), "$tab")
        }
    }

    @Test
    fun encodingIsDeterministic() {
        val reordered = stacks.entries.reversed().associate { it.key to it.value }
        assertEquals(
            encodeNavigationSnapshot(BottomBarRoute.HomeRoute, tabs, stacks),
            encodeNavigationSnapshot(BottomBarRoute.HomeRoute, tabs, reordered),
        )
    }

    @Test
    fun rejectsMissingForeignAndInconsistentData() {
        val valid = encodeNavigationSnapshot(BottomBarRoute.HomeRoute, tabs, stacks)
        val invalid =
            listOf(
                null,
                "",
                "not json",
                "x".repeat(100_001),
                // Written by a version with another format or a route that no longer exists.
                valid.replace("\"version\":1", "\"version\":2"),
                valid.replace(GameDetailsRoute.serializer().descriptor.serialName, "removed.Route"),
                // Edited or corrupted storage must not crash startup.
                valid.substringBefore("\"backStacks\"") + "\"backStacks\":[[]]}",
                """{"version":1,"topLevelRoute":null,"backStacks":null}""",
                "[]",
            )
        for (snapshotJson in invalid) {
            assertNull(decodeNavigationSnapshot(snapshotJson, tabSet), snapshotJson?.take(40))
        }
        // A detail screen cannot be a tab, and a stack must start with its tab route.
        val detailTab = encodeNavigationSnapshot(GameDetailsRoute(1), listOf(GameDetailsRoute(1)), stacks)
        assertNull(decodeNavigationSnapshot(detailTab, tabSet))
        val tabInsideStack =
            encodeNavigationSnapshot(
                BottomBarRoute.HomeRoute,
                tabs,
                stacks + (BottomBarRoute.HomeRoute to listOf(BottomBarRoute.HomeRoute, BottomBarRoute.NewsRoute)),
            )
        assertNull(decodeNavigationSnapshot(tabInsideStack, tabSet))
    }
}
