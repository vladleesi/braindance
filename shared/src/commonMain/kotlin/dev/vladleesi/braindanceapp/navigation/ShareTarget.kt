// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.routes.CompilationRoute
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute

/**
 * Content people can share. Each one links to its screen in the Web app, whose address the Android app also opens as
 * an App Link; [name] is how the share message refers to it.
 */
sealed interface ShareTarget {
    val name: String

    /** The screen the link opens. */
    val route: NavKey

    data class Game(
        val id: Int,
        override val name: String,
    ) : ShareTarget {
        override val route: NavKey get() = GameDetailsRoute(id)
    }

    /**
     * A giveaway links to its Braindance page rather than to the store: the page opens in the apps, shows the
     * platforms, instructions, and end date, and offers the claim link, so the shared address never depends on a third
     * party's tracking or redirect URL.
     */
    data class Giveaway(
        val id: Int,
        override val name: String,
    ) : ShareTarget {
        override val route: NavKey get() = GiveawayDetailsRoute(id)
    }

    data class Event(
        val id: Int,
        override val name: String,
    ) : ShareTarget {
        override val route: NavKey get() = EventsRoute(id)
    }

    /** A Home section's full list, such as Popular Right Now. */
    data class CompilationList(
        val compilation: Compilation,
        override val name: String,
    ) : ShareTarget {
        override val route: NavKey get() = CompilationRoute(compilation)
    }
}

/**
 * The canonical link to [this] content: the Web app's `https` origin and the route's address, never an API endpoint or
 * a third-party URL. Null when the link would not open the same content again, such as for an ID no screen accepts, so
 * a broken link is never shared.
 */
fun ShareTarget.shareUrl(): String? {
    val url = APP_LINK_ORIGIN + route.toWebPath()
    return url.takeIf { appLinkRoute(it) == route }
}
