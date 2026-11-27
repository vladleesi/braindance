// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routeLink
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.CompilationRoute
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerNestedRoute
import dev.vladleesi.braindanceapp.navigation.routes.registerTopLevelRoute
import dev.vladleesi.braindanceapp.navigation.toEntries
import dev.vladleesi.braindanceapp.system.DesktopLayout
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.system.layoutPreferences
import dev.vladleesi.braindanceapp.system.prefersReducedMotion
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopSidebar
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopSidebarCollapsedWidth
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopSidebarWidth
import dev.vladleesi.braindanceapp.ui.components.desktop.NavigationHeader
import dev.vladleesi.braindanceapp.ui.components.search.LocalSearchQuery
import dev.vladleesi.braindanceapp.ui.components.search.SearchQueryState
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
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
    // Only the Web host enables the desktop layout, and with it the sidebar. Elsewhere this is null.
    val desktop = desktopLayout()
    val useSidebar = desktop != null
    val sidebar = remember { SidebarState(collapsedByChoice = layoutPreferences?.isSidebarCollapsed == true) }
    val sidebarWidth = animatedSidebarWidth(sidebar, desktop)
    val navigator = LocalNavigator.current
    // The scaffold and items share the width-only policy; defaults also consider height and posture.
    val navigationSuiteType =
        when {
            useSidebar -> NavigationSuiteType.None
            useRail -> NavigationSuiteType.WideNavigationRailCollapsed
            else -> NavigationSuiteType.ShortNavigationBarCompact
        }
    val items: @Composable () -> Unit = { NavigationItems(navigator = navigator, type = navigationSuiteType) }
    // Discover's search, shared with the desktop header's field and kept across recreation.
    val search = rememberSaveable(saver = SearchQueryState.Saver) { SearchQueryState() }
    CompositionLocalProvider(LocalSearchQuery provides search) {
        NavigationSuiteScaffold(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(background)
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            navigationSuiteType = navigationSuiteType,
            navigationSuiteColors =
                NavigationSuiteDefaults.colors(
                    shortNavigationBarContainerColor = navBar,
                    shortNavigationBarContentColor = foreground,
                    wideNavigationRailColors =
                        WideNavigationRailDefaults.colors(containerColor = navBar, contentColor = foreground),
                ),
            // The modifier's canvas background already fills this area, including the display-cutout padding, and is
            // the one background every screen shows. A second opaque fill, here or in a screen, would only repaint
            // every screen pixel once more each frame. The bar, rail, sidebar, and desktop header paint the darker
            // chrome color themselves.
            containerColor = Color.Transparent,
            contentColor = foreground,
            navigationItems = {
                if (useRail) {
                    // Keep every destination reachable in short landscape windows and with enlarged text. The
                    // scroll container clips to its content, so the bottom padding keeps the last item's selected
                    // line inside it.
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
                NavigationSidebar(
                    navigator = navigator,
                    collapsed = sidebar.isCollapsed(desktop),
                    onToggleCollapsed = if (desktop.hasSidebar) sidebar::toggle else null,
                    width = sidebarWidth,
                    modifier = Modifier.zIndex(1f),
                )
            }
            NavigationContent(
                navigator = navigator,
                sidebarWidth = sidebarWidth,
                search = search,
                content = content,
            )
        }
    }
}

/**
 * The screens, under the header in the desktop layout. The screens stay in the same place in the composition whether
 * the header shows or not, so resizing across the desktop layout's width keeps each screen's state. [search] is the
 * one Discover and the header's field share.
 */
@Composable
private fun NavigationContent(
    navigator: Navigator,
    sidebarWidth: Dp,
    search: SearchQueryState,
    content: @Composable (Modifier) -> Unit,
) {
    // System bars and cutouts only, not the keyboard: the keyboard covers the navigation bar, and a screen with
    // a text field makes room for it in its own scrolling content. Padding every screen by the keyboard would
    // count the navigation bar twice and squeeze screens to their title.
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                ).padding(start = sidebarWidth),
    ) {
        if (desktopLayout() != null) {
            NavigationHeader(navigator = navigator, search = search)
        }
        content(Modifier.fillMaxWidth().weight(1f))
    }
}

