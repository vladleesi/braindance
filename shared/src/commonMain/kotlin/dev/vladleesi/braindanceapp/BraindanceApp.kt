// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.navigation.BrowserHistorySync
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.rememberNavigationState
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.system.LocalDesktopLayoutEnabled
import dev.vladleesi.braindanceapp.ui.components.NavigationGraph
import dev.vladleesi.braindanceapp.ui.components.NavigationScaffold
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.utils.ImageLoaderInitializer

/** [desktopLayoutEnabled] lets wide windows use the desktop presentation; only the Web host enables it. */
@Composable
fun BraindanceApp(
    modifier: Modifier = Modifier,
    desktopLayoutEnabled: Boolean = false,
) {
    // Coil documents this as safe to call repeatedly; an image loader that already exists is never replaced.
    ImageLoaderInitializer.initialize()

    val navigationState =
        rememberNavigationState(
            startRoute = BottomBarRoute.HomeRoute,
            topLevelRoutes = BottomBarRoute.routes.toSet(),
        )

    val navigator = remember { Navigator(navigationState) }
    BrowserHistorySync(navigationState, navigator)

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalDesktopLayoutEnabled provides desktopLayoutEnabled,
    ) {
        BraindanceTheme {
            NavigationScaffold(modifier = modifier) { contentModifier ->
                NavigationGraph(modifier = contentModifier)
            }
        }
    }
}
