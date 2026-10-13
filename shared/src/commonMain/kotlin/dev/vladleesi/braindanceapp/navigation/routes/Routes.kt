// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dev.vladleesi.braindanceapp.navigation.routes.RouteType.Nested
import dev.vladleesi.braindanceapp.navigation.routes.RouteType.Top
import kotlinx.serialization.Serializable

/**
 * Every Route subclass must be annotated with @Serializable.
 */
@Serializable
sealed interface Route : NavKey {
    @Composable
    fun Content(
        modifier: Modifier,
        paddingValues: PaddingValues,
    )
}

inline fun <reified T : Route> EntryProviderScope<in Route>.registerTopLevelRoute() {
    registerRoute<T>(RouteType.Top)
}

inline fun <reified T : Route> EntryProviderScope<in Route>.registerNestedRoute() {
    registerRoute<T>(RouteType.Nested)
}

inline fun <reified T : Route> EntryProviderScope<in Route>.registerRoute(routeType: RouteType) {
    entry<T>(
        metadata =
            NavDisplay.transitionSpec {
                when (routeType) {
                    RouteType.Top -> EnterTransition.None
                    RouteType.Nested ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        ) + fadeIn()
                } togetherWith ExitTransition.None
            } +
                NavDisplay.popTransitionSpec {
                    EnterTransition.None togetherWith
                        when (routeType) {
                            RouteType.Top -> ExitTransition.None
                            RouteType.Nested ->
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                ) + fadeOut()
                        }
                } +
                NavDisplay.predictivePopTransitionSpec {
                    EnterTransition.None togetherWith
                        when (routeType) {
                            RouteType.Top -> ExitTransition.None
                            RouteType.Nested ->
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                ) + fadeOut()
                        }
                },
    ) { entry ->
        entry.Content(Modifier, PaddingValues())
    }
}

/**
 * Defines the type of a navigation route.
 *
 * - [Top]: Top-level screen in the navigation graph (e.g., bottom bar destinations).
 * - [Nested]: Nested screen within a top-level section.
 */
enum class RouteType {
    Top,
    Nested,
}
