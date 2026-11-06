// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.navigation.routes.SingleInstanceRoute

/**
 * The browser address bar and session history, where one exists. Paths are app routes such as `/game/42`, relative
 * to the page's base path. Platforms without one provide `null`.
 */
internal interface BrowserLocation {
    /** The current route path, or null when the address has none. */
    fun read(): String?

    /** The index this app stored on the current history entry, or null for an entry it did not create. */
    fun currentIndex(): Int?

    /** Adds a history entry for [path] holding the app's [snapshotJson] at [index]. */
    fun push(
        path: String,
        index: Int,
        snapshotJson: String,
    )

    /** Rewrites the current history entry for [path] with the app's [snapshotJson] at [index]. */
    fun replace(
        path: String,
        index: Int,
        snapshotJson: String,
    )

    /** Moves the browser one entry back, as its Back button does. */
    fun back()

    /**
     * Calls [onChange] when the user moves through history or edits the address, with the new path and the index and
     * navigation snapshot stored on that entry, both null for an entry this app did not create. Returns a function
     * that stops observing.
     */
    fun observe(onChange: (path: String?, index: Int?, snapshotJson: String?) -> Unit): () -> Unit
}

internal expect val browserLocation: BrowserLocation?

private const val GAME_PREFIX = "/game/"
private const val GIVEAWAY_PREFIX = "/giveaway/"
private val fixedPaths =
    mapOf<NavKey, String>(
        BottomBarRoute.HomeRoute to "/",
        BottomBarRoute.DiscoverRoute to "/discover",
        BottomBarRoute.ReleasesRoute to "/releases",
        BottomBarRoute.NewsRoute to "/news",
        BottomBarRoute.LibraryRoute to "/library",
        ProfileRoute to "/profile",
    )

/** The address of [this] route; equal routes always produce the same path. Unknown routes map to Home. */
internal fun NavKey.toWebPath(): String =
    when (this) {
        is GameDetailsRoute -> "$GAME_PREFIX$id"
        is GiveawayDetailsRoute -> "$GIVEAWAY_PREFIX$id"
        else -> fixedPaths[this] ?: "/"
    }

/** The route at [path], or null for an address this app does not serve, such as an invalid or non-positive ID. */
internal fun webPathToRoute(path: String?): NavKey? {
    val normalized = path?.takeIf { it.isNotEmpty() } ?: return BottomBarRoute.HomeRoute
    fixedPaths.entries.firstOrNull { it.value == normalized }?.let { return it.key }

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
 * screen is already [linked]; a tab's address opens that tab at its root; any other screen, such as a game or
 * Profile, is opened on top of the current tab. Without a link, or for an unknown one, [snapshot] is used as it is.
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
        // A tab's address is that tab's root screen, so its stack starts over at the root.
        val withTab = stacks.filterNot { it.first() == linked } + listOf(listOf(linked))
        return NavigationSnapshot(NavigationSnapshot.VERSION, linked, withTab)
    }
    // A single-instance screen open in another tab moves to the current one.
    val others = if (linked is SingleInstanceRoute) stacks.map { stack -> stack.filterNot { it == linked } } else stacks
    val opened = others.map { if (it.first() == topLevel) it + linked else it }
    val withCurrent = if (opened.any { it.first() == topLevel }) opened else opened + listOf(current + linked)
    return NavigationSnapshot(NavigationSnapshot.VERSION, topLevel, withCurrent)
}

/**
 * Mirrors app navigation in browser history. Every in-app navigation, including a tab switch, adds an entry that
 * stores the whole navigation snapshot; Back and Forward restore the stored snapshot exactly and never add entries.
 * Browsers skip entries a page adds without a user action, so adding one while handling Back would desynchronize the
 * two histories. An in-app step back to the previous entry's state moves the browser back too, keeping them aligned.
 */
internal class HistoryMirror(
    private val location: BrowserLocation,
) {
    // Snapshots of this page's entries by index; entries from before a reload are filled in as the user visits them.
    private val entries = mutableListOf<String?>()
    private var index = 0
    private var restoring: String? = null

    fun start(
        snapshotJson: String,
        path: String,
    ) {
        index = location.currentIndex() ?: 0
        store(index, snapshotJson)
        location.replace(path, index, snapshotJson)
    }

    /** The app's navigation changed to [snapshotJson], showing [path]. */
    fun onNavigated(
        snapshotJson: String,
        path: String,
    ) {
        if (snapshotJson == restoring) {
            restoring = null
            return
        }
        if (snapshotJson == entries.getOrNull(index)) return
        if (index > 0 && entries[index - 1] == snapshotJson) {
            index -= 1
            location.back()
            return
        }
        index += 1
        truncateAfter(index - 1)
        store(index, snapshotJson)
        location.push(path, index, snapshotJson)
    }

    /**
     * The browser moved to an entry this app created. Returns the snapshot the app must now show, or null when it
     * already shows it.
     */
    fun onBrowserMoved(
        entryIndex: Int,
        snapshotJson: String,
        currentSnapshotJson: String,
    ): String? {
        index = entryIndex
        store(index, snapshotJson)
        if (snapshotJson == currentSnapshotJson) return null
        restoring = snapshotJson
        return snapshotJson
    }

    /** Takes over a new entry the browser created itself, such as an edited address, for the app's current state. */
    fun adopt(
        snapshotJson: String,
        path: String,
    ) {
        index += 1
        truncateAfter(index - 1)
        store(index, snapshotJson)
        location.replace(path, index, snapshotJson)
    }

    private fun store(
        at: Int,
        snapshotJson: String,
    ) {
        while (entries.size <= at) entries.add(null)
        entries[at] = snapshotJson
    }

    private fun truncateAfter(last: Int) {
        while (entries.size > last + 1) entries.removeAt(entries.lastIndex)
    }
}

/** Keeps the browser address and history in step with navigation, where a browser exists; see [HistoryMirror]. */
@Composable
internal fun BrowserHistorySync(
    state: NavigationState,
    navigator: Navigator,
) {
    val location = browserLocation ?: return
    val currentNavigator = rememberUpdatedState(navigator)
    val mirror = remember(location) { HistoryMirror(location) }
    val snapshot = { encodeNavigationSnapshot(state.topLevelRoute, state.backStacks.keys, state.backStacks) }
    val path = { (state.backStacks[state.topLevelRoute]?.lastOrNull() ?: state.startRoute).toWebPath() }
    DisposableEffect(state, location) {
        mirror.start(snapshot(), path())
        val dispose =
            location.observe { changedPath, entryIndex, entrySnapshot ->
                if (entryIndex != null && entrySnapshot != null) {
                    mirror
                        .onBrowserMoved(entryIndex, entrySnapshot, snapshot())
                        ?.let { decodeNavigationSnapshot(it, state.backStacks.keys) }
                        ?.let(state::restore)
                } else {
                    // An address the user typed or an old link: open its route on a history entry of its own.
                    webPathToRoute(changedPath)?.let { route ->
                        currentNavigator.value.navigate(route)
                        mirror.adopt(snapshot(), path())
                    }
                }
            }
        onDispose { dispose() }
    }
    LaunchedEffect(state, location) {
        snapshotFlow { snapshot() to path() }.collect { (snapshotJson, routePath) ->
            mirror.onNavigated(snapshotJson, routePath)
        }
    }
}
