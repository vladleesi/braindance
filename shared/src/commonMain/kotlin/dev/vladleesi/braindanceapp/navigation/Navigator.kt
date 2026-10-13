// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.logger.BLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(
    val state: NavigationState,
) {
    fun navigate(route: NavKey) {
        val backStacks = state.backStacks
        val currentTopLevel = state.topLevelRoute

        when {
            route in backStacks.keys -> {
                if (route == currentTopLevel) {
                    // Double-tap on current tab, reset to root (only keep the tab's root route)
                    backStacks[route]?.clear()
                    backStacks[route]?.add(route)
                } else {
                    // Switch to another top-level tab
                    state.topLevelRoute = route
                }
            }

            else -> {
                // Navigate deeper in current tab
                backStacks[currentTopLevel]?.add(route)
            }
        }
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

    private val resultsFlow = MutableStateFlow<Map<String, Any?>>(emptyMap())

    fun setResult(
        key: String,
        value: Any?,
    ) {
        resultsFlow.update { it + (key to value) }
    }

    /**
     * Observes updates for [key] and calls [onResult] on each emission.
     */
    suspend fun observeResult(
        key: String,
        onResult: (Any?) -> Unit,
    ) {
        resultsFlow(key)
            .collect(onResult)
    }

    /**
     * Returns a flow that emits values for [key] whenever they change.
     */
    fun resultsFlow(key: String) =
        resultsFlow
            .mapNotNull { it[key] }
            .catch { BLogger.error(throwable = it, message = "Result observation failed for key: $key") }
}

val LocalNavigator =
    staticCompositionLocalOf<Navigator> {
        error("No Navigator provided")
    }
