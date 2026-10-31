// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Stores navigation where platform saved state does not survive a restart, such as a browser page reload.
 * Platforms whose saved state already restores navigation provide `null`.
 */
internal interface NavigationPersistence {
    fun read(): String?

    fun write(snapshotJson: String)
}

internal expect val navigationPersistence: NavigationPersistence?

/** Every route must be registered here to be saved or restored. */
internal val navSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            // Bottom bar
            subclass(BottomBarRoute.HomeRoute::class)
            subclass(BottomBarRoute.NewsRoute::class)
            subclass(BottomBarRoute.SearchRoute::class)
            subclass(BottomBarRoute.CollectionsRoute::class)
            subclass(BottomBarRoute.ProfileRoute::class)

            // Other routes
            subclass(GameDetailsRoute::class)
            subclass(GiveawayDetailsRoute::class)
        }
    }

/** The selected tab and every tab's back stack; each stack starts with its tab route. */
@Serializable
internal data class NavigationSnapshot(
    val version: Int,
    val topLevelRoute: NavKey,
    val backStacks: List<List<NavKey>>,
) {
    fun backStack(topLevelRoute: NavKey): List<NavKey>? = backStacks.firstOrNull { it.first() == topLevelRoute }

    companion object {
        const val VERSION = 1
    }
}

private const val MAX_SNAPSHOT_LENGTH = 100_000

private val snapshotFormat = Json { serializersModule = navSerializersModule }

/** Stacks are written in [topLevelRoutes] order so equal navigation always produces identical text. */
internal fun encodeNavigationSnapshot(
    topLevelRoute: NavKey,
    topLevelRoutes: Collection<NavKey>,
    backStacks: Map<NavKey, List<NavKey>>,
): String =
    snapshotFormat.encodeToString(
        NavigationSnapshot(
            version = NavigationSnapshot.VERSION,
            topLevelRoute = topLevelRoute,
            backStacks = topLevelRoutes.mapNotNull { backStacks[it]?.toList()?.takeIf(List<NavKey>::isNotEmpty) },
        ),
    )

/**
 * Decodes [snapshotJson], the stored output of [encodeNavigationSnapshot], or returns null when nothing is stored.
 * Also returns null for oversized, unreadable, or structurally invalid data, such as snapshots written by another
 * app version with routes that no longer exist, so the app starts fresh instead of failing.
 */
internal fun decodeNavigationSnapshot(
    snapshotJson: String?,
    topLevelRoutes: Set<NavKey>,
): NavigationSnapshot? {
    if (snapshotJson.isNullOrEmpty() || snapshotJson.length > MAX_SNAPSHOT_LENGTH) return null
    // Malformed JSON and unknown route types fail decoding; decoding does not suspend, so nothing is canceled here.
    return runCatching { snapshotFormat.decodeFromString<NavigationSnapshot>(snapshotJson) }
        .getOrNull()
        ?.takeIf { it.isValid(topLevelRoutes) }
}

private fun NavigationSnapshot.isValid(topLevelRoutes: Set<NavKey>): Boolean {
    // Check emptiness first: later checks read each stack's first route.
    if (backStacks.any(List<NavKey>::isEmpty)) return false
    val tabs = backStacks.map { it.first() }
    return version == NavigationSnapshot.VERSION &&
        topLevelRoute in topLevelRoutes &&
        tabs.all { it in topLevelRoutes } &&
        tabs.distinct().size == tabs.size &&
        backStacks.all { stack -> stack.drop(1).none { it in topLevelRoutes } }
}
