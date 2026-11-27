// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.routes.CompilationRoute
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.navigation.routes.SingleInstanceRoute

/**
 * The browser address bar and session history, where one exists. Paths are app routes such as `/games/42`, relative
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

    /**
     * Names the page at [path], such as a game's name, for the browser tab and its history entries. Null forgets the
     * name.
     */
    fun setTitle(
        path: String,
        title: String?,
    )
}

internal expect val browserLocation: BrowserLocation?

private const val GAME_PREFIX = "/games/"

// Game links published before the `/games/` address; still opened, then rewritten to the current address.
private const val LEGACY_GAME_PREFIX = "/game/"
private const val GIVEAWAY_PREFIX = "/giveaway/"
private const val EVENTS_PATH = "/events"
private const val EVENTS_PREFIX = "$EVENTS_PATH/"

// Each fixed path also needs a page in webPages (webApp/build.gradle.kts), and the ID paths a route in the Web app's
// _routes.json and _worker.js, so Cloudflare Pages answers them with the app instead of a 404.
private val fixedPaths =
    mapOf<NavKey, String>(
        EventsRoute() to EVENTS_PATH,
        BottomBarRoute.HomeRoute to "/",
        BottomBarRoute.DiscoverRoute to "/discover",
        BottomBarRoute.ReleasesRoute to "/releases",
        BottomBarRoute.NewsRoute to "/news",
        BottomBarRoute.LibraryRoute to "/library",
        ProfileRoute to "/profile",
        CompilationRoute(Compilation.Popular) to "/popular",
        CompilationRoute(Compilation.Anticipated) to "/anticipated",
        CompilationRoute(Compilation.Giveaways) to "/giveaways",
    )

