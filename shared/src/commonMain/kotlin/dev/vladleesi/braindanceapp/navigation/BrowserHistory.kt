// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute

/**
 * The browser address bar, where one exists. Paths are the part after `#`, such as `/game/42`. Platforms without
 * one provide `null`.
 */
internal interface BrowserLocation {
    /** The current path, or null when the address has none. */
    fun read(): String?

    /** Adds a history entry for [path], so the browser's Back button returns to the previous one. */
    fun push(path: String)

    /** Replaces the current history entry's [path]. */
    fun replace(path: String)

    /**
     * Calls [onChange] with the new path when the user goes back or forward or edits the address. Returns a function
     * that stops observing.
     */
    fun observe(onChange: (String?) -> Unit): () -> Unit
}

internal expect val browserLocation: BrowserLocation?

private const val GAME_PREFIX = "/game/"
private const val GIVEAWAY_PREFIX = "/giveaway/"
private val topLevelPaths =
    mapOf<NavKey, String>(
        BottomBarRoute.HomeRoute to "/",
        BottomBarRoute.NewsRoute to "/news",
        BottomBarRoute.SearchRoute to "/search",
        BottomBarRoute.CollectionsRoute to "/collections",
        BottomBarRoute.ProfileRoute to "/profile",
    )

/** The address of [this] route; equal routes always produce the same path. Unknown routes map to Home. */
internal fun NavKey.toWebPath(): String =
    when (this) {
        is GameDetailsRoute -> "$GAME_PREFIX$id"
        is GiveawayDetailsRoute -> "$GIVEAWAY_PREFIX$id"
        else -> topLevelPaths[this] ?: "/"
    }

/** The route at [path], or null for an address this app does not serve, such as an invalid or non-positive ID. */
internal fun webPathToRoute(path: String?): NavKey? {
    val normalized = path?.takeIf { it.isNotEmpty() } ?: return BottomBarRoute.HomeRoute
    topLevelPaths.entries.firstOrNull { it.value == normalized }?.let { return it.key }

    fun id(prefix: String) =
        normalized
            .removePrefix(prefix)
            .takeIf { normalized.startsWith(prefix) && it.all(Char::isDigit) }
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
    id(GAME_PREFIX)?.let { return GameDetailsRoute(it) }
    id(GIVEAWAY_PREFIX)?.let { return GiveawayDetailsRoute(it) }
    return null
}

/**
 * The navigation to start with when the page opens at [linked]. A reload keeps the restored [snapshot] if its current
 * screen is already [linked]; a tab opens that tab; a detail screen is opened on top of the current tab. Without a
 * link, or for an unknown one, [snapshot] is used as it is.
 */
internal fun applyDeepLink(
    snapshot: NavigationSnapshot?,
    linked: NavKey?,
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationSnapshot? {
    linked ?: return snapshot
    val topLevel = snapshot?.topLevelRoute ?: startRoute
    val stacks = snapshot?.backStacks ?: listOf(listOf(startRoute))
    val current = snapshot?.backStack(topLevel) ?: listOf(topLevel)
    if (current.last() == linked) return snapshot
    if (linked in topLevelRoutes) {
        val withTab = if (stacks.any { it.first() == linked }) stacks else stacks + listOf(listOf(linked))
        return NavigationSnapshot(NavigationSnapshot.VERSION, linked, withTab)
    }
    val opened = stacks.map { if (it.first() == topLevel) it + linked else it }
    val withCurrent = if (opened.any { it.first() == topLevel }) opened else opened + listOf(current + linked)
    return NavigationSnapshot(NavigationSnapshot.VERSION, topLevel, withCurrent)
}

/**
 * Keeps the browser address and history in step with navigation, where a browser exists. Moving deeper adds a history
 * entry; other changes replace the current one. Back, forward, and edited addresses navigate the app.
 */
@Composable
internal fun BrowserHistorySync(
    state: NavigationState,
    navigator: Navigator,
) {
    val location = browserLocation ?: return
    val currentNavigator = rememberUpdatedState(navigator)
    LaunchedEffect(state, location) {
        var previousDepth = -1
        snapshotFlow { state.backStacks[state.topLevelRoute].orEmpty().let { it.lastOrNull() to it.size } }
            .collect { (route, depth) ->
                val path = (route ?: state.startRoute).toWebPath()
                if (path != location.read()) {
                    if (previousDepth in 1 until depth) location.push(path) else location.replace(path)
                }
                previousDepth = depth
            }
    }
    DisposableEffect(state, location) {
        val dispose =
            location.observe { path ->
                val route = webPathToRoute(path) ?: return@observe
                val stack = state.backStacks[state.topLevelRoute].orEmpty()
                when {
                    stack.lastOrNull() == route -> Unit
                    stack.size >= 2 && stack[stack.size - 2] == route -> currentNavigator.value.goBack()
                    else -> currentNavigator.value.navigate(route)
                }
            }
        onDispose { dispose() }
    }
}
