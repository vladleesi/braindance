// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** How long shared caches such as Cloudflare may keep a route's public responses. */
internal sealed interface EdgeLifetime {
    data class Fixed(
        val duration: Duration,
    ) : EdgeLifetime

    /** Until the current Popular Right Now feed window ends, so its pages expire together. */
    data object FeedWindow : EdgeLifetime

    /** Never publicly cached; the route does not attest to a public body. */
    data object None : EdgeLifetime
}

/**
 * Every route the API serves, with the query keys it accepts and its shared-cache lifetime. Adding a route here
 * registers it for dispatch, query validation, cache headers, and timing logs; the dispatcher's exhaustive `when` will
 * not compile until it has a handler. A route with an ID is registered under its [template].
 */
internal enum class ApiRoute(
    val template: String,
    val queryKeys: Set<String>,
    val edgeLifetime: EdgeLifetime,
) {
    HEALTH("/health", emptySet(), EdgeLifetime.None),
    HEALTHZ("/healthz", emptySet(), EdgeLifetime.None),

    // IGDB game data and rankings change slowly.
    GAME_DETAILS("/v1/games/details", setOf("id"), EdgeLifetime.Fixed(1.days)),
    ANTICIPATED("/v1/games/anticipated", setOf("pageSize"), EdgeLifetime.Fixed(6.hours)),
    RECOMMENDATIONS("/v1/games/recommendations", setOf("id", "limit"), EdgeLifetime.Fixed(1.days)),
    TRENDING("/v1/games/trending", setOf("page", "pageSize"), EdgeLifetime.FeedWindow),

    // Giveaways expire within hours; proxied images never change.
    GIVEAWAYS("/v1/giveaways", emptySet(), EdgeLifetime.Fixed(1.hours)),
    GIVEAWAY_DETAILS("/v1/giveaways/{id}", emptySet(), EdgeLifetime.Fixed(1.hours)),
    GIVEAWAY_IMAGE("/v1/giveaways/image", setOf("url"), EdgeLifetime.Fixed(7.days)),
    ;

    companion object {
        private val byTemplate = entries.associateBy { it.template }

        /** The route registered under [template], a value returned by [routeOf]. */
        fun forTemplate(template: String?): ApiRoute? = template?.let(byTemplate::get)
    }
}

internal val RECOMMENDATIONS_PATH = ApiRoute.RECOMMENDATIONS.template
internal val TRENDING_PATH = ApiRoute.TRENDING.template
internal val GIVEAWAY_DETAILS_ROUTE = ApiRoute.GIVEAWAY_DETAILS.template

/** Query keys of every route, by template; cache eligibility rejects any other key. */
internal val routeQueryParameters: Map<String, Set<String>> = ApiRoute.entries.associate { it.template to it.queryKeys }

/** The IGDB game routes, whose validated query parameters become APICalypse input. */
internal val gameQueryParameters: Map<String, Set<String>> =
    listOf(ApiRoute.GAME_DETAILS, ApiRoute.ANTICIPATED).associate { it.template to it.queryKeys }

/** The registered route template for a request path, or null for unknown paths. */
internal fun routeOf(path: String): String? =
    when {
        validGiveawayId(path) -> GIVEAWAY_DETAILS_ROUTE
        // The template is a registry key, not a requestable path.
        path == GIVEAWAY_DETAILS_ROUTE -> null
        path in routeQueryParameters -> path
        else -> null
    }
