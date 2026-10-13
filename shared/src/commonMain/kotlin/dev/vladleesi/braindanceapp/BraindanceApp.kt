// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.rememberNavigationState
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.ui.components.BottomBar
import dev.vladleesi.braindanceapp.ui.components.NavigationGraph
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.utils.ImageLoaderInitializer

@Composable
fun BraindanceApp(modifier: Modifier = Modifier) {
    // TODO: Refactor
    val isInitialized = remember { mutableStateOf(false) }
    if (!isInitialized.value) {
        ImageLoaderInitializer.initialize()
        isInitialized.value = true
    }

    val navigationState =
        rememberNavigationState(
            startRoute = BottomBarRoute.HomeRoute,
            topLevelRoutes = BottomBarRoute.routes.toSet(),
        )

    val navigator = remember { Navigator(navigationState) }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        BraindanceTheme {
            Scaffold(
                bottomBar = { BottomBar() },
                modifier = modifier.fillMaxSize(),
            ) { paddingValues ->
                NavigationGraph(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .consumeWindowInsets(paddingValues)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Horizontal,
                                ),
                            ),
                )
            }
        }
    }
}
