// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.navigation.AppLinkHandler
import dev.vladleesi.braindanceapp.navigation.AppLinkInbox
import dev.vladleesi.braindanceapp.navigation.BrowserHistorySync
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.LocalOverlayHistory
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.appLinkRoute
import dev.vladleesi.braindanceapp.navigation.rememberNavigationState
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.system.LocalDesktopLayoutEnabled
import dev.vladleesi.braindanceapp.ui.components.LocalOverlayHost
import dev.vladleesi.braindanceapp.ui.components.NavigationGraph
import dev.vladleesi.braindanceapp.ui.components.NavigationScaffold
import dev.vladleesi.braindanceapp.ui.components.OverlayHost
import dev.vladleesi.braindanceapp.ui.components.OverlayHostLayout
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.utils.ImageLoaderInitializer

/**
 * [desktopLayoutEnabled] lets wide windows use the desktop presentation; only the Web host enables it. Mobile hosts
 * pass the link to the Web app that launched them as [launchLink], and later links as they arrive through [appLinks].
 */
@Composable
fun BraindanceApp(
    modifier: Modifier = Modifier,
    desktopLayoutEnabled: Boolean = false,
    launchLink: String? = null,
    appLinks: AppLinkInbox? = null,
) {
    // Coil documents this as safe to call repeatedly; an image loader that already exists is never replaced.
    ImageLoaderInitializer.initialize()

    val navigationState =
        rememberNavigationState(
            startRoute = BottomBarRoute.HomeRoute,
            topLevelRoutes = BottomBarRoute.routes.toSet(),
            launchRoute = remember(launchLink) { appLinkRoute(launchLink) },
        )

    val navigator = remember { Navigator(navigationState) }
    val overlayHistory = BrowserHistorySync(navigationState, navigator)
    AppLinkHandler(appLinks, navigator)
    val overlayHost = remember { OverlayHost() }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalDesktopLayoutEnabled provides desktopLayoutEnabled,
        LocalOverlayHost provides overlayHost,
        LocalOverlayHistory provides overlayHistory,
    ) {
        BraindanceTheme {
            OverlayHostLayout(overlayHost) {
                NavigationScaffold(modifier = modifier) { contentModifier ->
                    NavigationGraph(modifier = contentModifier)
                }
            }
        }
    }
}
