// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation.routes

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
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

/** A screen open at most once across all tabs: opening it again moves it to the current tab (see [Navigator]). */
interface SingleInstanceRoute : Route

inline fun <reified T : Route> EntryProviderScope<in Route>.registerTopLevelRoute() {
    registerRoute<T>(RouteType.Top)
}

inline fun <reified T : Route> EntryProviderScope<in Route>.registerNestedRoute() {
    registerRoute<T>(RouteType.Nested)
}

inline fun <reified T : Route> EntryProviderScope<in Route>.registerRoute(routeType: RouteType) {
    entry<T>(
        metadata =
            NavDisplay.transitionSpec { routeTransform(routeType, forward = true) } +
                NavDisplay.popTransitionSpec { routeTransform(routeType, forward = false) } +
                NavDisplay.predictivePopTransitionSpec { routeTransform(routeType, forward = false) },
    ) { entry ->
        entry.Content(Modifier, PaddingValues())
    }
}

private const val OUTGOING_FADE_MILLIS = 90
private const val INCOMING_FADE_MILLIS = 210
private const val SLIDE_MILLIS = 300

// Nested screens move a tenth of the width, a short shift that keeps the direction visible without a full slide.
private const val SLIDE_WIDTH_DIVISOR = 10

/**
 * Material motion for route changes. Tabs fade through; nested screens also move along a shared horizontal axis,
 * forward to the left and back to the right. The outgoing screen fades out before the incoming one fades in, so
 * screens with transparent backgrounds never show on top of each other.
 */
@PublishedApi
internal fun routeTransform(
    routeType: RouteType,
    forward: Boolean,
): ContentTransform {
    val enter = fadeIn(tween(INCOMING_FADE_MILLIS, delayMillis = OUTGOING_FADE_MILLIS, easing = LinearOutSlowInEasing))
    val exit = fadeOut(tween(OUTGOING_FADE_MILLIS, easing = FastOutLinearInEasing))
    return when (routeType) {
        RouteType.Top -> enter togetherWith exit
        RouteType.Nested -> {
            val direction = if (forward) 1 else -1
            val slide = tween<IntOffset>(SLIDE_MILLIS, easing = FastOutSlowInEasing)
            (enter + slideInHorizontally(slide) { direction * it / SLIDE_WIDTH_DIVISOR }) togetherWith
                (exit + slideOutHorizontally(slide) { -direction * it / SLIDE_WIDTH_DIVISOR })
        }
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
