// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** The route registry is the single place a route is declared; these checks keep its entries complete. */
class ApiRouteTest {
    private val privateRoutes = setOf(ApiRoute.HEALTH, ApiRoute.HEALTHZ)

    @Test
    fun everyPublicRouteDeclaresAnEdgeLifetime() {
        for (route in ApiRoute.entries) {
            if (route in privateRoutes) {
                assertEquals(EdgeLifetime.None, route.edgeLifetime, route.name)
            } else {
                assertNotEquals(EdgeLifetime.None, route.edgeLifetime, route.name)
            }
        }
    }

    @Test
    fun templatesAreUniqueAndResolveToTheirRoute() {
        assertEquals(
            ApiRoute.entries.size,
            ApiRoute.entries
                .map { it.template }
                .toSet()
                .size,
        )
        for (route in ApiRoute.entries - ApiRoute.GIVEAWAY_DETAILS) {
            assertEquals(route, ApiRoute.forTemplate(routeOf(route.template)), route.name)
        }
        assertEquals(ApiRoute.GIVEAWAY_DETAILS, ApiRoute.forTemplate(routeOf("/v1/giveaways/42")))
    }

    @Test
    fun derivedRegistriesFollowTheRoutes() {
        assertEquals(ApiRoute.entries.map { it.template }.toSet(), routeQueryParameters.keys)
        assertEquals(setOf(ApiRoute.GAME_DETAILS.template, ApiRoute.ANTICIPATED.template), gameQueryParameters.keys)
    }
}
