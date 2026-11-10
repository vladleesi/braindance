// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Bell
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.home_events_description
import dev.vladleesi.braindanceapp.resources.home_events_title
import dev.vladleesi.braindanceapp.resources.home_latest_news_description
import dev.vladleesi.braindanceapp.resources.home_latest_news_title
import dev.vladleesi.braindanceapp.resources.home_notifications_coming_soon
import dev.vladleesi.braindanceapp.resources.home_open_news
import dev.vladleesi.braindanceapp.resources.home_open_releases
import dev.vladleesi.braindanceapp.resources.home_recommendations_description
import dev.vladleesi.braindanceapp.resources.home_recommendations_title
import dev.vladleesi.braindanceapp.resources.home_screen_popular_latest_giveaways
import dev.vladleesi.braindanceapp.resources.home_screen_popular_most_anticipated
import dev.vladleesi.braindanceapp.resources.home_screen_popular_right_now
import dev.vladleesi.braindanceapp.resources.home_upcoming_releases_description
import dev.vladleesi.braindanceapp.resources.navigation_home
import dev.vladleesi.braindanceapp.resources.releases_upcoming_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawaysList
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardList
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith80Alpha
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import dev.vladleesi.braindanceapp.utils.rememberCachedLazyListState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    // Wide Web windows show the desktop layout of the same state; Android and iOS never enable it.
    val desktopLayout = desktopLayout()
    if (desktopLayout != null) {
        DesktopHomeScreen(layout = desktopLayout, viewModel = viewModel, modifier = modifier)
    } else {
        StandardHomeScreen(modifier = modifier, viewModel = viewModel)
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun StandardHomeScreen(
    modifier: Modifier,
    viewModel: HomeViewModel,
) {
    val navigator = LocalNavigator.current

    val trendingNow by viewModel.trendingNow.collectAsStateWithLifecycle()
    val mostAnticipated by viewModel.mostAnticipated.collectAsStateWithLifecycle()
    val giveaways by viewModel.giveaways.collectAsStateWithLifecycle()

    // Load the screen only on first navigation
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val pullState =
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = viewModel::refresh,
        )
    // A mouse wheel or trackpad overscroll has no release that ends the pull, so it would leave the indicator stuck
    // part-way down. Pull-to-refresh therefore follows the most recent pointer: it works for touch only.
    var isMousePointer by remember { mutableStateOf(false) }
    // Each Home item is a whole section, such as a carousel with its images, so composing one costs a frame.
    val listState = rememberCachedLazyListState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxSize()
                    .observePointerType { isMouse -> isMousePointer = isMouse }
                    .pullRefresh(pullState, enabled = !isMousePointer),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            item { HomeTopBar(onProfileClick = { navigator.navigate(ProfileRoute) }) }
            item {
                MiniGameCardList(
                    title = stringResource(Res.string.home_screen_popular_right_now),
                    state = trendingNow,
                    onRefresh = { viewModel.loadTrendingNow() },
                    onItemClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                )
            }
            item { HomePreviewCard(Res.string.home_recommendations_title, Res.string.home_recommendations_description) }
            item {
                MiniGameCardList(
                    title = stringResource(Res.string.home_screen_popular_most_anticipated),
                    state = mostAnticipated,
                    onRefresh = { viewModel.loadMostAnticipated() },
                    onItemClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                )
            }
            item {
                HomePreviewCard(
                    title = Res.string.releases_upcoming_title,
                    description = Res.string.home_upcoming_releases_description,
                    link = Res.string.home_open_releases,
                    onLinkClick = { navigator.navigate(BottomBarRoute.ReleasesRoute) },
                )
            }
            item {
                GiveawaysList(
                    title = stringResource(Res.string.home_screen_popular_latest_giveaways),
                    state = giveaways,
                    onRefresh = { viewModel.loadGiveaways() },
                    onItemClick = { id -> navigator.navigate(GiveawayDetailsRoute(id)) },
                )
            }
            item {
                HomePreviewCard(
                    title = Res.string.home_latest_news_title,
                    description = Res.string.home_latest_news_description,
                    link = Res.string.home_open_news,
                    onLinkClick = { navigator.navigate(BottomBarRoute.NewsRoute) },
                )
            }
            item { HomePreviewCard(Res.string.home_events_title, Res.string.home_events_description) }
            item { Spacer(Modifier.height(Dimens.large)) }
        }
        StatusBarOverlay(backgroundColor = overlayBlackWith80Alpha)
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullState,
            modifier =
                Modifier
                    .padding(top = Dimens.statusBarInsets)
                    .align(TopCenter),
        )
    }
}

/**
 * Reports whether each pointer event, including wheel scrolls, comes from a mouse or trackpad. It observes events
 * before its children without consuming them.
 */
private fun Modifier.observePointerType(onPointerType: (isMouse: Boolean) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                onPointerType(event.changes.any { change -> change.type == PointerType.Mouse })
            }
        }
    }

@Composable
private fun HomeTopBar(onProfileClick: () -> Unit) {
    // One list item, so the list spacing starts below the bar rather than between it and the status bar.
    Column {
        SpacerStatusBarInsets()
        TopLevelTopAppBar(
            title = stringResource(Res.string.navigation_home),
            onProfileClick = onProfileClick,
            actions = {
                ComingSoonAction(
                    imageVector = Bell,
                    label = stringResource(Res.string.home_notifications_coming_soon),
                )
            },
        )
    }
}

/** A planned Home section; [link], when given, opens the destination that will host it through [onLinkClick]. */
@Composable
private fun HomePreviewCard(
    title: StringResource,
    description: StringResource,
    link: StringResource? = null,
    onLinkClick: (() -> Unit)? = null,
) {
    ComingSoonCard(
        title = stringResource(title),
        description = stringResource(description),
        modifier = Modifier.padding(horizontal = Dimens.medium),
        action =
            if (link != null && onLinkClick != null) {
                {
                    TextButton(onClick = onLinkClick) {
                        Text(stringResource(link), color = white)
                    }
                }
            } else {
                null
            },
    )
}