/** The top-level destinations in the bottom bar or the rail, as [type] shows them. */
@Composable
private fun NavigationItems(
    navigator: Navigator,
    type: NavigationSuiteType,
) {
    val itemColors =
        ShortNavigationBarItemDefaults.colors(
            selectedIconColor = foreground,
            selectedTextColor = foreground,
            selectedIndicatorColor = secondary,
            unselectedIconColor = secondaryText,
            unselectedTextColor = secondaryText,
        )
    val labelStyle = MaterialTheme.typography.caption
    BottomBarRoute.routes.forEach { route ->
        val selected = route == navigator.state.topLevelRoute
        NavigationSuiteItem(
            navigationSuiteType = type,
            icon = { Icon(imageVector = route.imageVector, contentDescription = null) },
            label = {
                Text(
                    text = stringResource(route.label),
                    style = labelStyle,
                    color = LocalContentColor.current,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (selected) Modifier.selectedLine(accent) else Modifier,
                )
            },
            selected = selected,
            colors = itemColors,
            onClick = { navigator.selectTab(route) },
            modifier = Modifier.routeLink(route),
        )
    }
}

/**
 * Whether the desktop sidebar is collapsed: by choice, which is remembered across visits on the Web, or because the
 * expanded sidebar does not fit the desktop layout.
 */
@Stable
private class SidebarState(
    collapsedByChoice: Boolean,
) {
    var collapsedByChoice by mutableStateOf(collapsedByChoice)
        private set

    fun isCollapsed(layout: DesktopLayout?): Boolean = collapsedByChoice || layout?.hasSidebar != true

    fun toggle() {
        collapsedByChoice = !collapsedByChoice
        layoutPreferences?.isSidebarCollapsed = collapsedByChoice
    }
}

// Material's emphasized easing over a short duration, for a change people asked for and watch.
private const val SIDEBAR_ANIMATION_MILLIS = 250

/**
 * How far the sidebar insets the content: none outside the desktop layout. Collapsing and expanding it by choice
 * animates the width, unless the system asks for less motion. Resizing the window across the desktop layout's sizes
 * changes it at once, so the page follows the window's edge.
 */
@Composable
private fun animatedSidebarWidth(
    sidebar: SidebarState,
    layout: DesktopLayout?,
): Dp {
    val expanded by animateFloatAsState(
        targetValue = if (sidebar.collapsedByChoice) 0f else 1f,
        animationSpec =
            if (prefersReducedMotion()) snap() else tween(SIDEBAR_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "Sidebar width",
    )
    return when {
        layout == null -> 0.dp
        !layout.hasSidebar -> DesktopSidebarCollapsedWidth
        else -> lerp(DesktopSidebarCollapsedWidth, DesktopSidebarWidth, expanded)
    }
}

/**
 * The desktop sidebar for the current navigation state, [width] wide. [collapsed] and [onToggleCollapsed] work as in
 * [DesktopSidebar].
 */
@Composable
private fun NavigationSidebar(
    navigator: Navigator,
    collapsed: Boolean,
    onToggleCollapsed: (() -> Unit)?,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val currentStack = navigator.state.backStacks[navigator.state.topLevelRoute]
    DesktopSidebar(
        selectedRoute = navigator.state.topLevelRoute,
        isProfileSelected = currentStack?.lastOrNull() == ProfileRoute,
        onRouteClick = { route -> navigator.selectTab(route) },
        links = rememberProjectLinks(),
        modifier = modifier,
        collapsed = collapsed,
        onToggleCollapsed = onToggleCollapsed,
        width = width,
    )
}

/**
 * A short line in the accent ([color]) centered under the selected destination's label, in the navigation rail and the
 * collapsed desktop sidebar. It is drawn just below the label in the item's bottom spacing, so the item's size and
 * layout stay as they are.
 */
internal fun Modifier.selectedLine(color: Color): Modifier =
    drawWithContent {
        drawContent()
        val halfWidth = SelectedLineWidth.toPx() / 2
        val y = size.height + SelectedLineGap.toPx() + SelectedLineThickness.toPx() / 2
        drawLine(
            color = color,
            start = Offset(center.x - halfWidth, y),
            end = Offset(center.x + halfWidth, y),
            strokeWidth = SelectedLineThickness.toPx(),
            cap = StrokeCap.Round,
        )
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
                        registerNestedRoute<EventsRoute>()
                        registerNestedRoute<CompilationRoute>()
                    },
            ),
        modifier = modifier,
        onBack = { navigator.goBack() },
    )
}
