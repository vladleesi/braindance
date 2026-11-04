// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.PolymorphicSerializer

/**
 * Create a navigation state that persists config changes and process death, and browser page reloads on the web.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    // Only used as initial values; platform saved state, where it exists, still takes precedence. In a browser, the
    // address the page opened at wins over the stored navigation, so shared and bookmarked links open their screen.
    val restored =
        remember(topLevelRoutes) {
            val stored = decodeNavigationSnapshot(navigationPersistence?.read(), topLevelRoutes)
            val linked = browserLocation?.let { webPathToRoute(it.read()) }
            applyDeepLink(stored, linked, startRoute, topLevelRoutes)
        }

    val topLevelRoute =
        rememberSerializable(
            startRoute,
            topLevelRoutes,
            stateSerializer = PolymorphicSerializer(NavKey::class),
            configuration = SavedStateConfiguration { serializersModule = navSerializersModule },
        ) {
            mutableStateOf(restored?.topLevelRoute ?: startRoute)
        }

    val backStacks =
        topLevelRoutes.associateWith { key ->
            rememberNavBackStack(
                configuration =
                    SavedStateConfiguration {
                        serializersModule = navSerializersModule
                    },
                elements = (restored?.backStack(key) ?: listOf(key)).toTypedArray(),
            )
        }

    val state =
        remember(startRoute, topLevelRoutes) {
            NavigationState(startRoute, topLevelRoute, backStacks)
        }
    navigationPersistence?.let { persistence ->
        LaunchedEffect(state, persistence) {
            // snapshotFlow skips unchanged text, so only real navigation changes are written.
            snapshotFlow {
                encodeNavigationSnapshot(state.topLevelRoute, topLevelRoutes, state.backStacks)
            }.collect(persistence::write)
        }
    }
    return state
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    /** Shows [snapshot] exactly: every tab's back stack and the selected tab, as Back and Forward in a browser need. */
    internal fun restore(snapshot: NavigationSnapshot) {
        for ((tab, stack) in backStacks) {
            val target = snapshot.backStack(tab) ?: listOf(tab)
            if (stack.toList() != target) {
                stack.clear()
                stack.addAll(target)
            }
        }
        topLevelRoute = snapshot.topLevelRoute
    }

    val stacksInUse: List<NavKey>
        get() =
            if (topLevelRoute == startRoute) {
                listOf(startRoute)
            } else {
                listOf(startRoute, topLevelRoute)
            }
}

/**
 * Convert NavigationState into NavEntries.
 */
@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries =
        backStacks.mapValues { (topLevelRoute, stack) ->
            // Keyed per tab, so each tab's saved state and ViewModels stay tied to that tab rather than to its position
            // in this loop; the same route open in two tabs keeps two ViewModels (see NavEntryViewModelScopeTest).
            key(topLevelRoute) {
                val decorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                        // Gives each entry its own ViewModelStore, cleared when the entry is popped.
                        rememberViewModelStoreNavEntryDecorator<NavKey>(),
                    )
                rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators = decorators,
                    entryProvider = entryProvider,
                )
            }
        }

    return stacksInUse
        .flatMap { decoratedEntries[it].orEmpty() }
        .toMutableStateList()
}
