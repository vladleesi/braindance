// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.SingleInstanceRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(
    val state: NavigationState,
) {
    fun navigate(route: NavKey) {
        val backStacks = state.backStacks
        val currentTopLevel = state.topLevelRoute

        when (route) {
            in backStacks.keys -> {
                if (route == currentTopLevel) {
                    // Double-tap on current tab, reset to root (only keep the tab's root route)
                    backStacks[route]?.clear()
                    backStacks[route]?.add(route)
                } else {
                    // Switch to another top-level tab
                    state.topLevelRoute = route
                }
            }

            // Already showing it; a single-instance screen is never stacked on itself.
            is SingleInstanceRoute if backStacks[currentTopLevel]?.lastOrNull() == route -> Unit
            else -> {
                if (route is SingleInstanceRoute) {
                    // Opening it from another tab moves it here rather than keeping a second copy.
                    backStacks.values.forEach { stack -> stack.removeAll { it == route } }
                }
                // Navigate deeper in current tab
                backStacks[currentTopLevel]?.add(route)
            }
        }
    }

    /**
     * Opens a screen from a link, as an address typed into the browser does: a tab's link switches to that tab, and
     * any other screen opens on top of the current tab, unless it is already showing there.
     */
    fun openLink(route: NavKey) {
        if (route !in state.backStacks.keys && state.backStacks[state.topLevelRoute]?.lastOrNull() == route) return
        navigate(route)
    }

    /**
     * Handles a tap on a tab in the bottom bar, rail, or sidebar. Another tab opens with its own stack as it was left.
     * Tapping the current tab returns it to its root screen; on Discover's root screen, it focuses the search field
     * instead, as [openSearch] does.
     */
    fun selectTab(route: BottomBarRoute) {
        val isAtRoot = state.backStacks[route]?.singleOrNull() == route
        if (route == state.topLevelRoute && route is BottomBarRoute.DiscoverRoute && isAtRoot) {
            openSearch()
        } else {
            navigate(route)
        }
    }

    private val searchFocusRequest = MutableStateFlow(false)

    /** True from [openSearch] until Discover's search field has taken focus and reports it with [onSearchFocused]. */
    val isSearchFocusRequested: StateFlow<Boolean> = searchFocusRequest.asStateFlow()

    /**
     * The one way every search action starts a search: shows Discover's root screen, whatever tab or screen is open,
     * and asks its search field to take focus, which opens the software keyboard on touch devices. Discover stays a
     * single screen, so repeating the action only focuses the field again.
     */
    fun openSearch() {
        val discover = BottomBarRoute.DiscoverRoute
        state.backStacks[discover]?.let { stack ->
            if (stack.singleOrNull() != discover) {
                stack.clear()
                stack.add(discover)
            }
        }
        state.topLevelRoute = discover
        searchFocusRequest.value = true
    }

    /** Discover's search field took the focus [openSearch] asked for. */
    fun onSearchFocused() {
        searchFocusRequest.value = false
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}

val LocalNavigator =
    staticCompositionLocalOf<Navigator> {
        error("No Navigator provided")
    }