/** The address of [this] route; equal routes always produce the same path. Unknown routes map to Home. */
internal fun NavKey.toWebPath(): String =
    when (this) {
        is EventsRoute -> id?.let { "$EVENTS_PREFIX$it" } ?: EVENTS_PATH
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
    (id(GAME_PREFIX) ?: id(LEGACY_GAME_PREFIX))?.let { return GameDetailsRoute(it) }
    id(EVENTS_PREFIX)?.let { return EventsRoute(it) }
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
    if (current.last().isSameScreen(linked)) return snapshot
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

    // The open overlay's entry and what closes the overlay; see openOverlay.
    private var overlayIndex = -1
    private var closeOverlay: (() -> Unit)? = null

    fun start(
        snapshotJson: String,
        path: String,
    ) {
        index = location.currentIndex() ?: 0
        store(index, snapshotJson)
        location.replace(path, index, snapshotJson)
    }

    /**
     * The app's navigation changed to [snapshotJson], showing [path]. A [replaced] screen ([Navigator.replace])
     * rewrites the current entry instead of adding one, so Back skips the screen it replaced, as in the app.
     */
    fun onNavigated(
        snapshotJson: String,
        path: String,
        replaced: Boolean = false,
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
        if (replaced) {
            store(index, snapshotJson)
            location.replace(path, index, snapshotJson)
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
        if (closeOverlay != null && entryIndex < overlayIndex) {
            // Back from the overlay's entry: close the overlay; the screen under it stays.
            val close = closeOverlay
            closeOverlay = null
            overlayIndex = -1
            close?.invoke()
        }
        index = entryIndex
        store(index, snapshotJson)
        if (snapshotJson == currentSnapshotJson) return null
        restoring = snapshotJson
        return snapshotJson
    }

    /**
     * An overlay opened over the current screen, such as the media viewer. It gets an entry of its own with the same
     * [snapshotJson] and [path], so the browser's Back calls [onBrowserBack] to close it instead of leaving the screen.
     * Only a user action opens an overlay, so the browser keeps the entry.
     */
    fun openOverlay(
        snapshotJson: String,
        path: String,
        onBrowserBack: () -> Unit,
    ) {
        index += 1
        truncateAfter(index - 1)
        store(index, snapshotJson)
        location.push(path, index, snapshotJson)
        overlayIndex = index
        closeOverlay = onBrowserBack
    }

    /**
     * The overlay closed in the app, such as with its close button. Its entry goes, as Back would remove it, so the
     * next Back leaves the screen. After the browser's Back closed it, there is nothing left to do.
     */
    fun onOverlayClosed() {
        if (closeOverlay == null) return
        closeOverlay = null
        if (index == overlayIndex) {
            index -= 1
            location.back()
        }
        overlayIndex = -1
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

/**
 * Keeps the browser address and history in step with navigation, where a browser exists; see [HistoryMirror]. Returns
 * the history overlays use for their own entries, or null without a browser.
 */
@Composable
internal fun BrowserHistorySync(
    state: NavigationState,
    navigator: Navigator,
): OverlayHistory? {
    val location = browserLocation ?: return null
    val currentNavigator = rememberUpdatedState(navigator)
    val mirror = remember(location) { HistoryMirror(location) }
    val snapshot = { encodeNavigationSnapshot(state.topLevelRoute, state.backStacks.keys, state.backStacks) }
    val path = { (state.backStacks[state.topLevelRoute]?.lastOrNull() ?: state.startRoute).toWebPath() }
    val overlayHistory =
        remember(mirror, state) {
            OverlayHistory(
                open = { onBrowserBack -> mirror.openOverlay(snapshot(), path(), onBrowserBack) },
                close = mirror::onOverlayClosed,
            )
        }
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
        var shown: NavigationSnapshot? = null
        snapshotFlow { Triple(snapshot(), path(), state.toSnapshot()) }.collect { (snapshotJson, routePath, current) ->
            mirror.onNavigated(snapshotJson, routePath, replaced = isReplacement(shown, current))
            shown = current
        }
    }
    return overlayHistory
}

/** Browser history entries for overlays over the current screen; see [HistoryMirror.openOverlay]. */
internal class OverlayHistory(
    val open: (onBrowserBack: () -> Unit) -> Unit,
    val close: () -> Unit,
)

/** The browser history for overlays, or null where there is no browser. */
internal val LocalOverlayHistory = staticCompositionLocalOf<OverlayHistory?> { null }

/**
 * While this is in the composition, the overlay calling it has its own browser history entry: the browser's Back
 * calls [onBack], which should close the overlay, and leaving the composition removes the entry. It does nothing
 * where there is no browser, where the platform's Back reaches the overlay's own back handler.
 */
@Composable
internal fun BrowserBackEntry(onBack: () -> Unit) {
    val history = LocalOverlayHistory.current ?: return
    val currentOnBack = rememberUpdatedState(onBack)
    DisposableEffect(history) {
        history.open { currentOnBack.value() }
        onDispose { history.close() }
    }
}

private fun NavigationState.toSnapshot() =
    NavigationSnapshot(NavigationSnapshot.VERSION, topLevelRoute, backStacks.values.map { it.toList() })

/**
 * Whether [after] differs from [before] only in the current screen of the same tab, as [Navigator.replace] leaves
 * it. A push or pop changes the stack's depth, and a tab switch the selected tab, so neither counts.
 */
internal fun isReplacement(
    before: NavigationSnapshot?,
    after: NavigationSnapshot,
): Boolean {
    before ?: return false
    if (before.topLevelRoute != after.topLevelRoute || before.backStacks.size != after.backStacks.size) return false
    val tab = after.topLevelRoute
    val old = before.backStack(tab) ?: return false
    val new = after.backStack(tab) ?: return false
    val othersEqual =
        before.backStacks.filter { it.first() != tab } == after.backStacks.filter { it.first() != tab }
    return othersEqual &&
        old.size == new.size &&
        old.size > 1 &&
        old.dropLast(1) == new.dropLast(1) &&
        old.last() != new.last()
}
