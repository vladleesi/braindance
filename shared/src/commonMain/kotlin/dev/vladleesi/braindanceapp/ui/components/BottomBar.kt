// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerNestedRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerTopLevelRoute
import dev.vladleesi.braindanceapp.navigation.toEntries
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NavigationScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val useRail = isLargeWindow()
    val navigator = LocalNavigator.current
    // The scaffold and items share the width-only policy; defaults also consider height and posture.
    val navigationSuiteType =
        if (useRail) NavigationSuiteType.WideNavigationRailCollapsed else NavigationSuiteType.ShortNavigationBarCompact
    val itemColors =
        ShortNavigationBarItemDefaults.colors(
            selectedIconColor = white,
            selectedTextColor = white,
            selectedIndicatorColor = secondary,
            unselectedIconColor = secondaryText,
            unselectedTextColor = secondaryText,
        )
    val labelStyle = MaterialTheme.typography.caption
    val items: @Composable () -> Unit = {
        BottomBarRoute.routes.forEach { route ->
            NavigationSuiteItem(
                navigationSuiteType = navigationSuiteType,
                icon = { Icon(imageVector = route.imageVector, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(route.label),
                        style = labelStyle,
                        color = LocalContentColor.current,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = route == navigator.state.topLevelRoute,
                colors = itemColors,
                onClick = { navigator.selectTab(route) },
            )
        }
    }
    NavigationSuiteScaffold(
        modifier =
            modifier
                .fillMaxSize()
                .background(navBar)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        navigationSuiteType = navigationSuiteType,
        navigationSuiteColors =
            NavigationSuiteDefaults.colors(
                shortNavigationBarContainerColor = navBar,
                shortNavigationBarContentColor = white,
                wideNavigationRailColors =
                    WideNavigationRailDefaults.colors(containerColor = navBar, contentColor = white),
            ),
        // The modifier's navBar background already fills this area, including the display-cutout padding. A second
        // opaque container fill would only repaint every screen pixel once more each frame.
        containerColor = Color.Transparent,
        contentColor = white,
        navigationItems = {
            if (useRail) {
                // Keep every destination reachable in short landscape windows and with enlarged text.
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.small),
                ) {
                    items()
                }
            } else {
                items()
            }
        },
    ) {
        content(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                ),
        )
    }
}

private fun Navigator.selectTab(route: BottomBarRoute) {
    if (route == state.topLevelRoute && route is BottomBarRoute.DiscoverRoute) {
        setResult(BottomBarRoute.DiscoverRoute.RESULT_KEY, true)
    } else {
        navigate(route)
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
                        registerTopLevelRoute<BottomBarRoute.DiscoverRoute>()
                        registerTopLevelRoute<BottomBarRoute.ReleasesRoute>()
                        registerTopLevelRoute<BottomBarRoute.NewsRoute>()
                        registerTopLevelRoute<BottomBarRoute.LibraryRoute>()
                        // Nested routes
                        registerNestedRoute<ProfileRoute>()
                        registerNestedRoute<GameDetailsRoute>()
                        registerNestedRoute<GiveawayDetailsRoute>()
                    },
            ),
        modifier = modifier,
        onBack = { navigator.goBack() },
    )
}
