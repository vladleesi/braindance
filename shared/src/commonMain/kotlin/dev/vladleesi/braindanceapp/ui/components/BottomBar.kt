// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerNestedRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerTopLevelRoute
import dev.vladleesi.braindanceapp.navigation.toEntries
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white

@Composable
fun BottomBar() {
    val navigator = LocalNavigator.current
    BottomNavigation(
        backgroundColor = navBar,
        modifier = Modifier.navigationBarsPadding(),
        elevation = 0.dp,
    ) {
        BottomBarRoute.routes.forEach { route ->
            val isSelected = route == navigator.state.topLevelRoute
            BottomNavigationItem(
                icon = {
                    Icon(
                        imageVector = route.imageVector,
                        contentDescription = null,
                    )
                },
                selected = isSelected,
                selectedContentColor = white,
                unselectedContentColor = secondaryText,
                onClick = {
                    // Handle double click on search tab
                    if (isSelected && route is BottomBarRoute.SearchRoute) {
                        navigator.setResult(BottomBarRoute.SearchRoute.RESULT_KEY, true)
                    } else {
                        navigator.navigate(route)
                    }
                },
            )
        }
    }
}

@Composable
fun NavigationGraph(modifier: Modifier) {
    val navigator = LocalNavigator.current
    NavDisplay(
        entries =
            navigator.state.toEntries(
                entryProvider =
                    entryProvider {
                        // Bottom bar
                        registerTopLevelRoute<BottomBarRoute.HomeRoute>()
                        registerTopLevelRoute<BottomBarRoute.NewsRoute>()
                        registerTopLevelRoute<BottomBarRoute.SearchRoute>()
                        registerTopLevelRoute<BottomBarRoute.CollectionsRoute>()
                        registerTopLevelRoute<BottomBarRoute.ProfileRoute>()
                        // Nested routes
                        registerNestedRoute<GameDetailsRoute>()
                        registerNestedRoute<GiveawayDetailsRoute>()
                    },
            ),
        modifier = modifier,
        onBack = { navigator.goBack() },
    )
}
