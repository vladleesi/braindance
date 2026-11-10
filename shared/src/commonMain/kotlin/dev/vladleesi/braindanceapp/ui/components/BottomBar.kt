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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerNestedRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerTopLevelRoute
import dev.vladleesi.braindanceapp.navigation.toEntries
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_screen_version
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopProjectLinks
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopSidebar
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopSidebarWidth
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white
import org.jetbrains.compose.resources.stringResource

private val SelectedLineWidth = Dimens.compact
private val SelectedLineThickness = Dimens.micro
private val SelectedLineGap = Dimens.micro

@Composable
internal fun NavigationScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val useRail = isLargeWindow()
    // Only the Web host enables the desktop sidebar; elsewhere this is always false.
    val useSidebar = desktopLayout()?.hasSidebar == true
    val navigator = LocalNavigator.current
    // The scaffold and items share the width-only policy; defaults also consider height and posture.
    val navigationSuiteType =
        when {
            useSidebar -> NavigationSuiteType.None
            useRail -> NavigationSuiteType.WideNavigationRailCollapsed
            else -> NavigationSuiteType.ShortNavigationBarCompact
        }
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
            val selected = route == navigator.state.topLevelRoute
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
                        modifier = if (selected) Modifier.selectedLine() else Modifier,
                    )
                },
                selected = selected,
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
                // Keep every destination reachable in short landscape windows and with enlarged text. The scroll
                // container clips to its content, so the bottom padding keeps the last item's selected line inside it.
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = SelectedLineGap + SelectedLineThickness),
                    verticalArrangement = Arrangement.spacedBy(Dimens.small),
                ) {
                    items()
                }
            } else {
                items()
            }
        },
    ) {
        // Composed before the content so keyboard focus reaches the navigation first, and drawn above it so sliding
        // screen transitions pass under it. The conditional is its own group, so resizing across the sidebar width
        // keeps the navigation display and each screen's state.
        if (useSidebar) {
            NavigationSidebar(navigator = navigator, modifier = Modifier.zIndex(1f))
        }
        content(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                ).then(if (useSidebar) Modifier.padding(start = DesktopSidebarWidth) else Modifier),
        )
    }
}

/** The desktop sidebar for the current navigation state, with the project links and app version at the bottom. */
@Composable
private fun NavigationSidebar(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val currentStack = navigator.state.backStacks[navigator.state.topLevelRoute]
    DesktopSidebar(
        selectedRoute = navigator.state.topLevelRoute,
        isProfileSelected = currentStack?.lastOrNull() == ProfileRoute,
        onRouteClick = { route -> navigator.selectTab(route) },
        onProfileClick = { navigator.navigate(ProfileRoute) },
        modifier = modifier,
        footer = {
            DesktopProjectLinks(
                links = rememberProjectLinks(),
                versionLabel = stringResource(Res.string.profile_screen_version, BuildKonfig.APP_VERSION),
            )
        },
    )
}

/**
 * A short cyan line centered under the selected destination's label. It is drawn just below the label in the item's
 * bottom spacing, so the item's size and layout stay as they are.
 */
private fun Modifier.selectedLine(): Modifier =
    drawWithContent {
        drawContent()
        val halfWidth = SelectedLineWidth.toPx() / 2
        val y = size.height + SelectedLineGap.toPx() + SelectedLineThickness.toPx() / 2
        drawLine(
            color = neonCyan,
            start = Offset(center.x - halfWidth, y),
            end = Offset(center.x + halfWidth, y),
            strokeWidth = SelectedLineThickness.toPx(),
            cap = StrokeCap.Round,
        )
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
